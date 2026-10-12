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

    // --- Doğrulama günü ve 1. istisna kuralı ---

    @Test
    fun `standart kuralda dogrulama ucuncu olcum gununde tamamlanir`() {
        val estimate = OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.6, 36.7))
        assertNotNull(estimate)
        assertEquals(start.plusDays(8), estimate.confirmedOn)
    }

    @Test
    fun `birinci istisna kurali dorduncu olcumle dogrular`() {
        // Üçüncü ölçüm örtü + 0.2'ye ulaşmıyor (36.5 < 36.6) ama dördüncü de çizginin üstünde.
        val estimate = OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.5, 36.5, 36.5))
        assertNotNull(estimate)
        assertEquals(36.4, estimate.coverlineCelsius)
        assertEquals(start.plusDays(6), estimate.firstHighReading)
        assertEquals(start.plusDays(5), estimate.ovulationDate)
        assertEquals(start.plusDays(9), estimate.confirmedOn)
    }

    @Test
    fun `istisna kuralinda dorduncu olcum cizginin ustunde degilse dogrulanmaz`() {
        assertNull(OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.5, 36.5, 36.4)))
    }

    // --- Bozuk ve aralık dışı ölçümler ---

    @Test
    fun `bozuk olcum ortu cizgisine girmez`() {
        // 7. gün ateşli (36.9, bozuk): hesaba girseydi örtü 36.9 olur ve yükselme görülmezdi.
        val list = readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.9, 36.5, 36.6, 36.7).toMutableList()
        list[6] = list[6].copy(disturbed = true)
        val estimate = OvulationDetector.detect(list)
        assertNotNull(estimate)
        assertEquals(36.4, estimate.coverlineCelsius)
        assertEquals(start.plusDays(7), estimate.firstHighReading)
        assertEquals(start.plusDays(6), estimate.ovulationDate)
        assertEquals(start.plusDays(9), estimate.confirmedOn)
    }

    @Test
    fun `yuksek evredeki bozuk olcum atlanir ve sonraki gecerli olcum sayilir`() {
        // 8. gün az uyku (36.1, bozuk): atlanmazsa "art arda üç yüksek" bozulurdu.
        val list = readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.1, 36.6, 36.7).toMutableList()
        list[7] = list[7].copy(disturbed = true)
        val estimate = OvulationDetector.detect(list)
        assertNotNull(estimate)
        assertEquals(start.plusDays(6), estimate.firstHighReading)
        assertEquals(start.plusDays(9), estimate.confirmedOn)
    }

    @Test
    fun `aralik disi degerler yok sayilir`() {
        // 42.0 ve 3.66 giriş hatasıdır; hesaba girseydi örtü çizgisi 42.0 olurdu.
        val estimate = OvulationDetector.detect(readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 42.0, 3.66, 36.5, 36.6, 36.7))
        assertNotNull(estimate)
        assertEquals(36.4, estimate.coverlineCelsius)
        assertEquals(start.plusDays(8), estimate.firstHighReading)
    }

    @Test
    fun `aralik sinirlari dahildir`() {
        val edge = TemperatureReading(start, 35.0)
        val top = TemperatureReading(start, 38.5)
        assertEquals(true, OvulationDetector.isUsable(edge))
        assertEquals(true, OvulationDetector.isUsable(top))
        assertEquals(false, OvulationDetector.isUsable(TemperatureReading(start, 34.99)))
        assertEquals(false, OvulationDetector.isUsable(TemperatureReading(start, 38.51)))
        assertEquals(false, OvulationDetector.isUsable(TemperatureReading(start, 36.5, disturbed = true)))
    }

    @Test
    fun `tum olcumler bozuksa sonuc yoktur`() {
        val list = readings(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.6, 36.7).map { it.copy(disturbed = true) }
        assertNull(OvulationDetector.detect(list))
    }
}
