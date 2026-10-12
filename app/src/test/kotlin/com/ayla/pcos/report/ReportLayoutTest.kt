package com.ayla.pcos.report

import com.ayla.pcos.report.ReportBlock.Bullets
import com.ayla.pcos.report.ReportBlock.KeyValues
import com.ayla.pcos.report.ReportBlock.Note
import com.ayla.pcos.report.ReportBlock.Paragraph
import com.ayla.pcos.report.ReportBlock.Table
import com.ayla.pcos.report.ReportBlock.TemperatureChart
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Yerleşim motoru (ADR-0009): sabit genişlikli ölçerle belirleyici sayfalama. BODY 10 pt → karakter 5,5 pt,
 * satır 14 pt; içerik genişliği 515 pt → satırda 93 karakter.
 */
class ReportLayoutTest {

    private val layout = ReportLayout(FixedWidthMeasurer())
    private val labels = mutableListOf<String>()

    private fun label(number: Int, total: Int): String = "$number/$total".also { labels += it }

    private fun report(
        sections: List<ReportSection>,
        title: String = "Ayla doktor raporu",
        subtitle: String = "1–28 Eylül 2026",
        disclaimer: String = "",
        footer: String = "Ayla",
    ) = DoctorReport(title, subtitle, disclaimer, sections, footer)

    private fun texts(page: ReportPage) = page.ops.filterIsInstance<DrawOp.Text>()

    private fun signature(op: DrawOp): String = when (op) {
        is DrawOp.Text -> "T:${op.style}:${op.text}"
        is DrawOp.Line -> "L"
        is DrawOp.Rect -> "R"
        is DrawOp.Circle -> "C"
        is DrawOp.Polyline -> "P"
    }

    private fun assertNear(expected: Float, actual: Float, message: String) =
        assertTrue(abs(expected - actual) < 0.01f, "$message: beklenen $expected, gelen $actual")

    @Test
    fun `kucuk rapor tek sayfaya sigar ve komut sirasi beklenen gibidir`() {
        val section = ReportSection(
            "Döngü",
            listOf(Paragraph("Son adet 1 Eylül."), Bullets(listOf("a", "b")), KeyValues(listOf("Ortalama" to "31 gün"))),
        )
        val pages = layout.layout(report(listOf(section), disclaimer = "Bu belge kayıt özetidir."), ::label)

        assertEquals(1, pages.size)
        assertEquals(listOf("1/1"), labels)
        assertEquals(
            listOf(
                "T:TITLE:Ayla doktor raporu", "T:SUBTITLE:1–28 Eylül 2026", "L",
                "R", "T:BODY:Bu belge kayıt özetidir.",
                "T:SECTION:Döngü",
                "T:BODY:Son adet 1 Eylül.",
                "T:BODY:•", "T:BODY:a", "T:BODY:•", "T:BODY:b",
                "T:BOLD:Ortalama", "T:BODY:31 gün",
                "L", "T:SMALL:Ayla", "T:SMALL:1/1",
            ),
            pages[0].ops.map(::signature),
        )
        val all = texts(pages[0])
        // Taban çizgileri yukarıdan aşağıya ilerler; alt bilgi en altta, sağa hizalı.
        assertTrue(all.zipWithNext().all { (a, b) -> a.y <= b.y }, "metinler yukarıdan aşağıya sıralı değil")
        val pageLabel = all.last()
        assertEquals(TextAlign.RIGHT, pageLabel.align)
        assertNear(555f, pageLabel.x, "sayfa etiketi sağ kenarda")
        assertTrue(all.all { it.y > 40f && it.y < 802f }, "metin kenar boşluğuna taştı")
    }

    @Test
    fun `uzun paragraf sayfalara tasar ve her sayfada ust ve alt bilgi bulunur`() {
        // 1521 kelime × 13 kelime/satır = 117 satır: ilk sayfa 47, ikinci 50, üçüncü 20.
        val text = List(1521) { "kelime" }.joinToString(" ")
        val pages = layout.layout(report(listOf(ReportSection("Günlük", listOf(Paragraph(text))))), ::label)

        assertEquals(3, pages.size)
        assertEquals(listOf("1/3", "2/3", "3/3"), labels)
        pages.forEachIndexed { index, page ->
            val ops = page.ops
            val last = ops.takeLast(3)
            assertTrue(last[0] is DrawOp.Line, "sayfa ${index + 1}: alt bilgi çizgisi yok")
            assertEquals("T:SMALL:Ayla", signature(last[1]))
            assertEquals("T:SMALL:${index + 1}/3", signature(last[2]))
            if (index > 0) {
                assertEquals(listOf("T:SMALL:Ayla doktor raporu", "T:SMALL:1–28 Eylül 2026", "L"), ops.take(3).map(::signature))
                assertEquals(TextAlign.RIGHT, (ops[1] as DrawOp.Text).align)
            }
        }
        val lines = pages.flatMap { texts(it) }.filter { it.style == TextStyle.BODY }
        assertEquals(117, lines.size)
        assertEquals(text, lines.joinToString(" ") { it.text })
    }

    @Test
    fun `bolum basligi sayfa sonunda yetim kalmaz`() {
        for (k in 1..70) {
            val a = ReportSection("A", List(k) { Paragraph("x") })
            val b = ReportSection("B", List(5) { Paragraph("y") })
            val pages = layout.layout(report(listOf(a, b)), ::label)
            pages.forEach { page ->
                val ops = page.ops
                val titleIndex = ops.indexOfFirst { it is DrawOp.Text && it.style == TextStyle.SECTION && it.text == "B" }
                if (titleIndex >= 0) {
                    val following = ops.drop(titleIndex + 1).count { it is DrawOp.Text && it.style == TextStyle.BODY && it.text == "y" }
                    assertTrue(following >= 2, "k=$k: B başlığının altında yalnızca $following satır var")
                }
            }
            assertEquals(1, pages.sumOf { page -> texts(page).count { it.text == "B" } }, "k=$k: başlık bir kez yazılmalı")
        }
    }

    @Test
    fun `tablo basligi sayfa kirildiginda yeni sayfada yinelenir`() {
        val table = Table(header = listOf("Tarih", "Not"), rows = List(120) { listOf("gün $it", "satır $it") })
        val pages = layout.layout(report(listOf(ReportSection("Kayıtlar", listOf(table)))), ::label)

        assertTrue(pages.size >= 3, "tablo en az üç sayfaya yayılmalı, ${pages.size} sayfa")
        var pagesWithBody = 0
        pages.forEach { page ->
            val ops = page.ops
            val firstBody = ops.indexOfFirst { it is DrawOp.Text && it.style == TextStyle.BODY && it.text.startsWith("satır") }
            if (firstBody < 0) return@forEach
            pagesWithBody++
            val header = ops.indexOfFirst { it is DrawOp.Text && it.style == TextStyle.BOLD && it.text == "Tarih" }
            assertTrue(header in 0 until firstBody, "başlık gövdeden önce gelmeli")
            // Başlığın altındaki çizgi, başlık metninden sonra ve ilk gövde satırından önce.
            val rule = ops.subList(header, firstBody).count { it is DrawOp.Line }
            assertEquals(1, rule)
        }
        assertEquals(pages.size, pagesWithBody)
        assertEquals(pages.size, pages.sumOf { page -> texts(page).count { it.text == "Tarih" } })
        val body = pages.flatMap { texts(it) }.filter { it.text.startsWith("satır") }
        assertEquals(List(120) { "satır $it" }, body.map { it.text })
    }

    @Test
    fun `tablo sutunlari agirliklara gore bolunur`() {
        val table = Table(header = listOf("a", "b", "c"), rows = listOf(listOf("1", "2", "3")), weights = listOf(2f, 1f, 1f))
        val page = layout.layout(report(listOf(ReportSection("T", listOf(table)))), ::label).single()
        val header = texts(page).filter { it.style == TextStyle.BOLD }
        assertEquals(listOf("a", "b", "c"), header.map { it.text })
        assertNear(43f, header[0].x, "1. sütun")
        assertNear(43f + 257.5f, header[1].x, "2. sütun")
        assertNear(43f + 386.25f, header[2].x, "3. sütun")
        val equal = Table(header = listOf("a", "b"), rows = emptyList(), weights = listOf(1f))
        val page2 = layout.layout(report(listOf(ReportSection("T", listOf(equal)))), ::label).single()
        assertNear(43f + 257.5f, texts(page2).first { it.text == "b" }.x, "uyumsuz ağırlık → eşit sütun")
    }

    @Test
    fun `sigmayan uzun kelime karakterden kirilir`() {
        val word = "a".repeat(300)
        val page = layout.layout(report(listOf(ReportSection("K", listOf(Paragraph("önce $word sonra"))))), ::label).single()
        val lines = texts(page).filter { it.style == TextStyle.BODY }.map { it.text }
        assertEquals(listOf("önce", "a".repeat(93), "a".repeat(93), "a".repeat(93), "a".repeat(21) + " sonra"), lines)
        assertTrue(lines.all { it.length * 5.5f <= 515f })
    }

    @Test
    fun `kelime bazli kaydirma acgozludur ve genisligi asmaz`() {
        val text = "Düzensiz döngü takibi, ilaç hatırlatması ve günlük kayıt; su, uyku ve semptomlar birlikte izlenir. " +
            "Bu rapor yalnızca kayıtların özetidir.\n\nYeni paragraf."
        val lines = layout.wrap(text.repeat(3), TextStyle.BODY, 200f)
        assertTrue(lines.all { it.length * 5.5f <= 200f }, "satır genişliği aşıldı")
        assertTrue(lines.contains(""), "boş satır korunmalı")
        lines.zipWithNext().forEach { (line, next) ->
            val nextWord = next.substringBefore(' ')
            if (line.isNotEmpty() && nextWord.isNotEmpty()) {
                assertTrue((line.length + 1 + nextWord.length) * 5.5f > 200f, "'$nextWord' önceki satıra sığardı")
            }
        }
    }

    @Test
    fun `grafik eksen etiketleri ve noktalar beklenen sayidadir`() {
        val temps = listOf(36.3, 36.4, 36.35, 36.5, 36.45, 36.6, 36.7, 36.8, 36.85, 36.9)
        val points = temps.mapIndexed { i, t -> ChartPoint(day = i + 1, celsius = t, excluded = i == 5) }
        val chart = TemperatureChart(points, coverlineCelsius = 36.55, lhPositiveDays = setOf(12, 13), ovulationDay = 14, xLabel = "Döngü günü", yLabel = "°C")
        val page = layout.layout(report(listOf(ReportSection("Doğurganlık", listOf(chart)))), ::label).single()
        val ops = page.ops

        val circles = ops.filterIsInstance<DrawOp.Circle>()
        assertEquals(10, circles.size)
        assertEquals(9, circles.count { it.filled })
        assertEquals(1, circles.count { !it.filled })
        assertTrue(circles.all { it.r == 2.5f })
        assertEquals(2, ops.filterIsInstance<DrawOp.Rect>().count { it.filled })
        assertEquals(1, ops.filterIsInstance<DrawOp.Line>().count { it.dashed })
        assertEquals(1, ops.filterIsInstance<DrawOp.Line>().count { it.x1 == it.x2 && it.width == 0.5f })
        val polylines = ops.filterIsInstance<DrawOp.Polyline>()
        assertEquals(1, polylines.size)
        assertEquals(9, polylines.single().points.size)

        val yLabels = texts(page).filter { it.align == TextAlign.RIGHT && it.style == TextStyle.SMALL && it.text != "1/1" }
        assertEquals(listOf("36,1", "36,3", "36,5", "36,7", "36,9", "37,1"), yLabels.map { it.text })
        val xLabels = texts(page).filter { it.align == TextAlign.CENTER && it.text != "Döngü günü" }
        assertEquals((1..14).map { it.toString() }, xLabels.map { it.text })
        assertTrue(texts(page).any { it.text == "Döngü günü" && it.align == TextAlign.CENTER })
        assertTrue(texts(page).any { it.text == "°C" })
        // Sıcaklık arttıkça nokta yukarı (y küçülür); bozuk nokta diğerleriyle aynı eksende.
        val byDay = circles.sortedBy { it.cx }
        assertTrue(byDay.first().cy > byDay.last().cy)
    }

    @Test
    fun `grafik sayfaya sigmazsa butunuyle yeni sayfaya gecer`() {
        val filler = List(33) { Paragraph("x") }
        val chart = TemperatureChart(List(5) { ChartPoint(it + 1, 36.5 + it * 0.05) }, xLabel = "gün", yLabel = "°C")
        val pages = layout.layout(report(listOf(ReportSection("A", filler + chart))), ::label)
        assertEquals(2, pages.size)
        assertEquals(0, pages[0].ops.count { it is DrawOp.Circle })
        assertEquals(5, pages[1].ops.count { it is DrawOp.Circle })
    }

    @Test
    fun `sicaklik ekseni araligi ve adimi belirleyicidir`() {
        assertEquals(Triple(360, 370, 2), ReportLayout.temperatureTicks(emptyList()))
        assertEquals(Triple(363, 367, 1), ReportLayout.temperatureTicks(listOf(36.5)))
        assertEquals(Triple(361, 371, 2), ReportLayout.temperatureTicks(listOf(36.3, 36.9)))
        val (lo, hi, step) = ReportLayout.temperatureTicks(listOf(36.0, 37.4))
        assertEquals(358, lo)
        assertEquals(376, hi)
        assertEquals(3, step)
        assertTrue((hi - lo) / step + 1 <= 8)
    }

    @Test
    fun `anahtar deger satirlari iki sutundur ve uzun deger kaydirilir`() {
        val longValue = List(40) { "değer" }.joinToString(" ")
        val block = KeyValues(listOf("Ortalama" to "31 gün", "Not" to longValue))
        val page = layout.layout(report(listOf(ReportSection("Özet", listOf(block)))), ::label).single()
        val keys = texts(page).filter { it.style == TextStyle.BOLD }
        val values = texts(page).filter { it.style == TextStyle.BODY }
        assertEquals(listOf("Ortalama", "Not"), keys.map { it.text })
        assertTrue(keys.all { it.x == 40f })
        assertTrue(values.all { abs(it.x - 220.25f) < 0.01f }, "değer sütunu %35'ten başlamalı")
        assertEquals(longValue, values.drop(1).joinToString(" ") { it.text })
        assertTrue(values.size > 2, "uzun değer birden çok satıra kaydırılmalı")
    }

    @Test
    fun `not cerceve icinde ic bosluklu yazilir`() {
        val page = layout.layout(report(listOf(ReportSection("N", listOf(Note("Dikkat"))))), ::label).single()
        val frame = page.ops.filterIsInstance<DrawOp.Rect>().single()
        val text = texts(page).single { it.text == "Dikkat" }
        assertEquals(false, frame.filled)
        assertNear(40f, frame.x, "çerçeve sol")
        assertNear(515f, frame.w, "çerçeve genişliği")
        assertNear(14f + 12f, frame.h, "tek satır + 2 × 6 pt")
        assertNear(46f, text.x, "metin iç boşluk")
        assertNear(frame.y + 6f + 14f * 0.75f, text.y, "taban çizgisi")
    }

    @Test
    fun `bos rapor tek sayfadir`() {
        val pages = layout.layout(DoctorReport("Rapor", "", "", emptyList(), ""), ::label)
        assertEquals(1, pages.size)
        assertEquals(listOf("T:TITLE:Rapor", "L", "L", "T:SMALL:1/1"), pages.single().ops.map(::signature))
        assertEquals(1, layout.pageCount(DoctorReport("Rapor", "", "", emptyList(), "")))
    }
}
