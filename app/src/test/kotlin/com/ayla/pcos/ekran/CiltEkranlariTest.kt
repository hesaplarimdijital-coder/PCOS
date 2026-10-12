package com.ayla.pcos.ekran

import androidx.compose.runtime.Composable
import com.ayla.pcos.ui.skin.SkinActions
import com.ayla.pcos.ui.skin.SkinContent
import com.ayla.pcos.ui.skin.SkinUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cilt ve saç ekranları (#25): rutin, gebelik dışlamaları, profil, doktor kartları, fotoğraflar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TELEFON)
class CiltEkranlariTest : EkranTemeli() {

    @Composable
    private fun Cilt(state: SkinUiState, sekme: Int = 0, secim: Set<String> = emptySet()) = SkinContent(
        state = state,
        actions = SkinActions(),
        loadThumbnail = { record, px -> OrnekCilt.kucukResim(record, px) },
        initialTab = sekme,
        initialCompare = secim,
    )

    @Config(qualifiers = UZUN)
    @Test fun t50_cilt_rutin() = ekran("50-cilt-rutin") { Cilt(OrnekCilt.rutin) }

    @Config(qualifiers = UZUN)
    @Test fun t51_cilt_rutin_gebelik() = ekran("51-cilt-rutin-gebelik") { Cilt(OrnekCilt.rutinGebelik) }

    @Config(qualifiers = UZUN)
    @Test fun t52_cilt_profil() = ekran("52-cilt-profil") { Cilt(OrnekCilt.rutin, sekme = 1) }

    @Config(qualifiers = UZUN)
    @Test fun t53_cilt_doktor() = ekran("53-cilt-doktor") { Cilt(OrnekCilt.rutin, sekme = 2) }

    @Test fun t54_cilt_fotograf_riza() = ekran("54-cilt-fotograf-riza") { Cilt(OrnekCilt.fotografRiza, sekme = 3) }

    @Config(qualifiers = UZUN)
    @Test fun t55_cilt_fotograflar() = ekran("55-cilt-fotograflar") { Cilt(OrnekCilt.fotograflarDurumu, sekme = 3) }

    @Config(qualifiers = UZUN)
    @Test fun t56_cilt_karsilastirma() = ekran("56-cilt-karsilastirma") {
        Cilt(OrnekCilt.fotograflarDurumu, sekme = 3, secim = OrnekCilt.karsilastirmaSecimi)
    }

    @Test fun t57_cilt_koyu() = ekran("57-cilt-koyu", koyu = true) { Cilt(OrnekCilt.rutin) }

    @Test fun t58_cilt_bos_profil() = ekran("58-cilt-bos-profil") { Cilt(OrnekCilt.bosProfil) }

    @Config(qualifiers = UZUN)
    @Test fun y20_yazi200_cilt_rutin() = ekran("y20-yazi200-cilt-rutin", sistemYaziOlcegi = 2f) { Cilt(OrnekCilt.rutin) }

    @Config(qualifiers = UZUN)
    @Test fun y21_yazi200_cilt_profil() = ekran("y21-yazi200-cilt-profil", sistemYaziOlcegi = 2f) { Cilt(OrnekCilt.rutin, sekme = 1) }

    @Config(qualifiers = UZUN)
    @Test fun y22_yazi200_cilt_fotograflar() = ekran("y22-yazi200-cilt-fotograflar", sistemYaziOlcegi = 2f) {
        Cilt(OrnekCilt.fotograflarDurumu, sekme = 3)
    }
}
