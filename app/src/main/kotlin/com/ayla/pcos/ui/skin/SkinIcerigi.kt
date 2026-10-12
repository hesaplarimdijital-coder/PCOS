package com.ayla.pcos.ui.skin

import androidx.annotation.StringRes
import com.ayla.pcos.R
import com.ayla.pcos.data.skin.AvoidItem
import com.ayla.pcos.data.skin.DoctorCard
import com.ayla.pcos.data.skin.RoutineStep
import com.ayla.pcos.data.skin.RoutineTip
import com.ayla.pcos.ui.components.EvidenceLevel
import com.ayla.pcos.ui.plan.BilgiKarti

/**
 * Cilt ve saç içeriği: kural anahtarlarından dize ve kanıt düzeyine eşleme (docs/arastirma/cilt-sac-icerigi.md §7).
 * Tanı ya da tedavi yok; içerik maddesi düzeyinde kalır.
 */
internal object SkinIcerigi {
    fun step(step: RoutineStep): BilgiKarti = when (step) {
        RoutineStep.CLEANSER -> BilgiKarti(R.string.skin_step_cleanser_title, R.string.skin_step_cleanser_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_step_cleanser_source)
        RoutineStep.CLEANSER_DRY -> BilgiKarti(
            R.string.skin_step_cleanser_dry_title, R.string.skin_step_cleanser_dry_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_step_cleanser_dry_source,
        )
        RoutineStep.NIACINAMIDE -> BilgiKarti(R.string.skin_step_niacinamide_title, R.string.skin_step_niacinamide_text, EvidenceLevel.LIMITED, R.string.skin_step_niacinamide_source)
        RoutineStep.MOISTURIZER -> BilgiKarti(
            R.string.skin_step_moisturizer_title, R.string.skin_step_moisturizer_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_step_moisturizer_source,
        )
        RoutineStep.SUNSCREEN -> BilgiKarti(R.string.skin_step_sunscreen_title, R.string.skin_step_sunscreen_text, EvidenceLevel.STRONG, R.string.skin_step_sunscreen_source)
        RoutineStep.SUNSCREEN_SENSITIVE -> BilgiKarti(
            R.string.skin_step_sunscreen_sensitive_title, R.string.skin_step_sunscreen_sensitive_text, EvidenceLevel.STRONG, R.string.skin_step_sunscreen_sensitive_source,
        )
        RoutineStep.SALICYLIC -> BilgiKarti(R.string.skin_step_salicylic_title, R.string.skin_step_salicylic_text, EvidenceLevel.LIMITED, R.string.skin_step_salicylic_source)
        RoutineStep.AZELAIC -> BilgiKarti(R.string.skin_step_azelaic_title, R.string.skin_step_azelaic_text, EvidenceLevel.LIMITED, R.string.skin_step_azelaic_source)
        RoutineStep.AHA -> BilgiKarti(R.string.skin_step_aha_title, R.string.skin_step_aha_text, EvidenceLevel.LIMITED, R.string.skin_step_aha_source)
    }

    /** Gebelik güvenlik uyarıları kanıt düzeyinden bağımsız gösterilir (çip yok); kaynak görünür. */
    fun avoid(item: AvoidItem): BilgiKarti = when (item) {
        AvoidItem.RETINOIDS -> BilgiKarti(R.string.skin_avoid_retinoids_title, R.string.skin_avoid_retinoids_text, null, R.string.skin_avoid_retinoids_source)
        AvoidItem.HYDROQUINONE -> BilgiKarti(R.string.skin_avoid_hydroquinone_title, R.string.skin_avoid_hydroquinone_text, null, R.string.skin_avoid_hydroquinone_source)
        AvoidItem.SALICYLIC -> BilgiKarti(R.string.skin_avoid_salicylic_title, R.string.skin_avoid_salicylic_text, null, R.string.skin_avoid_salicylic_source)
        AvoidItem.ESSENTIAL_OILS -> BilgiKarti(
            R.string.skin_avoid_essential_oils_title, R.string.skin_avoid_essential_oils_text, null, R.string.skin_avoid_essential_oils_source,
        )
        AvoidItem.RX -> BilgiKarti(R.string.skin_avoid_rx_title, R.string.skin_avoid_rx_text, null, R.string.skin_avoid_rx_source)
        AvoidItem.KEPT -> BilgiKarti(R.string.skin_avoid_kept_title, R.string.skin_avoid_kept_text, null, R.string.skin_avoid_kept_source)
    }

    fun tip(tip: RoutineTip): BilgiKarti = when (tip) {
        RoutineTip.HANDS_OFF -> BilgiKarti(R.string.skin_tip_hands_off_title, R.string.skin_tip_hands_off_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_tip_hands_off_source)
        RoutineTip.HAIR_OILY -> BilgiKarti(R.string.skin_tip_hair_oily_title, R.string.skin_tip_hair_oily_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_tip_hair_oily_source)
    }

    /** Aciliyet: "Gecikmeden" / "Aynı gün" / "Yakın zamanda" (renk tek başına anlam taşımaz; metin rozet). */
    enum class Aciliyet(@StringRes val label: Int) {
        NOW(R.string.skin_doctor_urgency_now),
        SAME_DAY(R.string.skin_doctor_urgency_same_day),
        SOON(R.string.skin_doctor_urgency_soon),
    }

    data class DoktorKarti(val key: String, val card: BilgiKarti, val urgency: Aciliyet?)

    /** Tüm "ne zaman doktora" kartları, belgedeki sırayla. */
    val doctorCards: List<DoktorKarti> = listOf(
        DoktorKarti(
            DoctorCard.VIRILIZATION.key,
            BilgiKarti(R.string.skin_doctor_virilization_title, R.string.skin_doctor_virilization_text, EvidenceLevel.STRONG, R.string.skin_doctor_virilization_source),
            Aciliyet.NOW,
        ),
        DoktorKarti(
            DoctorCard.ACNE_SEVERE.key,
            BilgiKarti(R.string.skin_doctor_acne_severe_title, R.string.skin_doctor_acne_severe_text, EvidenceLevel.STRONG, R.string.skin_doctor_acne_severe_source),
            Aciliyet.SOON,
        ),
        DoktorKarti(
            "acne_mood",
            BilgiKarti(R.string.skin_doctor_acne_mood_title, R.string.skin_doctor_acne_mood_text, EvidenceLevel.STRONG, R.string.skin_doctor_acne_mood_source),
            Aciliyet.SOON,
        ),
        DoktorKarti(
            "infection",
            BilgiKarti(R.string.skin_doctor_infection_title, R.string.skin_doctor_infection_text, EvidenceLevel.STRONG, R.string.skin_doctor_infection_source),
            Aciliyet.SAME_DAY,
        ),
        DoktorKarti(
            DoctorCard.ACANTHOSIS.key,
            BilgiKarti(R.string.skin_doctor_acanthosis_title, R.string.skin_doctor_acanthosis_text, EvidenceLevel.STRONG, R.string.skin_doctor_acanthosis_source),
            Aciliyet.SOON,
        ),
        DoktorKarti(
            DoctorCard.HAIR_LOSS.key,
            BilgiKarti(R.string.skin_doctor_hair_loss_title, R.string.skin_doctor_hair_loss_text, EvidenceLevel.STRONG, R.string.skin_doctor_hair_loss_source),
            Aciliyet.SOON,
        ),
        DoktorKarti(
            "mole",
            BilgiKarti(R.string.skin_doctor_mole_title, R.string.skin_doctor_mole_text, EvidenceLevel.STRONG, R.string.skin_doctor_mole_source),
            Aciliyet.SOON,
        ),
        DoktorKarti(
            DoctorCard.PREGNANCY.key,
            BilgiKarti(R.string.skin_doctor_pregnancy_title, R.string.skin_doctor_pregnancy_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_doctor_pregnancy_source),
            null,
        ),
    )

    val routineRules = listOf(
        R.string.skin_routine_rule_one_product,
        R.string.skin_routine_rule_patch,
        R.string.skin_routine_rule_irritation,
        R.string.skin_routine_rule_one_active,
    )

    val hairCards = listOf(
        BilgiKarti(R.string.skin_hair_gentle_title, R.string.skin_hair_gentle_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_gentle_source),
        BilgiKarti(R.string.skin_hair_wash_title, R.string.skin_hair_wash_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_wash_source),
        BilgiKarti(R.string.skin_hair_thinning_title, R.string.skin_hair_thinning_text, EvidenceLevel.STRONG, R.string.skin_hair_thinning_source),
    )

    /** Tüy alma yöntemleri: bilgi, tedavi önerisi değil (lazer/IPL tıbbi işlem; uzmanla planlanır). */
    val hairRemovalCards = listOf(
        BilgiKarti(R.string.skin_hair_removal_shave_title, R.string.skin_hair_removal_shave_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_removal_shave_source),
        BilgiKarti(R.string.skin_hair_removal_wax_title, R.string.skin_hair_removal_wax_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_removal_wax_source),
        BilgiKarti(R.string.skin_hair_removal_cream_title, R.string.skin_hair_removal_cream_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_removal_cream_source),
        BilgiKarti(R.string.skin_hair_removal_laser_title, R.string.skin_hair_removal_laser_text, EvidenceLevel.STRONG, R.string.skin_hair_removal_laser_source),
        BilgiKarti(
            R.string.skin_hair_removal_laser_skin_title, R.string.skin_hair_removal_laser_skin_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_removal_laser_skin_source,
        ),
        BilgiKarti(
            R.string.skin_hair_removal_paradox_title, R.string.skin_hair_removal_paradox_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_removal_paradox_source,
        ),
        BilgiKarti(
            R.string.skin_hair_removal_pregnancy_title, R.string.skin_hair_removal_pregnancy_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_hair_removal_pregnancy_source,
        ),
    )

    val allergenCards = listOf(
        BilgiKarti(R.string.skin_allergen_title, R.string.skin_allergen_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_allergen_source),
        BilgiKarti(R.string.skin_allergen_reaction_title, R.string.skin_allergen_reaction_text, EvidenceLevel.EXPERT_OPINION, R.string.skin_allergen_reaction_source),
    )

    val photoTips = listOf(
        R.string.skin_photo_tip_light,
        R.string.skin_photo_tip_distance,
        R.string.skin_photo_tip_consistency,
        R.string.skin_photo_tip_helper,
    )
}
