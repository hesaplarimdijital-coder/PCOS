package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.DoseEventDao
import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Bir güne planlanmış tek doz ve varsa kaydı. */
data class DoseItem(
    val medication: MedicationEntity,
    val date: LocalDate,
    val time: LocalTime,
    val event: DoseEventEntity?,
) {
    val status: DoseStatus get() = event?.status ?: DoseStatus.PENDING
}

/** Belirli bir aralıktaki uyum. */
data class Adherence(val taken: Int, val scheduled: Int) {
    val ratio: Float get() = if (scheduled == 0) 0f else taken.toFloat() / scheduled
}

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val doseDao: DoseEventDao,
    private val scheduler: ReminderScheduler,
) {
    val activeMedications: Flow<List<MedicationEntity>> = medicationDao.observeActive()

    /** Günün tüm dozları, saat sırasıyla. */
    fun observeDoses(date: LocalDate): Flow<List<DoseItem>> =
        combine(medicationDao.observeActive(), doseDao.observeForDate(date)) { meds, events ->
            meds.flatMap { med ->
                med.times.map { time ->
                    DoseItem(med, date, time, events.firstOrNull { it.medicationId == med.id && it.time == time })
                }
            }.sortedBy { it.time }
        }

    /** [from]–[to] aralığında planlanan ve alınan doz sayısı. */
    fun observeAdherence(from: LocalDate, to: LocalDate): Flow<Adherence> =
        combine(medicationDao.observeActive(), doseDao.observeRange(from, to)) { meds, events ->
            val activeIds = meds.map { it.id }.toSet()
            val scheduled = meds.sumOf { med ->
                val since = med.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
                val firstDay = maxOf(from, since)
                val activeDays = if (firstDay.isAfter(to)) 0 else (to.toEpochDay() - firstDay.toEpochDay() + 1).toInt()
                med.times.size * activeDays
            }
            val taken = events.count { it.status == DoseStatus.TAKEN && it.medicationId in activeIds }
            Adherence(taken = taken, scheduled = maxOf(scheduled, taken))
        }

    suspend fun get(id: Long): MedicationEntity? = medicationDao.get(id)

    suspend fun add(medication: MedicationEntity): Long {
        val id = medicationDao.insert(medication)
        scheduler.rescheduleAll()
        return id
    }

    suspend fun update(medication: MedicationEntity) {
        medicationDao.update(medication)
        scheduler.rescheduleAll()
    }

    suspend fun deactivate(id: Long) {
        medicationDao.deactivate(id)
        scheduler.rescheduleAll()
    }

    /** Bir ilacın [from]–[to] arasında en az bir dozunun alındığı günler (hap paketi görünümü için). */
    fun observeTakenDates(medicationId: Long, from: LocalDate, to: LocalDate): Flow<List<LocalDate>> =
        doseDao.observeRange(from, to).map { events ->
            events.filter { it.medicationId == medicationId && it.status == DoseStatus.TAKEN }.map { it.date }.distinct()
        }

    suspend fun status(medicationId: Long, date: LocalDate, time: LocalTime): DoseStatus =
        doseDao.find(medicationId, date, time)?.status ?: DoseStatus.PENDING

    /** Hatırlatma çaldığında çağrılır; bekleyen doz satırını garanti eder. */
    suspend fun ensurePending(medicationId: Long, date: LocalDate, time: LocalTime) {
        doseDao.insertIgnore(DoseEventEntity(medicationId = medicationId, date = date, time = time))
    }

    suspend fun markTaken(medicationId: Long, date: LocalDate, time: LocalTime, at: Instant = Instant.now()) {
        val existing = doseDao.find(medicationId, date, time)
        val wasTaken = existing?.status == DoseStatus.TAKEN
        doseDao.upsert(
            (existing ?: DoseEventEntity(medicationId = medicationId, date = date, time = time))
                .copy(status = DoseStatus.TAKEN, actedAt = at),
        )
        if (!wasTaken) adjustStock(medicationId, -1)
    }

    suspend fun markSkipped(medicationId: Long, date: LocalDate, time: LocalTime, at: Instant = Instant.now()) {
        val existing = doseDao.find(medicationId, date, time)
        if (existing?.status == DoseStatus.TAKEN) adjustStock(medicationId, +1)
        doseDao.upsert(
            (existing ?: DoseEventEntity(medicationId = medicationId, date = date, time = time))
                .copy(status = DoseStatus.SKIPPED, actedAt = at),
        )
    }

    /** "Aldım"ı geri alır. */
    suspend fun markPending(medicationId: Long, date: LocalDate, time: LocalTime) {
        val existing = doseDao.find(medicationId, date, time) ?: return
        if (existing.status == DoseStatus.TAKEN) adjustStock(medicationId, +1)
        doseDao.upsert(existing.copy(status = DoseStatus.PENDING, actedAt = null))
    }

    /** Günlük kontrolde çağrılır: dünden önceki bekleyen dozlar kaçırılmış sayılır. */
    suspend fun markOldPendingAsMissed(today: LocalDate) = doseDao.markPendingBeforeAsMissed(today)

    suspend fun setStock(id: Long, count: Int?) = medicationDao.setStock(id, count)

    private suspend fun adjustStock(medicationId: Long, delta: Int) {
        val med = medicationDao.get(medicationId) ?: return
        val stock = med.stockCount ?: return
        medicationDao.setStock(medicationId, (stock + delta).coerceAtLeast(0))
    }
}

/** Hızlı ekleme şablonları; saatler kullanıcı tarafından değiştirilebilir. */
object MedicationTemplates {
    data class Template(
        val key: String,
        val name: String,
        val doseText: String,
        val kind: MedicationKind,
        val form: MedicationForm,
        val times: List<LocalTime>,
        val withFood: Boolean?,
        val pillActiveDays: Int? = null,
        val pillPlaceboDays: Int? = null,
    )

    val INOFOLIC_COMBI = Template(
        key = "inofolic_combi",
        name = "Inofolic Combi",
        doseText = "1 kapsül",
        kind = MedicationKind.SUPPLEMENT,
        form = MedicationForm.CAPSULE,
        times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
        withFood = true,
    )

    val PILL_24_4 = Template(
        key = "pill_24_4",
        name = "Doğum kontrol hapı",
        doseText = "1 tablet",
        kind = MedicationKind.CONTRACEPTIVE,
        form = MedicationForm.TABLET,
        times = listOf(LocalTime.of(22, 0)),
        withFood = null,
        pillActiveDays = 24,
        pillPlaceboDays = 4,
    )

    val PILL_21_7 = PILL_24_4.copy(key = "pill_21_7", pillActiveDays = 21, pillPlaceboDays = 7)

    val METFORMIN = Template(
        key = "metformin",
        name = "Metformin",
        doseText = "1 tablet",
        kind = MedicationKind.PRESCRIPTION,
        form = MedicationForm.TABLET,
        times = listOf(LocalTime.of(8, 30), LocalTime.of(20, 30)),
        withFood = true,
    )

    val VITAMIN_D = Template(
        key = "vitamin_d",
        name = "D vitamini",
        doseText = "1 damla/kapsül",
        kind = MedicationKind.SUPPLEMENT,
        form = MedicationForm.DROPS,
        times = listOf(LocalTime.of(8, 30)),
        withFood = true,
    )

    val SPEARMINT_TEA = Template(
        key = "spearmint_tea",
        name = "Nane çayı",
        doseText = "1 fincan",
        kind = MedicationKind.HERBAL,
        form = MedicationForm.TEA,
        times = listOf(LocalTime.of(10, 0), LocalTime.of(16, 0)),
        withFood = null,
    )

    val all: List<Template> = listOf(INOFOLIC_COMBI, PILL_24_4, PILL_21_7, METFORMIN, VITAMIN_D, SPEARMINT_TEA)

    fun Template.toEntity(packStart: LocalDate? = null, stockCount: Int? = null): MedicationEntity = MedicationEntity(
        name = name,
        doseText = doseText,
        kind = kind,
        form = form,
        times = times,
        withFood = withFood,
        stockCount = stockCount,
        pillActiveDays = pillActiveDays,
        pillPlaceboDays = pillPlaceboDays,
        packStartDate = if (kind == MedicationKind.CONTRACEPTIVE) packStart else null,
    )
}
