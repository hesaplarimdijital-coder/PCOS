package com.ayla.pcos.ui.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.CycleOverview
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.Adherence
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.FertilityRepository
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
    /** "Hamile kalmayı planlıyorum" açık ve hap modunda değil: Doğurganlık girişi görünür (#24). */
    val showFertility: Boolean = false,
)

@HiltViewModel
class MeViewModel @Inject constructor(
    cycleRepository: CycleRepository,
    medicationRepository: MedicationRepository,
    hydrationRepository: HydrationRepository,
    dailyLogRepository: DailyLogRepository,
    prefs: UserPreferencesRepository,
    ticker: DateTicker,
    fertilityRepository: FertilityRepository,
) : ViewModel() {

    private val preferencesAndPill = combine(prefs.preferences, fertilityRepository.onPill) { p, pill -> p to pill }


    val uiState: StateFlow<MeUiState> = ticker.today
        .flatMapLatest { today ->
            val weekStart = today.with(DayOfWeek.MONDAY)
            val gridStart = weekStart.minusWeeks(3)
            combine(
                cycleRepository.overview,
                medicationRepository.observeAdherence(today.minusDays(6), today),
                hydrationRepository.observeRange(weekStart, today),
                dailyLogRepository.observeRange(gridStart, today),
                preferencesAndPill,
            ) { overview, adherence, water, logs, (p, onPill) ->
                val logDates = logs.map { it.date }.toSet()
                MeUiState(
                    loading = false,
                    today = today,
                    displayName = p.displayName,
                    cycleLengths = eligibleCycleLengths(overview).takeLast(6),
                    trend = cycleTrend(overview),
                    adherence = adherence,
                    hasMedications = adherence.scheduled > 0,
                    waterDaysThisWeek = water.count { it.cups >= p.waterGoalCups },
                    logDaysThisWeek = logDates.count { !it.isBefore(weekStart) },
                    logGrid = (0 until 28).map { i ->
                        val d = gridStart.plusDays(i.toLong())
                        if (d.isAfter(today)) null else d in logDates
                    },
                    showFertility = Goal.TTC in p.goals && !onPill,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeUiState())
}

/** Ortalamaya katılan tamamlanmış döngülerin uzunlukları, eskiden yeniye. */
internal fun eligibleCycleLengths(overview: CycleOverview): List<Int> =
    overview.cycles.filter { it.countsForStats }.mapNotNull { it.lengthDays }

/**
 * Döngü düzeni özeti:
 * - 3'ten az döngü → veri yetersiz
 * - sondan art arda en az 3 döngü kısaldıysa ve sonuncusu 21–35 gündeyse → kısalıyor
 * - standart sapma ≤ 3 gün → düzenli
 * - aksi hâlde → değişken (en kısa–en uzun)
 */
internal fun cycleTrend(overview: CycleOverview): CycleTrend {
    val lengths = eligibleCycleLengths(overview)
    if (lengths.size < 3) return CycleTrend.NeedData
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
