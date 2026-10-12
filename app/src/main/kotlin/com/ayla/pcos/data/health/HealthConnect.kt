package com.ayla.pcos.data.health

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Instant
import java.time.ZoneOffset

/** Health Connect'in bu cihazdaki durumu (#11). */
enum class HealthConnectAvailability {
    /** Android 14+ sistem modülü ya da güncel Health Connect uygulaması var. */
    AVAILABLE,

    /** Android 9–13: Health Connect uygulaması yok ya da eski; Google Play'den kurulur. */
    UPDATE_REQUIRED,

    /** Desteklenmeyen cihaz; elle giriş her zaman açık. */
    UNAVAILABLE,
}

/** Health Connect'ten okunan bir uyku oturumu. Evreler, başlık, not ve kaynak uygulama alınmaz. */
data class ExternalSleepSession(val id: String, val start: Instant, val end: Instant, val endOffset: ZoneOffset?)

/** Health Connect erişimi; testlerde sahtesiyle değiştirilir. Yalnızca uyku okunur, hiçbir şey yazılmaz. */
interface HealthConnectSource {
    fun availability(): HealthConnectAvailability

    suspend fun hasSleepPermission(): Boolean

    /** [from]–[to] aralığıyla kesişen uyku oturumları; izin yoksa SecurityException fırlatabilir. */
    suspend fun readSleepSessions(from: Instant, to: Instant): List<ExternalSleepSession>

    suspend fun revokeAllPermissions()
}

/** Android 13 ve altındaki Health Connect uygulaması. */
const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

/** İstenen tek izin: uyku okuma (READ_SLEEP). Başka veri türü istenmez. */
val SLEEP_PERMISSIONS: Set<String> = setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))

/** Health Connect'i Google Play'de açar (kurulum ya da güncelleme); internet izni gerektirmez. */
fun openHealthConnectInPlayStore(context: Context) {
    val uri = "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .setPackage("com.android.vending")
        .putExtra("overlay", true)
        .putExtra("callerId", context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}

/** Health Connect ayarları (izin yönetimi); eylem adı Android sürümüne göre kütüphanede seçilir. */
fun openHealthConnectSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
