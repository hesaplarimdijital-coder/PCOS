package com.ayla.pcos.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

/**
 * Hareket ilkeleri: mekânsal değişimler hafif taşan yaylarla, efektler (renk, opaklık)
 * taşmayan yaylarla. "Animasyonları kaldır" ayarı açıkken süreler 0'a iner (bkz. AylaTheme).
 */
object AylaMotion {
    /** Konum, boyut, biçim değişimi; hafif taşar. */
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = 380f)

    /** Hale dolumu, su seviyesi gibi yavaş ve yumuşak hareket. */
    fun <T> slow(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 110f)

    /** Renk ve opaklık; asla taşmaz. */
    fun <T> effect(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    /** Ekrana giriş: M3 "emphasized decelerate" eğrisi. */
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    fun <T> enter(durationMillis: Int = 420): TweenSpec<T> = tween(durationMillis, easing = emphasizedDecelerate)
}

/**
 * "Animasyonları kaldır" açıkken ([LocalReducedMotion]) anında biten, aksi hâlde verilen hareket.
 * Dolum, çubuk ve ölçüt gibi değer animasyonları bununla beslenir: `hareket(AylaMotion.slow())`.
 */
@Composable
fun <T> hareket(spec: AnimationSpec<T>): AnimationSpec<T> = if (LocalReducedMotion.current) snap() else spec
