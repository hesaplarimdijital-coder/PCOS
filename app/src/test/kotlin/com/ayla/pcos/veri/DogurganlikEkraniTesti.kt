package com.ayla.pcos.veri

import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.ui.fertility.FertilityViewModel
import com.ayla.pcos.ui.log.DailyLogViewModel
import com.ayla.pcos.ui.log.FertilityInput
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Doğurganlık arayüzü (#24): günlük kayıttaki alanlar ve Doğurganlık ekranının durumu. */
class DogurganlikEkraniTesti : VeriTestOrtami() {

    private fun gunluk() = kaydet(DailyLogViewModel(logs, prefs, ticker, fertility))

    private fun ekran() = kaydet(FertilityViewModel(fertility, prefs, ticker))

    private fun ttcAc() = calistir { prefs.update { it.copy(goals = it.goals + Goal.TTC) } }

    @Test
    fun `TTC kapaliyken gunluk kayitta dogurganlik bolumu yoktur`() {
        val vm = gunluk()
        vm.load(bugun)
        assertNull(bekle(vm.state) { it.loaded }.fertility)
    }

    @Test
    fun `hap modunda TTC acik olsa da bolum gizlenir`() {
        ttcAc()
        calistir {
            meds.add(MedicationEntity(name = "Hap", doseText = "1", kind = MedicationKind.CONTRACEPTIVE, times = listOf(LocalTime.of(21, 0))))
        }
        val vm = gunluk()
        vm.load(bugun)
        assertNull(bekle(vm.state) { it.loaded }.fertility)
    }

    @Test
    fun `isi LH ve mukus kaydedilir, bosaltilinca kayit silinir`() {
        ttcAc()
        val vm = gunluk()
        vm.load(bugun)
        assertNotNull(bekle(vm.state) { it.loaded }.fertility)
        vm.setBbt("36,55")
        vm.setBbtTime(LocalTime.of(6, 45))
        vm.setBbtDisturbed(true)
        vm.setLh(LhResult.POSITIVE)
        vm.setMucus(CervicalMucus.EGG_WHITE)
        vm.save {}
        val kayit = bekle(fertility.observe(bugun)) { it != null }!!
        assertEquals(36.55, kayit.bbtCelsius)
        assertEquals(LocalTime.of(6, 45), kayit.bbtTime)
        assertTrue(kayit.bbtDisturbed)
        assertEquals(LhResult.POSITIVE, kayit.lhResult)
        assertEquals(CervicalMucus.EGG_WHITE, kayit.mucus)

        // Yeniden açılınca alanlar dolu gelir (Türkçe ondalık).
        val ikinci = gunluk()
        ikinci.load(bugun)
        val girdi = bekle(ikinci.state) { it.loaded }.fertility!!
        assertEquals("36,55", girdi.bbtText)
        assertTrue(bekle(ikinci.state) { it.loaded }.hasSavedLog, "yalnızca doğurganlık kaydı olsa da silme görünür")

        ikinci.setBbt("")
        ikinci.setLh(null)
        ikinci.setMucus(CervicalMucus.EGG_WHITE) // aynı çip ikinci kez: seçim kalkar
        ikinci.save {}
        assertNull(bekle(fertility.observe(bugun)) { it == null })
    }

    @Test
    fun `gecersiz isi kaydi durdurur, aralik disi deger uyariyla kaydedilir`() {
        assertTrue(FertilityInput(bbtText = "36,5,5").bbtInvalid)
        assertFalse(FertilityInput(bbtText = "36.55").bbtInvalid)
        assertTrue(FertilityInput(bbtText = "39,4").bbtOutOfRange)
        assertNull(FertilityInput(bbtText = "  ").bbtCelsius)

        ttcAc()
        val vm = gunluk()
        vm.load(bugun)
        bekle(vm.state) { it.loaded }
        vm.setBbt("36..5")
        var kapandi = false
        vm.save { kapandi = true }
        Thread.sleep(200)
        assertFalse(kapandi, "geçersiz ısıyla kaydetme yapılmaz")
        assertNull(calistir { fertility.get(bugun) })
    }

    @Test
    fun `dogurganlik ekrani hap modunda kapali, olcumle donguyu gosterir ve donguler arasinda gezinir`() {
        ttcAc()
        calistir {
            listOf(40L, 12L).forEach { cycles.addPeriodStart(bugun.minusDays(it)) }
            fertility.save(FertilityEntryEntity(date = bugun.minusDays(30), bbtCelsius = 36.4))
            (0L until 10L).forEach { d -> fertility.save(FertilityEntryEntity(date = bugun.minusDays(11 - d), bbtCelsius = 36.3 + d * 0.02)) }
        }
        val vm = ekran()
        val durum = bekle(vm.state) { it.loaded && it.cycles.size == 2 }
        assertEquals(1, durum.selected, "varsayılan en yeni döngü")
        assertTrue(durum.showsCurrent)
        assertEquals(13, durum.cycleDay)
        assertTrue(durum.canGoPrevious)
        assertFalse(durum.canGoNext)

        vm.previous()
        val onceki = bekle(vm.state) { it.selected == 0 }
        assertFalse(onceki.showsCurrent)
        assertEquals(1, onceki.cycle!!.readings.size)
        vm.next()
        bekle(vm.state) { it.selected == 1 }

        calistir { prefs.update { it.copy(goals = it.goals + Goal.PILL) } }
        assertTrue(bekle(vm.state) { it.disabled }.cycles.isEmpty())
    }
}
