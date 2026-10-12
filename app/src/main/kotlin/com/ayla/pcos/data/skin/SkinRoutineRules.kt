package com.ayla.pcos.data.skin

import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.SkinType

/*
 * Cilt bakım rutini seçim kuralları (#25, docs/arastirma/cilt-sac-icerigi.md §3.2–3.5). Saf Kotlin: profil
 * öz-beyanlarından sabah/akşam adımlarını, gebelik planında kaçınılacaklar listesini ve "doktora görün"
 * kartlarını türetir. Adımlar yalnızca içerik maddesi düzeyindedir (temizleyici, nemlendirici, güneş koruyucu,
 * niasinamid, azelaik asit, salisilik asitli temizleyici, AHA); reçeteli ya da eczane ürünü önerilmez. Metinler
 * `strings_cilt.xml`'de (`skin_step_<key>_*`, `skin_avoid_<key>_*`, `skin_doctor_<key>_*`); kurallar yalnızca
 * anahtar döndürür. Tanı ya da tedavi kararı değildir (TİTCK/MDCG 2019-11 wellness sınırı).
 */

/** Kuralların girdisi: cilt profili öz-beyanları + tercihlerdeki gebelik planı (Goal.TTC). */
data class SkinRoutineInput(
    val skinType: SkinType? = null,
    val acne: AcneSelfReport? = null,
    val darkSpots: Boolean = false,
    val acanthosis: Boolean = false,
    val unwantedHair: Boolean = false,
    val hairThinning: Boolean = false,
    /** Tercihlerde "hamile kalmayı planlıyorum" (Goal.TTC). */
    val pregnancyPlan: Boolean = false,
    /** Profilde "gebeyim ya da emziriyorum". */
    val pregnantOrBreastfeeding: Boolean = false,
) {
    /** Gebelik süzgeci: plan ya da gebelik/emzirme işaretli (§3.5 kuralı). */
    val pregnancyFilter: Boolean get() = pregnancyPlan || pregnantOrBreastfeeding

    val sensitive: Boolean get() = skinType == SkinType.SENSITIVE

    val drySkin: Boolean get() = skinType == SkinType.DRY || skinType == SkinType.SENSITIVE
}

/** Rutin adımı; [key] `skin_step_<key>_title/text/source` dizelerinin ve rutin işaretlerinin anahtarıdır. */
enum class RoutineStep(val key: String, val active: Boolean) {
    CLEANSER("cleanser", active = false),
    CLEANSER_DRY("cleanser_dry", active = false),
    NIACINAMIDE("niacinamide", active = true),
    MOISTURIZER("moisturizer", active = false),
    SUNSCREEN("sunscreen", active = false),
    SUNSCREEN_SENSITIVE("sunscreen_sensitive", active = false),
    SALICYLIC("salicylic", active = true),
    AZELAIC("azelaic", active = true),
    AHA("aha", active = true),
    ;

    companion object {
        fun fromKey(key: String): RoutineStep? = entries.firstOrNull { it.key == key }
    }
}

/** Gebelik planında rutinden çıkarılan ya da açıkça listelenen içerikler (`skin_avoid_<key>_*`). */
enum class AvoidItem(val key: String) {
    RETINOIDS("retinoids"),
    HYDROQUINONE("hydroquinone"),
    SALICYLIC("salicylic"),
    ESSENTIAL_OILS("essential_oils"),
    RX("rx"),
    /** Rutinde kalanlar (olumlu liste); her zaman en sonda. */
    KEPT("kept"),
}

/** Adım kartlarına eklenen notlar; metinleri `strings_cilt.xml`'de. */
enum class RoutineNote {
    /** Gebelik planında niasinamid kartına "kaynaklarımızda uyarı yok, doktoruna sor" notu. */
    TTC_NIACINAMIDE,

    /** Akşam birden çok aktif içerik var: bir gecede tek aktif, dönüşümlü (§3.1 `skin_routine_rule_one_active`). */
    ONE_ACTIVE_PER_NIGHT,

    /** Hassas ciltte aktif içerikten önce yama testi (§3.6). */
    PATCH_TEST_FIRST,
}

/** Profile göre öne çıkan "ne zaman doktora" kartları (`skin_doctor_<key>_*`); genel liste ayrıca her zaman görünür. */
enum class DoctorCard(val key: String) {
    ACNE_SEVERE("acne_severe"),
    ACANTHOSIS("acanthosis"),
    VIRILIZATION("virilization"),
    HAIR_LOSS("hair_loss"),
    PREGNANCY("pregnancy"),
}

/** Rutin dışı ipuçları (`skin_tip_<key>_*`). */
enum class RoutineTip(val key: String) {
    HANDS_OFF("hands_off"),
    HAIR_OILY("hair_oily"),
}

data class RoutinePlan(
    val morning: List<RoutineStep>,
    val evening: List<RoutineStep>,
    /** Boş değilse gebelik planı süzgeci açıktır; `skin_avoid` bandı ve listesi gösterilir. */
    val avoid: List<AvoidItem>,
    val notes: Set<RoutineNote>,
    /** Rutinin üstünde gösterilen kartlar, öncelik sırasıyla. */
    val doctorCards: List<DoctorCard>,
    val tips: List<RoutineTip>,
) {
    /** Yoğun akne: aktif içerik önerilmez, dermatolog kartı rutinin üstündedir (§3.3 2e). */
    val activesSuppressed: Boolean get() = DoctorCard.ACNE_SEVERE in doctorCards
}

object SkinRoutineRules {

    /** Belirleyici: aynı girdi aynı plan; adımlar yinelenmez ve tablo sırasını korur. */
    fun plan(input: SkinRoutineInput): RoutinePlan {
        val pregnancy = input.pregnancyFilter
        val severeAcne = input.acne == AcneSelfReport.SEVERE
        val mildOrModerateAcne = input.acne == AcneSelfReport.MILD || input.acne == AcneSelfReport.MODERATE
        val notes = mutableSetOf<RoutineNote>()

        // §3.2 Sabah
        val morning = mutableListOf<RoutineStep>()
        morning += if (input.drySkin) RoutineStep.CLEANSER_DRY else RoutineStep.CLEANSER
        val oilyOrCombination = input.skinType == SkinType.OILY || input.skinType == SkinType.COMBINATION
        if ((oilyOrCombination || mildOrModerateAcne || input.darkSpots) && !severeAcne) {
            morning += RoutineStep.NIACINAMIDE
            if (pregnancy) notes += RoutineNote.TTC_NIACINAMIDE
        }
        morning += RoutineStep.MOISTURIZER
        morning += if (input.sensitive) RoutineStep.SUNSCREEN_SENSITIVE else RoutineStep.SUNSCREEN

        // §3.3 Akşam
        val evening = mutableListOf(RoutineStep.CLEANSER)
        if (!severeAcne) {
            val actives = linkedSetOf<RoutineStep>()
            if (mildOrModerateAcne) {
                // 2a / 2b: gebelik planında salisilik asit (durulanan düşük yüzde dışında) rutinde yok.
                if (!pregnancy) actives += RoutineStep.SALICYLIC
                actives += RoutineStep.AZELAIC
            }
            if (input.darkSpots) {
                if (input.sensitive) {
                    // 2d: yalnızca niasinamid; AHA ve salisilik yok.
                    actives += RoutineStep.NIACINAMIDE
                } else {
                    // 2c: azelaik ya da niasinamid; AHA haftada 1–2 gece (salisilik gecesiyle aynı gece değil).
                    actives += RoutineStep.AZELAIC
                    actives += RoutineStep.NIACINAMIDE
                    actives += RoutineStep.AHA
                }
            }
            if (input.sensitive) {
                // 2d'nin dışında kalan hassas senaryolar: AHA ve salisilik hassas ciltte önerilmez (§3.6).
                actives -= RoutineStep.AHA
                actives -= RoutineStep.SALICYLIC
                if (actives.isNotEmpty()) notes += RoutineNote.PATCH_TEST_FIRST
            }
            if (pregnancy) {
                // §3.5: sürülüp bırakılan asitler (salisilik, AHA) ihtiyaten rutinden çıkar; azelaik ve niasinamid kalır.
                actives -= RoutineStep.SALICYLIC
                actives -= RoutineStep.AHA
                if (RoutineStep.NIACINAMIDE in actives) notes += RoutineNote.TTC_NIACINAMIDE
            }
            evening += actives
            if (actives.size > 1) notes += RoutineNote.ONE_ACTIVE_PER_NIGHT
        }
        evening += RoutineStep.MOISTURIZER

        // §3.5 Gebelik planında kaçınılacaklar (hepsi açıkça listelenir; KEPT olumlu liste en sonda).
        val avoid = if (pregnancy) AvoidItem.entries.toList() else emptyList()

        // §5 Profile göre öne çıkan kartlar: aciliyet sırası (virilizasyon → yoğun akne → akantozis → saç → gebelik).
        val cards = buildList {
            if (input.unwantedHair) add(DoctorCard.VIRILIZATION)
            if (severeAcne) add(DoctorCard.ACNE_SEVERE)
            if (input.acanthosis) add(DoctorCard.ACANTHOSIS)
            if (input.hairThinning) add(DoctorCard.HAIR_LOSS)
            if (pregnancy) add(DoctorCard.PREGNANCY)
        }

        val tips = buildList {
            add(RoutineTip.HANDS_OFF)
            // Saç yağlılığı profilde yok; yağlı cilt yakın gösterge sayılır.
            if (input.skinType == SkinType.OILY) add(RoutineTip.HAIR_OILY)
        }

        return RoutinePlan(
            morning = morning.distinct(),
            evening = evening.distinct(),
            avoid = avoid,
            notes = notes,
            doctorCards = cards,
            tips = tips,
        )
    }
}
