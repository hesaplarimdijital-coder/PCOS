package com.ayla.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Motorun ayarlanabilir eşikleri. Varsayılanlar 2023 uluslararası PCOS/PMOS kılavuzu ve
 * NHS endometrium koruma pratiğine dayanır (bkz. docs/arastirma.md).
 */
data class CycleEngineConfig(
    /** Bu değerin altındaki "döngüler" ara kanama sayılır ve istatistiğe girmez. */
    val minPlausibleCycleDays: Int = 14,
    /** Tahminde kullanılan en fazla döngü sayısı (en yeniler). */
    val recentCyclesForPrediction: Int = 6,
    /** Eski döngülerin ağırlığı her adımda bu oranla azalır (0 < decay < 1). */
    val recencyDecay: Double = 0.65,
    /** Aralığın alt sınırı için yayılım çarpanı. */
    val lowerSpreadMultiplier: Double = 1.0,
    /** Üst sınır için yayılım çarpanı; PCOS'ta gecikme daha olası olduğundan daha geniş. */
    val upperSpreadMultiplier: Double = 1.5,
    /** Yayılımın asla düşmeyeceği taban (gün). */
    val minSpreadDays: Double = 3.0,
    /** Yayılımın tavanı (gün); çok dağınık geçmişte aralık anlamsızca büyümesin. */
    val maxSpreadDays: Double = 21.0,
    /** Aralığın en erken başlayabileceği döngü günü. */
    val minPredictedDay: Int = 15,
    /** Aralığın en küçük genişliği (gün). */
    val minWindowWidthDays: Int = 4,
    /** Nazik uzun döngü hatırlatmasının günü. */
    val gentleAlertDay: Int = 60,
    /** Doktora başvuru önerisinin günü (yaklaşık 3 ay). */
    val seeDoctorAlertDay: Int = 90,
    /** Yılda bu sayının altında adet "seyrek adet" işareti verir. */
    val minPeriodsPerYear: Int = 4,
)

/**
 * Döngü motoru: adet kayıtlarından döngüleri, istatistikleri, aralıklı tahmini ve sağlık
 * işaretlerini üretir. Saf fonksiyonlardan oluşur; "bugün" her çağrıda dışarıdan verilir.
 */
class CycleEngine(private val config: CycleEngineConfig = CycleEngineConfig()) {

    /** Kayıtları sıralar, aynı güne düşenleri birleştirir ve döngülere dönüştürür. */
    fun buildCycles(records: List<PeriodRecord>): List<Cycle> {
        val sorted = records
            .groupBy { it.start }
            .map { (_, group) ->
                // Aynı gün iki kayıt varsa en geniş olanı ve hariç tutma işaretini koru.
                PeriodRecord(
                    start = group.first().start,
                    end = group.mapNotNull { it.end }.maxOrNull(),
                    excludedFromStats = group.any { it.excludedFromStats },
                )
            }
            .sortedBy { it.start }
        return sorted.mapIndexed { index, record ->
            val next = sorted.getOrNull(index + 1)?.start
            val length = next?.let { ChronoUnit.DAYS.between(record.start, it).toInt() }
            val exclusion = when {
                record.excludedFromStats -> ExclusionReason.USER
                length != null && length < config.minPlausibleCycleDays -> ExclusionReason.IMPLAUSIBLY_SHORT
                else -> null
            }
            Cycle(start = record.start, nextStart = next, exclusion = exclusion)
        }
    }

    fun stats(cycles: List<Cycle>, today: LocalDate): CycleStats {
        val lengths = cycles.filter { it.countsForStats }.mapNotNull { it.lengthDays }
        val yearAgo = today.minusDays(365)
        val periodsInYear = cycles.count { !it.start.isBefore(yearAgo) && !it.start.isAfter(today) }
        if (lengths.isEmpty()) {
            return CycleStats(lengths, null, null, null, null, null, periodsInYear)
        }
        val mean = lengths.average()
        val sorted = lengths.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2].toDouble()
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val sd = if (lengths.size > 1) {
            sqrt(lengths.sumOf { (it - mean).pow(2) } / (lengths.size - 1))
        } else {
            null
        }
        return CycleStats(
            lengths = lengths,
            mean = mean,
            median = median,
            standardDeviation = sd,
            shortest = sorted.first(),
            longest = sorted.last(),
            periodsInTrailingYear = periodsInYear,
        )
    }

    /**
     * Aralıklı tahmin. Yakın döngüler daha ağır sayılır; yayılım, ağırlıklı ortalama mutlak
     * sapmadan türetilir ve alt sınır [CycleEngineConfig.minSpreadDays] ile korunur.
     */
    fun predict(cycles: List<Cycle>, today: LocalDate): Prediction {
        val last = cycles.lastOrNull() ?: return Prediction.NotEnoughData
        val recent = cycles
            .filter { it.countsForStats }
            .mapNotNull { it.lengthDays }
            .takeLast(config.recentCyclesForPrediction)
        if (recent.isEmpty()) return Prediction.NotEnoughData

        val n = recent.size
        val weights = recent.indices.map { i -> config.recencyDecay.pow(n - 1 - i) }
        val weightSum = weights.sum()
        val center = recent.indices.sumOf { recent[it] * weights[it] } / weightSum

        // Ağırlıklı ortalama mutlak sapma; normal dağılımda 1,25 çarpanı standart sapmaya yaklaşır.
        val rawSpread = weightedMad(recent, weights, center) * 1.25
        val spread = when (n) {
            1 -> max(config.minSpreadDays, 7.0)
            2 -> max(config.minSpreadDays, max(5.0, rawSpread))
            else -> max(config.minSpreadDays, rawSpread)
        }.coerceAtMost(config.maxSpreadDays)

        var lowDay = (center - config.lowerSpreadMultiplier * spread).roundToInt()
        var highDay = (center + config.upperSpreadMultiplier * spread).roundToInt()
        lowDay = max(lowDay, config.minPredictedDay)
        if (highDay - lowDay < config.minWindowWidthDays) highDay = lowDay + config.minWindowWidthDays

        // Güven, tabana çekilmemiş ham yayılıma ve döngü sayısına bakar.
        val confidence = when {
            n >= 4 && rawSpread <= 3.0 && recent.max() <= 45 -> Confidence.HIGH
            n >= 2 && rawSpread / center <= 0.25 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        // Aralık, son adet başlangıcından sayılır: 1. gün başlangıç günüdür.
        val earliest = last.start.plusDays((lowDay - 1).toLong())
        val latest = last.start.plusDays((highDay - 1).toLong())
        return if (today.isAfter(latest)) {
            Prediction.PastWindow(
                latestExpected = latest,
                daysPast = ChronoUnit.DAYS.between(latest, today).toInt(),
                basedOnCycles = n,
            )
        } else {
            Prediction.Window(
                earliest = earliest,
                latest = latest,
                expectedLengthDays = center.roundToInt(),
                confidence = confidence,
                basedOnCycles = n,
            )
        }
    }

    /** Bugünün döngü günü: başlangıç günü 1'dir. */
    fun currentCycleDay(cycles: List<Cycle>, today: LocalDate): Int? {
        val last = cycles.lastOrNull { !it.start.isAfter(today) } ?: return null
        return ChronoUnit.DAYS.between(last.start, today).toInt() + 1
    }

    fun flags(cycles: List<Cycle>, stats: CycleStats, today: LocalDate, trackingSince: LocalDate?): HealthFlags {
        val day = currentCycleDay(cycles, today)
        val longCycle = when {
            day == null -> LongCycleAlert.NONE
            day >= config.seeDoctorAlertDay -> LongCycleAlert.SEE_DOCTOR
            day >= config.gentleAlertDay -> LongCycleAlert.GENTLE
            else -> LongCycleAlert.NONE
        }
        val trackedForAYear = trackingSince != null &&
            ChronoUnit.DAYS.between(trackingSince, today) >= 365
        val few = trackedForAYear && stats.periodsInTrailingYear < config.minPeriodsPerYear
        return HealthFlags(longCycle = longCycle, fewPeriodsInTrailingYear = few)
    }

    /** Tüm hesapları tek çağrıda yapar; arayüz katmanı bunu kullanır. */
    fun overview(records: List<PeriodRecord>, today: LocalDate, trackingSince: LocalDate? = null): CycleOverview {
        // Gelecek tarihli kayıtlar (yanlış giriş) döngü hesabına hiç girmez.
        val cycles = buildCycles(records.filter { !it.start.isAfter(today) })
        val stats = stats(cycles, today)
        val tracking = trackingSince ?: cycles.firstOrNull()?.start
        return CycleOverview(
            cycles = cycles,
            lastPeriodStart = cycles.lastOrNull()?.start,
            currentCycleDay = currentCycleDay(cycles, today),
            stats = stats,
            prediction = predict(cycles, today),
            flags = flags(cycles, stats, today, tracking),
        )
    }

    /**
     * Takvim için bir günün türü. Bitişi bilinmeyen bir adet, en fazla [defaultPeriodLengthDays]
     * gün sürmüş sayılır; bir sonraki adete ya da bugüne taşmaz.
     */
    fun dayKind(
        date: LocalDate,
        records: List<PeriodRecord>,
        prediction: Prediction,
        today: LocalDate,
        defaultPeriodLengthDays: Int = 5,
    ): DayKind {
        val sorted = records.sortedBy { it.start }
        for ((index, record) in sorted.withIndex()) {
            val nextStart = sorted.getOrNull(index + 1)?.start
            var end = record.end ?: record.start.plusDays((defaultPeriodLengthDays - 1).toLong())
            if (record.end == null && end.isAfter(today)) end = today
            if (nextStart != null && !end.isBefore(nextStart)) end = nextStart.minusDays(1)
            if (!date.isBefore(record.start) && !date.isAfter(end)) return DayKind.PERIOD
        }
        if (prediction is Prediction.Window &&
            !date.isBefore(prediction.earliest) && !date.isAfter(prediction.latest)
        ) {
            return DayKind.PREDICTED
        }
        return DayKind.NONE
    }

    private fun weightedMad(values: List<Int>, weights: List<Double>, center: Double): Double {
        val weightSum = weights.sum()
        return values.indices.sumOf { abs(values[it] - center) * weights[it] } / weightSum
    }
}
