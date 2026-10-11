package com.ayla.pcos.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.CycleOverview
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.Adherence
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

/** Döngü düzeni özeti; metinler ekranda üretilir. */
sealed interface CycleTrend {
    data object NeedData : CycleTrend
    /** Son [count] döngü art arda kısaldı ve sonuncusu 21–35 aralığında. */
    data class Shortening(val count: Int) : CycleTrend
    data class Stable(val mean: Int, val deviation: Int) : CycleTrend
    data class Variable(val shortest: Int, val longest: Int) : CycleTrend
}

data class MeUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val displayName: String = "",
    /** Hesaba katılan son 6 döngünün uzunlukları, eskiden yeniye. */
    val cycleLengths: List<Int> = emptyList(),
    val trend: CycleTrend = CycleTrend.NeedData,
    val adherence: Adherence = Adherence(0, 0),
    val hasMedications: Boolean = false,
    /** Bu hafta (Pazartesiden bugüne) su hedefine ulaşılan gün sayısı. */
    val waterDaysThisWeek: Int = 0,
    val logDaysThisWeek: Int = 0,
    /**
     * Son 4 hafta, Pazartesiden Pazara satırlar hâlinde (28 hücre, eskiden yeniye): kayıt var mı.
     * Bu haftanın henüz gelmemiş günleri null.
     */
    val logGrid: List<Boolean?> = emptyList(),
)

@HiltViewModel
class MeViewModel @Inject constructor(
    cycleRepository: CycleRepository,
    medicationRepository: MedicationRepository,
    hydrationRepository: HydrationRepository,
    dailyLogRepository: DailyLogRepository,
    prefs: UserPreferencesRepository,
    ticker: DateTicker,
) : ViewModel() {

    val uiState: StateFlow<MeUiState> = ticker.today
        .flatMapLatest { today ->
            val weekStart = today.with(DayOfWeek.MONDAY)
            val gridStart = weekStart.minusWeeks(3)
            combine(
                cycleRepository.overview,
                medicationRepository.observeAdherence(today.minusDays(6), today),
                hydrationRepository.observeRange(weekStart, today),
                dailyLogRepository.observeRange(gridStart, today),
                prefs.preferences,
            ) { overview, adherence, water, logs, p ->
                val logDates = logs.map { it.date }.toSet()
                MeUiState(
                    loading = false,
                    today = today,
                    displayName = p.displayName,
                    cycleLengths = eligibleLengths(overview).takeLast(6),
                    trend = trend(overview),
                    adherence = adherence,
                    hasMedications = adherence.scheduled > 0,
                    waterDaysThisWeek = water.count { it.cups >= p.waterGoalCups },
                    logDaysThisWeek = logDates.count { !it.isBefore(weekStart) },
                    logGrid = (0 until 28).map { i ->
                        val d = gridStart.plusDays(i.toLong())
                        if (d.isAfter(today)) null else d in logDates
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeUiState())

    private fun eligibleLengths(overview: CycleOverview): List<Int> =
        overview.cycles.filter { it.countsForStats }.mapNotNull { it.lengthDays }

    private fun trend(overview: CycleOverview): CycleTrend {
        val lengths = eligibleLengths(overview)
        if (lengths.size < 3) return CycleTrend.NeedData
        // Sondan geriye art arda kısalma zinciri.
        var run = 1
        var i = lengths.lastIndex
        while (i > 0 && lengths[i] < lengths[i - 1]) {
            run++
            i--
        }
        if (run >= 3 && lengths.last() in 21..35) return CycleTrend.Shortening(run)
        val stats = overview.stats
        val mean = stats.mean
        val sd = stats.standardDeviation
        if (mean != null && sd != null && sd <= 3.0) return CycleTrend.Stable(mean.roundToInt(), sd.roundToInt().coerceAtLeast(1))
        return CycleTrend.Variable(lengths.min(), lengths.max())
    }
}
