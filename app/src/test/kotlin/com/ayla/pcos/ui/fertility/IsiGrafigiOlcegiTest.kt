package com.ayla.pcos.ui.fertility

import com.ayla.cycle.ChartPoint
import com.ayla.cycle.CycleFertility
import com.ayla.cycle.OvulationEstimate
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Isı grafiği ölçeği (#24): eksen aralığı, etiketler, çizgi parçaları ve kırpma. */
class IsiGrafigiOlcegiTest {

    private val start = LocalDate.of(2026, 9, 20)

    private fun cycle(vararg points: Pair<Int, Double>, excluded: Set<Int> = emptySet(), end: LocalDate? = null) = CycleFertility(
        start = start,
        end = end,
        entries = emptyList(),
        readings = points.map { (day, c) -> ChartPoint(start.plusDays(day - 1L), c, disturbed = day in excluded, excluded = day in excluded) },
        coverlineCelsius = null,
        ovulation = null,
        lhPositiveDates = emptyList(),
        lutealDay = null,
        notes = emptyList(),
    )

    @Test
    fun `aralik en dusuk ve en yuksek olcumun 0,2 disina onluga yuvarlanir`() {
        val o = IsiGrafigiOlcegi.of(cycle(1 to 36.3, 2 to 36.45, 3 to 36.9), today = start.plusDays(2))
        assertEquals(36.1, o.minCelsius, 1e-9)
        assertEquals(37.1, o.maxCelsius, 1e-9)
        // 1,0 °C aralık = 10 onluk > 8 etiket: 0,2 adım.
        assertEquals(listOf(36.1, 36.3, 36.5, 36.7, 36.9, 37.1), o.yTicks)
    }

    @Test
    fun `duz olcumlerde en az 0,6 derece genislik ve en az 14 gun`() {
        val o = IsiGrafigiOlcegi.of(cycle(1 to 36.5, 2 to 36.5), today = start.plusDays(3))
        assertTrue(o.maxCelsius - o.minCelsius >= 0.6 - 1e-9)
        assertEquals(14, o.lastDay)
        assertEquals(listOf(1, 7, 14), o.xTicks)
    }

    @Test
    fun `hesaba girmeyen yazim hatasi ekseni bozmaz ve kenara kirpilir`() {
        val o = IsiGrafigiOlcegi.of(cycle(1 to 36.4, 2 to 3.65, 3 to 36.6, excluded = setOf(2)), today = start.plusDays(2))
        assertEquals(36.2, o.minCelsius, 1e-9)
        assertEquals(0f, o.yOf(3.65))
        assertEquals(1f, o.yOf(45.0))
    }

    @Test
    fun `cizgi gecerli olcumlerden gecer ve 3 gunden uzun boslukta kesilir`() {
        val o = IsiGrafigiOlcegi.of(cycle(1 to 36.3, 2 to 36.3, 3 to 36.4, 8 to 36.5, 9 to 36.6, excluded = setOf(3)), today = start.plusDays(20))
        assertEquals(listOf(listOf(1, 2), listOf(8, 9)), o.segments.map { s -> s.map { it.day } })
        assertEquals(21, o.lastDay, "açık döngüde bugüne kadar")
        assertEquals(listOf(1, 7, 14, 21), o.xTicks)
    }

    @Test
    fun `kapali dongu sonuna kadar cizilir, yumurtlama gunu ve LH gunleri doner`() {
        val c = cycle(1 to 36.3, end = start.plusDays(29)).copy(
            ovulation = OvulationEstimate(start.plusDays(13), 36.4, start.plusDays(14), start.plusDays(16)),
            lhPositiveDates = listOf(start.plusDays(12)),
        )
        val o = IsiGrafigiOlcegi.of(c, today = start.plusDays(60))
        assertEquals(30, o.lastDay)
        assertEquals(14, o.ovulationDay)
        assertEquals(setOf(13), o.lhDays)
        assertEquals(0f, o.xOf(1))
        assertEquals(1f, o.xOf(30))
    }
}
