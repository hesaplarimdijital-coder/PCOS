package com.ayla.pcos.data.skin

import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.SkinType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Rutin seçim kuralları (#25, cilt-sac-icerigi.md §3.2–3.5): güvenlik süzgeçleri ve tablo sırası. */
class SkinRoutineRulesTest {

    private fun plan(
        skinType: SkinType? = SkinType.NORMAL,
        acne: AcneSelfReport? = AcneSelfReport.NONE,
        darkSpots: Boolean = false,
        pregnancyPlan: Boolean = false,
        pregnant: Boolean = false,
        acanthosis: Boolean = false,
        unwantedHair: Boolean = false,
        hairThinning: Boolean = false,
    ) = SkinRoutineRules.plan(
        SkinRoutineInput(
            skinType = skinType, acne = acne, darkSpots = darkSpots, acanthosis = acanthosis, unwantedHair = unwantedHair,
            hairThinning = hairThinning, pregnancyPlan = pregnancyPlan, pregnantOrBreastfeeding = pregnant,
        ),
    )

    @Test
    fun `sorunsuz normal cilt yalnizca temel adimlari alir`() {
        val p = plan()
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.MOISTURIZER, RoutineStep.SUNSCREEN), p.morning)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.MOISTURIZER), p.evening)
        assertTrue(p.avoid.isEmpty())
        assertTrue(p.notes.isEmpty())
        assertTrue(p.doctorCards.isEmpty())
        assertEquals(listOf(RoutineTip.HANDS_OFF), p.tips)
    }

    @Test
    fun `yagli ciltte sabah niasinamid, akne hafifse aksam salisilik ve azelaik`() {
        val p = plan(skinType = SkinType.OILY, acne = AcneSelfReport.MILD)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.NIACINAMIDE, RoutineStep.MOISTURIZER, RoutineStep.SUNSCREEN), p.morning)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.SALICYLIC, RoutineStep.AZELAIC, RoutineStep.MOISTURIZER), p.evening)
        assertTrue(RoutineNote.ONE_ACTIVE_PER_NIGHT in p.notes, "iki aktif: tek aktif notu")
        assertEquals(listOf(RoutineTip.HANDS_OFF, RoutineTip.HAIR_OILY), p.tips)
    }

    @Test
    fun `gebelik plani salisilik asidi cikarir, kacinilacaklar listesini acar, azelaik kalir`() {
        val p = plan(skinType = SkinType.COMBINATION, acne = AcneSelfReport.MODERATE, pregnancyPlan = true)
        assertFalse(RoutineStep.SALICYLIC in p.evening)
        assertTrue(RoutineStep.AZELAIC in p.evening)
        assertEquals(AvoidItem.entries.toList(), p.avoid, "hepsi açıkça listelenir, KEPT en sonda")
        assertEquals(AvoidItem.KEPT, p.avoid.last())
        assertTrue(RoutineNote.TTC_NIACINAMIDE in p.notes, "sabah niasinamid kartına gebelik notu")
        assertTrue(DoctorCard.PREGNANCY in p.doctorCards)
        // Gebelik/emzirme işareti de aynı süzgeci açar.
        assertEquals(p.avoid, plan(skinType = SkinType.COMBINATION, acne = AcneSelfReport.MODERATE, pregnant = true).avoid)
    }

    @Test
    fun `lekede hassas olmayan cilt azelaik niasinamid ve AHA alir, gebelikte AHA ve salisilik cikar`() {
        val p = plan(skinType = SkinType.NORMAL, darkSpots = true)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.AZELAIC, RoutineStep.NIACINAMIDE, RoutineStep.AHA, RoutineStep.MOISTURIZER), p.evening)
        assertTrue(RoutineStep.NIACINAMIDE in p.morning)
        val gebe = plan(skinType = SkinType.NORMAL, darkSpots = true, pregnancyPlan = true)
        assertFalse(RoutineStep.AHA in gebe.evening, "sürülüp bırakılan asitler ihtiyaten çıkar (skin_avoid_kept ile tutarlı)")
        assertFalse(RoutineStep.SALICYLIC in gebe.evening)
        assertTrue(RoutineStep.AZELAIC in gebe.evening && RoutineStep.NIACINAMIDE in gebe.evening)
        assertTrue(RoutineNote.TTC_NIACINAMIDE in gebe.notes)
    }

    @Test
    fun `hassas ciltte AHA ve salisilik yok, yama testi notu var`() {
        val p = plan(skinType = SkinType.SENSITIVE, acne = AcneSelfReport.MILD, darkSpots = true)
        assertEquals(RoutineStep.CLEANSER_DRY, p.morning.first())
        assertEquals(RoutineStep.SUNSCREEN_SENSITIVE, p.morning.last())
        assertFalse(RoutineStep.AHA in p.evening)
        assertFalse(RoutineStep.SALICYLIC in p.evening)
        assertTrue(RoutineStep.NIACINAMIDE in p.evening)
        assertTrue(RoutineNote.PATCH_TEST_FIRST in p.notes)
    }

    @Test
    fun `yogun aknede aktif icerik yok, dermatolog karti ustte`() {
        val p = plan(skinType = SkinType.OILY, acne = AcneSelfReport.SEVERE, darkSpots = true)
        assertTrue(p.activesSuppressed)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.MOISTURIZER), p.evening)
        assertFalse(RoutineStep.NIACINAMIDE in p.morning, "yoğun aknede sabah niasinamid de yok")
        assertEquals(DoctorCard.ACNE_SEVERE, p.doctorCards.first())
        assertTrue(p.notes.isEmpty())
    }

    @Test
    fun `doktor kartlari aciliyet sirasinda ve profile baglidir`() {
        val p = plan(acne = AcneSelfReport.SEVERE, acanthosis = true, unwantedHair = true, hairThinning = true, pregnant = true)
        assertEquals(
            listOf(DoctorCard.VIRILIZATION, DoctorCard.ACNE_SEVERE, DoctorCard.ACANTHOSIS, DoctorCard.HAIR_LOSS, DoctorCard.PREGNANCY),
            p.doctorCards,
        )
        assertTrue(plan(acanthosis = true).doctorCards == listOf(DoctorCard.ACANTHOSIS))
    }

    @Test
    fun `kuru ciltte sabah su ile temizlik, leke yoksa aktif yok, adimlar yinelenmez`() {
        val p = plan(skinType = SkinType.DRY)
        assertEquals(listOf(RoutineStep.CLEANSER_DRY, RoutineStep.MOISTURIZER, RoutineStep.SUNSCREEN), p.morning)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.MOISTURIZER), p.evening)
        RoutineStep.entries.forEach { step -> assertEquals(step, RoutineStep.fromKey(step.key)) }
        assertEquals(null, RoutineStep.fromKey("retinol"), "rutinde retinoid anahtarı yok")
        val tekrar = SkinRoutineRules.plan(SkinRoutineInput(skinType = SkinType.DRY))
        assertEquals(p, tekrar, "belirleyici")
    }
}
