package com.ayla.pcos.report

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Metin hizası: [DrawOp.Text.x] LEFT'te sol kenar, RIGHT'ta sağ kenar, CENTER'da orta noktadır. */
enum class TextAlign { LEFT, RIGHT, CENTER }

/**
 * Sayfa üzerinde tek çizim komutu. Birim pt, köken sol üst, y aşağı doğru artar (Canvas ile aynı).
 * [PdfReportWriter] bunları Canvas'a çizer; yerleşim testleri listeyi doğrudan doğrular.
 */
sealed interface DrawOp {
    /** [y] taban çizgisidir (`Canvas.drawText` ile aynı anlam). */
    data class Text(val x: Float, val y: Float, val text: String, val style: TextStyle, val align: TextAlign = TextAlign.LEFT) : DrawOp

    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val width: Float, val dashed: Boolean = false) : DrawOp

    /** [filled] false ise yalnızca [stroke] kalınlığında çerçeve. */
    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float, val filled: Boolean, val stroke: Float = ReportLayout.RULE_WIDTH) : DrawOp

    data class Circle(val cx: Float, val cy: Float, val r: Float, val filled: Boolean) : DrawOp

    data class Polyline(val points: List<Pair<Float, Float>>, val width: Float) : DrawOp
}

/** Bir sayfanın komutları: önce üst bilgi, sonra içerik, en sonda alt bilgi. */
data class ReportPage(val ops: List<DrawOp>)

/**
 * Saf Kotlin yerleşim motoru (ADR-0009): [DoctorReport]'u A4 sayfalara ve [DrawOp] listelerine dönüştürür.
 * Metin ölçümü [measurer]'dan gelir; aynı girdi her zaman aynı çıktıyı verir.
 *
 * Kurallar: ilk sayfada büyük başlık + alt başlık, sonraki sayfalarda küçük başlık (solda) ve alt başlık (sağda);
 * her sayfada ince çizgi üstünde alt bilgi (solda) ve sayfa etiketi (sağda); kelime bazlı satır kaydırma (sığmayan
 * kelime karakterden kırılır); bölüm başlığı altında en az iki satır içerik yoksa yeni sayfaya; tablo başlığı
 * kırılımda yinelenir; grafik bölünmez.
 *
 * [tickLabel] sıcaklık eksen etiketini biçimler (varsayılan: Türkçe ondalık, bir basamak).
 */
class ReportLayout(
    private val measurer: TextMeasurer,
    private val pageWidth: Float = PAGE_WIDTH,
    private val pageHeight: Float = PAGE_HEIGHT,
    private val margin: Float = MARGIN,
    private val tickLabel: (Double) -> String = ::defaultTickLabel,
) {
    private val left = margin
    private val right = pageWidth - margin
    private val contentWidth = right - left

    /** Raporu sayfalar. [pageLabel] her sayfa için bir kez, (sayfa numarası 1'den, toplam) ile çağrılır. */
    fun layout(report: DoctorReport, pageLabel: (Int, Int) -> String): List<ReportPage> {
        val laterTop = header(report, first = false).first
        val bottom = contentBottom()
        val rows = ArrayList<Row>()
        if (report.disclaimer.isNotBlank()) rows += noteRows(report.disclaimer, maxHeight = bottom - laterTop)
        report.sections.forEach { rows += sectionRows(it, maxNoteHeight = bottom - laterTop) }
        val pager = Pager(report, bottom)
        pager.place(rows)
        val total = pager.pages.size
        return pager.pages.mapIndexed { index, ops -> ReportPage(ops + footerOps(report.footer, pageLabel(index + 1, total))) }
    }

    /** Sayfa sayısı önizlemesi için kısayol. */
    fun pageCount(report: DoctorReport): Int = layout(report) { _, _ -> "" }.size

    // ---- Satırlar ve sayfalayıcı ------------------------------------------------------------------------------

    /**
     * Sayfalanabilir en küçük birim: yüksekliği önceden bilinir, üst kenarı verilince komutlarını üretir.
     * [keepWithNext] bu satırla aynı sayfada kalması gereken sonraki (boşluk olmayan) satır sayısı;
     * [repeatOnBreak] sayfa kırılımında yeni sayfanın başına yeniden çizilecek satır (tablo başlığı);
     * [isGap] boşluk satırı: sayfa başında atlanır, sığmazsa yalnızca sayfayı kapatır.
     */
    private class Row(
        val height: Float,
        val keepWithNext: Int = 0,
        val repeatOnBreak: Row? = null,
        val isGap: Boolean = false,
        val draw: (top: Float) -> List<DrawOp>,
    )

    private fun gap(height: Float) = Row(height, isGap = true) { emptyList() }

    private inner class Pager(private val report: DoctorReport, private val bottom: Float) {
        val pages = ArrayList<MutableList<DrawOp>>()
        private var top = 0f
        private var y = 0f

        init {
            newPage()
        }

        private fun newPage() {
            val (contentTop, ops) = header(report, first = pages.isEmpty())
            pages += ops.toMutableList()
            top = contentTop
            y = top
        }

        private fun emit(row: Row) {
            pages.last().addAll(row.draw(y))
            y += row.height
        }

        fun place(rows: List<Row>) {
            var i = 0
            while (i < rows.size) {
                val row = rows[i]
                if (row.isGap) {
                    if (y > top) {
                        if (y + row.height <= bottom) y += row.height else newPage()
                    }
                    i++
                    continue
                }
                var needed = row.height
                var kept = 0
                var k = i + 1
                while (kept < row.keepWithNext && k < rows.size) {
                    val next = rows[k]
                    needed += next.height
                    if (!next.isGap) kept++
                    k++
                }
                // Sayfa başındaysak sığmasa da yerleştiririz; aksi hâlde sonsuz döngü olurdu.
                if (y + needed > bottom && y > top) {
                    newPage()
                    row.repeatOnBreak?.let { emit(it) }
                }
                emit(row)
                i++
            }
        }
    }

    // ---- Üst ve alt bilgi -------------------------------------------------------------------------------------

    private fun footerHeight() = FOOTER_LINE_GAP + lh(TextStyle.SMALL)

    private fun contentBottom() = pageHeight - margin - footerHeight() - FOOTER_GAP

    /** Üst bilgi komutları ve içeriğin başlayacağı y. */
    private fun header(report: DoctorReport, first: Boolean): Pair<Float, List<DrawOp>> {
        val ops = ArrayList<DrawOp>()
        var y = margin
        if (first) {
            if (report.title.isNotBlank()) {
                wrap(report.title, TextStyle.TITLE, contentWidth).forEach { line ->
                    ops += textOps(left, y, line, TextStyle.TITLE)
                    y += lh(TextStyle.TITLE)
                }
            }
            if (report.subtitle.isNotBlank()) {
                wrap(report.subtitle, TextStyle.SUBTITLE, contentWidth).forEach { line ->
                    ops += textOps(left, y, line, TextStyle.SUBTITLE)
                    y += lh(TextStyle.SUBTITLE)
                }
            }
        } else {
            val sameLine = width(report.title, TextStyle.SMALL) + HEADER_MIN_GAP + width(report.subtitle, TextStyle.SMALL) <= contentWidth
            ops += textOps(left, y, report.title, TextStyle.SMALL)
            if (!sameLine && report.title.isNotBlank() && report.subtitle.isNotBlank()) y += lh(TextStyle.SMALL)
            ops += textOps(right, y, report.subtitle, TextStyle.SMALL, TextAlign.RIGHT)
            y += lh(TextStyle.SMALL)
        }
        y += HEADER_GAP
        ops += DrawOp.Line(left, y, right, y, RULE_WIDTH)
        y += HEADER_GAP + HEADER_BELOW
        return y to ops
    }

    private fun footerOps(footer: String, label: String): List<DrawOp> {
        val lineY = pageHeight - margin - footerHeight()
        val textTop = lineY + FOOTER_LINE_GAP
        return listOf<DrawOp>(DrawOp.Line(left, lineY, right, lineY, RULE_WIDTH)) +
            textOps(left, textTop, footer, TextStyle.SMALL) +
            textOps(right, textTop, label, TextStyle.SMALL, TextAlign.RIGHT)
    }

    // ---- Bloklar ----------------------------------------------------------------------------------------------

    private fun sectionRows(section: ReportSection, maxNoteHeight: Float): List<Row> {
        val rows = ArrayList<Row>()
        rows += gap(SECTION_GAP_ABOVE)
        val lines = wrap(section.title, TextStyle.SECTION, contentWidth)
        lines.forEachIndexed { index, line ->
            val titleLinesLeft = lines.size - 1 - index
            rows += Row(lh(TextStyle.SECTION), keepWithNext = titleLinesLeft + ORPHAN_ROWS) { top -> textOps(left, top, line, TextStyle.SECTION) }
        }
        rows += gap(SECTION_GAP_BELOW)
        section.blocks.forEach { rows += blockRows(it, maxNoteHeight) }
        return rows
    }

    private fun blockRows(block: ReportBlock, maxNoteHeight: Float): List<Row> = when (block) {
        is ReportBlock.Paragraph -> paragraphRows(block.text, block.style) + gap(BLOCK_GAP)
        is ReportBlock.Bullets -> bulletRows(block.items) + gap(BLOCK_GAP)
        is ReportBlock.KeyValues -> keyValueRows(block.rows) + gap(BLOCK_GAP)
        is ReportBlock.Table -> tableRows(block) + gap(BLOCK_GAP)
        is ReportBlock.Note -> noteRows(block.text, maxNoteHeight) + gap(BLOCK_GAP)
        is ReportBlock.TemperatureChart -> listOf(chartRow(block), gap(BLOCK_GAP))
        is ReportBlock.Spacer -> listOf(gap(block.height))
    }

    private fun paragraphRows(text: String, style: TextStyle): List<Row> =
        wrap(text, style, contentWidth).map { line -> Row(lh(style)) { top -> textOps(left, top, line, style) } }

    private fun bulletRows(items: List<String>): List<Row> = items.flatMap { item ->
        wrap(item, TextStyle.BODY, contentWidth - BULLET_INDENT).mapIndexed { index, line ->
            Row(lh(TextStyle.BODY)) { top ->
                val ops = ArrayList<DrawOp>()
                if (index == 0) ops += textOps(left + BULLET_X, top, BULLET, TextStyle.BODY)
                ops += textOps(left + BULLET_INDENT, top, line, TextStyle.BODY)
                ops
            }
        }
    }

    private fun keyValueRows(rows: List<Pair<String, String>>): List<Row> {
        val keyWidth = contentWidth * KEY_COLUMN_FRACTION
        val valueX = left + keyWidth
        val valueWidth = contentWidth - keyWidth
        return rows.map { (key, value) ->
            val keyLines = wrap(key, TextStyle.BOLD, keyWidth - CELL_PAD_X)
            val valueLines = wrap(value, TextStyle.BODY, valueWidth - CELL_PAD_X)
            val height = max(keyLines.size * lh(TextStyle.BOLD), valueLines.size * lh(TextStyle.BODY)) + 2 * CELL_PAD_Y
            Row(height) { top ->
                val ops = ArrayList<DrawOp>()
                keyLines.forEachIndexed { i, line -> ops += textOps(left, top + CELL_PAD_Y + i * lh(TextStyle.BOLD), line, TextStyle.BOLD) }
                valueLines.forEachIndexed { i, line -> ops += textOps(valueX, top + CELL_PAD_Y + i * lh(TextStyle.BODY), line, TextStyle.BODY) }
                ops
            }
        }
    }

    private fun tableRows(table: ReportBlock.Table): List<Row> {
        val columns = max(table.header.size, table.rows.maxOfOrNull { it.size } ?: 0)
        if (columns == 0) return emptyList()
        val widths = columnWidths(table.weights, columns)
        val columnLeft = widths.runningFold(left) { acc, w -> acc + w }

        fun cellRow(cells: List<String>, style: TextStyle, underline: Boolean, keepWithNext: Int = 0, repeatOnBreak: Row? = null): Row {
            val wrapped = List(columns) { c -> wrap(cells.getOrElse(c) { "" }, style, widths[c] - 2 * CELL_PAD_X) }
            val lineCount = wrapped.maxOf { it.size }
            val height = lineCount * lh(style) + 2 * CELL_PAD_Y
            return Row(height, keepWithNext, repeatOnBreak) { top ->
                val ops = ArrayList<DrawOp>()
                wrapped.forEachIndexed { c, cellLines ->
                    cellLines.forEachIndexed { i, line -> ops += textOps(columnLeft[c] + CELL_PAD_X, top + CELL_PAD_Y + i * lh(style), line, style) }
                }
                if (underline) ops += DrawOp.Line(left, top + height, right, top + height, RULE_WIDTH)
                ops
            }
        }

        val header = if (table.header.isEmpty()) null else cellRow(table.header, TextStyle.BOLD, underline = true, keepWithNext = 1)
        val body = table.rows.map { cellRow(it, TextStyle.BODY, underline = false, repeatOnBreak = header) }
        return listOfNotNull(header) + body
    }

    private fun columnWidths(weights: List<Float>?, columns: Int): List<Float> {
        val usable = weights?.takeIf { it.size == columns && it.all { w -> w > 0f } }
        val total = usable?.sum() ?: columns.toFloat()
        return List(columns) { c -> contentWidth * (usable?.get(c) ?: 1f) / total }
    }

    /** Çerçeveli not; bir sayfaya sığmayacak kadar uzunsa satırlar sayfaya sığan parçalara bölünür, her parça çerçevelidir. */
    private fun noteRows(text: String, maxHeight: Float): List<Row> {
        val lineHeight = lh(TextStyle.BODY)
        val lines = wrap(text, TextStyle.BODY, contentWidth - 2 * NOTE_PAD)
        val maxLines = max(1, floor((maxHeight - 2 * NOTE_PAD) / lineHeight).toInt())
        return lines.chunked(maxLines).map { chunk ->
            val height = chunk.size * lineHeight + 2 * NOTE_PAD
            Row(height) { top ->
                val ops = ArrayList<DrawOp>()
                ops += DrawOp.Rect(left, top, contentWidth, height, filled = false, stroke = NOTE_STROKE)
                chunk.forEachIndexed { i, line -> ops += textOps(left + NOTE_PAD, top + NOTE_PAD + i * lineHeight, line, TextStyle.BODY) }
                ops
            }
        }
    }

    // ---- Sıcaklık grafiği -------------------------------------------------------------------------------------

    private fun chartRow(chart: ReportBlock.TemperatureChart): Row {
        val small = lh(TextStyle.SMALL)
        val height = CHART_TOP_PAD + small + CHART_LABEL_GAP + CHART_PLOT_HEIGHT + CHART_TICK_GAP + small + small + CHART_BOTTOM_PAD
        return Row(height) { top -> chartOps(chart, top) }
    }

    private fun chartOps(chart: ReportBlock.TemperatureChart, top: Float): List<DrawOp> {
        val ops = ArrayList<DrawOp>()
        val small = lh(TextStyle.SMALL)

        // y ekseni: onda birlik tam sayılarla belirleyici aralık ve adım.
        val (loTenths, hiTenths, stepTenths) = temperatureTicks(chart.points.map { it.celsius } + listOfNotNull(chart.coverlineCelsius))
        val tickTenths = (loTenths..hiTenths step stepTenths).toList()
        val tickTexts = tickTenths.map { tickLabel(it / 10.0) }
        val axisLabelWidth = tickTexts.maxOf { width(it, TextStyle.SMALL) }

        val plotLeft = left + axisLabelWidth + CHART_AXIS_GAP
        val plotRight = right - CHART_RIGHT_PAD
        val plotTop = top + CHART_TOP_PAD + small + CHART_LABEL_GAP
        val plotBottom = plotTop + CHART_PLOT_HEIGHT
        val lo = loTenths / 10.0
        val hi = hiTenths / 10.0
        fun yOf(celsius: Double): Float = (plotBottom - (celsius - lo) / (hi - lo) * CHART_PLOT_HEIGHT).toFloat()

        // x ekseni: döngü günü; her zaman 1'den başlar, işaretli günleri de kapsar.
        val days = chart.points.map { it.day } + chart.lhPositiveDays + listOfNotNull(chart.ovulationDay)
        val xMin = min(1, days.minOrNull() ?: 1)
        val xMax = max(days.maxOrNull() ?: DEFAULT_DAYS, xMin + 1)
        val span = xMax - xMin
        val stepDays = X_STEPS.firstOrNull { span / it + 1 <= MAX_X_TICKS } ?: ceil(span / (MAX_X_TICKS - 1).toDouble()).toInt()
        val usableWidth = plotRight - plotLeft - 2 * CHART_X_INSET
        fun xOf(day: Int): Float = plotLeft + CHART_X_INSET + (day - xMin).toFloat() / span * usableWidth

        // Kılavuz çizgileri ve y etiketleri (ekseninin solunda, sağa hizalı).
        tickTenths.forEachIndexed { index, tenths ->
            val y = yOf(tenths / 10.0)
            if (index > 0) ops += DrawOp.Line(plotLeft, y, plotRight, y, GRID_WIDTH)
            ops += DrawOp.Text(plotLeft - CHART_AXIS_GAP / 2, y + small * TICK_LABEL_BASELINE, tickTexts[index], TextStyle.SMALL, TextAlign.RIGHT)
        }
        // Eksenler.
        ops += DrawOp.Line(plotLeft, plotTop, plotLeft, plotBottom, AXIS_WIDTH)
        ops += DrawOp.Line(plotLeft, plotBottom, plotRight, plotBottom, AXIS_WIDTH)
        // Eksen başlıkları.
        ops += textOps(left, top + CHART_TOP_PAD, chart.yLabel, TextStyle.SMALL)
        val tickTextTop = plotBottom + CHART_TICK_GAP
        ops += textOps((plotLeft + plotRight) / 2, tickTextTop + small, chart.xLabel, TextStyle.SMALL, TextAlign.CENTER)
        // x etiketleri.
        var day = xMin
        while (day <= xMax) {
            ops += DrawOp.Text(xOf(day), tickTextTop + baseline(TextStyle.SMALL), day.toString(), TextStyle.SMALL, TextAlign.CENTER)
            day += stepDays
        }
        // Örtü çizgisi (kesikli) ve yumurtlama günü (dikey ince).
        chart.coverlineCelsius?.let { ops += DrawOp.Line(plotLeft, yOf(it), plotRight, yOf(it), COVERLINE_WIDTH, dashed = true) }
        chart.ovulationDay?.let { ops += DrawOp.Line(xOf(it), plotTop, xOf(it), plotBottom, OVULATION_WIDTH) }
        // Çizgi: bozuk olmayan ölçümler gün sırasıyla; üç günden uzun boşlukta çizgi kesilir.
        val included = chart.points.filter { !it.excluded }.sortedBy { it.day }
        val segments = ArrayList<MutableList<Pair<Float, Float>>>()
        var previous: ChartPoint? = null
        for (point in included) {
            if (previous == null || point.day - previous.day > MAX_LINE_GAP_DAYS) segments.add(ArrayList())
            segments.last().add(xOf(point.day) to yOf(point.celsius))
            previous = point
        }
        segments.filter { it.size >= 2 }.forEach { ops += DrawOp.Polyline(it, LINE_WIDTH) }
        // Noktalar: normal dolu, bozuk içi boş.
        chart.points.sortedWith(compareBy<ChartPoint> { it.day }.thenBy { it.celsius }).forEach { point ->
            ops += DrawOp.Circle(xOf(point.day), yOf(point.celsius), POINT_RADIUS, filled = !point.excluded)
        }
        // LH pozitif günler: x ekseninin hemen üstünde küçük dolu kare.
        chart.lhPositiveDays.sorted().forEach { d ->
            ops += DrawOp.Rect(xOf(d) - LH_SQUARE / 2, plotBottom - LH_SQUARE - CHART_TICK_GAP, LH_SQUARE, LH_SQUARE, filled = true)
        }
        return ops
    }

    // ---- Yardımcılar ------------------------------------------------------------------------------------------

    private fun lh(style: TextStyle) = measurer.lineHeight(style)

    private fun baseline(style: TextStyle) = lh(style) * BASELINE_RATIO

    private fun width(text: String, style: TextStyle) = measurer.width(text, style)

    /** [top] satırın üst kenarı; boş metin için komut üretilmez. */
    private fun textOps(x: Float, top: Float, text: String, style: TextStyle, align: TextAlign = TextAlign.LEFT): List<DrawOp> =
        if (text.isEmpty()) emptyList() else listOf(DrawOp.Text(x, top + baseline(style), text, style, align))

    /**
     * Kelime bazlı satır kaydırma: `\n` yeni satır; boşluklarda kırar; tek kelime sığmıyorsa karakterden
     * (kod noktasından) kırar. Boş paragraf tek boş satır üretir.
     */
    internal fun wrap(text: String, style: TextStyle, maxWidth: Float): List<String> {
        val out = ArrayList<String>()
        for (paragraph in text.split('\n')) {
            val words = paragraph.split(' ').filter { it.isNotEmpty() }
            if (words.isEmpty()) {
                out += ""
                continue
            }
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (width(candidate, style) <= maxWidth) {
                    line = candidate
                    continue
                }
                if (line.isNotEmpty()) {
                    out += line
                    line = ""
                }
                if (width(word, style) <= maxWidth) {
                    line = word
                    continue
                }
                // Sığmayan kelime: kod noktası kod noktası doldur, en az bir karakter her satıra.
                var chunk = ""
                var i = 0
                while (i < word.length) {
                    val codePoint = word.codePointAt(i)
                    val piece = String(Character.toChars(codePoint))
                    i += Character.charCount(codePoint)
                    val next = chunk + piece
                    if (chunk.isEmpty() || width(next, style) <= maxWidth) {
                        chunk = next
                    } else {
                        out += chunk
                        chunk = piece
                    }
                }
                line = chunk
            }
            out += line
        }
        return out
    }

    companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val MARGIN = 40f
        const val RULE_WIDTH = 0.5f

        /** Taban çizgisinin satır üst kenarına uzaklığı (satır yüksekliğinin oranı); Roboto ölçüleriyle ~0,73. */
        private const val BASELINE_RATIO = 0.75f
        private const val HEADER_GAP = 4f
        private const val HEADER_BELOW = 8f
        private const val HEADER_MIN_GAP = 12f
        private const val FOOTER_LINE_GAP = 4f
        private const val FOOTER_GAP = 8f
        private const val BLOCK_GAP = 4f
        private const val SECTION_GAP_ABOVE = 10f
        private const val SECTION_GAP_BELOW = 3f
        /** Bölüm başlığının altında aynı sayfada kalması gereken satır sayısı. */
        private const val ORPHAN_ROWS = 2
        private const val BULLET = "•"
        private const val BULLET_X = 4f
        private const val BULLET_INDENT = 14f
        private const val KEY_COLUMN_FRACTION = 0.35f
        private const val CELL_PAD_X = 3f
        private const val CELL_PAD_Y = 2f
        private const val NOTE_PAD = 6f
        private const val NOTE_STROKE = 0.75f

        const val CHART_PLOT_HEIGHT = 160f
        private const val CHART_TOP_PAD = 4f
        private const val CHART_LABEL_GAP = 6f
        private const val CHART_TICK_GAP = 3f
        private const val CHART_BOTTOM_PAD = 8f
        private const val CHART_AXIS_GAP = 6f
        private const val CHART_X_INSET = 6f
        private const val CHART_RIGHT_PAD = 4f
        private const val TICK_LABEL_BASELINE = 0.3f
        private const val AXIS_WIDTH = 0.75f
        private const val GRID_WIDTH = 0.25f
        private const val COVERLINE_WIDTH = 0.75f
        private const val OVULATION_WIDTH = 0.5f
        private const val LINE_WIDTH = 1f
        private const val POINT_RADIUS = 2.5f
        private const val LH_SQUARE = 4f
        private const val MAX_Y_TICKS = 8
        private const val MAX_X_TICKS = 15
        private const val MAX_LINE_GAP_DAYS = 3
        private const val DEFAULT_DAYS = 28
        private const val DEFAULT_LO_TENTHS = 360
        private const val DEFAULT_HI_TENTHS = 370
        private const val EPS = 1e-6
        private val X_STEPS = listOf(1, 2, 5, 7, 10, 14, 28, 56)

        /**
         * Sıcaklık ekseni, onda birlik tam sayılarla (36,3 → 363): alt sınır (min − 0,2), üst sınır (max + 0,2, adıma
         * hizalı) ve adım. Etiket sayısı en fazla [MAX_Y_TICKS]; sığdıkça adım 0,1'dir. Boş girdi 36,0–37,0 verir.
         */
        internal fun temperatureTicks(values: List<Double>): Triple<Int, Int, Int> {
            val minValue = values.minOrNull()
            val maxValue = values.maxOrNull()
            val lo = if (minValue == null) DEFAULT_LO_TENTHS else floor(minValue * 10 + EPS).toInt() - 2
            val hi = if (maxValue == null) DEFAULT_HI_TENTHS else ceil(maxValue * 10 - EPS).toInt() + 2
            val step = max(1, ceil((hi - lo) / (MAX_Y_TICKS - 1).toDouble()).toInt())
            val alignedHi = lo + step * ceil((hi - lo).toDouble() / step).toInt()
            return Triple(lo, alignedHi, step)
        }
    }
}

private fun defaultTickLabel(celsius: Double): String = String.format(Locale.forLanguageTag("tr"), "%.1f", celsius)
