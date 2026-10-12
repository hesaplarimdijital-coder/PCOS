package com.ayla.pcos.widget

import com.ayla.cycle.CycleEngine
import com.ayla.cycle.PeriodRecord
import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.repo.DoseItem
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Widget durum kuralları (#14): gizlilik, başlık, sıradaki doz, su. Saf kurallar. */
class WidgetStateTest {

    private val bugun = LocalDate.of(2026, 10, 12)
    private val ayarlar = UserPreferences(onboardingDone = true, healthDataConsentAt = Instant.parse("2026-09-01T08:00:00Z"), waterGoalCups = 8)
    private val inofolic = MedicationEntity(
        id = 1, name = "Inofolic Combi", doseText = "1 kapsül", kind = MedicationKind.SUPPLEMENT,
        times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
    )

    private fun doz(saat: Int, durum: DoseStatus? = null, ilac: MedicationEntity = inofolic) = DoseItem(
        medication = ilac,
        date = bugun,
        time = LocalTime.of(saat, 0),
        event = durum?.let { DoseEventEntity(medicationId = ilac.id, date = bugun, time = LocalTime.of(saat, 0), status = it) },
    )

    private fun durum(
        prefs: UserPreferences = ayarlar,
        meds: List<MedicationEntity> = listOf(inofolic),
        doses: List<DoseItem> = listOf(doz(9, DoseStatus.TAKEN), doz(21)),
        saat: Int = 15,
        adetler: List<PeriodRecord> = listOf(PeriodRecord(bugun.minusDays(22), bugun.minusDays(18))),
    ): WidgetState = buildWidgetState(
        prefs = prefs,
        overview = CycleEngine().overview(adetler, bugun),
        activeMedications = meds,
        todayDoses = doses,
        waterCups = 5,
        now = bugun.atTime(saat, 0),
    )

    @Test
    fun `uygulama kilidi ya da gizli bildirimler acikken notr icerik gosterilir`() {
        assertEquals(WidgetState.Neutral, durum(prefs = ayarlar.copy(appLockEnabled = true)))
        assertEquals(WidgetState.Neutral, durum(prefs = ayarlar.copy(hideNotificationContent = true)))
    }

    @Test
    fun `karsilama bitmemisse ya da riza yoksa notr icerik gosterilir`() {
        assertEquals(WidgetState.Neutral, durum(prefs = ayarlar.copy(onboardingDone = false)))
        assertEquals(WidgetState.Neutral, durum(prefs = ayarlar.copy(healthDataConsentAt = null)))
    }

    @Test
    fun `dongu gunu, siradaki doz ve su gosterilir`() {
        val s = assertIs<WidgetState.Ready>(durum())
        assertEquals(WidgetHeadline.CycleDay(23), s.headline)
        val next = s.nextDose!!
        assertEquals(LocalTime.of(21, 0), next.time)
        assertEquals("Inofolic Combi", next.name)
        assertFalse(next.overdue)
        assertEquals(5, s.waterCups)
        assertEquals(8, s.waterGoal)
    }

    @Test
    fun `saati gecmis isaretlenmemis doz once gelir ve bekliyor olarak isaretlenir`() {
        val s = assertIs<WidgetState.Ready>(durum(doses = listOf(doz(9), doz(21)), saat = 15))
        assertEquals(LocalTime.of(9, 0), s.nextDose?.time)
        assertTrue(s.nextDose!!.overdue)
    }

    @Test
    fun `butun dozlar islenmisse siradaki doz yok, doz plani yoksa da yok`() {
        val tamam = assertIs<WidgetState.Ready>(durum(doses = listOf(doz(9, DoseStatus.TAKEN), doz(21, DoseStatus.SKIPPED))))
        assertNull(tamam.nextDose)
        assertTrue(tamam.hasDosesToday)
        val bos = assertIs<WidgetState.Ready>(durum(meds = emptyList(), doses = emptyList()))
        assertNull(bos.nextDose)
        assertFalse(bos.hasDosesToday)
    }

    @Test
    fun `hap kullaniminda dongu gunu yerine paket gunu gosterilir`() {
        val hap = MedicationEntity(
            id = 2, name = "Hap", doseText = "1 tablet", kind = MedicationKind.CONTRACEPTIVE, times = listOf(LocalTime.of(22, 0)),
            pillActiveDays = 24, pillPlaceboDays = 4, packStartDate = bugun.minusDays(25),
        )
        val s = assertIs<WidgetState.Ready>(durum(meds = listOf(inofolic, hap)))
        assertEquals(WidgetHeadline.PillDay(26, placebo = true), s.headline)
    }

    @Test
    fun `paket baslangici gelecekteyse ve adet kaydi yoksa kayit yok basligi gosterilir`() {
        val hap = MedicationEntity(
            id = 2, name = "Hap", doseText = "1 tablet", kind = MedicationKind.CONTRACEPTIVE, times = listOf(LocalTime.of(22, 0)),
            pillActiveDays = 21, pillPlaceboDays = 7, packStartDate = bugun.plusDays(3),
        )
        val s = assertIs<WidgetState.Ready>(durum(meds = listOf(hap), adetler = emptyList()))
        assertEquals(WidgetHeadline.NoRecord, s.headline)
    }

    @Test
    fun `ayni saat ani bekliyor sayilmaz`() {
        val s = assertIs<WidgetState.Ready>(
            buildWidgetState(ayarlar, null, listOf(inofolic), listOf(doz(21)), 0, LocalDateTime.of(bugun, LocalTime.of(21, 0))),
        )
        assertFalse(s.nextDose!!.overdue)
    }
}
