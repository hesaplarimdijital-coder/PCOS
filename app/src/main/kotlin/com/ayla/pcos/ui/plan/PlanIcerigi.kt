package com.ayla.pcos.ui.plan

import androidx.annotation.StringRes
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.EvidenceLevel
import com.ayla.pcos.ui.components.EvidenceLevel.EXPERT_OPINION
import com.ayla.pcos.ui.components.EvidenceLevel.LIMITED
import com.ayla.pcos.ui.components.EvidenceLevel.MODERATE
import com.ayla.pcos.ui.components.EvidenceLevel.STRONG

/** Kaynaklı bilgi kartı: başlık, metin, kanıt düzeyi ve kısa künye. */
data class BilgiKarti(
    @param:StringRes val title: Int,
    @param:StringRes val text: Int,
    val evidence: EvidenceLevel?,
    @param:StringRes val source: Int?,
)

/** Kısa rutin; süre dakika olarak tutulur, adımlar sırayla gösterilir. */
data class Rutin(
    val key: String,
    val card: BilgiKarti,
    val minutes: Int,
    val steps: List<Int>,
)

/** Bitkisel destek kartı: "Ne denendi?", alerji, dikkat ve "doktoruna söyle" birlikte görünür (#13). */
data class BitkiselOge(
    @param:StringRes val title: Int,
    @param:StringRes val tried: Int,
    @param:StringRes val note: Int?,
    @param:StringRes val allergen: Int,
    val cautions: List<Int>,
    val evidence: EvidenceLevel,
    @param:StringRes val source: Int,
)

/** Hareket sekmesindeki döngü kartı: evre tahmini yok, yalnızca kaydedilmiş adet günleri (B4). */
enum class MovementCycleCard { GENERAL, PERIOD, OTHER }

/**
 * Planım içeriği. Metinler ve kanıt düzeyleri docs/arastirma/plan-icerigi.md'den gelir
 * (res/values/strings_plan.xml o belgeden üretilir). Tanı, doz, kür ya da kişiye özel öneri yok.
 */
object PlanIcerigi {

    // --- Beslenme ---
    val nutritionIntro = BilgiKarti(R.string.nutrition_intro_title, R.string.nutrition_intro_text, STRONG, R.string.nutrition_intro_source)

    val nutritionPrinciples = listOf(
        BilgiKarti(R.string.nutrition_flexible_title, R.string.nutrition_flexible_text, STRONG, R.string.nutrition_flexible_source),
        BilgiKarti(R.string.nutrition_rhythm_title, R.string.nutrition_rhythm_text, EXPERT_OPINION, R.string.nutrition_rhythm_source),
        BilgiKarti(R.string.nutrition_fiber_protein_title, R.string.nutrition_fiber_protein_text, MODERATE, R.string.nutrition_fiber_protein_source),
        BilgiKarti(R.string.nutrition_slow_carbs_title, R.string.nutrition_slow_carbs_text, LIMITED, R.string.nutrition_slow_carbs_source),
        BilgiKarti(R.string.nutrition_water_title, R.string.nutrition_water_text, STRONG, R.string.nutrition_water_source),
        BilgiKarti(R.string.nutrition_whole_foods_title, R.string.nutrition_whole_foods_text, LIMITED, R.string.nutrition_whole_foods_source),
    )

    val plate = BilgiKarti(R.string.nutrition_plate_title, R.string.nutrition_plate_text, EXPERT_OPINION, R.string.nutrition_plate_source)

    val walkTip = BilgiKarti(R.string.nutrition_walk_tip_title, R.string.nutrition_walk_tip_text, MODERATE, R.string.nutrition_walk_tip_source)

    val eatingSupport = BilgiKarti(R.string.nutrition_support_title, R.string.nutrition_support_text, STRONG, R.string.nutrition_support_source)

    // --- Bitkisel destekler (#13) ---
    val herbal = listOf(
        BitkiselOge(
            title = R.string.herbal_spearmint_title,
            tried = R.string.herbal_spearmint_tried,
            note = R.string.herbal_spearmint_note,
            allergen = R.string.herbal_spearmint_allergen,
            cautions = listOf(R.string.herbal_spearmint_caution),
            evidence = LIMITED,
            source = R.string.herbal_spearmint_source,
        ),
        BitkiselOge(
            title = R.string.herbal_cinnamon_title,
            tried = R.string.herbal_cinnamon_tried,
            note = null,
            allergen = R.string.herbal_cinnamon_allergen,
            cautions = listOf(R.string.herbal_cinnamon_caution, R.string.herbal_cinnamon_caution_2),
            evidence = LIMITED,
            source = R.string.herbal_cinnamon_source,
        ),
    )

    /** Öneri listesine girmeyen, yalnızca uyarı olarak gösterilen bitkiler. */
    val herbalExcluded = listOf(
        BilgiKarti(R.string.herbal_excluded_st_johns_wort_title, R.string.herbal_excluded_st_johns_wort_text, STRONG, R.string.herbal_excluded_st_johns_wort_source),
        BilgiKarti(R.string.herbal_excluded_licorice_title, R.string.herbal_excluded_licorice_text, STRONG, R.string.herbal_excluded_licorice_source),
        BilgiKarti(R.string.herbal_excluded_berberine_title, R.string.herbal_excluded_berberine_text, MODERATE, R.string.herbal_excluded_berberine_source),
        BilgiKarti(R.string.herbal_excluded_vitex_title, R.string.herbal_excluded_vitex_text, LIMITED, R.string.herbal_excluded_vitex_source),
        BilgiKarti(R.string.herbal_excluded_turmeric_title, R.string.herbal_excluded_turmeric_text, LIMITED, R.string.herbal_excluded_turmeric_source),
    )

    // --- Hareket ---
    val movementIntro = BilgiKarti(R.string.movement_intro_title, R.string.movement_intro_text, STRONG, R.string.movement_intro_source)

    val movementWeekly = listOf(
        BilgiKarti(R.string.movement_weekly_title, R.string.movement_weekly_text, STRONG, R.string.movement_weekly_source),
        BilgiKarti(R.string.movement_every_minute_title, R.string.movement_every_minute_text, STRONG, R.string.movement_every_minute_source),
        BilgiKarti(R.string.movement_intensity_title, R.string.movement_intensity_text, EXPERT_OPINION, R.string.movement_intensity_source),
    )

    val movementSafety = BilgiKarti(R.string.movement_safety_title, R.string.movement_safety_text, EXPERT_OPINION, R.string.movement_safety_source)

    const val ROUTINE_WALK = "walk"

    val routines = listOf(
        Rutin(
            key = ROUTINE_WALK,
            card = BilgiKarti(R.string.movement_routine_walk_title, R.string.movement_routine_walk_text, MODERATE, R.string.movement_routine_walk_source),
            minutes = 10,
            steps = listOf(
                R.string.movement_routine_walk_step_1, R.string.movement_routine_walk_step_2, R.string.movement_routine_walk_step_3,
                R.string.movement_routine_walk_step_4, R.string.movement_routine_walk_step_5,
            ),
        ),
        Rutin(
            key = "strength",
            card = BilgiKarti(R.string.movement_routine_strength_title, R.string.movement_routine_strength_text, STRONG, R.string.movement_routine_strength_source),
            minutes = 15,
            steps = listOf(
                R.string.movement_routine_strength_step_1, R.string.movement_routine_strength_step_2, R.string.movement_routine_strength_step_3,
                R.string.movement_routine_strength_step_4, R.string.movement_routine_strength_step_5, R.string.movement_routine_strength_step_6,
            ),
        ),
        Rutin(
            key = "stretch",
            card = BilgiKarti(R.string.movement_routine_stretch_title, R.string.movement_routine_stretch_text, LIMITED, R.string.movement_routine_stretch_source),
            minutes = 10,
            steps = listOf(
                R.string.movement_routine_stretch_step_1, R.string.movement_routine_stretch_step_2, R.string.movement_routine_stretch_step_3,
                R.string.movement_routine_stretch_step_4, R.string.movement_routine_stretch_step_5, R.string.movement_routine_stretch_step_6,
            ),
        ),
        Rutin(
            key = "sit_break",
            card = BilgiKarti(R.string.movement_routine_sit_break_title, R.string.movement_routine_sit_break_text, MODERATE, R.string.movement_routine_sit_break_source),
            minutes = 3,
            steps = listOf(
                R.string.movement_routine_sit_break_step_1, R.string.movement_routine_sit_break_step_2,
                R.string.movement_routine_sit_break_step_3, R.string.movement_routine_sit_break_step_4,
            ),
        ),
    )

    fun cycleCard(card: MovementCycleCard): BilgiKarti = when (card) {
        MovementCycleCard.PERIOD -> BilgiKarti(R.string.movement_cycle_period_title, R.string.movement_cycle_period_text, LIMITED, R.string.movement_cycle_period_source)
        MovementCycleCard.OTHER -> BilgiKarti(R.string.movement_cycle_other_title, R.string.movement_cycle_other_text, LIMITED, R.string.movement_cycle_other_source)
        MovementCycleCard.GENERAL -> BilgiKarti(R.string.movement_cycle_general_title, R.string.movement_cycle_general_text, LIMITED, R.string.movement_cycle_general_source)
    }

    /** "Neden evreye göre değil?" açıklaması; yalnızca OTHER kartının altında. */
    val cycleWhy = BilgiKarti(R.string.movement_cycle_why_title, R.string.movement_cycle_why_text, LIMITED, R.string.movement_cycle_why_source)
}
