package com.ayla.pcos.ekran

import androidx.compose.runtime.Composable
import com.ayla.pcos.ui.fertility.FertilityContent
import com.ayla.pcos.ui.fertility.FertilityUiState
import com.ayla.pcos.ui.log.DailyLogContent
import com.ayla.pcos.ui.log.DailyLogUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Doğurganlık ekranları (#24): ısı grafiği, hap modu, boş durum ve günlük kayıttaki bölüm. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TELEFON)
class DogurganlikEkranlariTest : EkranTemeli() {

    @Composable
    private fun Dogurganlik(state: FertilityUiState) = FertilityContent(state = state, onBack = {}, onPrevious = {}, onNext = {}, onAddToday = {})

    @Composable
    private fun Gunluk(state: DailyLogUiState) = DailyLogContent(
        state = state, onClose = {}, onMood = {}, onEnergy = {}, onFlow = {}, onSymptom = {},
        onWeight = {}, onWaist = {}, onNote = {}, onSave = {},
    )

    @Config(qualifiers = UZUN)
    @Test fun t40_dogurganlik() = ekran("40-dogurganlik") { Dogurganlik(OrnekDogurganlik.dongu) }

    @Test fun t41_dogurganlik_hap() = ekran("41-dogurganlik-hap") { Dogurganlik(OrnekDogurganlik.hap) }

    @Test fun t42_dogurganlik_bos() = ekran("42-dogurganlik-bos") { Dogurganlik(OrnekDogurganlik.bos) }

    @Config(qualifiers = UZUN)
    @Test fun t43_gunluk_dogurganlik() = ekran("43-gunluk-dogurganlik") { Gunluk(OrnekDogurganlik.gunlukKayit) }

    @Test fun t44_dogurganlik_koyu() = ekran("44-dogurganlik-koyu", koyu = true) { Dogurganlik(OrnekDogurganlik.dongu) }

    @Config(qualifiers = UZUN)
    @Test fun y17_yazi200_dogurganlik() = ekran("y17-yazi200-dogurganlik", sistemYaziOlcegi = 2f) { Dogurganlik(OrnekDogurganlik.dongu) }

    @Config(qualifiers = UZUN)
    @Test fun y18_yazi200_gunluk_dogurganlik() = ekran("y18-yazi200-gunluk-dogurganlik", sistemYaziOlcegi = 2f) { Gunluk(OrnekDogurganlik.gunlukKayit) }
}
