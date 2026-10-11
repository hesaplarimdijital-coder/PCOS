package com.ayla.pcos.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Sistemde "animasyonları kaldır" açıksa true; bileşenler morf ve dalga gibi süsleri atlar. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * @param reducedMotion null ise sistem ayarından okunur; ekran görüntüsü testleri belirleyici çıktı için true verir.
 */
@Composable
fun AylaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemReducedMotion = remember(context) {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        scale == 0f
    }
    val effectiveReducedMotion = reducedMotion ?: systemReducedMotion
    val aylaColors = if (darkTheme) AylaDarkColors else AylaLightColors
    CompositionLocalProvider(
        LocalAylaColors provides aylaColors,
        LocalReducedMotion provides effectiveReducedMotion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) AylaDarkColorScheme else AylaLightColorScheme,
            typography = AylaTypography,
            shapes = AylaShapes,
            content = content,
        )
    }
}

/** Kısa erişim: `AylaTheme.colors.basari` */
object AylaThemeTokens {
    val colors: AylaColors
        @Composable get() = LocalAylaColors.current
}
