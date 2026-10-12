package com.ayla.pcos.ui.fertility

import com.ayla.cycle.CycleFertility
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Grafikteki bir ölçüm noktası: döngü günü (1 = adet başlangıcı), °C, hesaba girip girmediği. */
internal data class GrafikNoktasi(val day: Int, val celsius: Double, val excluded: Boolean)

/**
 * Isı grafiğinin saf ölçeği (#24): x ekseni döngü günleri, y ekseni °C. Çizim katmanı yalnızca bu değerleri
 * piksele çevirir; böylece eksen aralığı ve etiketler JVM'de test edilir.
 *
 * - y aralığı: en düşük − 0,2 … en yüksek + 0,2, 0,1'e yuvarlanır; en az 0,6 °C genişlik (düz grafik yassılmasın).
 * - y etiketleri 0,1 adımla, sekizden fazlaysa 0,2 adımla.
 * - Hesaba girmeyen ve aralık dışı noktalar eksen aralığını değiştirmez; çizimde kenara kırpılır ([yOf]).
 * - x ekseni 1. günden başlar; en az 14 gün, açık döngüde bugüne kadar.
 */
internal data class IsiGrafigiOlcegi(
    val points: List<GrafikNoktasi>,
    val minCelsius: Double,
    val maxCelsius: Double,
    val lastDay: Int,
    val coverline: Double?,
    val lhDays: Set<Int>,
    val ovulationDay: Int?,
) {
    val yTicks: List<Double>
        get() {
            val tenths = ((maxCelsius - minCelsius) * 10).roundToInt()
            val step = if (tenths > MAX_Y_LABELS) 2 else 1
            return (0..tenths step step).map { ((minCelsius * 10).roundToInt() + it) / 10.0 }
        }

    /** 0 (alt) … 1 (üst) arası dikey konum; aralık dışı değerler kenara kırpılır. */
    fun yOf(celsius: Double): Float = ((celsius - minCelsius) / (maxCelsius - minCelsius)).toFloat().coerceIn(0f, 1f)

    /** 0 (1. gün) … 1 (son gün) arası yatay konum. */
    fun xOf(day: Int): Float = if (lastDay <= 1) 0f else ((day - 1).toFloat() / (lastDay - 1)).coerceIn(0f, 1f)

    /** x etiketleri: 1, 7, 14, 21, … ve son gün (çakışmıyorsa). */
    val xTicks: List<Int>
        get() = buildList {
            add(1)
            var d = 7
            while (d < lastDay) {
                add(d)
                d += 7
            }
            if (lastDay - last() >= 3) add(lastDay)
        }

    /** Çizgi parçaları: yalnızca geçerli ölçümler; 3 günden uzun boşlukta çizgi kesilir. */
    val segments: List<List<GrafikNoktasi>>
        get() {
            val valid = points.filter { !it.excluded }
            val result = mutableListOf<MutableList<GrafikNoktasi>>()
            valid.forEach { p ->
                val current = result.lastOrNull()
                if (current == null || p.day - current.last().day > MAX_GAP_DAYS) result += mutableListOf(p) else current += p
            }
            return result
        }

    companion object {
        const val MIN_DAYS = 14
        const val MIN_SPAN = 0.6
        const val MARGIN = 0.2
        const val MAX_Y_LABELS = 8
        const val MAX_GAP_DAYS = 3

        /** 0,1'e aşağı/yukarı yuvarlama; kayan nokta artığı (36,099999…) bir alt onluğa düşürmesin. */
        private fun floorTenth(x: Double): Double = floor(x * 10 + EPS) / 10

        private fun ceilTenth(x: Double): Double = ceil(x * 10 - EPS) / 10

        private const val EPS = 1e-6

        fun of(cycle: CycleFertility, today: LocalDate): IsiGrafigiOlcegi {
            fun day(date: LocalDate) = (ChronoUnit.DAYS.between(cycle.start, date) + 1).toInt()
            val points = cycle.readings.map { GrafikNoktasi(day(it.date), it.celsius, it.excluded) }
            val endDay = cycle.end?.let { day(it) } ?: day(today)
            val lastDay = maxOf(MIN_DAYS, endDay, points.maxOfOrNull { it.day } ?: 0)
            // Aralık geçerli ölçümlerden; hesaba girmeyen (ör. 3,65 gibi yazım hatası) nokta ekseni bozmasın, kenara kırpılır.
            val values = points.filter { !it.excluded }.ifEmpty { points.filter { it.celsius in 34.0..40.0 } }.map { it.celsius }
            var lo = if (values.isEmpty()) 36.2 else floorTenth(values.min() - MARGIN)
            var hi = if (values.isEmpty()) 37.0 else ceilTenth(values.max() + MARGIN)
            if (hi - lo < MIN_SPAN) {
                val pad = (MIN_SPAN - (hi - lo)) / 2
                lo = floorTenth(lo - pad)
                hi = ceilTenth(hi + pad)
            }
            return IsiGrafigiOlcegi(
                points = points,
                minCelsius = lo,
                maxCelsius = hi,
                lastDay = lastDay,
                coverline = cycle.coverlineCelsius,
                lhDays = cycle.lhPositiveDates.map { day(it) }.toSet(),
                ovulationDay = cycle.ovulation?.let { day(it.ovulationDate) },
            )
        }
    }
}
