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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Hatırlatma günlüğü (#17): kalıcılık, alarm niyetindeki planlanan an ve paylaşım metni. */
class AlarmGunluguTesti : VeriTestOrtami() {

    private val dokuz: Instant get() = bugun.atTime(9, 0).atZone(zone).toInstant()

    private fun gunluk() = AlarmLog(context, Clock.fixed(bugun.atTime(10, 0).atZone(zone).toInstant(), zone))

    private fun kayit(planned: Instant, gecikmeSn: Long, kind: ReminderKind = ReminderKind.DOSE) =
        AlarmLogEntry(kind, planned, planned.plusSeconds(gecikmeSn), exact = true)

    @Test
    fun `gunluk kayitlari sirayla saklar, en eski kayitlar sinirda duser`() {
        val log = gunluk()
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
        val okunan = gunluk().read()
        assertEquals(listOf(ReminderKind.DOSE, ReminderKind.EVENING), okunan.map { it.kind })
        assertEquals(false, okunan.last().exact)
    }

    @Test
    fun `14 gunden eski kayitlar okunmaz ve bir sonraki yazmada duser`() {
        val log = gunluk()
        log.record(kayit(dokuz.minus(Duration.ofDays(15)), gecikmeSn = 5))
        log.record(kayit(dokuz.minus(Duration.ofDays(13)), gecikmeSn = 5))
        log.record(kayit(dokuz, gecikmeSn = 5))
        assertEquals(2, log.read().size)
        val ham = context.getSharedPreferences("ayla_alarm_log", Context.MODE_PRIVATE).getString("entries", null).orEmpty()
        assertEquals(2, ham.lines().size, "eski kayıt dosyada da tutulmaz")
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

    @Suppress("DEPRECATION") // ScheduledAlarm.operation Robolectric'te eskimiş ama erişimcisi yok
    private fun dozAlarmGunu(): String? {
        val am = context.getSystemService(AlarmManager::class.java)
        return shadowOf(am).scheduledAlarms
            .mapNotNull { alarm -> alarm.operation?.let { shadowOf(it).savedIntent } }
            .firstOrNull { it.dataString?.contains("/dose/") == true }
            ?.getStringExtra(ReminderScheduler.EXTRA_DATE)
    }

    @Test
    fun `tam alarm izni yokken teslim edilmemis bugunku doz yeniden kurulumda yarina itilmez`() {
        // Robolectric'te tam zamanlı alarm izni varsayılan olarak kapalıdır (Android 14+ yeni kurulum gibi).
        val saat2105 = Clock.fixed(bugun.atTime(21, 5).atZone(zone).toInstant(), zone)
        val zamanlayici = ReminderScheduler(context, db.medicationDao(), prefs, saat2105)
        val id = calistir {
            db.medicationDao().insert(
                MedicationEntity(name = "D", doseText = "1", kind = MedicationKind.SUPPLEMENT, times = listOf(LocalTime.of(21, 0))),
            )
        }
        calistir { zamanlayici.rescheduleAll() }
        assertEquals(bugun.toString(), dozAlarmGunu(), "21:00 dozu 21:05'te hâlâ bugün için kurulu")

        zamanlayici.markDoseDelivered(id, LocalTime.of(21, 0), bugun)
        calistir { zamanlayici.rescheduleAll() }
        assertEquals(bugun.plusDays(1).toString(), dozAlarmGunu(), "teslim edildiyse sıradaki gün")
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
