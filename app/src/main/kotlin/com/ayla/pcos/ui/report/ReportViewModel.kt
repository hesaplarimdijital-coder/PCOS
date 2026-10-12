package com.ayla.pcos.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.DoctorQuestionRepository
import com.ayla.pcos.data.repo.FertilityRepository
import com.ayla.pcos.di.ApplicationScope
import com.ayla.pcos.report.DoctorReportBuilder
import com.ayla.pcos.report.PaintTextMeasurer
import com.ayla.pcos.report.PdfReportWriter
import com.ayla.pcos.report.ReportLayout
import com.ayla.pcos.report.ReportOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/** Hazır aralıklar ve "özel". */
enum class RangeChoice(val days: Long?) { DAYS_28(28), DAYS_90(90), DAYS_180(180), CUSTOM(null) }

/** Rapor bölümleri; ekrandaki anahtarlar bu sırayla gösterilir. */
enum class ReportSectionChoice { CYCLE, LOG, MEDS, SLEEP_WATER, FERTILITY, SKIN, QUESTIONS }

data class ReportUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val range: RangeChoice = RangeChoice.DAYS_90,
    val customFrom: LocalDate = LocalDate.now().minusDays(89),
    val customTo: LocalDate = LocalDate.now(),
    val sections: Set<ReportSectionChoice> = ReportSectionChoice.entries.toSet(),
    /** Hap modunda doğurganlık bölümü yazılmaz; anahtar pasif görünür. */
    val fertilityAvailable: Boolean = true,
    /** null: hesaplanıyor. */
    val pageCount: Int? = null,
    val openQuestions: Int = 0,
    val busy: Boolean = false,
    val error: Boolean = false,
) {
    val from: LocalDate get() = range.days?.let { today.minusDays(it - 1) } ?: customFrom
    val to: LocalDate get() = if (range == RangeChoice.CUSTOM) customTo else today

    fun options(): ReportOptions = ReportOptions(
        from = from,
        to = to,
        includeCycle = ReportSectionChoice.CYCLE in sections,
        includeLog = ReportSectionChoice.LOG in sections,
        includeMeds = ReportSectionChoice.MEDS in sections,
        includeSleepWater = ReportSectionChoice.SLEEP_WATER in sections,
        includeFertility = fertilityAvailable && ReportSectionChoice.FERTILITY in sections,
        includeSkin = ReportSectionChoice.SKIN in sections,
        includeQuestions = ReportSectionChoice.QUESTIONS in sections,
    )
}

/** Ekranın tek seferlik eylemleri: paylaşım menüsü açılır (uygulama dosyayı kendisi hiçbir yere göndermez). */
sealed interface ReportEvent {
    data class SharePdf(val file: File, val subject: String) : ReportEvent

    data class ShareText(val subject: String, val text: String) : ReportEvent
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val builder: DoctorReportBuilder,
    private val writer: PdfReportWriter,
    private val fertility: FertilityRepository,
    questions: DoctorQuestionRepository,
    ticker: DateTicker,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState(today = ticker.now(), customFrom = ticker.now().minusDays(89), customTo = ticker.now()))
    val state: StateFlow<ReportUiState> = _state

    private val _events = MutableSharedFlow<ReportEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ReportEvent> = _events

    init {
        viewModelScope.launch {
            val available = !fertility.isOnPill()
            _state.update { it.copy(loaded = true, fertilityAvailable = available) }
        }
        viewModelScope.launch {
            questions.observeAll().collect { list -> _state.update { s -> s.copy(openQuestions = list.count { it.askedAt == null }) } }
        }
        // Seçim değişince sayfa sayısı yeniden hesaplanır (yerleşim motoru ucuzdur; kısa bekleme ardışık dokunuşları birleştirir).
        viewModelScope.launch {
            _state.map { it.takeIf { s -> s.loaded }?.options() }
                .distinctUntilChanged()
                .debounce(PAGE_COUNT_DEBOUNCE_MS)
                .collect { options ->
                    if (options == null) return@collect
                    _state.update { it.copy(pageCount = null) }
                    val pages = runCatching { pageCount(options) }.getOrNull()
                    _state.update { it.copy(pageCount = pages) }
                }
        }
    }

    internal suspend fun pageCount(options: ReportOptions): Int = withContext(Dispatchers.Default) {
        ReportLayout(PaintTextMeasurer()).pageCount(builder.build(options))
    }

    fun setRange(range: RangeChoice) = _state.update { it.copy(range = range, error = false) }

    /** Özel aralık: başlangıç bitişi, bitiş bugünü geçemez; ters seçimde diğer uç kaydırılır. */
    fun setCustomFrom(date: LocalDate) = _state.update { s ->
        val from = minOf(date, s.today)
        s.copy(range = RangeChoice.CUSTOM, customFrom = from, customTo = maxOf(s.customTo, from), error = false)
    }

    fun setCustomTo(date: LocalDate) = _state.update { s ->
        val to = minOf(date, s.today)
        s.copy(range = RangeChoice.CUSTOM, customTo = to, customFrom = minOf(s.customFrom, to), error = false)
    }

    fun toggleSection(section: ReportSectionChoice) = _state.update { s ->
        s.copy(sections = if (section in s.sections) s.sections - section else s.sections + section)
    }

    /** PDF'i uygulama kapsamında üretir (ekran kapanırsa yarıda kalmaz) ve paylaşım olayını yayar. */
    fun sharePdf(subject: String) {
        val options = _state.value.options()
        _state.update { it.copy(busy = true, error = false) }
        appScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    writer.report(builder.build(options), builder.fileName(), pageLabel = builder::pageLabel)
                }
                _events.tryEmit(ReportEvent.SharePdf(file, subject))
                _state.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = true) }
            }
        }
    }

    fun shareText(subject: String) {
        val options = _state.value.options()
        viewModelScope.launch {
            _events.tryEmit(ReportEvent.ShareText(subject, builder.plainText(options)))
        }
    }

    companion object {
        const val PAGE_COUNT_DEBOUNCE_MS = 300L
    }
}
