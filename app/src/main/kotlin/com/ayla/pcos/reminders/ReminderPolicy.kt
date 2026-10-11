package com.ayla.pcos.reminders

import com.ayla.pcos.data.prefs.UserPreferences
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Hatırlatma zamanlama kuralları; Android'e bağımlı değildir, JVM'de test edilir.
 *
 * - Sessiz saatler gece yarısını geçebilir (22:30–08:00). Sessizken yalnızca doz hatırlatması ve
 *   kullanıcının kendi seçtiği akşam rutini gelir; su, döngü ve stok bildirimleri gelmez.
 * - Su hatırlatması aktif saatler içinde eşit aralıklarla kurulur; aralık dışına düşerse ertesi
 *   günün ilk aralığına kayar.
 * - Günlük kontrol (kaçan dozlar, uzun döngü uyarısı, stok) sessiz saate denk geliyorsa sessizliğin
 *   bitişine kayar; böylece uyarı hiç gösterilmeden "gösterildi" sayılmaz.
 */
object ReminderPolicy {

    /** [start]–[end] aralığı gece yarısını geçiyorsa iki parçaya bölerek bakar; uçlar dâhildir. */
    fun isWithin(now: LocalTime, start: LocalTime, end: LocalTime): Boolean =
        if (!start.isAfter(end)) !now.isBefore(start) && !now.isAfter(end)
        else !now.isBefore(start) || !now.isAfter(end)

    fun isQuiet(now: LocalTime, p: UserPreferences): Boolean =
        p.quietHoursEnabled && isWithin(now, p.quietHoursStart, p.quietHoursEnd)

    fun isActive(now: LocalTime, p: UserPreferences): Boolean =
        isWithin(now, p.activeHoursStart, p.activeHoursEnd)

    /** Günlük kontrol saati sessiz saate düşüyorsa sessizliğin bittiği an. */
    fun effectiveDailyCheckTime(p: UserPreferences): LocalTime =
        if (isQuiet(p.dailyCheckTime, p)) p.quietHoursEnd else p.dailyCheckTime

    /** [earliestDate] gününden itibaren [time] saatinin [now]'dan sonraki ilk anı. */
    fun nextOccurrence(time: LocalTime, earliestDate: LocalDate, now: ZonedDateTime): ZonedDateTime {
        var candidate = earliestDate.atTime(time).atZone(now.zone)
        while (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate
    }

    /**
     * Bir sonraki su hatırlatması. [fromNow] true ise tam bir aralık sonra, değilse hemen (1 dk) başlar;
     * sonuç aktif saatlerin dışına ya da sessiz saate düşerse ertesi aktif başlangıç + aralık.
     * Aktif saat aralığı ters girilmişse (başlangıç ≥ bitiş) kısıt uygulanmaz.
     */
    fun nextWater(now: ZonedDateTime, p: UserPreferences, fromNow: Boolean): ZonedDateTime {
        val interval = p.waterIntervalMinutes.coerceAtLeast(MIN_WATER_INTERVAL_MINUTES).toLong()
        val candidate = now.plusMinutes(if (fromNow) interval else 1)
        if (!p.activeHoursStart.isBefore(p.activeHoursEnd)) return candidate
        val today = now.toLocalDate()
        val start = today.atTime(p.activeHoursStart).atZone(now.zone)
        val end = today.atTime(p.activeHoursEnd).atZone(now.zone)
        val clamped = when {
            candidate.isBefore(start) -> start.plusMinutes(interval)
            candidate.isAfter(end) -> start.plusDays(1).plusMinutes(interval)
            else -> candidate
        }
        return if (isQuiet(clamped.toLocalTime(), p) && !clamped.isAfter(end)) {
            start.plusDays(1).plusMinutes(interval)
        } else {
            clamped
        }
    }

    /**
     * Uygulama her açıldığında su alarmı ileri atılmasın: kayıtlı an gelecekte ve makul uzaklıktaysa
     * (en fazla 24 saat + bir aralık) dokunulmaz.
     */
    fun shouldRescheduleWater(storedAtEpochMs: Long, now: Instant, p: UserPreferences): Boolean {
        if (storedAtEpochMs <= 0L) return true
        val at = Instant.ofEpochMilli(storedAtEpochMs)
        if (!at.isAfter(now)) return true
        val maxAhead = Duration.ofHours(24).plusMinutes(p.waterIntervalMinutes.toLong())
        return at.isAfter(now.plus(maxAhead))
    }

    /** Haftalık özetin günü ve saati: Pazar akşamı, sessiz saatlerden önce. */
    val WEEKLY_DAY: DayOfWeek = DayOfWeek.SUNDAY
    val WEEKLY_TIME: LocalTime = LocalTime.of(19, 0)

    /** [now]'dan sonraki ilk [day] günü [time] anı (bugün o günse ve saat geçmediyse bugün). */
    fun nextWeekly(now: ZonedDateTime, day: DayOfWeek = WEEKLY_DAY, time: LocalTime = WEEKLY_TIME): ZonedDateTime {
        var candidate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day)).atTime(time).atZone(now.zone)
        if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
        return candidate
    }

    const val MIN_WATER_INTERVAL_MINUTES = 15
}

/** Haftalık ilerleme özeti; hiç veri yoksa bildirim gönderilmez. */
data class HaftalikOzet(val logDays: Int, val dosesTaken: Int, val dosesScheduled: Int, val waterGoalDays: Int) {
    val isEmpty: Boolean get() = logDays == 0 && dosesScheduled == 0 && waterGoalDays == 0
}
