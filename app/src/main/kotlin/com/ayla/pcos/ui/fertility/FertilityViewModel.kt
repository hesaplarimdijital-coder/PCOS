package com.ayla.pcos.ui.fertility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.CycleFertility
import com.ayla.cycle.FertilityOverview
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.FertilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * Doğurganlık ekranının durumu (#24). [cycles] eskiden yeniye; [selected] gösterilen döngünün indeksi
 * (varsayılan en yeni). Hap modunda [disabled]: hiçbir hesap gösterilmez (ADR-0005).
 */
data class FertilityUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val ttcGoal: Boolean = false,
    val disabled: Boolean = false,
    val cycles: List<CycleFertility> = emptyList(),
    val selected: Int = -1,
) {
    val cycle: CycleFertility? get() = cycles.getOrNull(selected)

    val canGoPrevious: Boolean get() = selected > 0

    val canGoNext: Boolean get() = selected in 0 until cycles.lastIndex

    /** Gösterilen döngü bugünü içeriyor; "bugünün ölçümünü ekle" yalnızca o zaman. */
    val showsCurrent: Boolean get() = cycle?.isOpen == true

    /** Açık döngüde bugünün döngü günü. */
    val cycleDay: Int? get() = cycle?.takeIf { it.isOpen }?.let { (today.toEpochDay() - it.start.toEpochDay() + 1).toInt() }
}

@HiltViewModel
class FertilityViewModel @Inject constructor(
    fertility: FertilityRepository,
    prefs: UserPreferencesRepository,
    ticker: DateTicker,
) : ViewModel() {

    /** null: en yeni döngü (yeni adet başlayınca otomatik ona geçer). */
    private val selection = MutableStateFlow<Int?>(null)

    val state: StateFlow<FertilityUiState> = combine(
        fertility.overview,
        prefs.preferences,
        selection,
        ticker.today,
    ) { overview, p, selected, today ->
        when (overview) {
            FertilityOverview.Disabled -> FertilityUiState(loaded = true, today = today, ttcGoal = Goal.TTC in p.goals, disabled = true)
            is FertilityOverview.Enabled -> {
                val cycles = overview.cycles
                val index = (selected ?: cycles.lastIndex).coerceIn(-1, cycles.lastIndex)
                FertilityUiState(loaded = true, today = today, ttcGoal = Goal.TTC in p.goals, cycles = cycles, selected = index)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FertilityUiState())

    fun previous() {
        val s = state.value
        if (s.canGoPrevious) selection.value = s.selected - 1
    }

    fun next() {
        val s = state.value
        if (!s.canGoNext) return
        val target = s.selected + 1
        selection.value = if (target == s.cycles.lastIndex) null else target
    }
}
