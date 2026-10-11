package com.ayla.pcos.reminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ayla.pcos.MainActivity
import com.ayla.pcos.R
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bildirimleri kurar ve gösterir. Metinler yargısız ve kısa; en fazla bir emoji.
 * "Gizli bildirim" açıkken kilit ekranında yalnızca genel bir metin görünür.
 */
@Singleton
class ReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun showDose(medication: MedicationEntity, date: LocalDate, time: LocalTime, p: UserPreferences) {
        val id = NotificationIds.dose(medication.id, time.toString())
        val title = context.getString(R.string.notif_dose_title, medication.name)
        val text = context.getString(R.string.notif_dose_text, medication.doseText, time.format(timeFormat))

        val takenIntent = actionIntent(ReminderActionReceiver.ACTION_TAKEN, medication.id, date, time, id)
        val snoozeIntent = actionIntent(ReminderActionReceiver.ACTION_SNOOZE, medication.id, date, time, id)

        val builder = base(NotificationChannels.MEDS, title, text, p, openTarget = "meds")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.action_taken), takenIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozeIntent)
        notify(id, builder)
    }

    fun showCycleAlert(title: String, text: String, p: UserPreferences) {
        val builder = base(NotificationChannels.CYCLE, title, text, p, openTarget = "today")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
        notify(NotificationIds.CYCLE_ALERT, builder)
    }

    fun showWater(cups: Int, goal: Int, p: UserPreferences) {
        val title = context.getString(R.string.notif_water_title)
        val text = context.getString(R.string.notif_water_text, cups, goal)
        val builder = base(NotificationChannels.WATER, title, text, p, openTarget = "plan")
            .setPriority(NotificationCompat.PRIORITY_LOW)
        notify(NotificationIds.WATER, builder)
    }

    fun showEvening(p: UserPreferences) {
        val builder = base(
            NotificationChannels.SLEEP,
            context.getString(R.string.notif_evening_title),
            context.getString(R.string.notif_evening_text),
            p,
            openTarget = "plan",
        ).setPriority(NotificationCompat.PRIORITY_LOW)
        notify(NotificationIds.EVENING, builder)
    }

    fun showStock(medication: MedicationEntity, daysLeft: Int, p: UserPreferences) {
        val builder = base(
            NotificationChannels.MEDS,
            context.getString(R.string.notif_stock_title, medication.name),
            context.getString(R.string.notif_stock_text, daysLeft),
            p,
            openTarget = "meds",
        ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
        notify(NotificationIds.stock(medication.id), builder)
    }

    fun cancel(id: Int) = manager.cancel(id)

    /** Kullanıcı uygulamanın bildirimlerini kapattıysa (ya da Android 13+ izni vermediyse) false. */
    fun notificationsEnabled(): Boolean = manager.areNotificationsEnabled()

    private fun base(channel: String, title: String, text: String, p: UserPreferences, openTarget: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(openTarget))
            .setOnlyAlertOnce(false)
        if (p.hideNotificationContent) {
            val public = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notif_private_title))
                .setContentText(context.getString(R.string.notif_private_text))
                .build()
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(public)
        }
        return builder
    }

    private fun openAppIntent(target: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN, target)
        }
        return PendingIntent.getActivity(
            context, target.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String, medicationId: Long, date: LocalDate, time: LocalTime, notificationId: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            data = android.net.Uri.parse("ayla://dose/$medicationId/$date/$time/$action")
            putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(ReminderScheduler.EXTRA_DATE, date.toString())
            putExtra(ReminderScheduler.EXTRA_TIME, time.toString())
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context, intent.dataString.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        if (!manager.areNotificationsEnabled()) return
        manager.notify(id, builder.build())
    }
}
