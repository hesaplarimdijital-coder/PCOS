package com.ayla.pcos.ui.me

import com.ayla.cycle.CycleEngine
import com.ayla.cycle.PeriodRecord
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class CycleTrendTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val engine = CycleEngine()

    /** Uzunluklar eskiden yeniye; son döngü bugün başladı. */
    private fun trendOf(vararg lengths: Int): CycleTrend {
        val starts = mutableListOf(today)
        lengths.reversed().forEach { starts.add(0, starts.first().minusDays(it.toLong())) }
        val overview = engine.overview(starts.map { PeriodRecord(it, it.plusDays(4)) }, today)
        return cycleTrend(overview)
    }

    @Test
    fun `uc donguden azsa veri yetersiz`() = assertEquals(CycleTrend.NeedData, trendOf(40, 38))

    @Test
    fun `art arda kisalip tipik araliga girerse kisaliyor`() = assertEquals(CycleTrend.Shortening(3), trendOf(45, 40, 34))

    @Test
    fun `kisalma tipik araligin disinda biterse kisaliyor sayilmaz`() =
        assertEquals(CycleTrend.Variable(38, 50), trendOf(50, 44, 38))

    @Test
    fun `dar dagilim duzenli`() {
        val t = trendOf(30, 31, 29, 30)
        assertEquals(true, t is CycleTrend.Stable && t.mean == 30)
    }

    @Test
    fun `genis dagilim degisken`() = assertEquals(CycleTrend.Variable(33, 44), trendOf(38, 33, 44, 36))
}
