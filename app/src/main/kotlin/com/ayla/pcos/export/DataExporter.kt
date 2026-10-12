package com.ayla.pcos.export

import android.content.Context
import android.net.Uri
import com.ayla.pcos.data.db.AylaDatabase
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Yedek dosyasının adı ve şu anki biçim sürümü. */
const val BACKUP_APP_NAME = "Ayla"
const val BACKUP_SCHEMA_VERSION = 2

/**
 * Dışa aktarılan dosyanın biçimi. Sürüm 2, sürüm 1'e yalnızca varsayılanı olan alanlar ekler
 * (stok eşiği, oluşturulma zamanı, Health Connect kimliği, tercihler); eski dosyalar aynen okunur.
 */
@Serializable
data class ExportBundle(
    val app: String = BACKUP_APP_NAME,
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: String,
    val periods: List<ExportPeriod> = emptyList(),
    val dailyLogs: List<ExportDailyLog> = emptyList(),
    val medications: List<ExportMedication> = emptyList(),
    val doses: List<ExportDose> = emptyList(),
    val hydration: List<ExportHydration> = emptyList(),
    val sleep: List<ExportSleep> = emptyList(),
    val preferences: ExportPreferences? = null,
)

@Serializable
data class ExportPeriod(val start: String, val end: String?, val excludedFromStats: Boolean)

@Serializable
data class ExportDailyLog(
    val date: String,
    val mood: Int?,
    val energy: Int?,
    val flow: Int?,
    val symptoms: List<String>,
    val note: String?,
    val weightKg: Double?,
    val waistCm: Double?,
)

@Serializable
data class ExportMedication(
    val id: Long,
    val name: String,
    val doseText: String,
    val kind: String,
    val form: String,
    val times: List<String>,
    val withFood: Boolean?,
    val active: Boolean,
    val stockCount: Int?,
    val pillActiveDays: Int?,
    val pillPlaceboDays: Int?,
    val packStartDate: String?,
    val stockWarnDays: Int = 5,
    val createdAt: String? = null,
)

@Serializable
data class ExportDose(val medicationId: Long, val date: String, val time: String, val status: String, val actedAt: String?)

@Serializable
data class ExportHydration(val date: String, val cups: Int)

@Serializable
data class ExportSleep(
    val date: String,
    val startAt: String,
    val endAt: String,
    val quality: Int?,
    val source: String,
    val externalId: String? = null,
)

/**
 * Geri yüklenebilen tercihler. Rızalar (KVKK) ve koruma ayarları (kilit, ekran görüntüsü, gizli bildirim)
 * bilerek dışarıda: bunlar her cihazda kullanıcının yeniden vermesi gereken kararlardır.
 */
@Serializable
data class ExportPreferences(
    val displayName: String = "",
    val goals: List<String> = emptyList(),
    val trackingSince: String? = null,
    val waterGoalCups: Int = 8,
    val cupMl: Int = 250,
    val waterRemindersEnabled: Boolean = true,
    val waterIntervalMinutes: Int = 120,
    val activeHoursStart: String = "09:00",
    val activeHoursEnd: String = "21:00",
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: String = "22:30",
    val quietHoursEnd: String = "08:00",
    val cycleNotificationsEnabled: Boolean = true,
    val progressNotificationsEnabled: Boolean = true,
    val eveningRoutineEnabled: Boolean = false,
    val eveningRoutineTime: String = "22:30",
    val dailyCheckTime: String = "09:00",
    val hidePrediction: Boolean = false,
    val showBodyMeasurements: Boolean = false,
)

/** Tüm verileri tek bir JSON dosyasına yazar (Depolama Erişim Çerçevesi ile kullanıcının seçtiği yere). */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AylaDatabase,
    private val json: Json,
    private val prefs: UserPreferencesRepository,
) {
    suspend fun buildBundle(): ExportBundle = withContext(Dispatchers.IO) {
        ExportBundle(
            exportedAt = Instant.now().toString(),
            periods = db.periodDao().getAll().map { ExportPeriod(it.startDate.toString(), it.endDate?.toString(), it.excludedFromStats) },
            dailyLogs = db.dailyLogDao().getAll().map {
                ExportDailyLog(it.date.toString(), it.mood, it.energy, it.flow, it.symptoms, it.note, it.weightKg, it.waistCm)
            },
            medications = db.medicationDao().getAll().map {
                ExportMedication(
                    it.id, it.name, it.doseText, it.kind.name, it.form.name, it.times.map(Any::toString), it.withFood,
                    it.isActive, it.stockCount, it.pillActiveDays, it.pillPlaceboDays, it.packStartDate?.toString(),
                    stockWarnDays = it.stockWarnDays,
                    createdAt = it.createdAt.toString(),
                )
            },
            doses = db.doseEventDao().getAll().map {
                ExportDose(it.medicationId, it.date.toString(), it.time.toString(), it.status.name, it.actedAt?.toString())
            },
            hydration = db.hydrationDao().getAll().map { ExportHydration(it.date.toString(), it.cups) },
            sleep = db.sleepDao().getAll().map {
                ExportSleep(it.date.toString(), it.startAt.toString(), it.endAt.toString(), it.quality, it.source.name, it.externalId)
            },
            preferences = prefs.current().toExport(),
        )
    }

    suspend fun writeTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = json.encodeToString(buildBundle())
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                ?: error("Dosya açılamadı")
        }
    }

    /**
     * Tüm tabloları boşaltır; geri alınamaz. Anahtar kaybında kenara ayrılan (artık okunamayan) eski
     * veritabanı dosyaları da silinir. Tercihler ayrıca sıfırlanır.
     */
    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        context.getDatabasePath(AylaDatabase.NAME).parentFile
            ?.listFiles { file -> file.name.startsWith(AylaDatabase.NAME + QUARANTINE_MARKER) }
            ?.forEach { it.delete() }
    }

    companion object {
        /** AppModule karantina adı: ayla.db.kilitli-<zaman>[-wal|-shm|-journal]. */
        const val QUARANTINE_MARKER = ".kilitli-"

        fun suggestedFileName(): String = "ayla-verilerim-${java.time.LocalDate.now()}.json"
    }
}

internal fun UserPreferences.toExport() = ExportPreferences(
    displayName = displayName,
    goals = goals.map { it.name }.sorted(),
    trackingSince = trackingSince?.toString(),
    waterGoalCups = waterGoalCups,
    cupMl = cupMl,
    waterRemindersEnabled = waterRemindersEnabled,
    waterIntervalMinutes = waterIntervalMinutes,
    activeHoursStart = activeHoursStart.toString(),
    activeHoursEnd = activeHoursEnd.toString(),
    quietHoursEnabled = quietHoursEnabled,
    quietHoursStart = quietHoursStart.toString(),
    quietHoursEnd = quietHoursEnd.toString(),
    cycleNotificationsEnabled = cycleNotificationsEnabled,
    progressNotificationsEnabled = progressNotificationsEnabled,
    eveningRoutineEnabled = eveningRoutineEnabled,
    eveningRoutineTime = eveningRoutineTime.toString(),
    dailyCheckTime = dailyCheckTime.toString(),
    hidePrediction = hidePrediction,
    showBodyMeasurements = showBodyMeasurements,
)
