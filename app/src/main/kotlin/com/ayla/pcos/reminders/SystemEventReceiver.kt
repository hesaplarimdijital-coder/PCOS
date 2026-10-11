package com.ayla.pcos.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Alarmlar yeniden başlatmada silinir; saat ya da saat dilimi değişince yanlış ana kayar;
 * tam zamanlı alarm izni verilince pencereli alarmlar tam zamanlıya çevrilmelidir.
 * Bu olaylarda tüm hatırlatmalar yeniden kurulur.
 */
@AndroidEntryPoint
class SystemEventReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return
        val pending = goAsync()
        scope.launch {
            try {
                scheduler.rescheduleAll()
            } catch (t: Throwable) {
                Log.w(TAG, "Alarmlar yeniden kurulamadı: $action", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SystemEventReceiver"
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31); eski sürümlerde hiç gelmez.
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
