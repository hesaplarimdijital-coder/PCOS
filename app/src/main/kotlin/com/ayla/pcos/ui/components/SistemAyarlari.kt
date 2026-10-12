package com.ayla.pcos.ui.components

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.ayla.pcos.R
import java.util.Locale

/** Uygulamanın sistem bildirim ayarları (Android 8+). */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** "Alarmlar ve hatırlatıcılar" izni (Android 12+); daha eski sürümlerde izin gerekmez. */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/**
 * Pil kısıtı durumu (#17).
 * - [UNRESTRICTED]: pil optimizasyonundan muaf ("Kısıtlanmamış").
 * - [OPTIMIZED]: varsayılan; tam zamanlı alarmlar Doze'da da çalar ama bazı üreticiler uygulamayı uyutur.
 * - [BACKGROUND_RESTRICTED]: kullanıcı ya da sistem arka planı kısıtlamış; Android bu durumda arka planda
 *   alarm çalıştırmaz, hatırlatmalar hiç gelmeyebilir.
 */
enum class BatteryStatus { UNRESTRICTED, OPTIMIZED, BACKGROUND_RESTRICTED }

fun batteryStatus(context: Context): BatteryStatus = when {
    context.getSystemService<ActivityManager>()?.isBackgroundRestricted == true -> BatteryStatus.BACKGROUND_RESTRICTED
    context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) != false -> BatteryStatus.UNRESTRICTED
    else -> BatteryStatus.OPTIMIZED
}

/**
 * Uygulama bilgileri ekranını açar; pil ayarı her sürümde ve üreticide buradan ("Pil" bölümü) bulunur.
 * Doğrudan muafiyet isteyen ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ek izin ve Play politikası
 * gerekçesi ister; hatırlatmalar tam zamanlı alarmla çalıştığı için Ayla bu izni istemez, seçimi kullanıcı
 * yapar. Ekran açılamazsa pil optimizasyonu listesi denenir.
 */
fun openAppBatterySettings(context: Context) {
    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(details) }.onFailure { runCatching { context.startActivity(list) } }
}

/**
 * Bazı üreticiler arka plandaki uygulamaları standart Android kurallarının ötesinde uyutur; üreticiye göre
 * kısa ek yönlendirme. Menü adları sürüme göre değiştiği için metinler bölüm adını tırnak içinde verir.
 */
@StringRes
fun oemBatteryNote(manufacturer: String): Int? = when (manufacturer.trim().lowercase(Locale.ROOT)) {
    "xiaomi", "redmi", "poco" -> R.string.battery_oem_xiaomi
    "samsung" -> R.string.battery_oem_samsung
    "huawei", "honor" -> R.string.battery_oem_huawei
    "oppo", "realme", "oneplus" -> R.string.battery_oem_oppo
    "vivo", "iqoo" -> R.string.battery_oem_vivo
    else -> null
}

@StringRes
fun BatteryStatus.labelRes(): Int = when (this) {
    BatteryStatus.UNRESTRICTED -> R.string.battery_status_unrestricted
    BatteryStatus.OPTIMIZED -> R.string.battery_status_optimized
    BatteryStatus.BACKGROUND_RESTRICTED -> R.string.battery_status_restricted
}
