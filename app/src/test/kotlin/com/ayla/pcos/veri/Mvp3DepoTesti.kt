package com.ayla.pcos.veri

import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.FertilityOverview
import com.ayla.cycle.LhResult
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.repo.isAsked
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MVP 3 depoları (ADR-0009): doğurganlık, cilt/saç profili ve rutin, doktora sorular; gerçek Room. */
class Mvp3DepoTesti : VeriTestOrtami() {

    private fun hap() = MedicationEntity(
        name = "Doğum kontrol hapı", doseText = "1 tablet", kind = MedicationKind.CONTRACEPTIVE,
        times = listOf(LocalTime.of(22, 0)), pillActiveDays = 21, pillPlaceboDays = 7, packStartDate = bugun.minusDays(10),
    )

    // --- Doğurganlık ---

    @Test
    fun `dogurganlik kaydi isiyi iki ondaliga yuvarlar ve ayni gune ikinci kayit oncekinin yerine gecer`() = calistir {
        fertility.save(FertilityEntryEntity(date = bugun, bbtCelsius = 36.575, bbtTime = LocalTime.of(6, 30), lhResult = LhResult.NEGATIVE))
        val ilk = fertility.get(bugun)
        assertNotNull(ilk)
        assertEquals(36.58, ilk.bbtCelsius)
        assertEquals(LocalTime.of(6, 30), ilk.bbtTime)
        assertEquals(LhResult.NEGATIVE, ilk.lhResult)
        assertNull(ilk.mucus)

        fertility.save(FertilityEntryEntity(date = bugun, bbtCelsius = 39.4, bbtDisturbed = true, mucus = CervicalMucus.EGG_WHITE))
        val son = fertility.get(bugun)
        assertNotNull(son)
        assertEquals(39.4, son.bbtCelsius, "aralık dışı değer reddedilmez, olduğu gibi saklanır")
        assertTrue(son.bbtDisturbed)
        assertEquals(CervicalMucus.EGG_WHITE, son.mucus)
        assertNull(son.lhResult, "yeni kayıt eskisinin üzerine yazar; alanlar birleşmez")
        assertEquals(1, db.fertilityEntryDao().getAll().size)

        fertility.delete(bugun)
        assertNull(fertility.get(bugun))
    }

    @Test
    fun `dogurganlik araligi gune gore sirali gelir`() {
        calistir {
            listOf(3L, 1L, 2L).forEach { fertility.save(FertilityEntryEntity(date = bugun.minusDays(it), bbtCelsius = 36.4)) }
            fertility.save(FertilityEntryEntity(date = bugun.minusDays(10), bbtCelsius = 36.3))
        }
        val aralik = bekle(fertility.observeRange(bugun.minusDays(5), bugun)) { it.size == 3 }
        assertEquals(listOf(bugun.minusDays(3), bugun.minusDays(2), bugun.minusDays(1)), aralik.map { it.date })
    }

    @Test
    fun `hap kullaniminda dogurganlik hesabi yapilmaz`() {
        calistir {
            cycles.addPeriodStart(bugun.minusDays(20))
            fertility.save(FertilityEntryEntity(date = bugun.minusDays(1), bbtCelsius = 36.4))
        }
        assertIs<FertilityOverview.Enabled>(calistir { fertility.analyze(bugun) })

        // Tercihte PILL hedefi.
        calistir { prefs.update { it.copy(goals = setOf(Goal.PILL)) } }
        assertIs<FertilityOverview.Disabled>(calistir { fertility.analyze(bugun) })
        assertIs<FertilityOverview.Disabled>(bekle(fertility.observeOverview()) { it is FertilityOverview.Disabled })

        // Hedef yok ama aktif doğum kontrol hapı var.
        val hapId = calistir {
            prefs.update { it.copy(goals = emptySet()) }
            meds.add(hap())
        }
        assertIs<FertilityOverview.Disabled>(calistir { fertility.analyze(bugun) })

        // Hap bırakılınca hesap geri gelir; kayıtlar silinmemiştir.
        calistir { meds.deactivate(hapId) }
        val genel = calistir { fertility.analyze(bugun) }
        assertIs<FertilityOverview.Enabled>(genel)
        assertEquals(1, genel.cycles.size)
        assertEquals(1, genel.current?.entries?.size)
    }

    @Test
    fun `dogurganlik genel bakisi adet baslangiclarina gore donguleri boler`() {
        calistir {
            cycles.addPeriodStart(bugun.minusDays(40))
            cycles.addPeriodStart(bugun.minusDays(10))
            fertility.save(FertilityEntryEntity(date = bugun.minusDays(30), bbtCelsius = 36.4))
            fertility.save(FertilityEntryEntity(date = bugun.minusDays(2), bbtCelsius = 36.5, lhResult = LhResult.POSITIVE))
        }
        val genel = bekle(fertility.observeOverview()) { it is FertilityOverview.Enabled && it.cycles.size == 2 }
        assertIs<FertilityOverview.Enabled>(genel)
        assertEquals(listOf(bugun.minusDays(40), bugun.minusDays(10)), genel.cycles.map { it.start })
        assertEquals(bugun.minusDays(11), genel.cycles.first().end)
        val simdiki = genel.current
        assertNotNull(simdiki)
        assertTrue(simdiki.isOpen)
        assertEquals(listOf(bugun.minusDays(2)), simdiki.lhPositiveDates)
    }

    // --- Cilt/saç ---

    @Test
    fun `cilt profili tek satirdir ve kaydetmek onu gunceller`() = calistir {
        assertNull(skin.profile())
        skin.saveProfile(SkinProfileEntity(skinType = SkinType.OILY, acne = AcneSelfReport.MODERATE, unwantedHair = true))
        skin.saveProfile(SkinProfileEntity(id = 7, skinType = SkinType.COMBINATION, hairThinning = true, morningReminder = LocalTime.of(8, 0)))
        val profil = skin.profile()
        assertNotNull(profil)
        assertEquals(SkinProfileEntity.SINGLE_ID, profil.id, "kimlik tek satıra sabitlenir")
        assertEquals(SkinType.COMBINATION, profil.skinType)
        assertNull(profil.acne, "profil bütün olarak yazılır")
        assertTrue(profil.hairThinning)
        assertFalse(profil.unwantedHair)
        assertEquals(LocalTime.of(8, 0), profil.morningReminder)
        assertEquals(1, db.query("SELECT COUNT(*) FROM skin_profile", null).use { it.moveToFirst(); it.getInt(0) })

        skin.clearProfile()
        assertNull(skin.profile())
    }

    @Test
    fun `rutin adimi isaretlenir, geri alinir ve bos kalan dilim silinir`() = calistir {
        assertTrue(skin.toggleStep(bugun, RoutineSlot.MORNING, "skin_step_cleanse"))
        assertTrue(skin.toggleStep(bugun, RoutineSlot.MORNING, "skin_step_spf"))
        assertTrue(skin.toggleStep(bugun, RoutineSlot.EVENING, "skin_step_cleanse"))
        assertEquals(listOf("skin_step_cleanse", "skin_step_spf"), skin.routine(bugun, RoutineSlot.MORNING)?.doneSteps)

        assertFalse(skin.toggleStep(bugun, RoutineSlot.MORNING, "skin_step_cleanse"))
        assertEquals(listOf("skin_step_spf"), skin.routine(bugun, RoutineSlot.MORNING)?.doneSteps)

        assertFalse(skin.toggleStep(bugun, RoutineSlot.MORNING, "skin_step_spf"))
        assertNull(skin.routine(bugun, RoutineSlot.MORNING), "hiç adım kalmayınca satır silinir")
        assertEquals(1, db.skinRoutineCheckDao().getAll().size, "akşam dilimi durur")
    }

    @Test
    fun `rutin akisi gunun dilimlerini verir ve aralik sorgusu gunleri kapsar`() {
        calistir {
            skin.setSteps(bugun, RoutineSlot.EVENING, listOf("skin_step_cleanse", "skin_step_cleanse", "skin_step_moisturize"))
            skin.setSteps(bugun.minusDays(1), RoutineSlot.MORNING, listOf("skin_step_spf"))
            skin.setSteps(bugun.minusDays(9), RoutineSlot.MORNING, listOf("skin_step_spf"))
        }
        val bugunku = bekle(skin.observeRoutine(bugun)) { it.isNotEmpty() }
        assertEquals(listOf(RoutineSlot.EVENING), bugunku.map { it.slot })
        assertEquals(listOf("skin_step_cleanse", "skin_step_moisturize"), bugunku.single().doneSteps, "yinelenen adım tek sayılır")
        val hafta = bekle(skin.observeRoutineRange(bugun.minusDays(6), bugun)) { it.size == 2 }
        assertEquals(listOf(bugun.minusDays(1), bugun), hafta.map { it.date })
    }

    // --- Doktora sorular ---

    @Test
    fun `soru kirpilarak eklenir, bos metin reddedilir, sordum isareti konur ve kaldirilir, silinir`() {
        val id = calistir {
            assertNull(questions.add("   "), "boş metin reddedilir")
            assertNull(questions.add(""))
            val ilk = questions.add("  Metformin dozum uygun mu?  ")
            assertNotNull(ilk)
            questions.add("İnositol ne kadar süre kullanılmalı?")
            ilk
        }
        val liste = bekle(questions.observeAll()) { it.size == 2 }
        assertEquals("Metformin dozum uygun mu?", liste.first().text)
        assertEquals(id, liste.first().id, "oluşturulma sırası korunur")
        assertFalse(liste.first().isAsked)

        calistir { questions.markAsked(id, true) }
        assertTrue(bekle(questions.observeAll()) { list -> list.any { it.id == id && it.isAsked } }.first { it.id == id }.isAsked)
        calistir { questions.markAsked(id, false) }
        assertNull(calistir { questions.get(id) }?.askedAt)
        assertFalse(calistir { questions.get(id) }!!.isAsked)

        calistir { questions.delete(id) }
        assertEquals(1, calistir { questions.getAll() }.size)
        assertNull(calistir { questions.get(id) })
    }

    @Test
    fun `cok uzun soru metni sinirlanir`() = calistir {
        val uzun = "a".repeat(2_000)
        val id = questions.add(uzun)
        assertNotNull(id)
        assertEquals(500, questions.get(id)?.text?.length)
    }
}
