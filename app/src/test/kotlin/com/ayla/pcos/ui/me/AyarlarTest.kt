package com.ayla.pcos.ui.me

import com.ayla.pcos.data.prefs.UserPreferences
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals

class AyarlarTest {

    @Test
    fun `litre turkce ondalik virgulle yazilir`() {
        assertEquals("2", litre(2000))
        assertEquals("2,25", litre(2250))
        assertEquals("1,5", litre(1500))
        assertEquals("0,25", litre(250))
        assertEquals("4,05", litre(4050))
    }

    @Test
    fun `saat alani dogru tercihi gunceller`() {
        val p = UserPreferences()
        val t = LocalTime.of(6, 45)
        assertEquals(t, p.withTime(SaatAlani.ACTIVE_START, t).activeHoursStart)
        assertEquals(t, p.withTime(SaatAlani.ACTIVE_END, t).activeHoursEnd)
        assertEquals(t, p.withTime(SaatAlani.QUIET_START, t).quietHoursStart)
        assertEquals(t, p.withTime(SaatAlani.QUIET_END, t).quietHoursEnd)
        assertEquals(t, p.withTime(SaatAlani.DAILY_CHECK, t).dailyCheckTime)
        assertEquals(t, p.withTime(SaatAlani.EVENING, t).eveningRoutineTime)
        // diğer alanlar değişmez
        assertEquals(p.copy(eveningRoutineTime = t), p.withTime(SaatAlani.EVENING, t))
    }

    @Test
    fun `su sinirlari tutarli`() {
        assertEquals(4..16, AyarSinirlari.waterGoalRange)
        assertEquals(0, (AyarSinirlari.cupMlRange.last - AyarSinirlari.cupMlRange.first) % AyarSinirlari.CUP_ML_STEP)
        assertEquals(true, UserPreferences().waterIntervalMinutes in AyarSinirlari.waterIntervals)
        assertEquals(true, UserPreferences().waterGoalCups in AyarSinirlari.waterGoalRange)
    }
}
