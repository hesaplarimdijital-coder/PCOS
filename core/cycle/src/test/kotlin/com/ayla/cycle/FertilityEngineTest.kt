package com.ayla.cycle

import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FertilityEngineTest {

    private fun d(iso: String): LocalDate = LocalDate.parse(iso)

    /** 1 Eylül 2026'da başlayan döngü; sabit tarihler, rastgelelik yok. */
    private val start = d("2026-09-01")

    /** Önceki altı düşük ölçüm (en yükseği 36.4). */
    private val low = doubleArrayOf(36.3, 36.4, 36.2, 36.3, 36.4, 36.3)

    /** Standart kuralı sağlayan üç yüksek ölçüm. */
    private val high = doubleArrayOf(36.5, 36.6, 36.7)

    /** [from] gününden başlayarak ardışık günlere ısı kayıtları üretir. */
    private fun temps(from: LocalDate, vararg celsius: Double): List<FertilityEntry> =
        celsius.mapIndexed { i, t -> FertilityEntry(from.plusDays(i.toLong()), bbtCelsius = t) }

    /** 1–6 Eylül düşük, 7–9 Eylül yüksek: yumurtlama 6 Eylül, doğrulama 9 Eylül, örtü 36.4. */
    private fun confirmedCycle(): List<FertilityEntry> = temps(start, *low, *high)

    /** 12 günlük düz seyir; yükselme yok. */
    private fun flatCycle(days: Int = 12): List<FertilityEntry> =
        temps(start, *DoubleArray(days) { low[it % low.size] })

    private fun analyze(
        entries: List<FertilityEntry>,
        today: LocalDate,
        starts: List<LocalDate> = listOf(start),
        onPill: Boolean = false,
    ): FertilityOverview.Enabled =
        assertIs<FertilityOverview.Enabled>(FertilityEngine.analyze(entries, starts, today, onPill))

    private fun single(entries: List<FertilityEntry>, today: LocalDate = d("2026-09-12")): CycleFertility {
        val overview = analyze(entries, today)
        assertEquals(1, overview.cycles.size)
        return overview.cycles.single()
    }

    // --- Hap modu ve boş girdi ---

    @Test
    fun `hap modunda hicbir hesap yapilmaz ve Disabled doner`() {
        val overview = FertilityEngine.analyze(confirmedCycle(), listOf(start), d("2026-09-12"), onPill = true)
        assertEquals(FertilityOverview.Disabled, overview)
    }

    @Test
    fun `bos girdi ile Enabled doner ve dongu yoktur`() {
        val overview = analyze(emptyList(), d("2026-09-12"), starts = emptyList())
        assertEquals(FertilityOverview.Enabled(emptyList(), null), overview)
    }

    @Test
    fun `adet baslangici yoksa kayitlar donguye girmez`() {
        val overview = analyze(confirmedCycle(), d("2026-09-12"), starts = emptyList())
        assertTrue(overview.cycles.isEmpty())
        assertNull(overview.current)
    }

    // --- Döngüye bölme ---

    @Test
    fun `kayitlar adet baslangiclarina gore donguye bolunur`() {
        val entries = temps(d("2026-09-20"), 36.3, 36.4, 36.2) + temps(d("2026-10-01"), 36.3, 36.4)
        val overview = analyze(entries, d("2026-10-05"), starts = listOf(d("2026-09-20"), d("2026-10-01")))
        assertEquals(2, overview.cycles.size)
        val first = overview.cycles[0]
        val second = overview.cycles[1]
        assertEquals(d("2026-09-20"), first.start)
        assertEquals(d("2026-09-30"), first.end)
        assertFalse(first.isOpen)
        assertEquals(3, first.readings.size)
        assertEquals(d("2026-10-01"), second.start)
        assertNull(second.end)
        assertTrue(second.isOpen)
        assertEquals(2, second.readings.size)
        assertEquals(second, overview.current)
    }

    @Test
    fun `ilk adet baslangicindan onceki kayitlar dongu disidir`() {
        val before = temps(d("2026-08-25"), 36.3, 36.4, 36.2)
        val cycle = single(before + confirmedCycle())
        assertEquals(start, cycle.entries.first().date)
        assertEquals(9, cycle.readings.size)
        assertNotNull(cycle.ovulation)
    }

    @Test
    fun `gelecek tarihli kayitlar ve adet baslangiclari hesaba girmez`() {
        val today = d("2026-09-08")
        // 9 Eylül'deki üçüncü yüksek ölçüm henüz gelecektedir; 15 Eylül'deki adet başlangıcı da öyle.
        val entries = confirmedCycle() + temps(d("2026-09-10"), 36.7, 36.7)
        val overview = analyze(entries, today, starts = listOf(start, d("2026-09-15")))
        assertEquals(1, overview.cycles.size)
        val cycle = overview.cycles.single()
        assertNull(cycle.end)
        assertEquals(8, cycle.readings.size)
        assertTrue(cycle.readings.none { it.date.isAfter(today) })
        assertNull(cycle.ovulation)
    }

    @Test
    fun `ayni gune iki kayit varsa listede sonuncusu kazanir`() {
        val date = d("2026-09-03")
        // İlk kayıt kazansaydı 36.9 geçerli sayılır, örtü çizgisi 36.9 olur ve yükselme görülmezdi.
        val first = FertilityEntry(date, bbtCelsius = 36.9, lh = LhResult.NEGATIVE)
        val last = FertilityEntry(date, bbtCelsius = 36.9, bbtDisturbed = true, lh = LhResult.POSITIVE)
        // 1–7 Eylül düşük (3 Eylül yerine yukarıdaki kayıtlar), 8–10 Eylül yüksek.
        val others = (temps(start, 36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.2) + temps(d("2026-09-08"), *high))
            .filter { it.date != date }
        // Sonuncu, listede tarih sırasından bağımsız olarak en sonda duran kayıttır.
        val cycle = single(listOf(first) + others + last)
        assertEquals(1, cycle.entries.count { it.date == date })
        assertEquals(last, cycle.entries.single { it.date == date })
        assertEquals(listOf(date), cycle.lhPositiveDates)
        assertEquals(36.4, cycle.coverlineCelsius)
        assertNotNull(cycle.ovulation)
    }

    @Test
    fun `siralama sonucu degistirmez`() {
        val entries = confirmedCycle().map { it.copy(lh = LhResult.NEGATIVE, mucus = CervicalMucus.CREAMY) }
        val starts = listOf(d("2026-08-01"), start)
        val today = d("2026-09-12")
        val base = FertilityEngine.analyze(entries, starts, today)
        assertEquals(base, FertilityEngine.analyze(entries.asReversed(), starts.asReversed(), today))
        assertEquals(base, FertilityEngine.analyze(entries.shuffled(Random(7)), starts + start, today))
        val cycle = assertIs<FertilityOverview.Enabled>(base).cycles.last()
        assertEquals(cycle.entries.map { it.date }, cycle.entries.map { it.date }.sorted())
        assertEquals(cycle.readings.map { it.date }, cycle.readings.map { it.date }.sorted())
    }

    @Test
    fun `isi olmayan kayitlar grafige girmez ama donguye girer`() {
        val entries = confirmedCycle() + FertilityEntry(d("2026-09-10"), mucus = CervicalMucus.EGG_WHITE, bbtTime = LocalTime.of(7, 0))
        val cycle = single(entries)
        assertEquals(10, cycle.entries.size)
        assertEquals(9, cycle.readings.size)
    }

    // --- Bozuk ve aralık dışı ölçümler ---

    @Test
    fun `bozuk olcum grafikte excluded kalir ve ortu cizgisini degistirmez`() {
        val entries = temps(start, *low) +
            FertilityEntry(d("2026-09-07"), bbtCelsius = 36.9, bbtDisturbed = true) +
            temps(d("2026-09-08"), *high)
        val cycle = single(entries)
        val point = cycle.readings.single { it.date == d("2026-09-07") }
        assertEquals(ChartPoint(d("2026-09-07"), 36.9, disturbed = true, excluded = true), point)
        assertEquals(10, cycle.readings.size)
        assertEquals(36.4, cycle.coverlineCelsius)
        val ovulation = assertNotNull(cycle.ovulation)
        assertEquals(d("2026-09-08"), ovulation.firstHighReading)
        assertEquals(d("2026-09-07"), ovulation.ovulationDate)
        assertEquals(d("2026-09-10"), ovulation.confirmedOn)
    }

    @Test
    fun `aralik disi deger excluded olur ama bozuk sayilmaz`() {
        val entries = temps(start, *low) +
            FertilityEntry(d("2026-09-07"), bbtCelsius = 3.66) +
            temps(d("2026-09-08"), *high)
        val cycle = single(entries)
        val point = cycle.readings.single { it.date == d("2026-09-07") }
        assertFalse(point.disturbed)
        assertTrue(point.excluded)
        assertEquals(36.4, cycle.coverlineCelsius)
        assertFalse(FertilityNote.MANY_DISTURBED in cycle.notes)
    }

    @Test
    fun `gecerli olcumler excluded degildir`() {
        val cycle = single(confirmedCycle())
        assertTrue(cycle.readings.none { it.excluded || it.disturbed })
    }

    // --- Doğrulama, örtü çizgisi ve istisna kuralı ---

    @Test
    fun `yumurtlama isi yukselmesiyle dogrulanir ve ortu cizgisi verilir`() {
        val cycle = single(confirmedCycle())
        val ovulation = assertNotNull(cycle.ovulation)
        assertEquals(d("2026-09-06"), ovulation.ovulationDate)
        assertEquals(d("2026-09-09"), ovulation.confirmedOn)
        assertEquals(36.4, cycle.coverlineCelsius)
        assertTrue(FertilityNote.OVULATION_CONFIRMED in cycle.notes)
    }

    @Test
    fun `ortu cizgisi yalnizca dogrulanmis dongude vardir`() {
        val cycle = single(flatCycle())
        assertNull(cycle.ovulation)
        assertNull(cycle.coverlineCelsius)
        assertFalse(FertilityNote.OVULATION_CONFIRMED in cycle.notes)
    }

    @Test
    fun `birinci istisna kurali ile dogrulama dorduncu olcumde tamamlanir`() {
        val cycle = single(temps(start, *low, 36.5, 36.5, 36.5, 36.5))
        val ovulation = assertNotNull(cycle.ovulation)
        assertEquals(d("2026-09-07"), ovulation.firstHighReading)
        assertEquals(d("2026-09-10"), ovulation.confirmedOn)
        assertEquals(36.4, cycle.coverlineCelsius)
    }

    // --- Luteal gün ---

    @Test
    fun `luteal gun yumurtlama gunu sifir olacak sekilde sayilir`() {
        assertEquals(3, single(confirmedCycle(), today = d("2026-09-09")).lutealDay)
        assertEquals(14, single(confirmedCycle(), today = d("2026-09-20")).lutealDay)
    }

    @Test
    fun `dogrulama yoksa luteal gun yoktur`() {
        assertNull(single(flatCycle()).lutealDay)
    }

    @Test
    fun `kapanmis dongude luteal gun yoktur`() {
        val overview = analyze(confirmedCycle(), d("2026-09-25"), starts = listOf(start, d("2026-09-20")))
        val closed = overview.cycles[0]
        assertEquals(d("2026-09-19"), closed.end)
        assertNotNull(closed.ovulation)
        assertNull(closed.lutealDay)
    }

    // --- TOO_FEW_READINGS ---

    @Test
    fun `dokuzdan az gecerli olcum ve dogrulama yoksa veri az notu verilir`() {
        assertTrue(FertilityNote.TOO_FEW_READINGS in single(flatCycle(days = 8)).notes)
        assertFalse(FertilityNote.TOO_FEW_READINGS in single(flatCycle(days = 9)).notes)
    }

    @Test
    fun `bozuk olcumler gecerli sayilmaz ve veri az notuna yol acar`() {
        val entries = flatCycle(days = 10).mapIndexed { i, e -> if (i < 2) e.copy(bbtDisturbed = true) else e }
        assertTrue(FertilityNote.TOO_FEW_READINGS in single(entries).notes)
    }

    @Test
    fun `dogrulanmis dongude veri az notu verilmez`() {
        assertFalse(FertilityNote.TOO_FEW_READINGS in single(confirmedCycle()).notes)
    }

    // --- MANY_DISTURBED ---

    @Test
    fun `bozuk olcum notu oran ve sayi esigini birlikte ister`() {
        fun withDisturbed(days: Int, disturbed: Int): List<FertilityEntry> =
            flatCycle(days).mapIndexed { i, e -> if (i < disturbed) e.copy(bbtDisturbed = true) else e }
        val today = d("2026-09-25")
        assertTrue(FertilityNote.MANY_DISTURBED in single(withDisturbed(10, 3), today).notes)
        assertFalse(FertilityNote.MANY_DISTURBED in single(withDisturbed(10, 2), today).notes)
        assertFalse(FertilityNote.MANY_DISTURBED in single(withDisturbed(20, 3), today).notes)
        assertTrue(FertilityNote.MANY_DISTURBED in single(withDisturbed(20, 6), today).notes)
    }

    @Test
    fun `bozuk olcum yoksa bozuk olcum notu verilmez`() {
        assertFalse(FertilityNote.MANY_DISTURBED in single(confirmedCycle()).notes)
    }

    // --- LH_POSITIVE_WITHOUT_RISE ---

    @Test
    fun `LH pozitif sonrasi bes olcumde yukselme yoksa LH notu verilir`() {
        // 1–6 Eylül altı geçerli ölçüm, 6 Eylül LH pozitif, 7–12 Eylül altı düz ölçüm.
        val entries = flatCycle(days = 12).map { if (it.date == d("2026-09-06")) it.copy(lh = LhResult.POSITIVE) else it }
        val cycle = single(entries)
        assertTrue(FertilityNote.LH_POSITIVE_WITHOUT_RISE in cycle.notes)
        assertEquals(listOf(d("2026-09-06")), cycle.lhPositiveDates)
    }

    @Test
    fun `LH pozitif sonrasi yukselme dogrulandiysa LH notu verilmez`() {
        val entries = confirmedCycle().map { if (it.date == d("2026-09-06")) it.copy(lh = LhResult.POSITIVE) else it }
        val cycle = single(entries)
        assertFalse(FertilityNote.LH_POSITIVE_WITHOUT_RISE in cycle.notes)
        assertFalse(FertilityNote.RISE_WITHOUT_LH_POSITIVE in cycle.notes)
        assertTrue(FertilityNote.OVULATION_CONFIRMED in cycle.notes)
    }

    @Test
    fun `LH pozitif sonrasi besten az olcum varsa LH notu verilmez`() {
        // 7–10 Eylül yalnızca dört ölçüm.
        val entries = flatCycle(days = 10).map { if (it.date == d("2026-09-06")) it.copy(lh = LhResult.POSITIVE) else it }
        assertFalse(FertilityNote.LH_POSITIVE_WITHOUT_RISE in single(entries).notes)
    }

    @Test
    fun `LH pozitif sonrasi bozuk olcumler bes olcume sayilmaz`() {
        val entries = flatCycle(days = 12).map {
            when {
                it.date == d("2026-09-06") -> it.copy(lh = LhResult.POSITIVE)
                it.date.isAfter(d("2026-09-10")) -> it.copy(bbtDisturbed = true)
                else -> it
            }
        }
        assertFalse(FertilityNote.LH_POSITIVE_WITHOUT_RISE in single(entries).notes)
    }

    @Test
    fun `LH pozitiften once alti gecerli olcum yoksa LH notu verilmez`() {
        // 3 Eylül'den başlayan 12 ölçüm; 6 Eylül'e kadar yalnızca dört ölçüm var, örtü çizgisi kurulamazdı.
        val entries = temps(d("2026-09-03"), *DoubleArray(12) { low[it % low.size] })
            .map { if (it.date == d("2026-09-06")) it.copy(lh = LhResult.POSITIVE) else it }
        val cycle = single(entries, today = d("2026-09-20"))
        assertFalse(FertilityNote.LH_POSITIVE_WITHOUT_RISE in cycle.notes)
        assertFalse(FertilityNote.TOO_FEW_READINGS in cycle.notes)
    }

    // --- RISE_WITHOUT_LH_POSITIVE ---

    @Test
    fun `yukselme dogrulandi ve LH testleri negatifse zamanlama notu verilir`() {
        val entries = confirmedCycle().map {
            if (it.date in listOf(d("2026-09-04"), d("2026-09-05"))) it.copy(lh = LhResult.NEGATIVE) else it
        }
        val cycle = single(entries)
        assertTrue(FertilityNote.RISE_WITHOUT_LH_POSITIVE in cycle.notes)
        assertTrue(cycle.lhPositiveDates.isEmpty())
    }

    @Test
    fun `LH pozitif varsa zamanlama notu verilmez`() {
        val entries = confirmedCycle().map {
            when (it.date) {
                d("2026-09-04") -> it.copy(lh = LhResult.NEGATIVE)
                d("2026-09-05") -> it.copy(lh = LhResult.POSITIVE)
                else -> it
            }
        }
        val cycle = single(entries)
        assertFalse(FertilityNote.RISE_WITHOUT_LH_POSITIVE in cycle.notes)
        assertEquals(listOf(d("2026-09-05")), cycle.lhPositiveDates)
    }

    @Test
    fun `LH kaydi hic yoksa zamanlama notu verilmez`() {
        assertFalse(FertilityNote.RISE_WITHOUT_LH_POSITIVE in single(confirmedCycle()).notes)
    }

    @Test
    fun `yalnizca gecersiz LH testi zamanlama notu vermez`() {
        val entries = confirmedCycle().map { if (it.date == d("2026-09-05")) it.copy(lh = LhResult.INVALID) else it }
        assertFalse(FertilityNote.RISE_WITHOUT_LH_POSITIVE in single(entries).notes)
    }

    @Test
    fun `yukselme yoksa zamanlama notu verilmez`() {
        val entries = flatCycle().map { if (it.date == d("2026-09-05")) it.copy(lh = LhResult.NEGATIVE) else it }
        assertFalse(FertilityNote.RISE_WITHOUT_LH_POSITIVE in single(entries).notes)
    }

    // --- LONG_LUTEAL ---

    @Test
    fun `acik dongude yumurtlama sonrasi 18 gunu asinca adet gelmedi notu verilir`() {
        // Yumurtlama 6 Eylül: 24 Eylül 18. gün (not yok), 25 Eylül 19. gün (not var).
        val day18 = single(confirmedCycle(), today = d("2026-09-24"))
        assertEquals(18, day18.lutealDay)
        assertFalse(FertilityNote.LONG_LUTEAL in day18.notes)
        val day19 = single(confirmedCycle(), today = d("2026-09-25"))
        assertEquals(19, day19.lutealDay)
        assertTrue(FertilityNote.LONG_LUTEAL in day19.notes)
    }

    @Test
    fun `kapanmis dongude adet gelmedi notu verilmez`() {
        val overview = analyze(confirmedCycle(), d("2026-10-12"), starts = listOf(start, d("2026-10-10")))
        val closed = overview.cycles[0]
        assertNotNull(closed.ovulation)
        assertFalse(FertilityNote.LONG_LUTEAL in closed.notes)
        assertNull(closed.lutealDay)
    }

    @Test
    fun `dogrulama yoksa adet gelmedi notu verilmez`() {
        val cycle = single(flatCycle(), today = d("2026-10-30"))
        assertFalse(FertilityNote.LONG_LUTEAL in cycle.notes)
    }

    // --- LH pozitif günleri ve not sırası ---

    @Test
    fun `lhPositiveDates sirali ve yalnizca pozitifleri icerir`() {
        val entries = listOf(
            FertilityEntry(d("2026-09-08"), lh = LhResult.POSITIVE),
            FertilityEntry(d("2026-09-06"), lh = LhResult.NEGATIVE),
            FertilityEntry(d("2026-09-09"), lh = LhResult.INVALID),
            FertilityEntry(d("2026-09-07"), lh = LhResult.POSITIVE),
        )
        assertEquals(listOf(d("2026-09-07"), d("2026-09-08")), single(entries).lhPositiveDates)
    }

    @Test
    fun `notlar bildirim sirasinda verilir`() {
        // Doğrulanmış döngü + 4 bozuk ölçüm (4/13 ≥ %30) + negatif LH + 19. luteal gün.
        val entries = confirmedCycle().map { if (it.date == d("2026-09-05")) it.copy(lh = LhResult.NEGATIVE) else it } +
            temps(d("2026-09-10"), 36.8, 36.8, 36.8, 36.8).map { it.copy(bbtDisturbed = true) }
        val cycle = single(entries, today = d("2026-09-25"))
        assertEquals(
            listOf(
                FertilityNote.OVULATION_CONFIRMED,
                FertilityNote.MANY_DISTURBED,
                FertilityNote.RISE_WITHOUT_LH_POSITIVE,
                FertilityNote.LONG_LUTEAL,
            ),
            cycle.notes,
        )
    }

    // --- Çok döngü ---

    @Test
    fun `cok dongude her dongu ayri degerlendirilir`() {
        val august = temps(d("2026-08-01"), *low, *high)
        val september = temps(start, 36.3, 36.4, 36.2, 36.3, 36.4)
        val today = d("2026-10-05")
        val overview = analyze(august + september, today, starts = listOf(d("2026-08-01"), start, d("2026-10-01")))
        assertEquals(3, overview.cycles.size)
        val (first, second, third) = overview.cycles

        assertEquals(d("2026-08-31"), first.end)
        assertEquals(listOf(FertilityNote.OVULATION_CONFIRMED), first.notes)
        assertEquals(36.4, first.coverlineCelsius)
        assertNull(first.lutealDay)

        assertEquals(d("2026-09-30"), second.end)
        assertEquals(5, second.readings.size)
        assertEquals(listOf(FertilityNote.TOO_FEW_READINGS), second.notes)
        assertNull(second.coverlineCelsius)

        assertNull(third.end)
        assertTrue(third.readings.isEmpty())
        assertEquals(listOf(FertilityNote.TOO_FEW_READINGS), third.notes)
        assertEquals(third, overview.current)
    }
}
