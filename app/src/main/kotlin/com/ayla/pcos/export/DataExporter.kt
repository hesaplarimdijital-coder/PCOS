package com.ayla.pcos.export

import android.content.Context
import android.net.Uri
import com.ayla.pcos.data.db.AylaDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Dışa aktarılan dosyanın biçimi; sürüm alanı ileride içe aktarma için gerekir. */
@Serializable
data class ExportBundle(
    val app: String = "Ayla",
    val schemaVersion: Int = 1,
    val exportedAt: String,
    val periods: List<ExportPeriod>,
    val dailyLogs: List<ExportDailyLog>,
    val medications: List<ExportMedication>,
    val doses: List<ExportDose>,
    val hydration: List<ExportHydration>,
    val sleep: List<ExportSleep>,
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
)

@Serializable
data class ExportDose(val medicationId: Long, val date: String, val time: String, val status: String, val actedAt: String?)

@Serializable
data class ExportHydration(val date: String, val cups: Int)

@Serializable
data class ExportSleep(val date: String, val startAt: String, val endAt: String, val quality: Int?, val source: String)

/** Tüm verileri tek bir JSON dosyasına yazar (Depolama Erişim Çerçevesi ile kullanıcının seçtiği yere). */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AylaDatabase,
    private val json: Json,
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
                )
            },
            doses = db.doseEventDao().getAll().map {
                ExportDose(it.medicationId, it.date.toString(), it.time.toString(), it.status.name, it.actedAt?.toString())
            },
            hydration = db.hydrationDao().getAll().map { ExportHydration(it.date.toString(), it.cups) },
            sleep = db.sleepDao().getAll().map {
                ExportSleep(it.date.toString(), it.startAt.toString(), it.endAt.toString(), it.quality, it.source.name)
            },
        )
    }

    suspend fun writeTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val text = json.encodeToString(buildBundle())
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
                ?: error("Dosya açılamadı")
        }
    }

    /** Tüm tabloları boşaltır; geri alınamaz. Tercihler ayrıca sıfırlanır. */
    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }

    companion object {
        fun suggestedFileName(): String = "ayla-verilerim-${java.time.LocalDate.now()}.json"
    }
}
