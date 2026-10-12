package com.ayla.pcos.reminders

import com.ayla.pcos.R
import com.ayla.pcos.ui.components.oemBatteryNote
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Hatırlatma günlüğü özeti ve üretici notu seçimi (#17): saf kurallar. */
class AlarmLogKurallariTest {

    private val simdi = Instant.parse("2026-10-12T07:00:00Z")

    private fun kayit(saatOnce: Long, gecikmeSn: Long, exact: Boolean = true): AlarmLogEntry {
        val planned = simdi.minus(Duration.ofHours(saatOnce))
        return AlarmLogEntry(ReminderKind.DOSE, planned, planned.plusSeconds(gecikmeSn), exact)
    }

    @Test
    fun `bos gunlukte ozet sifirdir`() {
        assertEquals(AlarmLogSummary(0, Duration.ZERO, Duration.ZERO, 0), summarizeAlarmLog(emptyList(), simdi))
    }

    @Test
    fun `ozet son 48 saati sayar, erken calma sifir gecikmedir`() {
        val kayitlar = listOf(
            kayit(saatOnce = 60, gecikmeSn = 7200), // pencere dışı
            kayit(saatOnce = 30, gecikmeSn = 0),
            kayit(saatOnce = 20, gecikmeSn = 30),
            kayit(saatOnce = 10, gecikmeSn = 15 * 60, exact = false),
            kayit(saatOnce = 2, gecikmeSn = -5), // saat geri alındı
        )
        val ozet = summarizeAlarmLog(kayitlar, simdi)
        assertEquals(4, ozet.count)
        assertEquals(Duration.ofMillis((30_000L + 900_000L) / 4), ozet.averageDelay)
        assertEquals(Duration.ofMinutes(15), ozet.maxDelay)
        assertEquals(1, ozet.lateCount)
    }

    @Test
    fun `tam 10 dakika gecikme gec sayilmaz, 10 dakikayi asan sayilir`() {
        val ozet = summarizeAlarmLog(listOf(kayit(5, 600), kayit(4, 601)), simdi)
        assertEquals(1, ozet.lateCount)
    }

    @Test
    fun `uretici notu marka adina gore secilir, bilinmeyen markada not yok`() {
        assertEquals(R.string.battery_oem_xiaomi, oemBatteryNote("Xiaomi"))
        assertEquals(R.string.battery_oem_xiaomi, oemBatteryNote("POCO"))
        assertEquals(R.string.battery_oem_samsung, oemBatteryNote(" samsung "))
        assertEquals(R.string.battery_oem_huawei, oemBatteryNote("HONOR"))
        assertEquals(R.string.battery_oem_oppo, oemBatteryNote("OnePlus"))
        assertEquals(R.string.battery_oem_vivo, oemBatteryNote("vivo"))
        assertNull(oemBatteryNote("Google"))
        assertNull(oemBatteryNote(""))
    }
}
