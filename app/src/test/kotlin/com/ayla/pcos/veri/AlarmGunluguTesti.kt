package com.ayla.pcos.veri

import android.app.AlarmManager
import android.content.Context
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.reminders.AlarmLog
import com.ayla.pcos.reminders.AlarmLogEntry
import com.ayla.pcos.reminders.ReminderKind
import com.ayla.pcos.reminders.ReminderScheduler
import com.ayla.pcos.ui.me.AlarmLogText
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Hatırlatma günlüğü (#17): kalıcılık, alarm niyetindeki planlanan an ve paylaşım metni. */
class AlarmGunluguTesti : VeriTestOrtami() {

    private val dokuz: Instant get() = bugun.atTime(9, 0).atZone(zone).toInstant()

    private fun kayit(planned: Instant, gecikmeSn: Long, kind: ReminderKind = ReminderKind.DOSE) =
        AlarmLogEntry(kind, planned, planned.plusSeconds(gecikmeSn), exact = true)

    @Test
    fun `gunluk kayitlari sirayla saklar, en eski kayitlar sinirda duser`() {
        val log = AlarmLog(context)
        repeat(AlarmLog.MAX_ENTRIES + 5) { i -> log.record(kayit(dokuz.plusSeconds(i * 60L), gecikmeSn = 3)) }
        val okunan = log.read()
        assertEquals(AlarmLog.MAX_ENTRIES, okunan.size)
        assertEquals(dokuz.plusSeconds(5 * 60L), okunan.first().scheduledAt)
        assertEquals(Duration.ofSeconds(3), okunan.last().delay)
        log.clear()
        assertTrue(log.read().isEmpty())
    }

    @Test
    fun `bozuk satirlar atlanir, diger kayitlar okunur`() {
        val ms = dokuz.toEpochMilli()
        context.getSharedPreferences("ayla_alarm_log", Context.MODE_PRIVATE).edit()
            .putString("entries", "DOSE|$ms|${ms + 4000}|1\nbozuk\nYOK|1|2|0\nWATER|$ms|abc|0\nEVENING|$ms|${ms + 1000}|0")
            .commit()
        val okunan = AlarmLog(context).read()
        assertEquals(listOf(ReminderKind.DOSE, ReminderKind.EVENING), okunan.map { it.kind })
        assertEquals(false, okunan.last().exact)
    }

    @Suppress("DEPRECATION") // ScheduledAlarm.operation Robolectric'te eskimiş ama erişimcisi yok
    @Test
    fun `alarm niyeti planlanan ani ve alarm turunu tasir`() {
        calistir {
            meds.add(MedicationEntity(name = "D", doseText = "1", kind = MedicationKind.SUPPLEMENT, times = listOf(LocalTime.of(13, 0))))
            scheduler.rescheduleAll()
        }
        val am = context.getSystemService(AlarmManager::class.java)
        val doz = shadowOf(am).scheduledAlarms.first { alarm ->
            alarm.operation?.let { shadowOf(it).savedIntent.dataString?.contains("/dose/") } == true
        }
        val niyet = shadowOf(requireNotNull(doz.operation)).savedIntent
        val beklenen = bugun.atTime(13, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(beklenen, niyet.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, -1))
        assertEquals(scheduler.exactAlarmsAllowed(), niyet.getBooleanExtra(ReminderScheduler.EXTRA_EXACT, !scheduler.exactAlarmsAllowed()))
    }

    @Test
    fun `gecikme saniye ya da dakika olarak yazilir`() {
        val r = context.resources
        assertEquals("0\u00A0sn", AlarmLogText.delay(r, Duration.ofSeconds(-4)))
        assertEquals("59\u00A0sn", AlarmLogText.delay(r, Duration.ofSeconds(59)))
        assertEquals("1\u00A0dk", AlarmLogText.delay(r, Duration.ofSeconds(60)))
        assertEquals("23\u00A0dk", AlarmLogText.delay(r, Duration.ofSeconds(23 * 60 + 40)))
    }

    @Test
    fun `kayit satiri tur, planlanan ve calan saati, gecikmeyi ve alarm turunu gosterir`() {
        val satir = AlarmLogText.entry(context.resources, kayit(dokuz, gecikmeSn = 12), zone)
        assertTrue(satir.startsWith("Doz · "), satir)
        assertTrue("09:00 planlandı, 09:00:12 çaldı (+12\u00A0sn) · tam zamanlı" in satir, satir)
    }

    @Test
    fun `paylasim metni cihaz satirlariyla baslar, kayitlar yeniden eskiye siralanir`() {
        val r = context.resources
        val simdi = bugun.atTime(10, 0).atZone(zone).toInstant()
        val eski = kayit(dokuz.minus(Duration.ofHours(12)), gecikmeSn = 30 * 60, kind = ReminderKind.EVENING)
        val yeni = kayit(dokuz, gecikmeSn = 5)
        val metin = AlarmLogText.shareText(r, listOf(eski, yeni), listOf("Cihaz: Test"), simdi, zone)
        val satirlar = metin.lines()
        assertEquals("Ayla hatırlatma günlüğü", satirlar[0])
        assertEquals("Cihaz: Test", satirlar[1])
        assertTrue(satirlar.any { it.startsWith("Son 48 saatte 2 hatırlatma çaldı.") }, metin)
        assertTrue(satirlar.any { it.startsWith("1 tanesi 10 dakikadan fazla gecikti") }, metin)
        val yeniSira = satirlar.indexOfFirst { it.startsWith("Doz · ") }
        val eskiSira = satirlar.indexOfFirst { it.startsWith("GEÇ · Akşam rutini · ") }
        assertTrue(yeniSira in 0 until eskiSira, metin)
    }
}
