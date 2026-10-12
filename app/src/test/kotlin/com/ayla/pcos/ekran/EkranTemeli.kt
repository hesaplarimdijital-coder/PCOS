package com.ayla.pcos.ekran

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.ayla.pcos.ui.components.AltGezinme
import com.ayla.pcos.ui.navigation.Screen
import com.ayla.pcos.ui.theme.AylaTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Before
import org.junit.Rule
import java.util.TimeZone

/**
 * Ekran görüntüsü testlerinin ortak tabanı: tema, yazı ölçeği, isteğe bağlı alt gezinme ve PNG yazımı.
 * Alt sınıflar `@RunWith(RobolectricTestRunner::class)`, `@GraphicsMode(NATIVE)` ve `@Config(qualifiers = …)`
 * anotasyonlarını kendileri taşır. Görüntü adları depo genelinde benzersizdir (bkz. EkranGoruntuleriTest).
 */
abstract class EkranTemeli {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun saatDilimi() {
        // Saatler (doz, uyku) kullanıcının diliminde gösterilir; CI sunucusu UTC'dir.
        TimeZone.setDefault(TimeZone.getTimeZone(Ornek.zone))
    }

    @OptIn(ExperimentalRoborazziApi::class)
    protected fun ekran(
        ad: String,
        sekme: Screen.Tab? = null,
        koyu: Boolean = false,
        yaziOlcegi: Float = 1f,
        sistemYaziOlcegi: Float? = null,
        tumEkran: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            AylaTheme(darkTheme = koyu, reducedMotion = true) {
                val density = LocalDensity.current
                val context = LocalContext.current
                val configuration = LocalConfiguration.current
                // yaziOlcegi: doğrusal ölçek (sınama için abartılı). sistemYaziOlcegi: cihazdaki gibi
                // yapılandırmadan okunur; Android 14+ büyük metni doğrusal olmayan biçimde daha az büyütür.
                val olcekli = remember(context, configuration, yaziOlcegi, sistemYaziOlcegi) {
                    if (sistemYaziOlcegi != null) {
                        val cfg = Configuration(configuration).apply { fontScale = sistemYaziOlcegi }
                        Density(context.createConfigurationContext(cfg))
                    } else {
                        Density(density.density, yaziOlcegi)
                    }
                }
                CompositionLocalProvider(LocalDensity provides olcekli) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (sekme != null) {
                            Scaffold(
                                bottomBar = { AltGezinme(selected = sekme, onSelect = {}) },
                                containerColor = MaterialTheme.colorScheme.background,
                            ) { padding ->
                                Box(Modifier.padding(padding)) { content() }
                            }
                        } else {
                            content()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        // Diyaloglar ayrı pencerede çizilir; tumEkran tüm pencereleri tek görüntüde birleştirir.
        if (tumEkran) captureScreenRoboImage("build/ekran/$ad.png") else compose.onRoot().captureRoboImage("build/ekran/$ad.png")
    }
}
