package com.ayla.cycle

import java.time.LocalDate
import java.time.LocalTime

/*
 * Doğurganlık kaydı modeli (MVP 3, #24). Motor yalnızca enum ve tarih döndürür; kullanıcıya görünen
 * metinleri arayüz belirler. Hiçbir değer tanı, tedavi ya da doğum kontrol kararı değildir.
 */

/** İdrar LH (yumurtlama) testi sonucu. Silik çizgi, test talimatlarına göre negatif okunur. */
enum class LhResult { NEGATIVE, POSITIVE, INVALID }

/** Servikal mukus gözlemi; yalnızca kayıt amaçlıdır, yumurtlama doğrulamasına girmez. */
enum class CervicalMucus { DRY, STICKY, CREAMY, WATERY, EGG_WHITE }

/** Bir güne ait doğurganlık kaydı; alanlar isteğe bağlı. */
data class FertilityEntry(
    val date: LocalDate,
    val bbtCelsius: Double? = null,
    val bbtTime: LocalTime? = null,
    /** Ateş, alkol, az/kesintili uyku, geç ölçüm: örtü çizgisi ve yükselme hesabına girmez. */
    val bbtDisturbed: Boolean = false,
    val lh: LhResult? = null,
    val mucus: CervicalMucus? = null,
)

/**
 * Grafik için bir ısı noktası.
 *
 * @property disturbed Kullanıcı ölçümü bozuk işaretledi.
 * @property excluded Hesaba girmedi: bozuk ya da akla yatkın aralığın dışında. Grafikte içi boş çizilir.
 */
data class ChartPoint(
    val date: LocalDate,
    val celsius: Double,
    val disturbed: Boolean,
    val excluded: Boolean,
)

/** Döngüye ilişkin notlar; metinleri arayüz belirler. Sırası bildirim sırasıdır. */
enum class FertilityNote {
    /** Yumurtlama bu döngüde ısı yükselmesiyle doğrulandı. */
    OVULATION_CONFIRMED,

    /** Döngüde 9'dan az geçerli ölçüm var ve doğrulama yok; kural uygulanamaz. */
    TOO_FEW_READINGS,

    /** Bozuk ölçüm oranı yüksek (≥ %30 ve en az 3); grafik güvenilir yorumlanamaz. */
    MANY_DISTURBED,

    /**
     * LH pozitif gününden sonra en az 5 geçerli ölçüm geçti, doğrulanmış yükselme yok.
     * PCOS'ta bazal LH yüksek olabildiğinden LH testleri yanıltabilir.
     */
    LH_POSITIVE_WITHOUT_RISE,

    /** Yükselme doğrulandı, döngüde negatif LH testi var ama hiç pozitif yok: test zamanlaması. */
    RISE_WITHOUT_LH_POSITIVE,

    /**
     * Açık döngüde yumurtlama sonrası 18 günü aşan süre, adet gelmedi. Gebelik olasılığı notu içindir;
     * tanı değildir, test ve hekim yönlendirmesi arayüzdedir.
     */
    LONG_LUTEAL,
}

/** Bir döngünün doğurganlık görünümü. */
data class CycleFertility(
    /** Adet başlangıcı (döngünün 1. günü). */
    val start: LocalDate,
    /** Döngünün son günü (bir sonraki adetten bir önceki gün); açık döngüde null. */
    val end: LocalDate?,
    /** Döngüye düşen, güne göre sıralı kayıtlar (aynı gün tekilleştirilmiş). */
    val entries: List<FertilityEntry>,
    /** Isı ölçümleri, güne göre sıralı; hesaba girmeyenler [ChartPoint.excluded] ile işaretli. */
    val readings: List<ChartPoint>,
    /** Örtü çizgisi; yalnızca yumurtlama doğrulandığında vardır. */
    val coverlineCelsius: Double?,
    val ovulation: OvulationEstimate?,
    /** LH pozitif günleri, sıralı. */
    val lhPositiveDates: List<LocalDate>,
    /** Açık döngüde ve yumurtlama doğrulanmışsa bugünün yumurtlama sonrası günü (yumurtlama günü 0). */
    val lutealDay: Int?,
    val notes: List<FertilityNote>,
) {
    val isOpen: Boolean get() = end == null
}

/** Doğurganlık motorunun tek seferde ürettiği görünüm. */
sealed interface FertilityOverview {
    /** Hap modunda hiçbir hesap yapılmaz (ADR-0005). */
    data object Disabled : FertilityOverview

    /**
     * @property cycles Adet başlangıçlarına göre bölünmüş döngüler, eskiden yeniye.
     * @property current Bugünü içeren açık döngü; adet başlangıcı yoksa null.
     */
    data class Enabled(
        val cycles: List<CycleFertility>,
        val current: CycleFertility?,
    ) : FertilityOverview
}
