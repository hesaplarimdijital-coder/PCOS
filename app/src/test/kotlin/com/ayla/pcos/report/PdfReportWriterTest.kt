package com.ayla.pcos.report

import android.content.Context
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PDF yazıcısı (ADR-0009). `PdfDocument` yerel kodla çalışır; Robolectric'in yerel grafik kipinde
 * `PdfDocument` natives'inin bağlı olup olmadığı belirsizdir — bu yüzden bu sınıf ayrı dosyadadır: CI'da
 * düşerse yalnızca `PdfDocument` kullanan testler `@Ignore` edilir, temizlik testi kalır
 * (Canvas/Paint katmanı [ReportCanvasTest]'te ayrıca sınanır).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfReportWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    private val now = LocalDate.of(2026, 10, 12).atTime(10, 0).atZone(zone).toInstant()
    private val clock: Clock = Clock.fixed(now, zone)
    private lateinit var context: Context
    private lateinit var writer: PdfReportWriter

    @Before
    fun kur() {
        context = RuntimeEnvironment.getApplication()
        writer = PdfReportWriter(context, clock)
        writer.directory.deleteRecursively()
    }

    private fun rapor(paragraphs: Int = 1) = DoctorReport(
        title = "Ayla doktor raporu",
        subtitle = "14 Eylül – 12 Ekim 2026",
        disclaimer = "Bu belge kullanıcının kendi kayıtlarının özetidir; tanı, tedavi ya da tavsiye değildir.",
        sections = listOf(
            ReportSection(
                "Döngü",
                List(paragraphs) { ReportBlock.Paragraph("Son adet başlangıcı 1 Ekim 2026; döngü uzunlukları 38, 41 ve 35 gün. Şikâyet: ağrı, şişkinlik.") } +
                    ReportBlock.KeyValues(listOf("Son 12 ayda adet" to "9")),
            ),
        ),
        footer = "Ayla",
    )

    private fun sayfalar(report: DoctorReport) = ReportLayout(PaintTextMeasurer()).layout(report) { n, t -> "$n/$t" }

    @Ignore("PdfDocument yerel kodu Robolectric'te yok (ADR-0009); cihazda doğrulanır. Deneme: pdf-deneme dalı")
    @Test
    fun `tek sayfa pdf yazar`() {
        val pages = sayfalar(rapor())
        assertEquals(1, pages.size)
        val file = File(tmp.root, "rapor.pdf")

        writer.write(pages, file)

        val bytes = file.readBytes()
        assertTrue(bytes.size > 1024, "dosya ${bytes.size} bayt")
        assertEquals("%PDF-", String(bytes, 0, 5, Charsets.US_ASCII))
    }

    @Ignore("PdfDocument yerel kodu Robolectric'te yok (ADR-0009); cihazda doğrulanır. Deneme: pdf-deneme dalı")
    @Test
    fun `iki sayfa yazilir ve sayfa sayisi dosyada gorunur`() {
        val pages = sayfalar(rapor(paragraphs = 60))
        assertTrue(pages.size >= 2, "${pages.size} sayfa")
        val file = File(tmp.root, "uzun.pdf")

        writer.write(pages.take(2), file)

        val content = String(file.readBytes(), Charsets.ISO_8859_1)
        val pageObjects = Regex("/Type\\s*/Page(?!s)").findAll(content).count()
        assertTrue(pageObjects == 2 || content.contains("/Count 2"), "sayfa nesneleri: $pageObjects")
    }

    @Ignore("PdfDocument yerel kodu Robolectric'te yok (ADR-0009); cihazda doğrulanır. Deneme: pdf-deneme dalı")
    @Test
    fun `rapor yardimcisi onbellek dizinine yazar ve eski dosyalari temizler`() {
        val dir = writer.directory
        dir.mkdirs()
        sahteDosya(dir, "eski-25-saat.pdf", Duration.ofHours(25))
        sahteDosya(dir, "yeni-1.pdf", Duration.ofHours(1))
        sahteDosya(dir, "yeni-2.pdf", Duration.ofHours(2))
        sahteDosya(dir, "yeni-3.pdf", Duration.ofHours(3))

        val file = writer.report(rapor(), "ayla-doktor-raporu-2026-10-12.pdf") { n, t -> "$n/$t" }

        assertEquals(File(context.cacheDir, "raporlar"), file.parentFile)
        assertTrue(file.length() > 1024)
        assertEquals(listOf("ayla-doktor-raporu-2026-10-12.pdf", "yeni-1.pdf", "yeni-2.pdf"), dir.list()!!.sorted())
    }

    @Test
    fun `temizlik en yeni uc dosyayi tutar ve 24 saatten eskileri siler`() {
        val dir = writer.directory
        dir.mkdirs()
        sahteDosya(dir, "a.pdf", Duration.ofHours(1))
        sahteDosya(dir, "b.pdf", Duration.ofHours(2))
        sahteDosya(dir, "c.pdf", Duration.ofHours(3))
        sahteDosya(dir, "d.pdf", Duration.ofHours(4))
        sahteDosya(dir, "e.pdf", Duration.ofHours(25))

        writer.cleanup()
        assertEquals(listOf("a.pdf", "b.pdf", "c.pdf"), dir.list()!!.sorted())

        writer.cleanup(keep = 1)
        assertEquals(listOf("a.pdf"), dir.list()!!.sorted())

        dir.deleteRecursively()
        writer.cleanup() // dizin yoksa sessizce geçer
        assertTrue(!dir.exists())
    }

    private fun sahteDosya(dir: File, name: String, age: Duration): File = File(dir, name).apply {
        writeText("x")
        assertTrue(setLastModified(now.toEpochMilli() - age.toMillis()), "mtime ayarlanamadı")
    }
}
