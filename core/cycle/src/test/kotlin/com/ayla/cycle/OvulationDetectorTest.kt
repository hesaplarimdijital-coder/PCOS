package com.ayla.cycle

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OvulationDetectorTest {

    private val start = LocalDate.parse("2026-09-01")

    private fun readings(vararg temps: Double): List<TemperatureReading> =
        temps.mapIndexed { i, t -> TemperatureReading(start.plusDays(i.toLong()), t) }

    @Test
    fun `uc yuksek olcum kurali yumurtlamayi dogrular`() {
        // 6 düşük ölçüm (max 36.4), ardından 36.5, 36.6, 36.7 → üçüncü, örtü + 0.2'nin üstünde
        val estimate = OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.6, 36.7))
        assertNotNull(estimate)
        assertEquals(36.4, estimate.coverlineCelsius)
        assertEquals(start.plusDays(6), estimate.firstHighReading)
        assertEquals(start.plusDays(5), estimate.ovulationDate)
    }

    @Test
    fun `ucuncu olcum yeterince yuksek degilse dogrulanmaz`() {
        val estimate = OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.5, 36.5))
        assertNull(estimate)
    }

    @Test
    fun `tek bir yuksek olcum yeterli degildir`() {
        assertNull(OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.9, 36.3, 36.4, 36.3)))
    }

    @Test
    fun `dokuzdan az olcumle sonuc yoktur`() {
        assertNull(OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.7, 36.8)))
    }

    @Test
    fun `siralama ve yinelenen gunler sonucu degistirmez`() {
        val base = readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.6, 36.7)
        val shuffled = base.shuffled() + base.first()
        assertEquals(OvulationDetector.detect(base), OvulationDetector.detect(shuffled))
    }
}
