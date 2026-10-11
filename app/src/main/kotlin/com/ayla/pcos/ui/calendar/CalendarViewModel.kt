package com.ayla.pcos.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.Cycle
import com.ayla.cycle.CycleEngine
import com.ayla.cycle.CycleOverview
import com.ayla.cycle.CycleStats
import com.ayla.cycle.DayKind
import com.ayla.cycle.PeriodRecord
import com.ayla.cycle.Prediction
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Takvim ızgarasında tek hücre. */
data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val kind: DayKind,
    val isPeriodStart: Boolean,
    val hasLog: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

/** Seçili güne uygulanabilecek eylemler; ekran bunlardan buton üretir. */
data class DayActions(
    val canMarkStart: Boolean,
    val canMarkEnd: Boolean,
    val canUnmark: Boolean,
    val canOpenLog: Boolean,
    /** Seçili günün içinde olduğu adet kaydı (varsa). */
    val period: PeriodEntity?,
    /** Bu adetle başlayan döngü (varsa); hariç tutma anahtarı için. */
    val cycle: Cycle?,
)

data class CycleRow(val cycle: Cycle, val period: PeriodEntity?)

data class CalendarUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    /** 5–6 hafta × 7 gün; Pazartesi başlar. */
    val weeks: List<List<CalendarDay>> = emptyList(),
    val selected: LocalDate? = null,
    val actions: DayActions? = null,
    val stats: CycleStats? = null,
    /** En yeni en üstte; yalnızca tamamlanmış döngüler. */
    val history: List<CycleRow> = emptyList(),
    val hidePrediction: Boolean = false,
)

/** Ay + veri birleşimi; seçimle birleştirilmeden önceki ara durum. */
private data class MonthSnapshot(
    val month: YearMonth,
    val today: LocalDate,
    val weeks: List<List<CalendarDay>>,
    val periods: List<PeriodEntity>,
    val overview: CycleOverview,
    val hidePrediction: Boolean,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val prefs: UserPreferencesRepository,
    private val engine: CycleEngine,
    private val ticker: DateTicker,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow<LocalDate?>(null)

    private val snapshot = month.flatMapLatest { m ->
        val (gridStart, gridEnd) = gridRange(m)
        combine(
            cycleRepository.periods,
            cycleRepository.overview,
            prefs.preferences,
            ticker.today,
            dailyLogRepository.observeRange(gridStart, gridEnd),
        ) { periods, overview, preferences, today, logs ->
            val records = periods.map { PeriodRecord(it.startDate, it.endDate, it.excludedFromStats) }
            val prediction = if (preferences.hidePrediction) Prediction.NotEnoughData else overview.prediction
            val logDates = logs.map { it.date }.toSet()
            val starts = periods.map { it.startDate }.toSet()
            val weeks = buildWeeks(gridStart) { date ->
                CalendarDay(
                    date = date,
                    inMonth = YearMonth.from(date) == m,
                    kind = engine.dayKind(date, records, prediction, today),
                    isPeriodStart = date in starts,
                    hasLog = date in logDates,
                    isToday = date == today,
                    isFuture = date.isAfter(today),
                )
            }
            MonthSnapshot(m, today, weeks, periods, overview, preferences.hidePrediction)
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(snapshot, selected) { snap, sel ->
        val cycles = snap.overview.cycles
        val history = cycles.filter { it.isComplete }.asReversed().map { c ->
            CycleRow(c, snap.periods.firstOrNull { it.startDate == c.start })
        }
        CalendarUiState(
            loading = false,
            month = snap.month,
            today = snap.today,
            weeks = snap.weeks,
            selected = sel,
            actions = sel?.let { dayActions(it, snap.periods, cycles, snap.today) },
            stats = snap.overview.stats,
            history = history,
            hidePrediction = snap.hidePrediction,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun goToToday() {
        val today = ticker.now()
        month.value = YearMonth.from(today)
        selected.value = today
    }

    /** Aynı güne ikinci dokunuş seçimi kaldırır. */
    fun select(date: LocalDate) {
        selected.value = if (selected.value == date) null else date
    }

    fun markPeriodStart(date: LocalDate) = viewModelScope.launch { cycleRepository.addPeriodStart(date) }

    fun markPeriodEnd(date: LocalDate) = viewModelScope.launch { cycleRepository.endPeriodAt(date) }

    fun unmarkPeriod(period: PeriodEntity) = viewModelScope.launch {
        cycleRepository.deletePeriod(period)
        selected.value = null
    }

    fun setExcluded(period: PeriodEntity, excluded: Boolean) = viewModelScope.launch {
        cycleRepository.setExcluded(period, excluded)
    }

    private fun dayActions(date: LocalDate, periods: List<PeriodEntity>, cycles: List<Cycle>, today: LocalDate): DayActions {
        val records = periods.map { PeriodRecord(it.startDate, it.endDate, it.excludedFromStats) }
        val rules = calendarDayRules(date, records, cycles, today)
        return DayActions(
            canMarkStart = rules.canMarkStart,
            canMarkEnd = rules.canMarkEnd,
            canUnmark = rules.canUnmark,
            canOpenLog = rules.canOpenLog,
            period = rules.containingStart?.let { start -> periods.firstOrNull { it.startDate == start } },
            cycle = rules.cycle,
        )
    }

    private companion object {
        /** Izgaranın ilk ve son günü: ayın ilk gününü içeren Pazartesiden 6 hafta. */
        fun gridRange(month: YearMonth): Pair<LocalDate, LocalDate> {
            val first = month.atDay(1)
            val offset = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val start = first.minusDays(offset.toLong())
            return start to start.plusDays(6 * 7 - 1L)
        }

        fun buildWeeks(gridStart: LocalDate, cell: (LocalDate) -> CalendarDay): List<List<CalendarDay>> {
            val weeks = (0 until 6).map { week ->
                (0 until 7).map { day -> cell(gridStart.plusDays((week * 7 + day).toLong())) }
            }
            // Son hafta tamamen sonraki aya aitse gösterilmez (5 satırlık aylar).
            return if (weeks.last().none { it.inMonth }) weeks.dropLast(1) else weeks
        }
    }
}

/** Takvimde bir güne uygulanabilecek düzenleme kuralları; saf, test edilebilir. */
data class CalendarDayRules(
    val canMarkStart: Boolean,
    val canMarkEnd: Boolean,
    val canUnmark: Boolean,
    val canOpenLog: Boolean,
    /** Günün içinde olduğu adetin başlangıcı (varsa). */
    val containingStart: LocalDate?,
    /** O adetle başlayan döngü (varsa). */
    val cycle: Cycle?,
)

/**
 * Kurallar:
 * - Gelecek günlerde hiçbir düzenleme ve kayıt yok.
 * - Bitişi girilmemiş adet, varsayılan [defaultPeriodLengthDays] gün (bugünü aşmadan) sürüyor sayılır.
 * - "Başlangıç olarak işaretle": gün bir adetin içinde değilse.
 * - "Bitiş olarak işaretle": günden önce (ya da aynı gün) başlayan son adet varsa, bitişi zaten bu gün
 *   değilse ve başlangıçtan en fazla [maxPeriodLengthDays] gün geçmişse.
 * - "Kaydı kaldır": gün bir adetin içindeyse.
 */
fun calendarDayRules(
    date: LocalDate,
    records: List<PeriodRecord>,
    cycles: List<Cycle>,
    today: LocalDate,
    defaultPeriodLengthDays: Long = 5,
    maxPeriodLengthDays: Long = 14,
): CalendarDayRules {
    val future = date.isAfter(today)
    val containing = records.firstOrNull { r ->
        val end = r.end ?: minOf(r.start.plusDays(defaultPeriodLengthDays - 1), today)
        !date.isBefore(r.start) && !date.isAfter(end)
    }
    val previous = records.filter { !it.start.isAfter(date) }.maxByOrNull { it.start }
    val sinceStart = previous?.let { java.time.temporal.ChronoUnit.DAYS.between(it.start, date) } ?: -1L
    return CalendarDayRules(
        canMarkStart = !future && containing == null,
        canMarkEnd = !future && previous != null && previous.end != date && sinceStart in 0..maxPeriodLengthDays,
        canUnmark = !future && containing != null,
        canOpenLog = !future,
        containingStart = containing?.start,
        cycle = containing?.let { c -> cycles.firstOrNull { it.start == c.start } },
    )
}
