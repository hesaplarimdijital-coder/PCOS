package com.ayla.pcos.ui.plan

import com.ayla.pcos.data.db.PeriodEntity
import org.junit.Test
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanKurallariTest {

    private val bugun = LocalDate.of(2026, 10, 12)

    @Test
    fun `kayitli adet araligi ve hafif ustu kanama adet gunu sayilir, tahmin sayilmaz`() {
        val acik = listOf(PeriodEntity(startDate = bugun.minusDays(2)))
        assertTrue(isRecordedPeriodDay(bugun, acik, todayFlow = null), "açık adetin 3. günü")
        assertFalse(isRecordedPeriodDay(bugun, listOf(PeriodEntity(startDate = bugun.minusDays(5))), null), "açık adet 5 gün sayılır")
        val kapali = listOf(PeriodEntity(startDate = bugun.minusDays(8), endDate = bugun.minusDays(1)))
        assertFalse(isRecordedPeriodDay(bugun, kapali, null), "bitişi dün olan adet")
        assertTrue(isRecordedPeriodDay(bugun.minusDays(1), kapali, null))
        assertTrue(isRecordedPeriodDay(bugun, emptyList(), todayFlow = 2), "hafif kanama kaydı")
        assertFalse(isRecordedPeriodDay(bugun, emptyList(), todayFlow = 1), "lekelenme adet günü sayılmaz")
        assertFalse(isRecordedPeriodDay(bugun, listOf(PeriodEntity(startDate = bugun.plusDays(1))), null), "gelecek kayıt")
    }

    @Test
    fun `riza kapali ya da hap modunda herkes genel karti gorur`() {
        assertEquals(MovementCycleCard.GENERAL, movementCycleCard(consentPersonalization = false, pillMode = false, periodDay = true))
        assertEquals(MovementCycleCard.GENERAL, movementCycleCard(consentPersonalization = true, pillMode = true, periodDay = true))
        assertEquals(MovementCycleCard.PERIOD, movementCycleCard(consentPersonalization = true, pillMode = false, periodDay = true))
        assertEquals(MovementCycleCard.OTHER, movementCycleCard(consentPersonalization = true, pillMode = false, periodDay = false))
    }

    @Test
    fun `plan iceriginde yargilayan ya da kilo odakli soz yok`() {
        // docs/arastirma/plan-icerigi.md "Dil" kuralı; "diyetisyen" meslek adı istisnadır.
        val metin = File("src/main/res/values/strings_plan.xml").readText().lowercase()
            .replace("diyetisyen", "")
        val yasakli = listOf("kalori", "kilo", "yağ yakma", "ideal", "diyet", "hedef", "kaçırdın", "yasak", "zayıflama", "detoks")
        val bulunan = yasakli.filter { it in metin }
        assertTrue(bulunan.isEmpty(), "yasaklı sözcük: $bulunan")
    }

    @Test
    fun `her icerik kartinin kaynagi ve bitkisel kartlarin dort alani var`() {
        val kartlar = PlanIcerigi.nutritionPrinciples + PlanIcerigi.movementWeekly + PlanIcerigi.herbalExcluded +
            PlanIcerigi.routines.map { it.card } + listOf(PlanIcerigi.nutritionIntro, PlanIcerigi.plate, PlanIcerigi.walkTip, PlanIcerigi.eatingSupport, PlanIcerigi.movementSafety)
        assertTrue(kartlar.all { it.source != null && it.evidence != null })
        assertTrue(PlanIcerigi.herbal.all { it.cautions.isNotEmpty() })
        assertTrue(PlanIcerigi.routines.all { it.steps.size in 3..6 && it.minutes > 0 })
    }
}
