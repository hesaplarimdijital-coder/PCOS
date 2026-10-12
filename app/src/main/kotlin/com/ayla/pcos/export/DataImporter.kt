package com.ayla.pcos.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.AylaDatabase
import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.db.DoctorQuestionEntity
import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.HydrationEntity
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinRoutineCheckEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.db.SleepSource
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DoctorQuestionRepository
import com.ayla.pcos.data.repo.normalized
import com.ayla.pcos.reminders.ReminderScheduler
import com.ayla.pcos.ui.me.AyarSinirlari
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** İçe aktarma kipi. */
enum class ImportMode {
    /** Cihazdaki kayıtlar korunur; yedekte olup cihazda olmayanlar eklenir. */
    MERGE,

    /** Cihazdaki kayıtlar silinir, yerine yedektekiler yazılır. Rızalar ve koruma ayarları değişmez. */
    REPLACE,
}

/** Yedek kullanılamıyor; [reason] kullanıcıya gösterilecek açıklamayı seçer. */
class InvalidBackupException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** Ayla yedeği değil (başka bir JSON). */
        NOT_AYLA,

        /** Daha yeni bir Ayla sürümünün biçimi; uygulama güncellenmeli. */
        TOO_NEW,

        /** Okunamadı ya da alanları bozuk. */
        CORRUPT,
    }
}

/** Önizleme: yedekte ne var (yazmadan önce kullanıcıya gösterilir). Sürüm 3 alanları varsayılanlıdır. */
data class ImportSummary(
    val exportedAt: Instant?,
    val periods: Int,
    val dailyLogs: Int,
    val medications: Int,
    val doses: Int,
    val hydrationDays: Int,
    val sleepNights: Int,
    val fertilityDays: Int = 0,
    val hasSkinProfile: Boolean = false,
    val skinRoutineSlots: Int = 0,
    val doctorQuestions: Int = 0,
) {
    val isEmpty: Boolean
        get() = periods + dailyLogs + medications + doses + hydrationDays + sleepNights +
            fertilityDays + skinRoutineSlots + doctorQuestions == 0 && !hasSkinProfile
}

/** Yazılan kayıt sayısı ve cihazda zaten bulunduğu (ya da bağlanamadığı) için atlanan sayı. */
data class ImportResult(val added: Int, val skipped: Int)

/** Yedeğin tamamı ayrıştırılmış hâli; veritabanına dokunmadan önce her alan doğrulanır. */
internal data class ParsedBackup(
    val periods: List<PeriodEntity>,
    val dailyLogs: List<DailyLogEntity>,
    /** Yedekteki ilaç kimliği → kimliksiz varlık (eklenince yeni kimlik alır). */
    val medications: List<Pair<Long, MedicationEntity>>,
    /** [DoseEventEntity.medicationId] yedekteki kimliktir; yazarken yeniden eşlenir. */
    val doses: List<DoseEventEntity>,
    val hydration: List<HydrationEntity>,
    val sleep: List<SleepEntity>,
    val preferences: ExportPreferences?,
    val fertility: List<FertilityEntryEntity> = emptyList(),
    val skinProfile: SkinProfileEntity? = null,
    val skinRoutine: List<SkinRoutineCheckEntity> = emptyList(),
    val doctorQuestions: List<DoctorQuestionEntity> = emptyList(),
)

/**
 * [DataExporter]'ın ürettiği JSON'u geri okur.
 *
 * - Önce dosyanın tamamı doğrulanır; tek bir alan bozuksa hiçbir şey yazılmaz.
 * - Yazma tek işlemdedir (ya hepsi ya hiçbiri).
 * - Birleştirmede cihazdaki kayıt kazanır: aynı güne ait günlük ve doz değiştirilmez; su için büyük
 *   sayı alınır; açık kalmış adetin bitişi yedekten tamamlanır; aynı ad ve türdeki ilaç yeniden eklenmez,
 *   yedekteki dozlar mevcut ilaca bağlanır. Sürüm 3 kayıtları (doğurganlık günü, cilt profili, rutin
 *   dilimi, aynı metinli soru) cihazda varsa atlanır.
 * - Bilinmeyen enum adı (ör. daha yeni sürümde eklenmiş LH sonucu) taşıyan kayıt sessizce atlanır;
 *   dosyanın tamamı reddedilmez.
 */
@Singleton
class DataImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AylaDatabase,
    private val json: Json,
    private val prefs: UserPreferencesRepository,
    private val scheduler: ReminderScheduler,
) {
    /** Dosyayı okur ve doğrular; yazma yapmaz. */
    suspend fun read(uri: Uri): ExportBundle = withContext(Dispatchers.IO) {
        val text: String? = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT, e)
        }
        parse(text ?: throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT))
    }

    /** Metni yedek olarak çözer; Ayla'ya ait değilse, daha yeni sürümdense ya da bozuksa istisna fırlatır. */
    fun parse(text: String): ExportBundle {
        val element = try {
            json.parseToJsonElement(text)
        } catch (e: Exception) {
            throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT, e)
        }
        val obj = element as? JsonObject ?: throw InvalidBackupException(InvalidBackupException.Reason.NOT_AYLA)
        val app = runCatching { obj["app"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        if (app != BACKUP_APP_NAME) throw InvalidBackupException(InvalidBackupException.Reason.NOT_AYLA)
        val version = runCatching { obj["schemaVersion"]?.jsonPrimitive?.intOrNull }.getOrNull()
            ?: throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT)
        if (version > BACKUP_SCHEMA_VERSION) throw InvalidBackupException(InvalidBackupException.Reason.TOO_NEW)
        if (version < 1) throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT)
        val bundle = try {
            json.decodeFromJsonElement<ExportBundle>(element)
        } catch (e: Exception) {
            throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT, e)
        }
        convert(bundle) // alan biçimleri şimdi doğrulansın; önizlemeden sonra sürpriz olmasın
        return bundle
    }

    fun summarize(bundle: ExportBundle) = ImportSummary(
        exportedAt = runCatching { Instant.parse(bundle.exportedAt) }.getOrNull(),
        periods = bundle.periods.size,
        dailyLogs = bundle.dailyLogs.size,
        medications = bundle.medications.size,
        doses = bundle.doses.size,
        hydrationDays = bundle.hydration.size,
        sleepNights = bundle.sleep.size,
        fertilityDays = bundle.fertility.size,
        hasSkinProfile = bundle.skinProfile != null,
        skinRoutineSlots = bundle.skinRoutine.size,
        doctorQuestions = bundle.doctorQuestions.size,
    )

    /** Yedeği yazar, tercihleri günceller ve hatırlatmaları yeniden kurar. */
    suspend fun apply(bundle: ExportBundle, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        val data = convert(bundle)
        val result = db.withTransaction {
            if (mode == ImportMode.REPLACE) clearRecords()
            write(data)
        }
        updatePreferences(data, mode)
        runCatching { scheduler.rescheduleAll() }
        result
    }

    internal fun convert(b: ExportBundle): ParsedBackup = try {
        val zone = ZoneId.systemDefault()
        ParsedBackup(
            periods = b.periods.map {
                PeriodEntity(
                    startDate = LocalDate.parse(it.start),
                    endDate = it.end?.let(LocalDate::parse),
                    excludedFromStats = it.excludedFromStats,
                )
            },
            dailyLogs = b.dailyLogs.map {
                DailyLogEntity(
                    date = LocalDate.parse(it.date),
                    mood = it.mood?.takeIf { v -> v in 1..5 },
                    energy = it.energy?.takeIf { v -> v in 1..3 },
                    flow = it.flow?.takeIf { v -> v in 0..4 },
                    symptoms = it.symptoms.distinct(),
                    note = it.note,
                    weightKg = it.weightKg,
                    waistCm = it.waistCm,
                )
            },
            medications = b.medications.map { m ->
                // Sürüm 1'de oluşturulma zamanı yok: uyum hesabı ilk dozdan başlasın.
                val firstDose = b.doses.filter { it.medicationId == m.id }.minOfOrNull { LocalDate.parse(it.date) }
                m.id to MedicationEntity(
                    name = m.name.trim(),
                    doseText = m.doseText,
                    kind = enumOr(m.kind, MedicationKind.SUPPLEMENT),
                    form = enumOr(m.form, MedicationForm.OTHER),
                    times = m.times.map(LocalTime::parse).distinct().sorted(),
                    withFood = m.withFood,
                    isActive = m.active,
                    stockCount = m.stockCount?.coerceAtLeast(0),
                    stockWarnDays = m.stockWarnDays.coerceIn(1, 14),
                    pillActiveDays = m.pillActiveDays,
                    pillPlaceboDays = m.pillPlaceboDays,
                    packStartDate = m.packStartDate?.let(LocalDate::parse),
                    createdAt = m.createdAt?.let(Instant::parse)
                        ?: firstDose?.atStartOfDay(zone)?.toInstant()
                        ?: Instant.now(),
                )
            },
            doses = b.doses.map {
                DoseEventEntity(
                    medicationId = it.medicationId,
                    date = LocalDate.parse(it.date),
                    time = LocalTime.parse(it.time),
                    status = DoseStatus.valueOf(it.status),
                    actedAt = it.actedAt?.let(Instant::parse),
                )
            },
            hydration = b.hydration.map { HydrationEntity(date = LocalDate.parse(it.date), cups = it.cups.coerceAtLeast(0)) },
            sleep = b.sleep.map {
                SleepEntity(
                    date = LocalDate.parse(it.date),
                    startAt = Instant.parse(it.startAt),
                    endAt = Instant.parse(it.endAt),
                    quality = it.quality?.takeIf { v -> v in 1..3 },
                    source = enumOr(it.source, SleepSource.MANUAL),
                    externalId = it.externalId,
                )
            }.filter { it.endAt.isAfter(it.startAt) },
            preferences = b.preferences,
            fertility = b.fertility.mapNotNull { f ->
                // Bilinmeyen enum adı: kayıt atlanır (null alan ise "girilmedi" demektir, kayıt kalır).
                val lh = f.lh?.let { enumOrNull<LhResult>(it) ?: return@mapNotNull null }
                val mucus = f.mucus?.let { enumOrNull<CervicalMucus>(it) ?: return@mapNotNull null }
                FertilityEntryEntity(
                    date = LocalDate.parse(f.date),
                    bbtCelsius = f.bbtCelsius,
                    bbtTime = f.bbtTime?.let(LocalTime::parse),
                    bbtDisturbed = f.bbtDisturbed,
                    lhResult = lh,
                    mucus = mucus,
                ).normalized()
            }.distinctBy { it.date },
            skinProfile = b.skinProfile?.let profile@{ p ->
                val skinType = p.skinType?.let { enumOrNull<SkinType>(it) ?: return@profile null }
                val acne = p.acne?.let { enumOrNull<AcneSelfReport>(it) ?: return@profile null }
                SkinProfileEntity(
                    skinType = skinType,
                    acne = acne,
                    darkSpots = p.darkSpots,
                    acanthosis = p.acanthosis,
                    unwantedHair = p.unwantedHair,
                    hairThinning = p.hairThinning,
                    pregnantOrBreastfeeding = p.pregnantOrBreastfeeding,
                    morningReminder = p.morningReminder?.let(LocalTime::parse),
                    eveningReminder = p.eveningReminder?.let(LocalTime::parse),
                )
            },
            skinRoutine = b.skinRoutine.mapNotNull { r ->
                val slot = enumOrNull<RoutineSlot>(r.slot) ?: return@mapNotNull null
                val steps = r.doneSteps.filter { it.isNotBlank() && '|' !in it }.distinct()
                if (steps.isEmpty()) return@mapNotNull null
                SkinRoutineCheckEntity(date = LocalDate.parse(r.date), slot = slot, doneSteps = steps)
            }.distinctBy { it.date to it.slot },
            doctorQuestions = b.doctorQuestions.mapNotNull { q ->
                val text = q.text.trim().take(DoctorQuestionRepository.MAX_LENGTH)
                if (text.isEmpty()) return@mapNotNull null
                DoctorQuestionEntity(
                    text = text,
                    createdAt = Instant.parse(q.createdAt),
                    askedAt = q.askedAt?.let(Instant::parse),
                )
            },
        )
    } catch (e: InvalidBackupException) {
        throw e
    } catch (e: Exception) {
        throw InvalidBackupException(InvalidBackupException.Reason.CORRUPT, e)
    }

    private suspend fun clearRecords() {
        db.doseEventDao().deleteAll()
        db.medicationDao().deleteAll()
        db.periodDao().deleteAll()
        db.dailyLogDao().deleteAll()
        db.hydrationDao().deleteAll()
        db.sleepDao().deleteAll()
        db.fertilityEntryDao().deleteAll()
        db.skinProfileDao().deleteAll()
        db.skinRoutineCheckDao().deleteAll()
        db.doctorQuestionDao().deleteAll()
    }

    private suspend fun write(d: ParsedBackup): ImportResult {
        var added = 0
        var skipped = 0

        val periodDao = db.periodDao()
        d.periods.forEach { period ->
            val existing = periodDao.getByStart(period.startDate)
            if (existing == null) {
                periodDao.upsert(period)
                added++
            } else {
                if (existing.endDate == null && period.endDate != null) periodDao.setEnd(existing.id, period.endDate)
                skipped++
            }
        }

        val logDao = db.dailyLogDao()
        d.dailyLogs.forEach { log ->
            if (logDao.get(log.date) == null) {
                logDao.upsert(log)
                added++
            } else {
                skipped++
            }
        }

        val medDao = db.medicationDao()
        val known = medDao.getAll().toMutableList()
        val idMap = mutableMapOf<Long, Long>()
        d.medications.forEach { (oldId, med) ->
            val match = known.firstOrNull { it.kind == med.kind && it.name.normalizedName() == med.name.normalizedName() }
            if (match != null) {
                idMap[oldId] = match.id
                skipped++
            } else {
                val newId = medDao.insert(med)
                idMap[oldId] = newId
                known += med.copy(id = newId)
                added++
            }
        }

        val doseDao = db.doseEventDao()
        d.doses.forEach { dose ->
            val medicationId = idMap[dose.medicationId]
            if (medicationId == null || doseDao.insertIgnore(dose.copy(medicationId = medicationId)) == -1L) {
                skipped++
            } else {
                added++
            }
        }

        val waterDao = db.hydrationDao()
        d.hydration.forEach { day ->
            val existing = waterDao.get(day.date)
            when {
                existing == null -> {
                    waterDao.upsert(day)
                    added++
                }
                day.cups > existing.cups -> {
                    waterDao.upsert(existing.copy(cups = day.cups, updatedAt = Instant.now()))
                    skipped++
                }
                else -> skipped++
            }
        }

        val sleepDao = db.sleepDao()
        val existingSleep = sleepDao.getAll()
        val externalIds = existingSleep.mapNotNull { it.externalId }.toMutableSet()
        val spans = existingSleep.map { it.startAt to it.endAt }.toMutableSet()
        d.sleep.forEach { night ->
            val duplicate = (night.externalId != null && night.externalId in externalIds) || (night.startAt to night.endAt) in spans
            if (duplicate) {
                skipped++
            } else {
                sleepDao.insert(night)
                night.externalId?.let(externalIds::add)
                spans += night.startAt to night.endAt
                added++
            }
        }

        // Sürüm 3: aynı anahtar cihazda varsa atlanır (cihazdaki kayıt kazanır).
        val fertilityDao = db.fertilityEntryDao()
        d.fertility.forEach { entry ->
            if (fertilityDao.get(entry.date) == null) {
                fertilityDao.upsert(entry)
                added++
            } else {
                skipped++
            }
        }

        val profileDao = db.skinProfileDao()
        d.skinProfile?.let { profile ->
            if (profileDao.get() == null) {
                profileDao.upsert(profile)
                added++
            } else {
                skipped++
            }
        }

        val routineDao = db.skinRoutineCheckDao()
        d.skinRoutine.forEach { check ->
            if (routineDao.get(check.date, check.slot) == null) {
                routineDao.upsert(check)
                added++
            } else {
                skipped++
            }
        }

        val questionDao = db.doctorQuestionDao()
        val knownQuestions = questionDao.getAll().map { it.text.normalizedName() }.toMutableSet()
        d.doctorQuestions.forEach { question ->
            if (knownQuestions.add(question.text.normalizedName())) {
                questionDao.insert(question)
                added++
            } else {
                skipped++
            }
        }
        return ImportResult(added, skipped)
    }

    private suspend fun updatePreferences(d: ParsedBackup, mode: ImportMode) {
        val earliestPeriod = d.periods.minOfOrNull { it.startDate }
        val backupSince = d.preferences?.trackingSince?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        prefs.update { current ->
            when (mode) {
                // Üzerine yazınca cihazın eski başlangıç tarihi geçersizdir; yedeğinki ve en eski adet esas alınır.
                ImportMode.REPLACE -> {
                    val restored = d.preferences?.let { current.withRestored(it) } ?: current
                    restored.copy(trackingSince = listOfNotNull(backupSince, earliestPeriod).minOrNull())
                }
                ImportMode.MERGE -> current.copy(trackingSince = listOfNotNull(current.trackingSince, backupSince, earliestPeriod).minOrNull())
            }
        }
    }
}

/** Yedekteki tercihleri uygular; rızalar, koruma ayarları ve karşılama durumu cihazdaki gibi kalır. */
internal fun UserPreferences.withRestored(p: ExportPreferences): UserPreferences {
    fun time(text: String, fallback: LocalTime) = runCatching { LocalTime.parse(text) }.getOrDefault(fallback)
    return copy(
        displayName = p.displayName.trim().take(40),
        goals = p.goals.mapNotNull { name -> Goal.entries.firstOrNull { it.name == name } }.toSet(),
        waterGoalCups = p.waterGoalCups.coerceIn(AyarSinirlari.waterGoalRange),
        cupMl = p.cupMl.coerceIn(AyarSinirlari.cupMlRange),
        waterRemindersEnabled = p.waterRemindersEnabled,
        waterIntervalMinutes = p.waterIntervalMinutes.takeIf { it in AyarSinirlari.waterIntervals } ?: waterIntervalMinutes,
        activeHoursStart = time(p.activeHoursStart, activeHoursStart),
        activeHoursEnd = time(p.activeHoursEnd, activeHoursEnd),
        quietHoursEnabled = p.quietHoursEnabled,
        quietHoursStart = time(p.quietHoursStart, quietHoursStart),
        quietHoursEnd = time(p.quietHoursEnd, quietHoursEnd),
        cycleNotificationsEnabled = p.cycleNotificationsEnabled,
        progressNotificationsEnabled = p.progressNotificationsEnabled,
        eveningRoutineEnabled = p.eveningRoutineEnabled,
        eveningRoutineTime = time(p.eveningRoutineTime, eveningRoutineTime),
        dailyCheckTime = time(p.dailyCheckTime, dailyCheckTime),
        hidePrediction = p.hidePrediction,
        showBodyMeasurements = p.showBodyMeasurements,
    )
}

/**
 * Ad karşılaştırması için sadeleştirme: büyük/küçük harf, fazla boşluk ve Türkçe I/İ farkı yok sayılır.
 * Türkçe yerelle küçültmek "Inofolic" → "ınofolic" yapar ve "inofolic" ile eşleşmez; bu yüzden kök yerel
 * kullanılır, noktasız ı ve birleşik nokta (İ → i̇) tek bir i'ye indirilir.
 */
internal fun String.normalizedName(): String =
    trim().lowercase(Locale.ROOT).replace("\u0307", "").replace('ı', 'i').replace(Regex("\\s+"), " ")

private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: fallback

/** Varsayılanı olmayan enum alanları için: ad bilinmiyorsa null (çağıran kaydı atlar). */
private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
    enumValues<E>().firstOrNull { it.name == name }
