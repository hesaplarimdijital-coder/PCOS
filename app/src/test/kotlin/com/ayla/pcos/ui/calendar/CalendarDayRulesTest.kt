package com.ayla.pcos.ui.calendar

import com.ayla.cycle.CycleEngine
import com.ayla.cycle.PeriodRecord
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarDayRulesTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 11)
    private val engine = CycleEngine()

    private fun rules(date: LocalDate, records: List<PeriodRecord>) =
        calendarDayRules(date, records, engine.buildCycles(records), today)

    @Test
    fun `kayit yokken yalnizca baslangic isaretlenir ve gunluk acilir`() {
        val r = rules(today.minusDays(3), emptyList())
        assertTrue(r.canMarkStart)
        assertFalse(r.canMarkEnd)
        assertFalse(r.canUnmark)
        assertTrue(r.canOpenLog)
        assertNull(r.containingStart)
    }

    @Test
    fun `gelecek gunde hic eylem yok`() {
        val r = rules(today.plusDays(1), listOf(PeriodRecord(today.minusDays(1), null)))
        assertFalse(r.canMarkStart)
        assertFalse(r.canMarkEnd)
        assertFalse(r.canUnmark)
        assertFalse(r.canOpenLog)
    }

    @Test
    fun `acik adetin icindeki gun bitis olarak isaretlenir ve kayit kaldirilabilir`() {
        val start = today.minusDays(2)
        val r = rules(today, listOf(PeriodRecord(start, null)))
        assertFalse(r.canMarkStart)
        assertTrue(r.canMarkEnd)
        assertTrue(r.canUnmark)
        assertEquals(start, r.containingStart)
    }

    @Test
    fun `bitis zaten o gunse yeniden bitis onerilmez`() {
        val start = today.minusDays(5)
        val r = rules(today.minusDays(1), listOf(PeriodRecord(start, today.minusDays(1))))
        assertFalse(r.canMarkEnd)
        assertTrue(r.canUnmark)
    }

    @Test
    fun `acik adet varsayilan 5 gun sayilir sonrasi serbesttir`() {
        val start = today.minusDays(20)
        val records = listOf(PeriodRecord(start, null))
        assertTrue(rules(start.plusDays(4), records).canUnmark)
        val later = rules(start.plusDays(5), records)
        assertFalse(later.canUnmark)
        assertTrue(later.canMarkStart)
        // 14 günü aştığı için bitiş olarak da işaretlenemez
        assertFalse(rules(start.plusDays(15), records).canMarkEnd)
        assertTrue(rules(start.plusDays(14), records).canMarkEnd)
    }

    @Test
    fun `tamamlanmis donguye ait adet gunu dongusunu tasir`() {
        val first = today.minusDays(60)
        val second = today.minusDays(30)
        val records = listOf(PeriodRecord(first, first.plusDays(4)), PeriodRecord(second, second.plusDays(3)))
        val r = rules(first.plusDays(1), records)
        val cycle = assertNotNull(r.cycle)
        assertEquals(first, cycle.start)
        assertTrue(cycle.isComplete)
        assertEquals(30, cycle.lengthDays)
        // Son adetin döngüsü henüz tamamlanmamış
        assertFalse(assertNotNull(rules(second, records).cycle).isComplete)
    }

    @Test
    fun `adetler arasindaki gun onceki adetin bitisi olabilir yalnizca 14 gun icinde`() {
        val start = today.minusDays(10)
        val records = listOf(PeriodRecord(start, start.plusDays(3)))
        val r = rules(start.plusDays(6), records)
        assertTrue(r.canMarkStart)
        assertTrue(r.canMarkEnd)
        assertFalse(r.canUnmark)
    }
}
