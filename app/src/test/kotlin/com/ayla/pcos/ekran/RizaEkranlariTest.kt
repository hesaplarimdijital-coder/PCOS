package com.ayla.pcos.ekran

import com.ayla.pcos.ui.AcikRizaPenceresi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Açık rıza penceresi: ilk rıza ve kapsam güncellemesi (#20). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TELEFON)
class RizaEkranlariTest : EkranTemeli() {

    @Test fun t66_riza_guncellendi() = ekran("66-riza-guncellendi", tumEkran = true) {
        AcikRizaPenceresi(scopeUpdated = true, onAccept = {}, onReadPolicy = {})
    }

    @Test fun t67_riza_ilk() = ekran("67-riza-ilk", tumEkran = true) {
        AcikRizaPenceresi(scopeUpdated = false, onAccept = {}, onReadPolicy = {})
    }

    // Diyalog kendi penceresinde çizilir; %200 yazı cihaz yapılandırmasıyla verilir.
    @Config(qualifiers = UZUN, fontScale = 2f)
    @Test fun y26_yazi200_riza_guncellendi() = ekran("y26-yazi200-riza-guncellendi", tumEkran = true) {
        AcikRizaPenceresi(scopeUpdated = true, onAccept = {}, onReadPolicy = {})
    }
}
