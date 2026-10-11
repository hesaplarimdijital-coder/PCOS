package com.ayla.pcos.reminders

import com.ayla.pcos.data.prefs.UserPreferences
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderPolicyTest {

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    private val defaults = UserPreferences() // sessiz 22:30–08:00, aktif 09:00–21:00, su aralığı 120 dk

    private fun at(date: LocalDate, time: LocalTime): ZonedDateTime = date.atTime(time).atZone(zone)
    private val day: LocalDate = LocalDate.of(2026, 10, 11)

    @Test
    fun `sessiz saat gece yarisini gecer`() {
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(23, 0), defaults))
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(3, 0), defaults))
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(22, 30), defaults))
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(8, 0), defaults))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(8, 1), defaults))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(12, 0), defaults))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(22, 29), defaults))
    }

    @Test
    fun `sessiz saat kapaliysa hic sessiz degil`() {
        val p = defaults.copy(quietHoursEnabled = false)
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(3, 0), p))
    }

    @Test
    fun `ayni gun icindeki sessiz aralik`() {
        val p = defaults.copy(quietHoursStart = LocalTime.of(13, 0), quietHoursEnd = LocalTime.of(15, 0))
        assertTrue(ReminderPolicy.isQuiet(LocalTime.of(14, 0), p))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(16, 0), p))
        assertFalse(ReminderPolicy.isQuiet(LocalTime.of(2, 0), p))
    }

    @Test
    fun `aktif saatler uclar dahil`() {
        assertFalse(ReminderPolicy.isActive(LocalTime.of(8, 59), defaults))
        assertTrue(ReminderPolicy.isActive(LocalTime.of(9, 0), defaults))
        assertTrue(ReminderPolicy.isActive(LocalTime.of(21, 0), defaults))
        assertFalse(ReminderPolicy.isActive(LocalTime.of(21, 1), defaults))
    }

    @Test
    fun `gunluk kontrol sessiz saate dusuyorsa sessizligin bitisine kayar`() {
        assertEquals(LocalTime.of(8, 0), ReminderPolicy.effectiveDailyCheckTime(defaults.copy(dailyCheckTime = LocalTime.of(7, 0))))
        assertEquals(LocalTime.of(9, 0), ReminderPolicy.effectiveDailyCheckTime(defaults))
        assertEquals(LocalTime.of(7, 0), ReminderPolicy.effectiveDailyCheckTime(defaults.copy(dailyCheckTime = LocalTime.of(7, 0), quietHoursEnabled = false)))
    }

    @Test
    fun `sonraki tetikleme saat gectiyse yarina kayar`() {
        val now = at(day, LocalTime.of(9, 30))
        assertEquals(at(day.plusDays(1), LocalTime.of(9, 0)), ReminderPolicy.nextOccurrence(LocalTime.of(9, 0), day, now))
        assertEquals(at(day, LocalTime.of(21, 0)), ReminderPolicy.nextOccurrence(LocalTime.of(21, 0), day, now))
        // tam o anda ise yine yarın (alarm geçmişe kurulmaz)
        assertEquals(at(day.plusDays(1), LocalTime.of(9, 30)), ReminderPolicy.nextOccurrence(LocalTime.of(9, 30), day, now))
    }

    @Test
    fun `sonraki tetikleme gelecekteki baslangic gununu korur`() {
        val now = at(day, LocalTime.of(9, 30))
        assertEquals(at(day.plusDays(3), LocalTime.of(9, 0)), ReminderPolicy.nextOccurrence(LocalTime.of(9, 0), day.plusDays(3), now))
    }

    @Test
    fun `su hatirlatmasi aktif saat icinde bir aralik sonra`() {
        val now = at(day, LocalTime.of(10, 0))
        assertEquals(at(day, LocalTime.of(12, 0)), ReminderPolicy.nextWater(now, defaults, fromNow = true))
        assertEquals(at(day, LocalTime.of(10, 1)), ReminderPolicy.nextWater(now, defaults, fromNow = false))
    }

    @Test
    fun `su hatirlatmasi aktif saat disina duserse ertesi gunun ilk araligina kayar`() {
        assertEquals(at(day.plusDays(1), LocalTime.of(11, 0)), ReminderPolicy.nextWater(at(day, LocalTime.of(20, 30)), defaults, fromNow = true))
        assertEquals(at(day, LocalTime.of(11, 0)), ReminderPolicy.nextWater(at(day, LocalTime.of(6, 0)), defaults, fromNow = true))
        assertEquals(at(day.plusDays(1), LocalTime.of(11, 0)), ReminderPolicy.nextWater(at(day, LocalTime.of(23, 0)), defaults, fromNow = true))
    }

    @Test
    fun `su hatirlatmasi sessiz saate duserse ertesi gune kayar`() {
        // Aktif saatler 09:00–23:30 ama sessizlik 22:30'da başlar → 22:00'den sonraki aralık 24:00 yerine ertesi gün 11:00
        val p = defaults.copy(activeHoursEnd = LocalTime.of(23, 30))
        assertEquals(at(day.plusDays(1), LocalTime.of(11, 0)), ReminderPolicy.nextWater(at(day, LocalTime.of(21, 0)), p, fromNow = true))
    }

    @Test
    fun `su araligi 15 dakikanin altina inmez`() {
        val p = defaults.copy(waterIntervalMinutes = 1)
        assertEquals(at(day, LocalTime.of(10, 15)), ReminderPolicy.nextWater(at(day, LocalTime.of(10, 0)), p, fromNow = true))
    }

    @Test
    fun `ters aktif saat araliginda kisit uygulanmaz`() {
        val p = defaults.copy(activeHoursStart = LocalTime.of(21, 0), activeHoursEnd = LocalTime.of(9, 0))
        assertEquals(at(day, LocalTime.of(12, 0)), ReminderPolicy.nextWater(at(day, LocalTime.of(10, 0)), p, fromNow = true))
    }

    @Test
    fun `haftalik ozet bir sonraki pazar 19da`() {
        // 12 Ekim 2026 Pazartesi → 18 Ekim Pazar
        val monday = LocalDate.of(2026, 10, 12)
        assertEquals(at(LocalDate.of(2026, 10, 18), LocalTime.of(19, 0)), ReminderPolicy.nextWeekly(at(monday, LocalTime.of(10, 0))))
        // Pazar 18:00 → aynı gün 19:00; Pazar 19:00 → bir hafta sonra
        val sunday = LocalDate.of(2026, 10, 18)
        assertEquals(at(sunday, LocalTime.of(19, 0)), ReminderPolicy.nextWeekly(at(sunday, LocalTime.of(18, 0))))
        assertEquals(at(sunday.plusWeeks(1), LocalTime.of(19, 0)), ReminderPolicy.nextWeekly(at(sunday, LocalTime.of(19, 0))))
    }

    @Test
    fun `veri yoksa haftalik ozet bos sayilir`() {
        assertTrue(HaftalikOzet(0, 0, 0, 0).isEmpty)
        assertFalse(HaftalikOzet(logDays = 1, dosesTaken = 0, dosesScheduled = 0, waterGoalDays = 0).isEmpty)
        assertFalse(HaftalikOzet(logDays = 0, dosesTaken = 0, dosesScheduled = 14, waterGoalDays = 0).isEmpty)
    }

    @Test
    fun `su alarmi gecerliyse yeniden kurulmaz`() {
        val now = at(day, LocalTime.of(10, 0)).toInstant()
        assertTrue(ReminderPolicy.shouldRescheduleWater(0L, now, defaults))
        assertTrue(ReminderPolicy.shouldRescheduleWater(now.minusSeconds(60).toEpochMilli(), now, defaults))
        assertFalse(ReminderPolicy.shouldRescheduleWater(now.plusSeconds(3600).toEpochMilli(), now, defaults))
        assertFalse(ReminderPolicy.shouldRescheduleWater(now.plusSeconds(25 * 3600).toEpochMilli(), now, defaults))
        assertTrue(ReminderPolicy.shouldRescheduleWater(now.plusSeconds(3 * 24 * 3600).toEpochMilli(), now, defaults))
        assertTrue(ReminderPolicy.shouldRescheduleWater(Instant.EPOCH.toEpochMilli(), now, defaults))
    }
}
