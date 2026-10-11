package com.ayla.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Kullanıcının kaydettiği bir adet.
 *
 * @property start Adetin ilk günü.
 * @property end Adetin son günü; bilinmiyorsa null.
 * @property excludedFromStats Kullanıcı bu adetle başlayan döngüyü istatistik dışı bıraktıysa true
 * (ör. ilaç değişikliği gibi bir istisna).
 */
data class PeriodRecord(
    val start: LocalDate,
    val end: LocalDate? = null,
    val excludedFromStats: Boolean = false,
)

/** Neden bir döngünün istatistiğe katılmadığı. */
enum class ExclusionReason {
    /** Kullanıcı elle hariç tuttu. */
    USER,

    /** Uzunluk [CycleEngineConfig.minPlausibleCycleDays] altında; büyük olasılıkla ara kanama. */
    IMPLAUSIBLY_SHORT,
}

/**
 * İki adet başlangıcı arasındaki döngü. Son döngü henüz bitmemiştir ([nextStart] null).
 */
data class Cycle(
    val start: LocalDate,
    val nextStart: LocalDate?,
    val exclusion: ExclusionReason? = null,
) {
    val lengthDays: Int?
        get() = nextStart?.let { ChronoUnit.DAYS.between(start, it).toInt() }

    val isComplete: Boolean
        get() = nextStart != null

    /** Tamamlanmış ve hariç tutulmamış döngüler istatistiğe girer. */
    val countsForStats: Boolean
        get() = isComplete && exclusion == null
}

/** Döngü uzunluklarının özeti. Tüm değerler yalnızca istatistiğe giren döngülerden hesaplanır. */
data class CycleStats(
    val lengths: List<Int>,
    val mean: Double?,
    val median: Double?,
    val standardDeviation: Double?,
    val shortest: Int?,
    val longest: Int?,
    /** Bugünden geriye 365 gün içindeki adet başlangıcı sayısı (hariç tutulanlar dahil). */
    val periodsInTrailingYear: Int,
) {
    val count: Int get() = lengths.size
}

/** Tahminin güven düzeyi; arayüzde üç noktayla ve sözcükle gösterilir. */
enum class Confidence { LOW, MEDIUM, HIGH }

/** Bir sonraki adetin ne zaman beklendiği. Tek bir tarih değil, her zaman bir aralık. */
sealed interface Prediction {
    /** Hiç tamamlanmış döngü yok; aralık çizilmez. */
    data object NotEnoughData : Prediction

    /**
     * Beklenen aralık. [earliest] ve [latest] tarihleri dahildir.
     * [expectedLengthDays] aralığın merkezidir ve yalnızca bilgi amaçlıdır.
     */
    data class Window(
        val earliest: LocalDate,
        val latest: LocalDate,
        val expectedLengthDays: Int,
        val confidence: Confidence,
        val basedOnCycles: Int,
    ) : Prediction

    /** Bugün beklenen aralığın sonunu geçti; döngü beklenenden uzun sürüyor. */
    data class PastWindow(
        val latestExpected: LocalDate,
        val daysPast: Int,
        val basedOnCycles: Int,
    ) : Prediction
}

/** Uzun döngü uyarısının kademesi. */
enum class LongCycleAlert {
    NONE,

    /** 60. gün: nazik hatırlatma. */
    GENTLE,

    /** 90. gün: doktora başvuru önerisi (önce hamilelik dışlanır). */
    SEE_DOCTOR,
}

/** Sağlıkla ilgili işaretler; metinleri arayüz belirler. */
data class HealthFlags(
    val longCycle: LongCycleAlert,
    /** Son 12 ayda 4'ten az adet ve en az 12 aydır kayıt tutuluyor. */
    val fewPeriodsInTrailingYear: Boolean,
)

/** Motorun tek seferde ürettiği genel görünüm. */
data class CycleOverview(
    val cycles: List<Cycle>,
    val lastPeriodStart: LocalDate?,
    /** Bugün döngünün kaçıncı günü (1 tabanlı); kayıt yoksa null. */
    val currentCycleDay: Int?,
    val stats: CycleStats,
    val prediction: Prediction,
    val flags: HealthFlags,
)

/** Takvimde bir günün türü. */
enum class DayKind {
    /** Kayıtlı adet günü. */
    PERIOD,

    /** Tahmini adet aralığında. */
    PREDICTED,

    NONE,
}
