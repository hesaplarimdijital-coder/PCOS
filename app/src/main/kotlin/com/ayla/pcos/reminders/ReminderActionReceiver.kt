package com.ayla.pcos.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Bildirim eylemleri: "Aldım" dozu kaydeder, "Ertele" 15 dakika sonra yeniden hatırlatır. */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MedicationRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getLongExtra(ReminderScheduler.EXTRA_MEDICATION_ID, -1L)
        val date = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (medicationId < 0 || date == null || time == null) return

        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    ACTION_TAKEN -> {
                        repository.markTaken(medicationId, date, time)
                        scheduler.cancelSnooze(medicationId, time)
                    }
                    ACTION_SNOOZE -> scheduler.scheduleSnooze(medicationId, date, time)
                    ACTION_SKIP -> {
                        repository.markSkipped(medicationId, date, time)
                        scheduler.cancelSnooze(medicationId, time)
                    }
                }
                if (notificationId >= 0) notifier.cancel(notificationId)
                // Widget'tan ya da bildirimden işaretlendi: widget sıradaki doza geçsin.
                WidgetUpdater.requestUpdate(context)
            } catch (t: Throwable) {
                Log.w(TAG, "Bildirim eylemi işlenemedi: $action", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.ayla.pcos.action.DOSE_TAKEN"
        const val ACTION_SNOOZE = "com.ayla.pcos.action.DOSE_SNOOZE"
        const val ACTION_SKIP = "com.ayla.pcos.action.DOSE_SKIP"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "ReminderAction"
    }
}
