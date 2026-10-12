package com.ayla.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Doğurganlık motoru (MVP 3, #24): günlük doğurganlık kayıtlarını adet başlangıçlarına göre döngülere
 * böler, her döngüde yumurtlamayı yalnızca ısı yükselmesiyle doğrular ([OvulationDetector]) ve
 * arayüzün metne dönüştüreceği notlar üretir. Saf fonksiyonlardan oluşur; "bugün" dışarıdan verilir.
 * Aynı girdi aynı çıktıyı verir; liste sırası sonucu değiştirmez; aynı güne iki kayıt varsa listede
 * sonuncusu kazanır. Hap modunda hiçbir hesap yapılmaz (ADR-0005, CLAUDE.md).
 *
 * Tasarımdan küçük sapmalar (gerekçeli):
 * - [CycleFertility.entries] eklendi: arayüz, mukus ve LH satırlarını grafikle hizalamak için döngüye
 *   düşen kayıtlara bölme mantığını yinelemeden erişsin.
 * - [FertilityNote.LH_POSITIVE_WITHOUT_RISE] için LH pozitif gününe kadar (o gün dahil) en az
 *   [OvulationDetector.MIN_PRIOR_READINGS] geçerli ölçüm de aranır: örtü çizgisi altı önceki ölçüm ister;
 *   bunlar yoksa yükselme zaten saptanamazdı ve "LH yanıltıcı" yerine "veri az" demek doğrudur.
 * - [FertilityNote.RISE_WITHOUT_LH_POSITIVE] yalnızca en az bir negatif LH kaydı varken verilir;
 *   yalnızca geçersiz testler "zamanlama" sonucu çıkarmaya yetmez.
 *
 * Eşiklerin dayanağı:
 * - 9 ölçüm = 6 önceki + 3 yüksek; kuralın asgarisi ([OvulationDetector.MIN_READINGS_FOR_RULE]).
 * - LH pozitifinden sonra 5 ölçüm günü: LH yükselişinden yumurtlamaya 24–36 saat, ısı yükselmesi
 *   yumurtlamadan 1–2 gün sonra, üçüncü yüksek ölçüm pozitiften yaklaşık 5 gün sonra (Direito ve ark.,
 *   Fertil Steril 2013; Sensiplan). PCOS'ta bazal LH yüksekliği LH testini yanlış pozitif yapabilir
 *   (2023 uluslararası PCOS kılavuzu; OPK üretici talimatlarındaki PCOS uyarısı).
 * - 18 gün: yumurtlama sonrası 18'i aşan gün ve adet yokluğu, doğal aile planlamasında gebelik
 *   olasılığının klasik işaretidir; normal luteal faz 11–17 gün (Lenton ve ark., BJOG 1984; Crawford ve
 *   ark., Fertil Steril 2017). Not tanı değildir; test ve hekim yönlendirmesi arayüz metnindedir.
 * - Bozuk ölçüm oranı ≥ %30 ve en az 3: klinik eşik değil, grafiğin okunabilirliği için ürün sezgisi.
 */
object FertilityEngine {
    /** Doğrulama yokken bu sayının altındaki geçerli ölçüm "veri az" notu verir. */
    private const val MIN_VALID_READINGS = OvulationDetector.MIN_READINGS_FOR_RULE

    /** LH pozitifinden sonra yükselme beklenen geçerli ölçüm sayısı. */
    private const val READINGS_AFTER_LH_POSITIVE = 5

    /** Bozuk ölçüm notu için en az bozuk ölçüm sayısı ve yüzdesi. */
    private const val MANY_DISTURBED_MIN_COUNT = 3
    private const val MANY_DISTURBED_PERCENT = 30

    /** Açık döngüde bu günü aşan yumurtlama sonrası süre "adet gelmedi" notu verir. */
    private const val LONG_LUTEAL_DAYS = 18

    /**
     * @param entries Günlük kayıtlar; sırası önemsizdir, aynı güne düşenlerden listede sonuncusu alınır.
     * @param periodStarts Adet başlangıçları; yinelenenler ve gelecek tarihliler atılır.
     * @param today Bugün; sonrası kayıtlar ve adet başlangıçları hesaba girmez.
     * @param onPill Doğum kontrol hapı kullanılıyor; true ise [FertilityOverview.Disabled] döner.
     */
    fun analyze(
        entries: List<FertilityEntry>,
        periodStarts: List<LocalDate>,
        today: LocalDate,
        onPill: Boolean = false,
    ): FertilityOverview {
        if (onPill) return FertilityOverview.Disabled

        val starts = periodStarts.filter { !it.isAfter(today) }.distinct().sorted()
        val entriesByDate = entries.filter { !it.date.isAfter(today) }.associateBy { it.date }
        val sortedEntries = entriesByDate.values.sortedBy { it.date }

        // İlk adet başlangıcından önceki kayıtlar döngü dışıdır ve hesaba girmez.
        val cycles = starts.mapIndexed { index, start ->
            val end = starts.getOrNull(index + 1)?.minusDays(1)
            val inCycle = sortedEntries.filter { entry ->
                !entry.date.isBefore(start) && (end == null || !entry.date.isAfter(end))
            }
            analyzeCycle(start, end, inCycle, today)
        }
        return FertilityOverview.Enabled(cycles = cycles, current = cycles.lastOrNull())
    }

    private fun analyzeCycle(
        start: LocalDate,
        end: LocalDate?,
        entries: List<FertilityEntry>,
        today: LocalDate,
    ): CycleFertility {
        val temperatures = entries.mapNotNull { entry ->
            entry.bbtCelsius?.let { TemperatureReading(entry.date, it, entry.bbtDisturbed) }
        }
        val readings = temperatures.map { reading ->
            ChartPoint(
                date = reading.date,
                celsius = reading.celsius,
                disturbed = reading.disturbed,
                excluded = !OvulationDetector.isUsable(reading),
            )
        }
        val valid = temperatures.filter(OvulationDetector::isUsable)
        val ovulation = OvulationDetector.detect(temperatures)
        val lhPositiveDates = entries.filter { it.lh == LhResult.POSITIVE }.map { it.date }
        val hasNegativeLh = entries.any { it.lh == LhResult.NEGATIVE }
        val lutealDay = if (end == null && ovulation != null) {
            ChronoUnit.DAYS.between(ovulation.ovulationDate, today).toInt()
        } else {
            null
        }
        val disturbedCount = temperatures.count { it.disturbed }

        // Oran karşılaştırması tam sayıyla: 100·bozuk ≥ 30·toplam.
        val manyDisturbed = disturbedCount >= MANY_DISTURBED_MIN_COUNT &&
            disturbedCount * 100 >= temperatures.size * MANY_DISTURBED_PERCENT

        // LH pozitif ama yükselme yok: pozitife kadar örtü çizgisine yetecek ölçüm ve sonrasında
        // yükselmenin görünmesi gereken sayıda ölçüm varken doğrulama yoksa.
        val lhPositiveWithoutRise = ovulation == null && lhPositiveDates.any { positive ->
            valid.count { !it.date.isAfter(positive) } >= OvulationDetector.MIN_PRIOR_READINGS &&
                valid.count { it.date.isAfter(positive) } >= READINGS_AFTER_LH_POSITIVE
        }

        val notes = buildList {
            if (ovulation != null) add(FertilityNote.OVULATION_CONFIRMED)
            if (ovulation == null && valid.size < MIN_VALID_READINGS) add(FertilityNote.TOO_FEW_READINGS)
            if (manyDisturbed) add(FertilityNote.MANY_DISTURBED)
            if (lhPositiveWithoutRise) add(FertilityNote.LH_POSITIVE_WITHOUT_RISE)
            if (ovulation != null && hasNegativeLh && lhPositiveDates.isEmpty()) {
                add(FertilityNote.RISE_WITHOUT_LH_POSITIVE)
            }
            if (lutealDay != null && lutealDay > LONG_LUTEAL_DAYS) add(FertilityNote.LONG_LUTEAL)
        }

        return CycleFertility(
            start = start,
            end = end,
            entries = entries,
            readings = readings,
            coverlineCelsius = ovulation?.coverlineCelsius,
            ovulation = ovulation,
            lhPositiveDates = lhPositiveDates,
            lutealDay = lutealDay,
            notes = notes,
        )
    }
}
