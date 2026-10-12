package com.ayla.pcos.ekran

import com.ayla.pcos.data.db.DoctorQuestionEntity
import com.ayla.pcos.ui.report.QuestionsUiState
import com.ayla.pcos.ui.report.RangeChoice
import com.ayla.pcos.ui.report.ReportSectionChoice
import com.ayla.pcos.ui.report.ReportUiState

/** Doktor raporu ve sorular ekran görüntüleri için örnek veri (#26). */
internal object OrnekRapor {
    private val bugun = Ornek.bugun
    private val an = bugun.atStartOfDay(Ornek.zone).toInstant()

    val rapor = ReportUiState(loaded = true, today = bugun, pageCount = 3, openQuestions = 2)

    val ozelAralik = rapor.copy(
        range = RangeChoice.CUSTOM,
        customFrom = bugun.minusDays(45),
        customTo = bugun.minusDays(3),
        sections = ReportSectionChoice.entries.toSet() - ReportSectionChoice.SLEEP_WATER,
        fertilityAvailable = false,
        pageCount = null,
    )

    val hata = rapor.copy(error = true)

    val sorular = QuestionsUiState(
        loaded = true,
        open = listOf(
            DoctorQuestionEntity(id = 1, text = "Döngüm düzensiz kalırsa ne zaman yeniden görüşmeliyiz?", createdAt = an),
            DoctorQuestionEntity(id = 2, text = "Kullandığım takviyeleri sürdürmeli miyim?", createdAt = an),
        ),
        asked = listOf(DoctorQuestionEntity(id = 3, text = "Tahlillerimi hangi sıklıkla tekrarlamalıyım?", createdAt = an, askedAt = an)),
        draft = "Cildimdeki koyu alanları",
    )

    val sorularBos = QuestionsUiState(loaded = true)
}
