package com.ayla.pcos.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Android çizim katmanı (ADR-0009): [PaintTextMeasurer] gerçek font ölçüleriyle, [DrawOpRenderer] bit eşleme
 * çizerek sınanır. `PdfDocument` gerekmez; o katman ayrı dosyadadır ([PdfReportWriterTest]).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReportCanvasTest {

    private fun ornekRapor() = DoctorReport(
        title = "Ayla doktor raporu",
        subtitle = "14 Eylül – 12 Ekim 2026",
        disclaimer = "Bu belge kullanıcının kendi kayıtlarının özetidir; tanı, tedavi ya da tavsiye değildir.",
        sections = listOf(
            ReportSection(
                "Döngü",
                listOf(
                    ReportBlock.Paragraph("Son adet başlangıcı 1 Ekim 2026, bugün döngünün 12. günü."),
                    ReportBlock.KeyValues(listOf("Son 12 ayda adet" to "9", "Ortalama uzunluk" to "38 gün")),
                    ReportBlock.Table(listOf("Başlangıç", "Uzunluk"), listOf(listOf("1 Ekim", "—"), listOf("24 Ağustos", "38 gün"))),
                ),
            ),
            ReportSection(
                "Doğurganlık",
                listOf(
                    ReportBlock.TemperatureChart(
                        // 5. gün bozuk ölçüm (sivri, komşuların doğrusu ve kılavuz çizgileri dışında).
                        points = listOf(36.40, 36.43, 36.46, 36.40, 36.95, 36.43, 36.70, 36.73, 36.76, 36.70, 36.73, 36.76)
                            .mapIndexed { i, t -> ChartPoint(i + 1, t, excluded = i == 4) },
                        coverlineCelsius = 36.55,
                        lhPositiveDays = setOf(6),
                        ovulationDay = 7,
                        xLabel = "Döngü günü",
                        yLabel = "°C",
                    ),
                    ReportBlock.Note("Doğum kontrolü için kullanılmaz."),
                ),
            ),
        ),
        footer = "Ayla",
    )

    private fun luminance(pixel: Int) = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000

    private fun koyuSayisi(bitmap: Bitmap, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        var count = 0
        for (y in y0 until y1) for (x in x0 until x1) if (luminance(bitmap.getPixel(x, y)) < 128) count++
        return count
    }

    @Test
    fun `paint olcer turkce metni olcer ve satir yuksekligi font olculerinden gelir`() {
        val measurer = PaintTextMeasurer()
        assertTrue(measurer.width("Şişli'de ğ, ı, İ", TextStyle.BODY) > 0f)
        assertTrue(measurer.width("ab", TextStyle.BODY) > measurer.width("a", TextStyle.BODY))
        assertTrue(measurer.width("Ayla", TextStyle.TITLE) > measurer.width("Ayla", TextStyle.BODY))
        assertEquals(0f, measurer.width("", TextStyle.BODY))
        val body = measurer.lineHeight(TextStyle.BODY)
        assertTrue(body > 10f && body < 20f, "10 punto satır yüksekliği: $body")
        assertTrue(measurer.lineHeight(TextStyle.TITLE) > body)
        assertTrue(measurer.lineHeight(TextStyle.SMALL) < body)
        assertEquals(measurer.width("döngü", TextStyle.BODY), measurer.width("döngü", TextStyle.BODY))
    }

    @Test
    fun `ornek rapor paint olcerle tek sayfaya sigar ve turkce metinler kaydirilir`() {
        val pages = ReportLayout(PaintTextMeasurer()).layout(ornekRapor()) { n, t -> "$n/$t" }
        assertEquals(1, pages.size)
        val texts = pages.single().ops.filterIsInstance<DrawOp.Text>()
        assertTrue(texts.any { it.text == "Doğurganlık" && it.style == TextStyle.SECTION })
        assertTrue(texts.any { it.text == "1/1" })
        assertTrue(texts.all { it.x >= 40f && it.x <= 555f })
    }

    @Test
    fun `komutlar bit esleme cizilir`() {
        val page = ReportLayout(PaintTextMeasurer()).layout(ornekRapor()) { n, t -> "$n/$t" }.single()
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        DrawOpRenderer().draw(Canvas(bitmap), page)

        // Başlık bölgesinde metin pikselleri var; sayfanın sağ alt köşesi (kenar boşluğu) bembeyaz.
        assertTrue(koyuSayisi(bitmap, 40, 40, 300, 75) > 50, "başlık çizilmedi")
        assertEquals(0, koyuSayisi(bitmap, 560, 810, 595, 842))
        // Dolu nokta merkezinde koyu piksel; içi boş noktanın merkezi açık.
        val circles = page.ops.filterIsInstance<DrawOp.Circle>()
        val filled = circles.first { it.filled }
        val hollow = circles.first { !it.filled }
        assertTrue(luminance(bitmap.getPixel(filled.cx.roundToInt(), filled.cy.roundToInt())) < 128, "dolu nokta")
        assertTrue(luminance(bitmap.getPixel(hollow.cx.roundToInt(), hollow.cy.roundToInt())) > 200, "içi boş nokta")
        // Not çerçevesinin sol kenarı boyunca pikseller koyulaşmış.
        val frame = page.ops.filterIsInstance<DrawOp.Rect>().first { !it.filled }
        val edgeX = frame.x.roundToInt()
        val edgeDark = (frame.y.roundToInt() + 2 until (frame.y + frame.h).roundToInt() - 2).count { y ->
            (edgeX - 1..edgeX + 1).any { x -> luminance(bitmap.getPixel(x, y)) < 250 }
        }
        assertTrue(edgeDark > frame.h.toInt() / 2, "çerçeve kenarı çizilmedi")
        bitmap.recycle()
    }
}
