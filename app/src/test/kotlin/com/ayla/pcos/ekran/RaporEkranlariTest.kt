package com.ayla.pcos.ekran

import androidx.compose.runtime.Composable
import com.ayla.pcos.ui.report.QuestionsContent
import com.ayla.pcos.ui.report.QuestionsUiState
import com.ayla.pcos.ui.report.ReportContent
import com.ayla.pcos.ui.report.ReportUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Doktor raporu ve doktora sorular ekranları (#26). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TELEFON)
class RaporEkranlariTest : EkranTemeli() {

    @Composable
    private fun Rapor(state: ReportUiState) = ReportContent(
        state = state, onBack = {}, onRange = {}, onCustomFrom = {}, onCustomTo = {}, onToggle = {},
        onOpenQuestions = {}, onSharePdf = {}, onShareText = {},
    )

    @Composable
    private fun Sorular(state: QuestionsUiState, sorulanlar: Boolean = false) = QuestionsContent(
        state = state, onBack = {}, onDraft = {}, onSuggestion = {}, onAdd = {}, onAsked = { _, _ -> }, onDelete = {},
        initialShowAsked = sorulanlar,
    )

    @Config(qualifiers = UZUN)
    @Test fun t60_rapor() = ekran("60-rapor") { Rapor(OrnekRapor.rapor) }

    @Config(qualifiers = UZUN)
    @Test fun t61_rapor_ozel_aralik() = ekran("61-rapor-ozel-aralik") { Rapor(OrnekRapor.ozelAralik) }

    @Test fun t62_rapor_hata() = ekran("62-rapor-hata") { Rapor(OrnekRapor.hata) }

    @Config(qualifiers = UZUN)
    @Test fun t63_sorular() = ekran("63-sorular") { Sorular(OrnekRapor.sorular, sorulanlar = true) }

    @Test fun t64_sorular_bos() = ekran("64-sorular-bos") { Sorular(OrnekRapor.sorularBos) }

    @Test fun t65_rapor_koyu() = ekran("65-rapor-koyu", koyu = true) { Rapor(OrnekRapor.rapor) }

    @Config(qualifiers = UZUN)
    @Test fun y24_yazi200_rapor() = ekran("y24-yazi200-rapor", sistemYaziOlcegi = 2f) { Rapor(OrnekRapor.rapor) }

    @Config(qualifiers = UZUN)
    @Test fun y25_yazi200_sorular() = ekran("y25-yazi200-sorular", sistemYaziOlcegi = 2f) { Sorular(OrnekRapor.sorular, sorulanlar = true) }
}
