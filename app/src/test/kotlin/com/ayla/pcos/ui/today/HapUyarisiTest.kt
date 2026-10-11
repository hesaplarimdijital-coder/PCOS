package com.ayla.pcos.ui.today

import com.ayla.cycle.PillRegimen
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals

class HapUyarisiTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 10, 12)

    private fun pill(todayPill: Int, taken: Set<Int>, addedDaysAgo: Long = 40, regimen: PillRegimen = PillRegimen.R24_4) = PillModeState(
        medication = MedicationEntity(
            name = "Hap", doseText = "1", kind = MedicationKind.CONTRACEPTIVE, times = listOf(LocalTime.of(22, 0)),
            createdAt = today.minusDays(addedDaysAgo).atStartOfDay(zone).toInstant(),
        ),
        regimen = regimen,
        todayPill = todayPill,
        isPlacebo = todayPill > regimen.activeDays,
        takenPills = taken,
        packStart = today.minusDays(todayPill - 1L),
    )

    @Test
    fun `dunku etken hap isaretlenmediyse once o soylenir`() =
        assertEquals(HapUyarisi.DUN_ISARETLENMEDI, hapUyarisi(pill(12, (1..10).toSet()), today, LocalTime.of(23, 0), zone))

    @Test
    fun `ilac dun eklenmediyse dun denetlenmez`() =
        assertEquals(HapUyarisi.YOK, hapUyarisi(pill(12, emptySet(), addedDaysAgo = 0), today, LocalTime.of(9, 0), zone))

    @Test
    fun `paketin ilk gunu dun denetlenmez`() =
        assertEquals(HapUyarisi.YOK, hapUyarisi(pill(1, emptySet()), today, LocalTime.of(9, 0), zone))

    @Test
    fun `plasebo gunu bilgi verilir`() =
        assertEquals(HapUyarisi.PLASEBO, hapUyarisi(pill(26, (1..25).toSet()), today, LocalTime.of(23, 0), zone))

    @Test
    fun `dunku plasebo hapi isaretlenmese de uyarilmaz`() =
        assertEquals(HapUyarisi.PLASEBO, hapUyarisi(pill(27, (1..24).toSet()), today, LocalTime.of(9, 0), zone))

    @Test
    fun `saat gecince bugunku hap bekliyor`() {
        assertEquals(HapUyarisi.YOK, hapUyarisi(pill(12, (1..11).toSet()), today, LocalTime.of(21, 0), zone))
        assertEquals(HapUyarisi.BUGUN_BEKLIYOR, hapUyarisi(pill(12, (1..11).toSet()), today, LocalTime.of(22, 30), zone))
        assertEquals(HapUyarisi.YOK, hapUyarisi(pill(12, (1..12).toSet()), today, LocalTime.of(22, 30), zone))
    }
}
