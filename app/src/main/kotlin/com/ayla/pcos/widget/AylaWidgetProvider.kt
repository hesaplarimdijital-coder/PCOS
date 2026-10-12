package com.ayla.pcos.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Sağlayıcı bir Hilt bileşeni değildir; bağımlılıklar uygulama bileşeninden alınır. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun stateSource(): WidgetStateSource

    fun widgetSync(): WidgetSync
}

/**
 * Ana ekran widget'ı (#14, ADR-0007): bugünün günü, sıradaki doz ve "Aldım", geniş boyutta su. Periyodik
 * güncelleme yok (`updatePeriodMillis = 0`); tazeleme doz eylemleri, alarmlar, saat değişimi, uygulama içi
 * değişiklikler ve gün dönümündeki uyandırmayan alarmla yapılır.
 */
class AylaWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdater.ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            refresh(context, manager, WidgetUpdater.widgetIds(context))
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        refresh(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        // Uygulama içi değişiklikleri izleyen toplayıcı ilk tazelemede (IO'da) başlatılır.
        WidgetUpdater.scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.cancelMidnight(context)
    }

    private fun entryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        scope.launch {
            try {
                val entryPoint = entryPoint(context)
                // Veritabanı açılamazsa (ör. anahtar sorunu) widget nötr içerikle kalır; içerik loglanmaz.
                val state = runCatching { entryPoint.stateSource().current() }.getOrDefault(WidgetState.Neutral)
                ids.forEach { id ->
                    manager.updateAppWidget(id, WidgetGorunumu.build(context, state, manager.getAppWidgetOptions(id)))
                }
                WidgetUpdater.scheduleMidnight(context)
                entryPoint.widgetSync().start()
            } catch (t: Throwable) {
                Log.w(TAG, "Widget tazelenemedi", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AylaWidget"
    }
}
