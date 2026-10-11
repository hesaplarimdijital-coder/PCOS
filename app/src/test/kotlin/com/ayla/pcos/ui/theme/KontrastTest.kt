package com.ayla.pcos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.math.pow
import kotlin.test.assertTrue

/**
 * Arayüzde gerçekten yan yana gelen metin/zemin çiftleri iki temada da WCAG AA'yı karşılar:
 * metin ≥ 4,5:1, metin dışı işaretler (çizgi, takvim dairesi) ≥ 3:1. Token değeri değişirse bu test uyarır.
 */
class KontrastTest {

    private class Cift(val ad: String, val on: Color, val zemin: Color, val esik: Double = 4.5)

    private fun ciftler(s: ColorScheme, a: AylaColors) = listOf(
        Cift("onBackground/background", s.onBackground, s.background),
        Cift("onSurface/kart", s.onSurface, a.kart),
        Cift("onSurface/cukur", s.onSurface, a.cukur),
        Cift("onSurfaceVariant/background", s.onSurfaceVariant, s.background),
        Cift("onSurfaceVariant/kart", s.onSurfaceVariant, a.kart),
        Cift("onSurfaceVariant/cukur", s.onSurfaceVariant, a.cukur),
        Cift("primary/background", s.primary, s.background),
        Cift("primary/kart", s.primary, a.kart),
        Cift("onPrimary/primary", s.onPrimary, s.primary),
        Cift("onPrimaryContainer/primaryContainer", s.onPrimaryContainer, s.primaryContainer),
        Cift("primary/primaryContainer", s.primary, s.primaryContainer),
        Cift("secondary/kart", s.secondary, a.kart),
        Cift("onSecondary/secondary", s.onSecondary, s.secondary),
        Cift("onSecondaryContainer/secondaryContainer", s.onSecondaryContainer, s.secondaryContainer),
        Cift("tertiary/kart", s.tertiary, a.kart),
        Cift("tertiary/tertiaryContainer", s.tertiary, s.tertiaryContainer),
        Cift("onTertiary/tertiary", s.onTertiary, s.tertiary),
        Cift("error/kart", s.error, a.kart),
        Cift("onError/error", s.onError, s.error),
        Cift("onErrorContainer/errorContainer", s.onErrorContainer, s.errorContainer),
        Cift("lavanta/kart", a.lavanta, a.kart),
        Cift("lavanta/lavantaAcik", a.lavanta, a.lavantaAcik),
        Cift("basari/kart", a.basari, a.kart),
        Cift("basari/basariAcik", a.basari, a.basariAcik),
        Cift("uyari/kart", a.uyari, a.kart),
        Cift("uyari/uyariAcik", a.uyari, a.uyariAcik),
        // Takvim: adet günü numarası dolu dairede, tahmin günü numarası açık dairede
        Cift("onPrimary/fazAdet", s.onPrimary, a.fazAdet),
        Cift("onSurfaceVariant/fazAdetAcik", s.onSurfaceVariant, a.fazAdetAcik),
        Cift("kayisiCanliUzeri/kayisiCanli", a.kayisiCanliUzeri, a.kayisiCanli),
        // Metin dışı
        Cift("outline/kart", s.outline, a.kart, esik = 3.0),
        Cift("fazAdet/kart", a.fazAdet, a.kart, esik = 3.0),
        Cift("secondary/cukur", s.secondary, a.cukur, esik = 3.0),
    )

    @Test
    fun `acik temada metin ve zemin ciftleri yeterli kontrast tasir`() = denetle(AylaLightColorScheme, AylaLightColors)

    @Test
    fun `koyu temada metin ve zemin ciftleri yeterli kontrast tasir`() = denetle(AylaDarkColorScheme, AylaDarkColors)

    private fun denetle(s: ColorScheme, a: AylaColors) {
        val zayif = ciftler(s, a)
            .map { it to kontrast(it.on, it.zemin) }
            .filter { (cift, oran) -> oran < cift.esik }
            .map { (cift, oran) -> "${cift.ad}: ${"%.2f".format(oran)} < ${cift.esik}" }
        assertTrue(zayif.isEmpty(), "Yetersiz kontrast:\n" + zayif.joinToString("\n"))
    }

    /** WCAG 2.x göreli parlaklık ve oran: (L1 + 0,05) / (L2 + 0,05). Renkler opak sRGB'dir. */
    private fun kontrast(a: Color, b: Color): Double {
        val la = parlaklik(a)
        val lb = parlaklik(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun parlaklik(c: Color): Double {
        fun kanal(v: Float): Double = if (v <= 0.03928f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * kanal(c.red) + 0.7152 * kanal(c.green) + 0.0722 * kanal(c.blue)
    }
}
