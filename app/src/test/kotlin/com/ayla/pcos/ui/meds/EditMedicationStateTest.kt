package com.ayla.pcos.ui.meds

import com.ayla.cycle.PillRegimen
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditMedicationStateTest {

    private val today = LocalDate.of(2026, 10, 12)
    private val base = MedicationEntity(
        id = 7, name = "Inofolic Combi", doseText = "1 kapsül", kind = MedicationKind.SUPPLEMENT,
        form = MedicationForm.SACHET, times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)), withFood = true,
        stockCount = 20, createdAt = Instant.parse("2026-09-01T10:00:00Z"),
    )

    @Test
    fun `ad bossa kaydedilemez`() {
        val s = EditMedicationState(loaded = true, name = "  ", today = today)
        assertFalse(s.canSave)
        assertNull(s.toEntity(null))
    }

    @Test
    fun `hapta paket baslangici olmadan kaydedilemez`() {
        val s = EditMedicationState(loaded = true, name = "Hap", kind = MedicationKind.CONTRACEPTIVE, packStart = null, today = today)
        assertFalse(s.canSave)
        assertTrue(s.copy(packStart = today).canSave)
    }

    @Test
    fun `yeni kayit varsayilan doz ve sirali benzersiz saatlerle olusur`() {
        val s = EditMedicationState(
            loaded = true, name = " Metformin ", doseText = "", kind = MedicationKind.PRESCRIPTION,
            times = listOf(LocalTime.of(20, 30), LocalTime.of(8, 30), LocalTime.of(20, 30)),
            food = YemekTercihi.YEMEKLE, stock = "60", today = today,
        )
        val e = assertNotNull(s.toEntity(null))
        assertEquals(0L, e.id)
        assertEquals("Metformin", e.name)
        assertEquals(EditMedicationState.DEFAULT_DOSE, e.doseText)
        assertEquals(listOf(LocalTime.of(8, 30), LocalTime.of(20, 30)), e.times)
        assertEquals(true, e.withFood)
        assertEquals(60, e.stockCount)
        assertEquals(MedicationForm.CAPSULE, e.form)
        assertNull(e.pillActiveDays)
        assertNull(e.packStartDate)
    }

    @Test
    fun `duzenleme kimligi olusturma zamanini ve bicimi korur`() {
        val s = EditMedicationState.from(base, today).copy(doseText = "2 kapsül", food = YemekTercihi.FARK_ETMEZ, stock = "")
        val e = assertNotNull(s.toEntity(base))
        assertEquals(7L, e.id)
        assertEquals(base.createdAt, e.createdAt)
        assertEquals(MedicationForm.SACHET, e.form)
        assertEquals("2 kapsül", e.doseText)
        assertNull(e.withFood)
        assertNull(e.stockCount)
    }

    @Test
    fun `tur degisince bicim yeni ture gore secilir`() {
        val s = EditMedicationState.from(base, today).copy(kind = MedicationKind.HERBAL)
        assertEquals(MedicationForm.TEA, assertNotNull(s.toEntity(base)).form)
    }

    @Test
    fun `hap kaydi duzen ve paket bilgisini tasir, yemek tercihi tasimaz`() {
        val s = EditMedicationState(
            loaded = true, name = "Yasmin", kind = MedicationKind.CONTRACEPTIVE, times = listOf(LocalTime.of(22, 0)),
            food = YemekTercihi.YEMEKLE, regimen = PillRegimen.R21_7, packStart = today.minusDays(3), today = today,
        )
        val e = assertNotNull(s.toEntity(null))
        assertEquals(21, e.pillActiveDays)
        assertEquals(7, e.pillPlaceboDays)
        assertEquals(today.minusDays(3), e.packStartDate)
        assertNull(e.withFood)
        assertEquals(MedicationForm.TABLET, e.form)
    }

    @Test
    fun `haptan takviyeye donunce paket alanlari temizlenir`() {
        val pill = base.copy(kind = MedicationKind.CONTRACEPTIVE, pillActiveDays = 24, pillPlaceboDays = 4, packStartDate = today)
        val s = EditMedicationState.from(pill, today).copy(kind = MedicationKind.SUPPLEMENT)
        val e = assertNotNull(s.toEntity(pill))
        assertNull(e.pillActiveDays)
        assertNull(e.pillPlaceboDays)
        assertNull(e.packStartDate)
    }

    @Test
    fun `stok uyari esigi sinirlar icinde tutulur`() {
        val s = EditMedicationState.from(base, today).copy(stockWarnDays = 99)
        assertEquals(EditMedicationState.STOCK_WARN_RANGE.last, assertNotNull(s.toEntity(base)).stockWarnDays)
    }

    @Test
    fun `kayittan form durumu dogru okunur`() {
        val s = EditMedicationState.from(base, today)
        assertEquals("20", s.stock)
        assertEquals(YemekTercihi.YEMEKLE, s.food)
        assertFalse(s.isNew)
        assertEquals(YemekTercihi.AC_KARNA, false.toYemekTercihi())
        assertEquals(YemekTercihi.FARK_ETMEZ, null.toYemekTercihi())
    }

    @Test
    fun `inositol urunleri taninir`() {
        assertTrue(isInositol("Inofolic Combi"))
        assertTrue(isInositol("Myo-inositol"))
        assertFalse(isInositol("D vitamini"))
    }
}
