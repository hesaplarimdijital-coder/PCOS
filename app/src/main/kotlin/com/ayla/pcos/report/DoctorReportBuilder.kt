package com.ayla.pcos.report

import android.content.Context
import com.ayla.cycle.CycleFertility
import com.ayla.cycle.ExclusionReason
import com.ayla.cycle.FertilityNote
import com.ayla.cycle.FertilityOverview
import com.ayla.cycle.LongCycleAlert
import com.ayla.pcos.R
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.DoseEventDao
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.DoctorQuestionRepository
import com.ayla.pcos.data.repo.FertilityRepository
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.SkinRepository
import com.ayla.pcos.data.repo.SleepRepository
import com.ayla.pcos.ui.log.symptomLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Raporun kapsadığı hazır aralıklar; "özel" aralık doğrudan [ReportOptions] ile verilir. */
enum class ReportRange(val days: Long) { DAYS_28(28), DAYS_90(90), DAYS_180(180) }

/**
 * Rapor seçenekleri. [from]–[to] kapalı aralıktır; bölüm bayrakları kullanıcı seçimidir. Doğurganlık bölümü
 * seçilse bile hap modunda ya da kayıt yokken yazılmaz; kilo/bel yalnızca ayarlardaki "vücut ölçüleri" açıksa.
 */
data class ReportOptions(
    val from: LocalDate,
    val to: LocalDate,
    val includeCycle: Boolean = true,
    val includeLog: Boolean = true,
    val includeMeds: Boolean = true,
    val includeSleepWater: Boolean = true,
    val includeFertility: Boolean = true,
    val includeSkin: Boolean = true,
    val includeQuestions: Boolean = true,
) {
    init {
        require(!to.isBefore(from)) { "Aralık ters" }
    }

    val dayCount: Int get() = (ChronoUnit.DAYS.between(from, to) + 1).toInt()

    companion object {
        fun lastDays(range: ReportRange, today: LocalDate): ReportOptions =
            ReportOptions(from = today.minusDays(range.days - 1), to = today)
    }
}

/**
 * Doktor raporunun içerik modelini depolardan kurar (ADR-0009). Yalnızca kayıtları özetler: yorum, tanı, öneri ve
 * doz hesabı yoktur; eşik uyarıları (60./90. gün, 12 ayda < 4 adet) kayıt cümlesi olarak yazılır. Fotoğraf
 * girmez. Tüm metinler `strings.xml`'den gelir; sayı ve tarih biçimi Türkçe.
 */
@Singleton
class DoctorReportBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val medicationDao: MedicationDao,
    private val doseEventDao: DoseEventDao,
    private val sleepRepository: SleepRepository,
    private val hydrationRepository: HydrationRepository,
    private val fertilityRepository: FertilityRepository,
    private val skinRepository: SkinRepository,
    private val questionRepository: DoctorQuestionRepository,
    private val prefs: UserPreferencesRepository,
    private val ticker: DateTicker,
) {
    private val tr = Locale.forLanguageTag("tr")
    private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", tr)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", tr)

    private fun s(id: Int, vararg args: Any): String = context.getString(id, *args)
    private fun days(n: Int): String = s(R.string.report_days_value, n)
    private fun yesNo(value: Boolean): String = if (value) s(R.string.report_yes) else s(R.string.report_no)
    private fun celsius(value: Double): String = String.format(tr, "%.2f", value)
    private fun oneDecimal(value: Double): String = String.format(tr, "%.1f", value)

    /** Varsayılan dosya adı; kişisel veri içermez. */
    fun fileName(today: LocalDate = ticker.now()): String = s(R.string.report_file_name, today.toString())

    fun pageLabel(number: Int, total: Int): String = s(R.string.report_page_label, number, total)

    suspend fun build(options: ReportOptions): DoctorReport {
        val today = ticker.now()
        val preferences = prefs.current()
        val sections = mutableListOf<ReportSection>()
        if (options.includeCycle) sections += cycleSection(options, preferences.goals.contains(Goal.PILL))
        if (options.includeLog) sections += logSection(options, preferences.showBodyMeasurements)
        if (options.includeMeds) sections += medsSection(options, today)
        if (options.includeSleepWater) sleepWaterSection(options, preferences.cupMl, preferences.waterGoalCups)?.let { sections += it }
        if (options.includeFertility) fertilitySection(options, today, preferences.goals.contains(Goal.TTC))?.let { sections += it }
        if (options.includeSkin) skinSection(options)?.let { sections += it }
        if (options.includeQuestions) questionsSection()?.let { sections += it }
        return DoctorReport(
            title = s(R.string.report_title),
            subtitle = s(R.string.report_subtitle, options.from.format(longDate), options.to.format(longDate), today.format(longDate)),
            disclaimer = s(R.string.report_disclaimer),
            sections = sections,
            footer = s(R.string.report_footer),
        )
    }

    /** Raporun düz metin hâli (hızlı paylaşım). Grafik atlanır; tablolar " · " ile tek satıra iner. */
    suspend fun plainText(options: ReportOptions): String = build(options).toPlainText()

    // --- Bölümler ---

    private suspend fun cycleSection(options: ReportOptions, pillGoal: Boolean): ReportSection {
        val overview = cycleRepository.snapshot()
        val blocks = mutableListOf<ReportBlock>()
        val last = overview.lastPeriodStart
        if (last == null) {
            blocks += ReportBlock.Paragraph(s(R.string.report_cycle_no_period))
        } else {
            val rows = mutableListOf<Pair<String, String>>()
            val day = overview.currentCycleDay
            rows += s(R.string.report_cycle_last_period) to
                (if (day != null) "${last.format(longDate)} · ${s(R.string.report_cycle_day, day)}" else last.format(longDate))
            rows += s(R.string.report_cycle_periods_year) to overview.stats.periodsInTrailingYear.toString()
            val stats = overview.stats
            val mean = stats.mean
            val shortest = stats.shortest
            val longest = stats.longest
            if (mean != null && shortest != null && longest != null) {
                rows += s(R.string.report_cycle_stats) to s(R.string.report_cycle_stats_value, mean.roundToInt(), shortest, longest)
            }
            blocks += ReportBlock.KeyValues(rows)

            // Aralıkla kesişen döngüler: başlangıcı aralık sonundan önce, bitişi aralık başından sonra (ya da açık).
            val inRange = overview.cycles.filter { c ->
                !c.start.isAfter(options.to) && c.nextStart?.isAfter(options.from) != false
            }
            if (inRange.isNotEmpty()) {
                blocks += ReportBlock.Table(
                    header = listOf(s(R.string.report_cycle_col_start), s(R.string.report_cycle_col_length), s(R.string.report_cycle_col_note)),
                    rows = inRange.sortedByDescending { it.start }.map { c ->
                        val length = c.lengthDays
                        val lengthText = when {
                            length != null -> days(length)
                            c.start == last && day != null -> s(R.string.report_cycle_ongoing, day)
                            else -> s(R.string.report_none)
                        }
                        val note = when (c.exclusion) {
                            ExclusionReason.USER -> s(R.string.report_cycle_excluded)
                            ExclusionReason.IMPLAUSIBLY_SHORT -> s(R.string.report_cycle_short)
                            null -> ""
                        }
                        listOf(c.start.format(longDate), lengthText, note)
                    },
                    weights = listOf(2f, 1.4f, 2f),
                )
            }
            val records = mutableListOf<String>()
            if (overview.flags.longCycle != LongCycleAlert.NONE && day != null) records += s(R.string.report_cycle_long_record, day)
            if (overview.flags.fewPeriodsInTrailingYear) records += s(R.string.report_cycle_few_periods)
            if (records.isNotEmpty()) blocks += ReportBlock.Bullets(records)
        }
        val pill = medicationDao.getActive().firstOrNull { it.kind == MedicationKind.CONTRACEPTIVE }
        val onPill = pill != null || pillGoal
        if (onPill) {
            val active = pill?.pillActiveDays
            blocks += ReportBlock.Note(
                if (pill != null && active != null) s(R.string.report_cycle_on_pill, active, pill.pillPlaceboDays ?: 0) else s(R.string.report_cycle_on_pill_plain),
            )
        }
        return ReportSection(s(R.string.report_section_cycle), blocks)
    }

    private suspend fun logSection(options: ReportOptions, showBodyMeasurements: Boolean): ReportSection {
        val logs = dailyLogRepository.observeRange(options.from, options.to).first()
        val blocks = mutableListOf<ReportBlock>()
        val rows = mutableListOf<Pair<String, String>>()
        rows += s(R.string.report_log_days) to s(R.string.report_log_days_value, logs.size, options.dayCount)
        val flowDays = logs.count { (it.flow ?: 0) > 0 }
        if (flowDays > 0) rows += s(R.string.report_log_flow_days) to days(flowDays)
        val moods = logs.mapNotNull { it.mood }
        if (moods.size >= MIN_LOGS_FOR_AVERAGE) rows += s(R.string.report_log_mood) to oneDecimal(moods.average())
        val energies = logs.mapNotNull { it.energy }
        if (energies.size >= MIN_LOGS_FOR_AVERAGE) rows += s(R.string.report_log_energy) to oneDecimal(energies.average())
        if (showBodyMeasurements) {
            logs.lastOrNull { it.weightKg != null }?.let { log ->
                rows += s(R.string.report_log_weight) to s(R.string.report_log_weight_value, oneDecimal(log.weightKg ?: 0.0), log.date.format(longDate))
            }
            logs.lastOrNull { it.waistCm != null }?.let { log ->
                rows += s(R.string.report_log_waist) to s(R.string.report_log_waist_value, oneDecimal(log.waistCm ?: 0.0), log.date.format(longDate))
            }
        }
        blocks += ReportBlock.KeyValues(rows)
        val top = topSymptoms(logs, limit = TOP_SYMPTOMS)
        if (top.isEmpty()) {
            blocks += ReportBlock.Paragraph(s(R.string.report_log_no_symptoms))
        } else {
            blocks += ReportBlock.Table(
                header = listOf(s(R.string.report_log_col_symptom), s(R.string.report_log_col_days)),
                rows = top.map { (key, count) -> listOf(context.getString(symptomLabel(key)), days(count)) },
                weights = listOf(3f, 1f),
            )
        }
        return ReportSection(s(R.string.report_section_log), blocks)
    }

    private suspend fun medsSection(options: ReportOptions, today: LocalDate): ReportSection {
        val meds = medicationDao.getActive()
        if (meds.isEmpty()) return ReportSection(s(R.string.report_section_meds), listOf(ReportBlock.Paragraph(s(R.string.report_meds_none))))
        val events = doseEventDao.getRange(options.from, options.to)
        val zone = ZoneId.systemDefault()
        val to = minOf(options.to, today)
        val rows = meds.map { med ->
            val times = if (med.times.isEmpty()) s(R.string.report_meds_no_times) else med.times.joinToString(", ") { it.toString() }
            val dose = buildString {
                append(med.doseText)
                val active = med.pillActiveDays
                if (med.kind == MedicationKind.CONTRACEPTIVE && active != null) {
                    append(" · ").append(s(R.string.report_meds_pill_regimen, active, med.pillPlaceboDays ?: 0))
                }
            }
            val adherence = adherenceFor(med, events, options.from, to, zone)
            val adherenceText = if (adherence.scheduled > 0) {
                s(R.string.report_meds_adherence_value, adherence.taken, adherence.scheduled, (adherence.ratio * 100).roundToInt())
            } else {
                s(R.string.report_none)
            }
            listOf(med.name, dose, times, adherenceText)
        }
        return ReportSection(
            s(R.string.report_section_meds),
            listOf(
                ReportBlock.Table(
                    header = listOf(s(R.string.report_meds_col_name), s(R.string.report_meds_col_dose), s(R.string.report_meds_col_times), s(R.string.report_meds_col_adherence)),
                    rows = rows,
                    weights = listOf(2.2f, 2f, 1.6f, 1.8f),
                ),
                ReportBlock.Paragraph(s(R.string.report_meds_note), TextStyle.SMALL),
            ),
        )
    }

    private suspend fun sleepWaterSection(options: ReportOptions, cupMl: Int, waterGoal: Int): ReportSection? {
        val nights = sleepRepository.observeRange(options.from, options.to).first()
        val water = hydrationRepository.observeRange(options.from, options.to).first().filter { it.cups > 0 }
        if (nights.isEmpty() && water.isEmpty()) return null
        val rows = mutableListOf<Pair<String, String>>()
        if (nights.isNotEmpty()) {
            val averageMinutes = nights.map { Duration.between(it.startAt, it.endAt).toMinutes() }.average().roundToInt()
            rows += s(R.string.report_sleep_average, nights.size) to s(R.string.report_sleep_value, averageMinutes / 60, averageMinutes % 60)
        }
        if (water.isNotEmpty()) {
            rows += s(R.string.report_water_average, water.size) to
                s(R.string.report_water_value, oneDecimal(water.map { it.cups }.average()), cupMl, waterGoal)
        }
        return ReportSection(s(R.string.report_section_sleep_water), listOf(ReportBlock.KeyValues(rows)))
    }

    /**
     * Doğurganlık bölümü: hap modunda ([FertilityOverview.Disabled]) ya da hedefte "hamile kalmayı planlıyorum" yokken
     * ve aralıkta kayıt da yokken yazılmaz. Döngü başına kayıt özeti; en son yeterli ölçümlü döngünün grafiği.
     */
    private suspend fun fertilitySection(options: ReportOptions, today: LocalDate, ttcGoal: Boolean): ReportSection? {
        val overview = fertilityRepository.analyze(today) as? FertilityOverview.Enabled ?: return null
        val cycles = overview.cycles.filter { c ->
            !c.start.isAfter(options.to) && c.end?.isBefore(options.from) != true && c.entries.isNotEmpty()
        }
        if (cycles.isEmpty() && !ttcGoal) return null
        val blocks = mutableListOf<ReportBlock>()
        cycles.sortedByDescending { it.start }.forEach { cycle ->
            blocks += ReportBlock.Paragraph(s(R.string.report_fertility_cycle, cycle.start.format(longDate)), TextStyle.BOLD)
            blocks += ReportBlock.KeyValues(fertilityRows(cycle))
        }
        val chartCycle = cycles.lastOrNull { c -> c.readings.count { !it.excluded } >= MIN_CHART_POINTS }
        if (chartCycle != null) {
            blocks += ReportBlock.TemperatureChart(
                points = chartCycle.readings.map { r -> ChartPoint(day = cycleDay(chartCycle, r.date), celsius = r.celsius, excluded = r.excluded) },
                coverlineCelsius = chartCycle.coverlineCelsius,
                lhPositiveDays = chartCycle.lhPositiveDates.map { cycleDay(chartCycle, it) }.toSet(),
                ovulationDay = chartCycle.ovulation?.let { cycleDay(chartCycle, it.ovulationDate) },
                xLabel = s(R.string.report_fertility_chart_x),
                yLabel = s(R.string.report_fertility_chart_y),
            )
            blocks += ReportBlock.Paragraph(s(R.string.report_fertility_chart_caption, chartCycle.start.format(longDate)), TextStyle.SMALL)
        }
        blocks += ReportBlock.Note(s(R.string.report_fertility_disclaimer))
        return ReportSection(s(R.string.report_section_fertility), blocks)
    }

    private fun fertilityRows(cycle: CycleFertility): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        val valid = cycle.readings.count { !it.excluded }
        rows += s(R.string.report_fertility_readings) to s(R.string.report_fertility_readings_value, valid, cycle.readings.size - valid)
        val ovulation = cycle.ovulation
        rows += s(R.string.report_fertility_ovulation) to if (ovulation != null) {
            s(
                R.string.report_fertility_ovulation_value,
                ovulation.ovulationDate.format(longDate),
                ovulation.confirmedOn.format(shortDate),
                celsius(ovulation.coverlineCelsius),
            )
        } else {
            s(R.string.report_fertility_ovulation_none)
        }
        if (cycle.lhPositiveDates.isNotEmpty()) {
            rows += s(R.string.report_fertility_lh_positive) to cycle.lhPositiveDates.joinToString(", ") { it.format(shortDate) }
        }
        cycle.lutealDay?.let { rows += s(R.string.report_fertility_luteal) to it.toString() }
        val notes = cycle.notes.filter { it != FertilityNote.OVULATION_CONFIRMED }.map { context.getString(noteTitle(it)) }
        if (notes.isNotEmpty()) rows += s(R.string.report_fertility_notes) to notes.joinToString("; ")
        return rows
    }

    private fun cycleDay(cycle: CycleFertility, date: LocalDate): Int = (ChronoUnit.DAYS.between(cycle.start, date) + 1).toInt()

    private suspend fun skinSection(options: ReportOptions): ReportSection? {
        val profile = skinRepository.profile() ?: return null
        val rows = mutableListOf<Pair<String, String>>()
        profile.skinType?.let { rows += s(R.string.report_skin_type) to context.getString(skinTypeLabel(it)) }
        profile.acne?.let { rows += s(R.string.report_skin_acne) to context.getString(acneLabel(it)) }
        rows += s(R.string.report_skin_dark_spots) to yesNo(profile.darkSpots)
        rows += s(R.string.report_skin_acanthosis) to flagged(profile.acanthosis)
        rows += s(R.string.report_skin_hair_growth) to flagged(profile.unwantedHair)
        rows += s(R.string.report_skin_hair_thinning) to flagged(profile.hairThinning)
        if (profile.pregnantOrBreastfeeding) rows += s(R.string.report_skin_pregnant) to yesNo(true)
        val checks = skinRepository.routineRange(options.from, options.to)
        if (checks.isNotEmpty()) {
            val morning = checks.count { it.slot == RoutineSlot.MORNING && it.doneSteps.isNotEmpty() }
            val evening = checks.count { it.slot == RoutineSlot.EVENING && it.doneSteps.isNotEmpty() }
            rows += s(R.string.report_skin_routine) to s(R.string.report_skin_routine_value, morning, evening, options.dayCount)
        }
        return ReportSection(s(R.string.report_section_skin), listOf(ReportBlock.KeyValues(rows)))
    }

    /** "Evet" ise kullanıcının doktoruna söylemek istediği bir işarettir; rapor bunu yorumlamaz. */
    private fun flagged(value: Boolean): String = if (value) "${s(R.string.report_yes)} · ${s(R.string.report_skin_flag)}" else s(R.string.report_no)

    private suspend fun questionsSection(): ReportSection? {
        val questions = questionRepository.getAll()
        if (questions.isEmpty()) return null
        val open = questions.filter { it.askedAt == null }
        val blocks = mutableListOf<ReportBlock>()
        if (open.isNotEmpty()) blocks += ReportBlock.Bullets(open.map { it.text })
        val asked = questions.size - open.size
        if (asked > 0) blocks += ReportBlock.Paragraph(s(R.string.report_questions_asked, asked), TextStyle.SMALL)
        return ReportSection(s(R.string.report_section_questions), blocks)
    }

    companion object {
        /** Ortalama için en az bu kadar kayıt; daha azı tek günün etkisinde kalır. */
        const val MIN_LOGS_FOR_AVERAGE = 5
        const val TOP_SYMPTOMS = 5
        /** Grafik çizmek için gereken geçerli ölçüm sayısı. */
        const val MIN_CHART_POINTS = 3

        fun skinTypeLabel(type: SkinType): Int = when (type) {
            SkinType.DRY -> R.string.skin_type_dry
            SkinType.OILY -> R.string.skin_type_oily
            SkinType.COMBINATION -> R.string.skin_type_combination
            SkinType.NORMAL -> R.string.skin_type_normal
            SkinType.SENSITIVE -> R.string.skin_type_sensitive
        }

        fun acneLabel(acne: AcneSelfReport): Int = when (acne) {
            AcneSelfReport.NONE -> R.string.skin_acne_none
            AcneSelfReport.MILD -> R.string.skin_acne_mild
            AcneSelfReport.MODERATE -> R.string.skin_acne_moderate
            AcneSelfReport.SEVERE -> R.string.skin_acne_severe
        }

        fun noteTitle(note: FertilityNote): Int = when (note) {
            FertilityNote.OVULATION_CONFIRMED -> R.string.fertility_note_confirmed_title
            FertilityNote.TOO_FEW_READINGS -> R.string.fertility_note_too_few_title
            FertilityNote.MANY_DISTURBED -> R.string.fertility_note_many_disturbed_title
            FertilityNote.LH_POSITIVE_WITHOUT_RISE -> R.string.fertility_note_lh_without_rise_title
            FertilityNote.RISE_WITHOUT_LH_POSITIVE -> R.string.fertility_note_rise_without_lh_title
            FertilityNote.LONG_LUTEAL -> R.string.fertility_note_long_luteal_title
        }
    }
}

/**
 * Modelin düz metin hâli: başlık, ibare, bölümler. Tablolar başlık satırı + " · " ile birleşen satırlar; grafik
 * atlanır (açıklama paragrafı zaten vardır). Paylaşım menüsünde ve hızlı notta kullanılır.
 */
fun DoctorReport.toPlainText(): String = buildString {
    appendLine(title)
    if (subtitle.isNotBlank()) appendLine(subtitle)
    if (disclaimer.isNotBlank()) appendLine(disclaimer)
    sections.forEach { section ->
        appendLine()
        appendLine(section.title.uppercase(Locale.forLanguageTag("tr")))
        section.blocks.forEach { block ->
            when (block) {
                is ReportBlock.Paragraph -> appendLine(block.text)
                is ReportBlock.Bullets -> block.items.forEach { appendLine("- $it") }
                is ReportBlock.KeyValues -> block.rows.forEach { (k, v) -> appendLine("- $k: $v") }
                is ReportBlock.Table -> {
                    appendLine(block.header.joinToString(" · "))
                    block.rows.forEach { row -> appendLine("- " + row.filter { it.isNotBlank() }.joinToString(" · ")) }
                }
                is ReportBlock.Note -> appendLine(block.text)
                is ReportBlock.TemperatureChart, is ReportBlock.Spacer -> Unit
            }
        }
    }
    if (footer.isNotBlank()) {
        appendLine()
        appendLine(footer)
    }
}.trimEnd()
