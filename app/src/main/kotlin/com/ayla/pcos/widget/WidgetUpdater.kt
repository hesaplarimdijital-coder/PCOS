package com.ayla.pcos.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import java.time.LocalDate
import java.time.ZoneId

/**
 * Widget'ı tazeleme tetikleri (#14). Widget yoksa hiçbir şey yapmaz. Tazeleme, sağlayıcıya açık bir yayın
 * göndererek yapılır; sağlayıcı durumu şifreli veritabanından okuyup görünümü kurar.
 */
object WidgetUpdater {
    const val ACTION_REFRESH = "com.ayla.pcos.action.WIDGET_REFRESH"

    fun widgetIds(context: Context): IntArray = runCatching {
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, AylaWidgetProvider::class.java))
    }.getOrNull() ?: IntArray(0)

    fun hasWidgets(context: Context): Boolean = widgetIds(context).isNotEmpty()

    /** Doz işaretlendi, alarm çaldı, saat değişti ya da uygulamada veri değişti. */
    fun requestUpdate(context: Context) {
        if (!hasWidgets(context)) return
        context.sendBroadcast(refreshIntent(context))
    }

    private fun refreshIntent(context: Context): Intent =
        Intent(context, AylaWidgetProvider::class.java).setAction(ACTION_REFRESH)

    private fun midnightPendingIntent(context: Context, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(context, REQUEST_MIDNIGHT, refreshIntent(context), flags or PendingIntent.FLAG_IMMUTABLE)

    /**
     * Gün dönümünde döngü günü ve günün dozları değişir. Uyandırmayan RTC alarmı: telefon uyanınca çalar,
     * tam zamanlı alarm izni gerekmez, WorkManager kullanılmaz. Pencere açıkça verilir; `set()` Android 12
     * öncesinde kalan sürenin %75'i kadar (saatlerce) kayabilir.
     */
    fun scheduleMidnight(context: Context, zone: ZoneId = ZoneId.systemDefault()) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pending = midnightPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val nextMidnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(1).toInstant().toEpochMilli()
        alarmManager.setWindow(AlarmManager.RTC, nextMidnight, MIDNIGHT_WINDOW_MS, pending)
    }

    fun cancelMidnight(context: Context) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        midnightPendingIntent(context, PendingIntent.FLAG_NO_CREATE)?.let { alarmManager.cancel(it) }
    }

    private const val REQUEST_MIDNIGHT = 7_001
    private const val MIDNIGHT_WINDOW_MS = 15L * 60L * 1000L
}
