package com.ayla.cycle

import java.time.LocalDate

/**
 * Bazal vücut ısısı ölçümü (°C).
 *
 * @property disturbed Ölçümü bozan bir etken vardı (ateş, alkol, az ya da kesintili uyku, geç ölçüm,
 * ilaç, yolculuk). Sensiplan'da bu "bozucu etkenler" (Störfaktoren) kullanıcı tarafından işaretlenir ve
 * işaretli ölçüm örtü çizgisi ile yükselme hesabına girmez. Varsayılan false; eski çağrılar değişmez.
 */
data class TemperatureReading(
    val date: LocalDate,
    val celsius: Double,
    val disturbed: Boolean = false,
)

/** Art arda yüksek ölçümlerle doğrulanmış yumurtlama. */
data class OvulationEstimate(
    /** Tahmini yumurtlama günü: ilk yüksek ölçümden bir önceki gün. */
    val ovulationDate: LocalDate,
    /** Önceki altı geçerli ölçümün en yükseği; "örtü çizgisi". */
    val coverlineCelsius: Double,
    /** Örtü çizgisinin üstündeki ilk ölçümün günü. */
    val firstHighReading: LocalDate,
    /**
     * Doğrulamayı tamamlayan ölçümün günü: standart kuralda üçüncü, 1. istisna kuralında dördüncü
     * yüksek ölçüm. Bu günden önce yumurtlama "doğrulanmış" sayılamaz.
     */
    val confirmedOn: LocalDate,
)

/**
 * Semptotermal yöntemin "üç yüksek ölçüm" kuralı (3-over-6):
 * önceki altı geçerli ölçümün en yükseğinin üstünde art arda üç ölçüm ve üçüncüsü
 * örtü çizgisinin en az 0,2 °C üstünde olmalı.
 *
 * Sensiplan 1. istisna kuralı: üçüncü ölçüm örtü çizgisinin +0,2 °C üstüne çıkmıyorsa ama dördüncü
 * ardışık ölçüm de örtü çizgisinin üstündeyse doğrulama dördüncü ölçümle tamamlanır (dördüncünün
 * +0,2 °C koşulu yoktur). 2. istisna kuralı (üç yüksek ölçümden birinin çizgiye düşmesi) bu sürümde
 * uygulanmaz; bkz. ADR-0005. Kaynak: Arbeitsgruppe NFP, "Natürlich und sicher" (Sensiplan el kitabı);
 * Frank-Herrmann ve ark., Human Reproduction 2007;22(5):1310-1319.
 *
 * Yalnızca geçerli ölçümler kullanılır: bozuk işaretli olmayan ve 35,0–38,5 °C aralığındaki değerler.
 * Aralık klinik bir eşik değil, giriş hatası süzgecidir: 35 °C altı hipotermi sınırıdır (NHS),
 * 38,5 °C ise ateş eşiğinin (NHS: 38 °C) belirgin üstüdür; bazal ölçümde bu bandın dışı akla yatkın değildir.
 * Ateşli günler kullanıcı tarafından "bozuk" olarak işaretlenir; motor bunu kendiliğinden çıkarsamaz.
 *
 * PCOS'ta LH testleri yanıltabildiği için uygulama yumurtlamayı ancak bu kuralla
 * "doğrulanmış" sayar. Tıbbi karar aracı değildir; doğum kontrolü için kullanılmaz.
 */
object OvulationDetector {
    private const val THIRD_DAY_MARGIN = 0.2
    private const val EPSILON = 1e-9

    /** Örtü çizgisi için ilk yüksek ölçümden önce gereken geçerli ölçüm sayısı. */
    const val MIN_PRIOR_READINGS = 6

    /** Hesaba giren en düşük bazal ısı (°C); altı giriş hatası ya da hipotermi sayılır. */
    const val MIN_PLAUSIBLE_CELSIUS = 35.0

    /** Hesaba giren en yüksek bazal ısı (°C); üstü giriş hatası ya da belirgin ateş sayılır. */
    const val MAX_PLAUSIBLE_CELSIUS = 38.5

    /** Kuralın uygulanabilmesi için bir döngüde gereken en az geçerli ölçüm sayısı (6 önceki + 3 yüksek). */
    const val MIN_READINGS_FOR_RULE = MIN_PRIOR_READINGS + 3

    /** Ölçüm örtü çizgisi ve yükselme hesabına girebilir mi: bozuk değil ve akla yatkın aralıkta. */
    fun isUsable(reading: TemperatureReading): Boolean =
        !reading.disturbed && reading.celsius in MIN_PLAUSIBLE_CELSIUS..MAX_PLAUSIBLE_CELSIUS

    /**
     * [readings] bir döngüye ait ölçümlerdir (sıralı olması gerekmez). Ölçümler arasında
     * atlanmış günler olabilir; kural ölçüm sırasına göre uygulanır. Bozuk ve aralık dışı ölçümler
     * hiç yokmuş gibi atlanır; aynı güne düşen ölçümlerden ilki alınır.
     */
    fun detect(readings: List<TemperatureReading>): OvulationEstimate? {
        val sorted = readings.filter(::isUsable).distinctBy { it.date }.sortedBy { it.date }
        if (sorted.size < MIN_READINGS_FOR_RULE) return null
        for (i in MIN_PRIOR_READINGS..sorted.size - 3) {
            val coverline = sorted.subList(i - MIN_PRIOR_READINGS, i).maxOf { it.celsius }
            val first = sorted[i]
            val second = sorted[i + 1]
            val third = sorted[i + 2]
            val allAbove = first.celsius > coverline + EPSILON &&
                second.celsius > coverline + EPSILON &&
                third.celsius > coverline + EPSILON
            if (!allAbove) continue
            val thirdHighEnough = third.celsius >= coverline + THIRD_DAY_MARGIN - EPSILON
            val confirmedOn = when {
                thirdHighEnough -> third.date
                // 1. istisna kuralı: dördüncü ardışık ölçüm de çizginin üstündeyse doğrulanır.
                i + 3 < sorted.size && sorted[i + 3].celsius > coverline + EPSILON -> sorted[i + 3].date
                else -> null
            }
            if (confirmedOn != null) {
                return OvulationEstimate(
                    ovulationDate = first.date.minusDays(1),
                    coverlineCelsius = coverline,
                    firstHighReading = first.date,
                    confirmedOn = confirmedOn,
                )
            }
        }
        return null
    }
}
