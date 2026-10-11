package com.ayla.cycle

import java.time.LocalDate

/** Bazal vücut ısısı ölçümü (°C). */
data class TemperatureReading(val date: LocalDate, val celsius: Double)

/** Üç yüksek ölçümle doğrulanmış yumurtlama. */
data class OvulationEstimate(
    /** Tahmini yumurtlama günü: ilk yüksek ölçümden bir önceki gün. */
    val ovulationDate: LocalDate,
    /** Önceki altı ölçümün en yükseği; "örtü çizgisi". */
    val coverlineCelsius: Double,
    val firstHighReading: LocalDate,
)

/**
 * Semptotermal yöntemin "üç yüksek ölçüm" kuralı (3-over-6):
 * önceki altı geçerli ölçümün en yükseğinin üstünde art arda üç ölçüm ve üçüncüsü
 * örtü çizgisinin en az 0,2 °C üstünde olmalı.
 *
 * PCOS'ta LH testleri yanıltabildiği için uygulama yumurtlamayı ancak bu kuralla
 * "doğrulanmış" sayar. Tıbbi karar aracı değildir; doğum kontrolü için kullanılmaz.
 */
object OvulationDetector {
    private const val MIN_PRIOR_READINGS = 6
    private const val THIRD_DAY_MARGIN = 0.2
    private const val EPSILON = 1e-9

    /**
     * [readings] bir döngüye ait ölçümlerdir (sıralı olması gerekmez). Ölçümler arasında
     * atlanmış günler olabilir; kural ölçüm sırasına göre uygulanır.
     */
    fun detect(readings: List<TemperatureReading>): OvulationEstimate? {
        val sorted = readings.distinctBy { it.date }.sortedBy { it.date }
        if (sorted.size < MIN_PRIOR_READINGS + 3) return null
        for (i in MIN_PRIOR_READINGS..sorted.size - 3) {
            val coverline = sorted.subList(i - MIN_PRIOR_READINGS, i).maxOf { it.celsius }
            val first = sorted[i]
            val second = sorted[i + 1]
            val third = sorted[i + 2]
            val allAbove = first.celsius > coverline + EPSILON &&
                second.celsius > coverline + EPSILON &&
                third.celsius > coverline + EPSILON
            val thirdHighEnough = third.celsius >= coverline + THIRD_DAY_MARGIN - EPSILON
            if (allAbove && thirdHighEnough) {
                return OvulationEstimate(
                    ovulationDate = first.date.minusDays(1),
                    coverlineCelsius = coverline,
                    firstHighReading = first.date,
                )
            }
        }
        return null
    }
}
