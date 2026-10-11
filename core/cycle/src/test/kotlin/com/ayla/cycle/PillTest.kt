package com.ayla.cycle

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PillTest {

    private val packStart = LocalDate.parse("2026-09-27")

    @Test
    fun `24 4 duzeninde 14 hap etken 25 hap plasebo`() {
        val day14 = PillRegimen.R24_4.statusOn(packStart, packStart.plusDays(13))
        assertEquals(14, day14.pillNumber)
        assertFalse(day14.isPlacebo)
        assertEquals(1, day14.packNumber)
        assertEquals(15, day14.daysUntilNextPack)

        val day25 = PillRegimen.R24_4.statusOn(packStart, packStart.plusDays(24))
        assertEquals(25, day25.pillNumber)
        assertTrue(day25.isPlacebo)
    }

    @Test
    fun `21 7 duzeninde paket 28 gunde yenilenir`() {
        val next = PillRegimen.R21_7.statusOn(packStart, packStart.plusDays(28))
        assertEquals(1, next.pillNumber)
        assertEquals(2, next.packNumber)
        assertFalse(next.isPlacebo)
        val last = PillRegimen.R21_7.statusOn(packStart, packStart.plusDays(27))
        assertEquals(28, last.pillNumber)
        assertTrue(last.isPlacebo)
        assertEquals(1, last.daysUntilNextPack)
    }

    @Test
    fun `surekli duzende plasebo yoktur`() {
        for (offset in 0 until 56) {
            assertFalse(PillRegimen.CONTINUOUS_28.statusOn(packStart, packStart.plusDays(offset.toLong())).isPlacebo)
        }
    }

    @Test
    fun `paket baslangicindan onceki tarih reddedilir`() {
        assertFailsWith<IllegalArgumentException> {
            PillRegimen.R24_4.statusOn(packStart, packStart.minusDays(1))
        }
    }

    @Test
    fun `gecersiz duzen reddedilir`() {
        assertFailsWith<IllegalArgumentException> { PillRegimen(0, 7) }
        assertFailsWith<IllegalArgumentException> { PillRegimen(21, -1) }
    }
}
