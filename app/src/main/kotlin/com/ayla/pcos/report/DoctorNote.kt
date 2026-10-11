package com.ayla.pcos.report

import android.content.Context
import com.ayla.cycle.CycleOverview
import com.ayla.cycle.LongCycleAlert
import com.ayla.pcos.R
import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.Adherence
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.db.DoseEventDao
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.ui.log.symptomLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Notun kapsadığı geriye dönük gün sayısı (semptomlar ve doz uyumu). */
const val DOCTOR_NOTE_DAYS = 28L

/** [from]–[to] aralığında tek bir ilacın planlanan ve alınan doz sayısı (oluşturulma gününden itibaren). */
fun adherenceFor(med: MedicationEntity, events: List<DoseEventEntity>, from: LocalDate, to: LocalDate, zone: ZoneId): Adherence {
    val since = med.createdAt.atZone(zone).toLocalDate()
    val firstDay = maxOf(from, since)
    val days = if (firstDay.isAfter(to)) 0 else (to.toEpochDay() - firstDay.toEpochDay() + 1).toInt()
    val taken = events.count { it.medicationId == med.id && it.status == DoseStatus.TAKEN && !it.date.isBefore(from) && !it.date.isAfter(to) }
    val scheduled = med.times.size * days
    return Adherence(taken = taken, scheduled = maxOf(scheduled, taken))
}

/** En sık görülen semptomlar: (anahtar, gün sayısı), çoktan aza; eşitlikte alfabetik (belirleyici). */
fun topSymptoms(logs: List<DailyLogEntity>, limit: Int = 3): List<Pair<String, Int>> =
    logs.flatMap { it.symptoms.distinct() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { it.key to it.value }

/**
 * Doktor görüşmesine götürülecek düz metin özet. Kullanıcı paylaşım menüsüyle kendisi gönderir;
 * uygulama hiçbir yere iletmez. Tanı ya da yorum içermez, yalnızca kayıtları özetler.
 */
@Singleton
class DoctorNoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val medicationDao: MedicationDao,
    private val doseEventDao: DoseEventDao,
    private val ticker: DateTicker,
) {
    private val tr = Locale.forLanguageTag("tr")
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", tr)

    fun title(): String = context.getString(R.string.note_title)

    suspend fun build(): String {
        val today = ticker.now()
        val from = today.minusDays(DOCTOR_NOTE_DAYS - 1)
        val overview = cycleRepository.snapshot()
        val meds = medicationDao.getActive()
        val events = doseEventDao.getRange(from, today)
        val logs = dailyLogRepository.observeRange(from, today).first()
        return render(today, overview, meds, events, logs, from)
    }

    private fun render(
        today: LocalDate,
        overview: CycleOverview,
        meds: List<MedicationEntity>,
        events: List<DoseEventEntity>,
        logs: List<DailyLogEntity>,
        from: LocalDate,
    ): String = buildString {
        fun line(text: String) = append(text).append('\n')
        fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

        line(s(R.string.note_title))
        line(s(R.string.note_prepared, today.format(longDate)))
        append('\n')

        line(s(R.string.note_section_cycle).uppercase(tr))
        val last = overview.lastPeriodStart
        if (last == null) {
            line("- " + s(R.string.note_no_period))
        } else {
            line("- " + s(R.string.note_last_period, last.format(longDate), overview.currentCycleDay ?: 0))
            val stats = overview.stats
            if (stats.lengths.isNotEmpty()) {
                line("- " + s(R.string.note_cycle_lengths, stats.lengths.asReversed().take(6).joinToString(", ")))
                val mean = stats.mean
                if (mean != null) line("- " + s(R.string.note_cycle_stats, mean.roundToInt(), stats.shortest ?: 0, stats.longest ?: 0))
            }
            line("- " + s(R.string.note_periods_year, stats.periodsInTrailingYear))
            if (overview.flags.longCycle != LongCycleAlert.NONE) {
                line("- " + s(R.string.note_long_cycle, overview.currentCycleDay ?: 0))
            }
        }
        append('\n')

        line(s(R.string.note_section_meds).uppercase(tr))
        if (meds.isEmpty()) {
            line("- " + s(R.string.note_no_meds))
        } else {
            val zone = ZoneId.systemDefault()
            meds.forEach { med ->
                val times = med.times.joinToString(", ") { it.toString() }
                val base = "${med.name} · ${med.doseText} · $times"
                val active = med.pillActiveDays
                val regimen = if (med.kind == MedicationKind.CONTRACEPTIVE && active != null) {
                    " · " + s(R.string.note_pill_regimen, active, med.pillPlaceboDays ?: 0)
                } else {
                    ""
                }
                val a = adherenceFor(med, events, from, today, zone)
                val adherence = if (a.scheduled > 0) " · " + s(R.string.note_adherence, a.taken, a.scheduled) else ""
                line("- $base$regimen$adherence")
            }
        }
        append('\n')

        line(s(R.string.note_section_symptoms).uppercase(tr))
        val top = topSymptoms(logs)
        if (top.isEmpty()) {
            line("- " + s(R.string.note_no_symptoms))
        } else {
            line("- " + top.joinToString(", ") { (key, days) -> s(R.string.note_symptom_days, context.getString(symptomLabel(key)), days) })
        }
        line("- " + s(R.string.note_log_days, logs.size))
        append('\n')

        line(s(R.string.note_disclaimer))
    }
}
