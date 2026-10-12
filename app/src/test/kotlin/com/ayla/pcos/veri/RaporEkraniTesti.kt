package com.ayla.pcos.veri

import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.report.DoctorReportBuilder
import com.ayla.pcos.report.PdfReportWriter
import com.ayla.pcos.ui.report.QuestionsViewModel
import com.ayla.pcos.ui.report.RangeChoice
import com.ayla.pcos.ui.report.ReportEvent
import com.ayla.pcos.ui.report.ReportSectionChoice
import com.ayla.pcos.ui.report.ReportViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Doktor raporu ekranı ve sorular (#26): seçenekler, özel aralık, hap modu, sayfa sayısı, metin paylaşımı. */
class RaporEkraniTesti : VeriTestOrtami() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun kapsamiKapat() = appScope.cancel()

    private fun builder() = DoctorReportBuilder(
        context, cycles, logs, db.medicationDao(), db.doseEventDao(), sleep, water, fertility, skin, questions, prefs, ticker,
    )

    private fun rapor() = kaydet(
        ReportViewModel(builder(), PdfReportWriter(context, Clock.systemDefaultZone()), fertility, questions, ticker, appScope),
    )

    @Test
    fun `hazir araliklar bugunden geriye sayar, ozel aralik bugunu ve ters secimi duzeltir`() {
        val vm = rapor()
        val ilk = bekle(vm.state) { it.loaded }
        assertEquals(RangeChoice.DAYS_90, ilk.range)
        assertEquals(bugun.minusDays(89), ilk.from)
        assertEquals(bugun, ilk.to)

        vm.setRange(RangeChoice.DAYS_28)
        assertEquals(bugun.minusDays(27), vm.state.value.options().from)

        vm.setCustomFrom(bugun.minusDays(10))
        vm.setCustomTo(bugun.plusDays(5))
        val ozel = vm.state.value
        assertEquals(RangeChoice.CUSTOM, ozel.range)
        assertEquals(bugun, ozel.to, "bitiş bugünü geçemez")
        vm.setCustomTo(bugun.minusDays(20))
        assertEquals(bugun.minusDays(20), vm.state.value.from, "bitiş başlangıçtan önceyse başlangıç kayar")
    }

    @Test
    fun `bolum anahtarlari secenekleri degistirir, hap modunda dogurganlik kapali`() {
        calistir { prefs.update { it.copy(goals = it.goals + Goal.PILL) } }
        val vm = rapor()
        val durum = bekle(vm.state) { it.loaded }
        assertFalse(durum.fertilityAvailable)
        assertFalse(durum.options().includeFertility)
        vm.toggleSection(ReportSectionChoice.MEDS)
        assertFalse(vm.state.value.options().includeMeds)
        vm.toggleSection(ReportSectionChoice.MEDS)
        assertTrue(vm.state.value.options().includeMeds)
    }

    @Test
    fun `sayfa sayisi hesaplanir ve metin paylasimi rapor metnini tasir`() {
        calistir { questions.add("Tahlillerimi ne sıklıkla yaptırmalıyım?") }
        val vm = rapor()
        val durum = bekle(vm.state, timeoutMs = 10_000) { it.loaded && it.pageCount != null && it.openQuestions == 1 }
        assertTrue(durum.pageCount!! >= 1)

        // Olaylar tekrar oynatılmaz: dinleyici paylaşımdan önce abone olur (ekranda LaunchedEffect gibi).
        val olay = calistir {
            coroutineScope {
                val bekleyen = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(5_000) { vm.events.first() } }
                vm.shareText("Rapor")
                bekleyen.await()
            }
        }
        val metin = assertNotNull(olay as? ReportEvent.ShareText).text
        assertTrue("Tahlillerimi ne sıklıkla yaptırmalıyım?" in metin, metin)
    }

    @Test
    fun `sorular eklenir, bos taslak eklenmez, isaretlenir ve silinir`() {
        val vm = kaydet(QuestionsViewModel(questions, appScope))
        bekle(vm.state) { it.loaded }
        vm.setDraft("   ")
        assertFalse(bekle(vm.state) { it.draft == "   " }.canAdd)
        vm.add()
        vm.useSuggestion("Takviyelerimi sürdürmeli miyim?")
        assertEquals("Takviyelerimi sürdürmeli miyim?", bekle(vm.state) { it.draft.isNotBlank() }.draft)
        vm.add()
        val acik = bekle(vm.state) { it.open.size == 1 }
        assertEquals("", acik.draft)
        val id = acik.open.single().id

        vm.setAsked(id, true)
        assertEquals(id, bekle(vm.state) { it.asked.size == 1 }.asked.single().id)
        vm.setAsked(id, false)
        bekle(vm.state) { it.open.size == 1 && it.asked.isEmpty() }
        vm.delete(id)
        bekle(vm.state) { it.open.isEmpty() }

        vm.setDraft("x".repeat(600))
        assertEquals(500, bekle(vm.state) { it.draft.isNotEmpty() }.draft.length)
    }
}
