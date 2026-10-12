package com.ayla.pcos.data.health

import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.db.SleepSource
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Health Connect uyku eşlemesi ve yinelenen/çakışan oturum kuralları (#11): saf kurallar. */
class SleepImportRulesTest {

    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun an(gun: Int, saat: Int, dakika: Int = 0): Instant =
        LocalDateTime.of(2026, 10, gun, saat, dakika).atZone(istanbul).toInstant()

    private fun hc(id: String, start: Instant, end: Instant) =
        ExternalSleepSession(id, start, end, ZoneOffset.ofHours(3)).toSleepEntity(istanbul)

    private fun elle(start: Instant, end: Instant) =
        SleepEntity(date = end.atZone(istanbul).toLocalDate(), startAt = start, endAt = end, quality = 2, source = SleepSource.MANUAL)

    @Test
    fun `uyanilan gun bitis ofsetine gore, ofset yoksa cihaz dilimine gore belirlenir`() {
        // 21:30 UTC'de biten oturum İstanbul'da (UTC+3) ertesi gün 00:30'da biter.
        val start = Instant.parse("2026-10-11T19:00:00Z")
        val end = Instant.parse("2026-10-11T21:30:00Z")
        val ofsetli = ExternalSleepSession("a", start, end, ZoneOffset.ofHours(3)).toSleepEntity(ZoneId.of("UTC"))
        assertEquals(LocalDate.of(2026, 10, 12), ofsetli.date)
        val ofsetsiz = ExternalSleepSession("b", start, end, null).toSleepEntity(ZoneId.of("UTC"))
        assertEquals(LocalDate.of(2026, 10, 11), ofsetsiz.date)
        assertEquals(SleepSource.HEALTH_CONNECT, ofsetli.source)
        assertEquals("a", ofsetli.externalId)
        assertEquals(null, ofsetli.quality)
    }

    @Test
    fun `bilinen kimlik ve elle girilen geceyle cakisan oturum alinmaz`() {
        val mevcut = listOf(
            elle(an(10, 23, 30), an(11, 7)),
            hc("eski", an(9, 23), an(10, 6, 45)),
        )
        val gelen = listOf(
            hc("eski", an(9, 23), an(10, 6, 45)), // zaten var
            hc("cakisan", an(10, 23, 10), an(11, 6, 50)), // elle girilen geceyle çakışıyor
            hc("yeni", an(11, 23, 40), an(12, 7, 5)),
        )
        assertEquals(listOf("yeni"), selectNewSleep(mevcut, gelen).map { it.externalId })
    }

    @Test
    fun `gelenler kendi aralarinda cakisirsa uzun olan kalir, ogle uykusu ayrica eklenir`() {
        val gelen = listOf(
            hc("telefon", an(11, 0, 30), an(11, 6, 30)),
            hc("saat", an(10, 23, 45), an(11, 7, 10)),
            hc("ogle", an(11, 14), an(11, 14, 40)),
        )
        val secilen = selectNewSleep(emptyList(), gelen)
        assertEquals(listOf("saat", "ogle"), secilen.map { it.externalId }, "başlangıca göre sıralı")
    }

    @Test
    fun `gecersiz ve 24 saati asan oturum alinmaz`() {
        val gelen = listOf(
            hc("ters", an(11, 7), an(11, 6)),
            hc("sifir", an(11, 7), an(11, 7)),
            hc("uzun", an(9, 22), an(11, 7)),
            hc("tam24", an(10, 7), an(11, 7)),
        )
        assertEquals(listOf("tam24"), selectNewSleep(emptyList(), gelen).map { it.externalId })
    }

    @Test
    fun `bitisik oturumlar cakisma sayilmaz`() {
        val mevcut = listOf(elle(an(11, 1), an(11, 3)))
        val gelen = listOf(hc("hemen-sonra", an(11, 3), an(11, 7)))
        assertTrue(selectNewSleep(mevcut, gelen).isNotEmpty())
    }
}
