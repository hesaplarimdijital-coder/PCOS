package com.ayla.pcos.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.Duration
import java.util.EnumMap
import javax.inject.Inject

/**
 * Stil başına tek [Paint]; ölçer ([PaintTextMeasurer]) ve çizici ([DrawOpRenderer]) aynı nesneleri kullanır ki
 * ölçüm ile çizim birebir örtüşsün. `Typeface.SANS_SERIF` (Roboto) gömülür; Türkçe glifler (ş, ğ, ı, İ) sorunsuzdur.
 */
internal class ReportPaints {
    private val cache = EnumMap<TextStyle, Paint>(TextStyle::class.java)

    fun text(textStyle: TextStyle): Paint = cache.getOrPut(textStyle) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (textStyle.bold) Typeface.DEFAULT_BOLD else Typeface.SANS_SERIF
            textSize = textStyle.pointSize
            color = Color.BLACK
            style = Paint.Style.FILL
        }
    }
}

/**
 * Üretim ölçeri: `Paint.measureText` ve font ölçüleri; satır yüksekliği (descent − ascent) × 1,25.
 * Değerler stil başına önbelleklenir; aynı metin her zaman aynı genişliği verir.
 */
class PaintTextMeasurer internal constructor(private val paints: ReportPaints) : TextMeasurer {
    constructor() : this(ReportPaints())

    private val lineHeights = EnumMap<TextStyle, Float>(TextStyle::class.java)

    override fun width(text: String, style: TextStyle): Float = paints.text(style).measureText(text)

    override fun lineHeight(style: TextStyle): Float = lineHeights.getOrPut(style) {
        val metrics = paints.text(style).fontMetrics
        (metrics.descent - metrics.ascent) * LINE_SPACING
    }

    private companion object {
        const val LINE_SPACING = 1.25f
    }
}

/** [DrawOp] listesini bir [Canvas]'a çizer; PDF sayfası da bit eşlem de olabilir (testler bit eşleme çizer). */
internal class DrawOpRenderer(private val paints: ReportPaints = ReportPaints()) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val dash = DashPathEffect(floatArrayOf(DASH_ON, DASH_OFF), 0f)

    fun draw(canvas: Canvas, page: ReportPage) {
        page.ops.forEach { draw(canvas, it) }
    }

    fun draw(canvas: Canvas, op: DrawOp) {
        when (op) {
            is DrawOp.Text -> {
                val paint = paints.text(op.style)
                paint.textAlign = when (op.align) {
                    TextAlign.LEFT -> Paint.Align.LEFT
                    TextAlign.RIGHT -> Paint.Align.RIGHT
                    TextAlign.CENTER -> Paint.Align.CENTER
                }
                canvas.drawText(op.text, op.x, op.y, paint)
            }
            is DrawOp.Line -> {
                stroke.strokeWidth = op.width
                stroke.pathEffect = if (op.dashed) dash else null
                canvas.drawLine(op.x1, op.y1, op.x2, op.y2, stroke)
                stroke.pathEffect = null
            }
            is DrawOp.Rect -> {
                if (op.filled) {
                    canvas.drawRect(op.x, op.y, op.x + op.w, op.y + op.h, fill)
                } else {
                    stroke.strokeWidth = op.stroke
                    canvas.drawRect(op.x, op.y, op.x + op.w, op.y + op.h, stroke)
                }
            }
            is DrawOp.Circle -> {
                if (op.filled) {
                    canvas.drawCircle(op.cx, op.cy, op.r, fill)
                } else {
                    stroke.strokeWidth = HOLLOW_STROKE
                    canvas.drawCircle(op.cx, op.cy, op.r, stroke)
                }
            }
            is DrawOp.Polyline -> {
                if (op.points.size < 2) return
                val path = Path()
                op.points.forEachIndexed { index, (x, y) -> if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                stroke.strokeWidth = op.width
                canvas.drawPath(path, stroke)
            }
        }
    }

    private companion object {
        const val DASH_ON = 3f
        const val DASH_OFF = 2f
        const val HOLLOW_STROKE = 0.75f
    }
}

/**
 * Doktor raporu PDF yazıcısı (ADR-0009): platform [PdfDocument], üçüncü taraf kitaplık yok. Sayfalar
 * `cacheDir/raporlar/` altına yazılır; dosya adı dışarıdan gelir ve kişisel veri içermez
 * (`ayla-doktor-raporu-<tarih>.pdf`). Paylaşım `FileProvider` (`<applicationId>.photos`, `cache-path raporlar`)
 * ile kullanıcının elindedir; uygulama dosyayı hiçbir yere iletmez.
 */
class PdfReportWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val paints = ReportPaints()
    private val renderer = DrawOpRenderer(paints)

    /** Rapor dosyalarının dizini (`cacheDir/raporlar`). */
    val directory: File
        get() = File(context.cacheDir, DIRECTORY)

    /** Sayfaları [target]'a yazar; üst dizin yoksa oluşturulur. Boş liste de geçerli bir (sayfasız) belge üretir. */
    fun write(pages: List<ReportPage>, target: File, pageWidth: Int = PAGE_WIDTH, pageHeight: Int = PAGE_HEIGHT) {
        target.parentFile?.mkdirs()
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = document.startPage(info)
                renderer.draw(pdfPage.canvas, page)
                document.finishPage(pdfPage)
            }
            FileOutputStream(target).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    /**
     * Modeli sayfalar, `cacheDir/raporlar/[fileName]` dosyasına yazar ve eski raporları temizler (en fazla
     * [KEEP_FILES] dosya, 24 saat). [pageLabel] sayfa etiketi (1'den başlayan numara, toplam) üretir.
     */
    fun report(
        report: DoctorReport,
        fileName: String,
        measurer: TextMeasurer = PaintTextMeasurer(paints),
        pageLabel: (Int, Int) -> String,
    ): File {
        val pages = ReportLayout(measurer).layout(report, pageLabel)
        cleanup(keep = KEEP_FILES - 1)
        val target = File(directory, fileName)
        write(pages, target)
        return target
    }

    /**
     * Eski rapor dosyalarını siler: en yeni [keep] dosya kalır, [olderThan]'dan eski olanlar her durumda gider.
     * Uygulama açılışında ve her yeni raporda çağrılır (ADR-0009). Dizin yoksa hiçbir şey yapmaz.
     */
    fun cleanup(keep: Int = KEEP_FILES, olderThan: Duration = Duration.ofHours(24)) {
        val files = directory.listFiles { file -> file.isFile } ?: return
        val now = clock.millis()
        files
            .sortedWith(compareByDescending<File> { it.lastModified() }.thenBy { it.name })
            .forEachIndexed { index, file ->
                if (index >= keep || now - file.lastModified() > olderThan.toMillis()) file.delete()
            }
    }

    /** Tam silme: önbellekteki tüm rapor dosyaları gider ("Tüm verilerimi sil", ADR-0009). */
    fun deleteAll() {
        directory.deleteRecursively()
    }

    companion object {
        const val DIRECTORY = "raporlar"
        const val KEEP_FILES = 3
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
    }
}
