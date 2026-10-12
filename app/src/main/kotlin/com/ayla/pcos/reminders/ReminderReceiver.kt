package com.ayla.pcos.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ayla.cycle.LongCycleAlert
import com.ayla.pcos.R
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Alarm tetiklendiğinde çalışır: bildirimi gösterir ve aynı hatırlatmanın bir sonraki
 * tetiklemesini kurar. İş veritabanına dokunduğu için [goAsync] ile kısa bir arka plan
 * penceresinde yürür.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var medicationDao: MedicationDao
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var cycleRepository: CycleRepository
    @Inject lateinit var hydrationRepository: HydrationRepository
    @Inject lateinit var dailyLogRepository: DailyLogRepository
    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject lateinit var clock: Clock
    @Inject lateinit var alarmLog: AlarmLog

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_FIRE) return
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() } ?: return
        val firedAt = clock.instant()
        val pending = goAsync()
        scope.launch {
            try {
                // Gecikme günlüğü (#17); yazılamazsa hatırlatma yine işlenir.
                val scheduledAt = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, -1L)
                if (scheduledAt > 0) {
                    runCatching {
                        alarmLog.record(
                            AlarmLogEntry(
                                kind = kind,
                                scheduledAt = Instant.ofEpochMilli(scheduledAt),
                                firedAt = firedAt,
                                exact = intent.getBooleanExtra(ReminderScheduler.EXTRA_EXACT, false),
                            ),
                        )
                    }
                }
                when (kind) {
                    ReminderKind.DOSE -> handleDose(context, intent)
                    ReminderKind.DAILY_CHECK -> handleDailyCheck(context)
                    ReminderKind.WATER -> handleWater()
                    ReminderKind.EVENING -> handleEvening()
                    ReminderKind.WEEKLY -> handleWeekly(context)
                }
                // Doz saati geldi ("bekliyor") ya da günlük kontrol çalıştı: widget tazelenir.
                WidgetUpdater.requestUpdate(context)
            } catch (t: Throwable) {
                Log.w(TAG, "Hatırlatma işlenemedi: $kind", t)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleDose(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(ReminderScheduler.EXTRA_MEDICATION_ID, -1L)
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val date = intent.getStringExtra(ReminderScheduler.EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now(clock)
        val snoozed = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZED, false)
        if (medicationId < 0 || time == null) return

        val medication = medicationDao.get(medicationId)
        if (medication == null || !medication.isActive) return
        val p = prefs.current()

        medicationRepository.ensurePending(medicationId, date, time)
        // Kullanıcı dozu erken işaretlemiş olabilir; o zaman bildirim gösterilmez.
        val status = medicationRepository.status(medicationId, date, time)
        if (status != DoseStatus.TAKEN && status != DoseStatus.SKIPPED) {
            notifier.showDose(medication, date, time, p)
        }

        // Düzenli alarm sıradaki günü kurar; erteleme alarmı kurmaz.
        if (!snoozed) {
            scheduler.markDoseDelivered(medicationId, time, date)
            scheduler.scheduleDose(medicationId, time, earliestDate = date.plusDays(1))
        }
    }

    private suspend fun handleDailyCheck(context: Context) {
        val today = LocalDate.now(clock)
        val p = prefs.current()
        medicationRepository.markOldPendingAsMissed(today)

        // Alarm gecikip sessiz saate sarktıysa uyarılar gösterilmez ve "gösterildi" sayılmaz; yarın tekrar denenir.
        val quietNow = ReminderPolicy.isQuiet(LocalTime.now(clock), p)

        val overview = cycleRepository.snapshot()
        val lastStart = overview.lastPeriodStart
        if (!quietNow && p.cycleNotificationsEnabled && lastStart != null) {
            val alert = overview.flags.longCycle
            val key = when (alert) {
                LongCycleAlert.NONE -> null
                LongCycleAlert.GENTLE -> "gentle:$lastStart"
                LongCycleAlert.SEE_DOCTOR -> "doctor:$lastStart"
            }
            if (key != null && key != p.lastLongCycleAlertKey) {
                val day = overview.currentCycleDay ?: 0
                val (title, text) = when (alert) {
                    LongCycleAlert.SEE_DOCTOR ->
                        context.getString(R.string.notif_cycle_90_title, day) to context.getString(R.string.notif_cycle_90_text)
                    else ->
                        context.getString(R.string.notif_cycle_60_title, day) to context.getString(R.string.notif_cycle_60_text)
                }
                notifier.showCycleAlert(title, text, p)
                prefs.update { it.copy(lastLongCycleAlertKey = key) }
            }
        }

        // Stok uyarısı: kalan gün sayısı eşiğin altındaysa
        if (!quietNow) {
            medicationDao.getActive().forEach { med ->
                val stock = med.stockCount ?: return@forEach
                val perDay = med.times.size.coerceAtLeast(1)
                val daysLeft = stock / perDay
                if (daysLeft <= med.stockWarnDays) notifier.showStock(med, daysLeft, p)
            }
        }

        scheduler.scheduleDailyCheck(ReminderPolicy.effectiveDailyCheckTime(p), earliestDate = today.plusDays(1))
    }

    private suspend fun handleWater() {
        val p = prefs.current()
        if (!p.waterRemindersEnabled) return
        val now = ZonedDateTime.now(clock).toLocalTime()
        val today = LocalDate.now(clock)
        val cups = hydrationRepository.cups(today)
        val allowed = ReminderPolicy.isActive(now, p) && !ReminderPolicy.isQuiet(now, p)
        if (allowed && cups < p.waterGoalCups) notifier.showWater(cups, p.waterGoalCups, p)
        scheduler.scheduleWater(p, fromNow = true)
    }

    /** Akşam rutini sessiz saatten muaftır: kullanıcının kendi seçtiği yatış işaretidir. */
    private suspend fun handleEvening() {
        val p = prefs.current()
        if (!p.eveningRoutineEnabled) return
        notifier.showEvening(p)
        scheduler.scheduleEvening(p.eveningRoutineTime, earliestDate = LocalDate.now(clock).plusDays(1))
    }

    /** Pazar akşamı: son 7 günün nazik özeti. Veri yoksa ya da sessiz saatteyse gönderilmez. */
    private suspend fun handleWeekly(context: Context) {
        val p = prefs.current()
        if (!p.progressNotificationsEnabled) return
        val today = LocalDate.now(clock)
        val from = today.minusDays(6)
        val adherence = medicationRepository.observeAdherence(from, today).first()
        val ozet = HaftalikOzet(
            logDays = dailyLogRepository.observeRange(from, today).first().size,
            dosesTaken = adherence.taken,
            dosesScheduled = adherence.scheduled,
            waterGoalDays = hydrationRepository.observeRange(from, today).first().count { it.cups >= p.waterGoalCups },
        )
        if (!ozet.isEmpty && !ReminderPolicy.isQuiet(LocalTime.now(clock), p)) {
            val parts = buildList {
                if (ozet.logDays > 0) add(context.getString(R.string.notif_weekly_logs, ozet.logDays))
                if (ozet.dosesScheduled > 0) add(context.getString(R.string.notif_weekly_doses, ozet.dosesTaken, ozet.dosesScheduled))
                if (ozet.waterGoalDays > 0) add(context.getString(R.string.notif_weekly_water, ozet.waterGoalDays))
            }
            if (parts.isNotEmpty()) notifier.showWeekly(context.getString(R.string.notif_weekly_text, parts.joinToString(" · ")), p)
        }
        scheduler.scheduleWeekly()
    }

    private companion object {
        const val TAG = "ReminderReceiver"
    }
}
