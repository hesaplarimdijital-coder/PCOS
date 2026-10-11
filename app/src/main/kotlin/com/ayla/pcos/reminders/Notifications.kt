package com.ayla.pcos.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat

/**
 * Bildirim kanalları. Kullanıcı her birini sistem ayarlarından ayrı yönetir; ses kanal üzerinde
 * tanımlıdır (Android 17 arka plan ses kısıtlaması nedeniyle uygulama kendi ses çalmaz).
 */
object NotificationChannels {
    const val MEDS = "ayla_meds"
    const val WATER = "ayla_water"
    const val CYCLE = "ayla_cycle"
    const val PROGRESS = "ayla_progress"
    const val SLEEP = "ayla_sleep"

    fun create(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannel(MEDS, "İlaç ve takviye", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Doz hatırlatmaları; sessiz saatlerde de gelir"
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(WATER, "Su", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Gün içinde su hatırlatmaları"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CYCLE, "Döngü", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tahmini adet ve uzun döngü bilgilendirmeleri"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(PROGRESS, "İlerleme", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Haftalık özet ve kutlamalar"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(SLEEP, "Uyku", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Akşam rutini hatırlatması"
            },
        )
    }
}

/** Bildirim kimlikleri; aynı doz için aynı kimlik üretilir ki güncelleme ve iptal tutarlı olsun. */
object NotificationIds {
    const val CYCLE_ALERT = 1001
    const val WATER = 1002
    const val EVENING = 1003
    const val WEEKLY = 1004
    const val STOCK_BASE = 20_000

    fun dose(medicationId: Long, time: String): Int = 30_000 + ("$medicationId:$time".hashCode() and 0xFFFF)
    fun stock(medicationId: Long): Int = STOCK_BASE + (medicationId % 10_000).toInt()
}
