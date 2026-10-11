package com.ayla.cycle

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CycleEngineTest {

    private val engine = CycleEngine()

    private fun d(iso: String): LocalDate = LocalDate.parse(iso)

    /** Verilen döngü uzunluklarından, [first] gününden başlayarak adet kayıtları üretir. */
    private fun periodsFromLengths(first: LocalDate, lengths: List<Int>): List<PeriodRecord> {
        val starts = mutableListOf(first)
        for (len in lengths) starts += starts.last().plusDays(len.toLong())
        return starts.map { PeriodRecord(it) }
    }

    // --- Döngü kurma ---

    @Test
    fun `kayitlar siralanir ve dongu uzunluklari hesaplanir`() {
        val records = listOf(
            PeriodRecord(d("2026-03-01")),
            PeriodRecord(d("2026-01-01")),
            PeriodRecord(d("2026-02-01")),
        )
        val cycles = engine.buildCycles(records)
        assertEquals(listOf(d("2026-01-01"), d("2026-02-01"), d("2026-03-01")), cycles.map { it.start })
        assertEquals(listOf(31, 28, null), cycles.map { it.lengthDays })
        assertTrue(cycles[0].countsForStats)
        assertFalse(cycles[2].isComplete)
    }

    @Test
    fun `ayni gune dusen kayitlar birlestirilir`() {
        val records = listOf(
            PeriodRecord(d("2026-01-01"), end = d("2026-01-04")),
            PeriodRecord(d("2026-01-01"), end = d("2026-01-06"), excludedFromStats = true),
            PeriodRecord(d("2026-02-01")),
        )
        val cycles = engine.buildCycles(records)
        assertEquals(2, cycles.size)
        assertEquals(ExclusionReason.USER, cycles[0].exclusion)
        assertFalse(cycles[0].countsForStats)
    }

    @Test
    fun `14 gunden kisa dongu ara kanama sayilir ve istatistige girmez`() {
        val records = listOf(
            PeriodRecord(d("2026-01-01")),
            PeriodRecord(d("2026-01-10")),
            PeriodRecord(d("2026-02-10")),
        )
        val cycles = engine.buildCycles(records)
        assertEquals(ExclusionReason.IMPLAUSIBLY_SHORT, cycles[0].exclusion)
        assertNull(cycles[1].exclusion)
        val stats = engine.stats(cycles, d("2026-02-20"))
        assertEquals(listOf(31), stats.lengths)
    }

    // --- İstatistik ---

    @Test
    fun `istatistikler dogru hesaplanir`() {
        val today = d("2026-10-10")
        val records = periodsFromLengths(d("2025-10-01"), listOf(74, 61, 52, 47, 44, 41))
        val cycles = engine.buildCycles(records)
        val stats = engine.stats(cycles, today)
        assertEquals(6, stats.count)
        assertEquals(53.166, stats.mean!!, 0.01)
        assertEquals(49.5, stats.median)
        assertEquals(41, stats.shortest)
        assertEquals(74, stats.longest)
        val sd = assertNotNull(stats.standardDeviation)
        assertTrue(sd > 11.0 && sd < 13.0, "standart sapma $sd")
    }

    @Test
    fun `son 12 aydaki adet sayisi hariç tutulanlari da sayar`() {
        val today = d("2026-10-10")
        val records = listOf(
            PeriodRecord(d("2025-09-01")), // 365 günden eski, sayılmaz
            PeriodRecord(d("2025-12-01"), excludedFromStats = true),
            PeriodRecord(d("2026-03-01")),
            PeriodRecord(d("2026-07-01")),
            PeriodRecord(d("2026-11-01")), // gelecek; overview filtreler, stats saymaz
        )
        val stats = engine.stats(engine.buildCycles(records), today)
        assertEquals(3, stats.periodsInTrailingYear)
    }

    @Test
    fun `tek dongu ile standart sapma yoktur`() {
        val stats = engine.stats(engine.buildCycles(periodsFromLengths(d("2026-01-01"), listOf(30))), d("2026-02-10"))
        assertEquals(30.0, stats.mean)
        assertNull(stats.standardDeviation)
    }

    // --- Tahmin ---

    @Test
    fun `tamamlanmis dongu yoksa tahmin yapilmaz`() {
        val cycles = engine.buildCycles(listOf(PeriodRecord(d("2026-09-03"))))
        assertEquals(Prediction.NotEnoughData, engine.predict(cycles, d("2026-10-10")))
    }

    @Test
    fun `tek dongu dusuk guvenle genis aralik verir`() {
        val cycles = engine.buildCycles(periodsFromLengths(d("2026-07-01"), listOf(30)))
        val prediction = engine.predict(cycles, d("2026-08-10"))
        val window = assertIs<Prediction.Window>(prediction)
        assertEquals(Confidence.LOW, window.confidence)
        assertEquals(1, window.basedOnCycles)
        // merkez 30, yayılım 7: 23. gün ile 41. gün arası (başlangıç 31 Temmuz)
        assertEquals(d("2026-07-31").plusDays(22), window.earliest)
        assertEquals(d("2026-07-31").plusDays(40), window.latest)
    }

    @Test
    fun `duzenli dongulerde yuksek guven ve dar aralik`() {
        val cycles = engine.buildCycles(periodsFromLengths(d("2026-01-01"), listOf(28, 29, 28, 30, 28)))
        val today = cycles.last().start.plusDays(10)
        val window = assertIs<Prediction.Window>(engine.predict(cycles, today))
        assertEquals(Confidence.HIGH, window.confidence)
        val widthDays = window.latest.toEpochDay() - window.earliest.toEpochDay()
        assertTrue(widthDays in 4..9, "aralık genişliği $widthDays")
        assertTrue(window.expectedLengthDays in 28..29)
    }

    @Test
    fun `kisalan PCOS dongulerinde merkez son dongulere yaklasir ve guven orta olur`() {
        // Rapordaki örnek: 47 → 44 → 41, son adet 3 Eylül 2026.
        val cycles = engine.buildCycles(periodsFromLengths(d("2026-04-24"), listOf(47, 44, 41)))
        assertEquals(d("2026-09-03"), cycles.last().start)
        val window = assertIs<Prediction.Window>(engine.predict(cycles, d("2026-10-10")))
        assertEquals(Confidence.MEDIUM, window.confidence)
        assertEquals(3, window.basedOnCycles)
        assertEquals(43, window.expectedLengthDays)
        assertEquals(d("2026-10-12"), window.earliest)
        assertEquals(d("2026-10-20"), window.latest)
    }

    @Test
    fun `cok dagilmis gecmiste guven dusuk ve aralik tavanla sinirli`() {
        val cycles = engine.buildCycles(periodsFromLengths(d("2025-01-01"), listOf(25, 90, 30, 120, 28, 75)))
        val window = assertIs<Prediction.Window>(engine.predict(cycles, cycles.last().start.plusDays(5)))
        assertEquals(Confidence.LOW, window.confidence)
        val widthDays = window.latest.toEpochDay() - window.earliest.toEpochDay()
        assertTrue(widthDays <= (21 * 2.5).toInt() + 1, "aralık genişliği $widthDays")
    }

    @Test
    fun `aralik gecildiyse PastWindow doner`() {
        val cycles = engine.buildCycles(periodsFromLengths(d("2026-01-01"), listOf(30, 31, 30)))
        val lastStart = cycles.last().start
        val prediction = engine.predict(cycles, lastStart.plusDays(70))
        val past = assertIs<Prediction.PastWindow>(prediction)
        assertTrue(past.daysPast > 0)
        assertTrue(past.latestExpected.isBefore(lastStart.plusDays(70)))
    }

    @Test
    fun `haric tutulan dongu tahmini etkilemez`() {
        val base = periodsFromLengths(d("2026-01-01"), listOf(30, 30, 30))
        val withOutlier = base.toMutableList()
        // 3. kayıttan sonra 120 günlük bir döngü ekle ve hariç tut
        val outlierStart = base.last().start
        withOutlier[withOutlier.lastIndex] = PeriodRecord(outlierStart, excludedFromStats = true)
        withOutlier += PeriodRecord(outlierStart.plusDays(120))
        val cycles = engine.buildCycles(withOutlier)
        val window = assertIs<Prediction.Window>(engine.predict(cycles, cycles.last().start.plusDays(3)))
        assertEquals(30, window.expectedLengthDays)
    }

    @Test
    fun `tahmin en fazla son alti donguyu kullanir`() {
        val lengths = List(10) { 60 } + List(6) { 30 }
        val cycles = engine.buildCycles(periodsFromLengths(d("2024-01-01"), lengths))
        val window = assertIs<Prediction.Window>(engine.predict(cycles, cycles.last().start.plusDays(1)))
        assertEquals(6, window.basedOnCycles)
        assertEquals(30, window.expectedLengthDays)
    }

    @Test
    fun `aralik en erken 15 gunde baslar ve en az 4 gun genis olur`() {
        val cycles = engine.buildCycles(periodsFromLengths(d("2026-01-01"), listOf(16, 15, 16, 15)))
        val window = assertIs<Prediction.Window>(engine.predict(cycles, cycles.last().start))
        assertEquals(cycles.last().start.plusDays(14), window.earliest)
        assertTrue(window.latest.toEpochDay() - window.earliest.toEpochDay() >= 4)
    }

    // --- Döngü günü ve işaretler ---

    @Test
    fun `dongu gunu baslangic gunu 1 olacak sekilde sayilir`() {
        val cycles = engine.buildCycles(listOf(PeriodRecord(d("2026-09-03"))))
        assertEquals(1, engine.currentCycleDay(cycles, d("2026-09-03")))
        assertEquals(38, engine.currentCycleDay(cycles, d("2026-10-10")))
        assertNull(engine.currentCycleDay(emptyList(), d("2026-10-10")))
    }

    @Test
    fun `uzun dongu uyarilari 60 ve 90 gunde kademelenir`() {
        val records = listOf(PeriodRecord(d("2026-01-01")))
        fun alertOn(day: Int): LongCycleAlert {
            val today = d("2026-01-01").plusDays((day - 1).toLong())
            return engine.overview(records, today).flags.longCycle
        }
        assertEquals(LongCycleAlert.NONE, alertOn(59))
        assertEquals(LongCycleAlert.GENTLE, alertOn(60))
        assertEquals(LongCycleAlert.GENTLE, alertOn(89))
        assertEquals(LongCycleAlert.SEE_DOCTOR, alertOn(90))
    }

    @Test
    fun `seyrek adet isareti bir yillik kayit gerektirir`() {
        val records = listOf(PeriodRecord(d("2026-01-01")), PeriodRecord(d("2026-05-01")))
        val today = d("2026-10-10")
        assertFalse(engine.overview(records, today, trackingSince = d("2026-01-01")).flags.fewPeriodsInTrailingYear)
        assertTrue(engine.overview(records, today, trackingSince = d("2025-06-01")).flags.fewPeriodsInTrailingYear)
    }

    @Test
    fun `yilda dort ve ustu adet seyrek sayilmaz`() {
        val records = periodsFromLengths(d("2025-11-01"), listOf(80, 80, 80))
        val today = d("2026-10-10")
        val overview = engine.overview(records, today, trackingSince = d("2025-06-01"))
        assertEquals(4, overview.stats.periodsInTrailingYear)
        assertFalse(overview.flags.fewPeriodsInTrailingYear)
    }

    @Test
    fun `overview gelecek tarihli kayitlari yok sayar`() {
        val records = listOf(PeriodRecord(d("2026-09-03")), PeriodRecord(d("2026-12-01")))
        val overview = engine.overview(records, d("2026-10-10"))
        assertEquals(d("2026-09-03"), overview.lastPeriodStart)
        assertEquals(38, overview.currentCycleDay)
        assertEquals(Prediction.NotEnoughData, overview.prediction)
    }

    // --- Takvim ---

    @Test
    fun `takvimde adet gunleri ve tahmin araligi isaretlenir`() {
        val records = listOf(
            PeriodRecord(d("2026-08-01"), end = d("2026-08-05")),
            PeriodRecord(d("2026-09-03")),
        )
        val today = d("2026-09-05")
        val prediction = Prediction.Window(d("2026-10-05"), d("2026-10-12"), 34, Confidence.MEDIUM, 1)
        assertEquals(DayKind.PERIOD, engine.dayKind(d("2026-08-03"), records, prediction, today))
        assertEquals(DayKind.NONE, engine.dayKind(d("2026-08-06"), records, prediction, today))
        // bitişi bilinmeyen adet bugüne kadar gösterilir, ötesine taşmaz
        assertEquals(DayKind.PERIOD, engine.dayKind(d("2026-09-05"), records, prediction, today))
        assertEquals(DayKind.NONE, engine.dayKind(d("2026-09-06"), records, prediction, today))
        assertEquals(DayKind.PREDICTED, engine.dayKind(d("2026-10-08"), records, prediction, today))
        assertEquals(DayKind.NONE, engine.dayKind(d("2026-10-13"), records, prediction, today))
    }

    @Test
    fun `bitisi bilinmeyen adet bir sonraki adete tasmaz`() {
        val records = listOf(PeriodRecord(d("2026-08-01")), PeriodRecord(d("2026-08-04")))
        val today = d("2026-08-20")
        assertEquals(DayKind.PERIOD, engine.dayKind(d("2026-08-03"), records, Prediction.NotEnoughData, today))
        assertEquals(DayKind.PERIOD, engine.dayKind(d("2026-08-04"), records, Prediction.NotEnoughData, today))
        assertEquals(DayKind.NONE, engine.dayKind(d("2026-08-09"), records, Prediction.NotEnoughData, today))
    }
}
