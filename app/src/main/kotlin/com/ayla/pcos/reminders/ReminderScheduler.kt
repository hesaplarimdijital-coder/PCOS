package com.ayla.pcos.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Alarmla tetiklenen hatırlatma türleri. */
enum class ReminderKind { DOSE, DAILY_CHECK, WATER, EVENING, WEEKLY }

/**
 * Tüm alarmların tek sahibi.
 *
 * Kurallar:
 * - Doz hatırlatmaları tam zamanlıdır: izin varsa `setExactAndAllowWhileIdle`, yoksa 10 dakikalık
 *   pencereli `setWindow` (Android 14+ yeni kurulumlarda tam zamanlı alarm izni kapalı gelir).
 * - Her hatırlatma yalnızca bir sonraki tetiklemesini kurar; tetiklenince alıcı sıradakini kurar.
 * - Kurulan doz anahtarları saklanır; [rescheduleAll] artık geçerli olmayanları iptal eder.
 * - Yeniden başlatma, saat/dilim değişimi ve izin değişiminde [rescheduleAll] çağrılır.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val prefs: UserPreferencesRepository,
    private val clock: Clock,
) {
    private val alarmManager: AlarmManager = requireNotNull(context.getSystemService<AlarmManager>())
    private val registry by lazy { context.getSharedPreferences(REGISTRY_PREFS, Context.MODE_PRIVATE) }

    /** Android 12+ tam zamanlı alarm izni; daha eski sürümlerde her zaman true. */
    fun exactAlarmsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun rescheduleAll() {
        val meds = medicationDao.getActive()
        val p = prefs.current()
        val today = LocalDate.now(clock)

        val newKeys = mutableSetOf<String>()
        meds.forEach { med ->
            med.times.forEach { time ->
                newKeys += doseKey(med.id, time)
                scheduleDose(med.id, time, earliestDate = today)
            }
        }
        val oldKeys = registry.getStringSet(KEY_DOSES, emptySet()).orEmpty()
        (oldKeys - newKeys).forEach { key ->
            val (medId, time) = parseDoseKey(key) ?: return@forEach
            cancel(doseIntent(medId, time, snoozed = false))
            cancel(doseIntent(medId, time, snoozed = true))
        }
        registry.edit { putStringSet(KEY_DOSES, newKeys) }

        scheduleDailyCheck(ReminderPolicy.effectiveDailyCheckTime(p), earliestDate = today)
        if (p.waterRemindersEnabled) {
            // Her açılışta ileri atmamak için kayıtlı alarm hâlâ geçerliyse dokunulmaz.
            val storedAt = registry.getLong(KEY_WATER_AT, 0L)
            if (ReminderPolicy.shouldRescheduleWater(storedAt, clock.instant(), p)) scheduleWater(p, fromNow = true)
        } else {
            cancel(simpleIntent(ReminderKind.WATER))
            registry.edit { remove(KEY_WATER_AT) }
        }
        if (p.eveningRoutineEnabled) {
            scheduleAt(simpleIntent(ReminderKind.EVENING), nextOccurrence(p.eveningRoutineTime, today))
        } else {
            cancel(simpleIntent(ReminderKind.EVENING))
        }
        if (p.progressNotificationsEnabled) scheduleWeekly() else cancel(simpleIntent(ReminderKind.WEEKLY))
    }

    /** Haftalık ilerleme özeti: bir sonraki Pazar 19:00. */
    fun scheduleWeekly() {
        scheduleAt(simpleIntent(ReminderKind.WEEKLY), ReminderPolicy.nextWeekly(ZonedDateTime.now(clock)))
    }

    /** [earliestDate] gününden başlayarak [time] saatinin bir sonraki geçerli anına doz alarmı kurar. */
    fun scheduleDose(medicationId: Long, time: LocalTime, earliestDate: LocalDate) {
        val at = nextOccurrence(time, earliestDate)
        val intent = doseIntent(medicationId, time, snoozed = false).apply {
            putExtra(EXTRA_DATE, at.toLocalDate().toString())
        }
        scheduleAt(intent, at)
    }

    /** Aynı doz için [minutes] dakika sonra tek seferlik hatırlatma. */
    fun scheduleSnooze(medicationId: Long, date: LocalDate, time: LocalTime, minutes: Long = SNOOZE_MINUTES) {
        val at = ZonedDateTime.now(clock).plusMinutes(minutes)
        val intent = doseIntent(medicationId, time, snoozed = true).apply {
            putExtra(EXTRA_DATE, date.toString())
        }
        scheduleAt(intent, at)
    }

    fun cancelSnooze(medicationId: Long, time: LocalTime) = cancel(doseIntent(medicationId, time, snoozed = true))

    fun scheduleDailyCheck(time: LocalTime, earliestDate: LocalDate) {
        scheduleAt(simpleIntent(ReminderKind.DAILY_CHECK), nextOccurrence(time, earliestDate))
    }

    /**
     * Su hatırlatması: aktif saatler içinde [UserPreferences.waterIntervalMinutes] aralıkla.
     * Aktif saat dışına düşerse bir sonraki günün başlangıcına kayar.
     */
    fun scheduleWater(p: UserPreferences, fromNow: Boolean) {
        val at = ReminderPolicy.nextWater(ZonedDateTime.now(clock), p, fromNow)
        scheduleAt(simpleIntent(ReminderKind.WATER), at)
        registry.edit { putLong(KEY_WATER_AT, at.toInstant().toEpochMilli()) }
    }

    fun scheduleEvening(time: LocalTime, earliestDate: LocalDate) {
        scheduleAt(simpleIntent(ReminderKind.EVENING), nextOccurrence(time, earliestDate))
    }

    // --- iç yardımcılar ---

    private fun nextOccurrence(time: LocalTime, earliestDate: LocalDate): ZonedDateTime =
        ReminderPolicy.nextOccurrence(time, earliestDate, ZonedDateTime.now(clock))

    private fun scheduleAt(intent: Intent, at: ZonedDateTime) {
        val millis = at.toInstant().toEpochMilli()
        // Alıcı gecikmeyi günlüğe yazabilsin diye planlanan an ve alarm türü niyete eklenir (#17).
        intent.putExtra(EXTRA_SCHEDULED_AT, millis)
        if (exactAlarmsAllowed()) {
            try {
                intent.putExtra(EXTRA_EXACT, true)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(intent))
                return
            } catch (_: SecurityException) {
                // izin tam bu anda geri alındıysa pencereli alarma düş
            }
        }
        intent.putExtra(EXTRA_EXACT, false)
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, millis, FALLBACK_WINDOW_MS, pendingIntent(intent))
    }

    private fun cancel(intent: Intent) {
        val pending = PendingIntent.getBroadcast(
            context, requestCode(intent), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(intent: Intent): PendingIntent = PendingIntent.getBroadcast(
        context, requestCode(intent), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Veri URI'si her hatırlatmayı benzersiz kılar; istek kodu da ondan türetilir. */
    private fun requestCode(intent: Intent): Int = intent.dataString.hashCode()

    private fun baseIntent(kind: ReminderKind, path: String): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            data = "ayla://reminder/${kind.name.lowercase()}/$path".toUri()
            putExtra(EXTRA_KIND, kind.name)
        }

    private fun simpleIntent(kind: ReminderKind): Intent = baseIntent(kind, "single")

    private fun doseIntent(medicationId: Long, time: LocalTime, snoozed: Boolean): Intent =
        baseIntent(ReminderKind.DOSE, "$medicationId/$time${if (snoozed) "/snooze" else ""}").apply {
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_TIME, time.toString())
            putExtra(EXTRA_SNOOZED, snoozed)
        }

    private fun doseKey(medicationId: Long, time: LocalTime) = "$medicationId|$time"

    private fun parseDoseKey(key: String): Pair<Long, LocalTime>? {
        val parts = key.split("|")
        if (parts.size != 2) return null
        val id = parts[0].toLongOrNull() ?: return null
        val time = runCatching { LocalTime.parse(parts[1]) }.getOrNull() ?: return null
        return id to time
    }

    companion object {
        const val ACTION_FIRE = "com.ayla.pcos.action.REMINDER_FIRE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_TIME = "time"
        const val EXTRA_DATE = "date"
        const val EXTRA_SNOOZED = "snoozed"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"
        const val EXTRA_EXACT = "exact"
        const val SNOOZE_MINUTES = 15L
        private const val FALLBACK_WINDOW_MS = 10L * 60L * 1000L
        private const val REGISTRY_PREFS = "ayla_reminder_registry"
        private const val KEY_DOSES = "dose_keys"
        private const val KEY_WATER_AT = "water_at"
    }
}
