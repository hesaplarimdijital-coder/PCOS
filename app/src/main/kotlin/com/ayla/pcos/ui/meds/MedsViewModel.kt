package com.ayla.pcos.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.Adherence
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.DoseItem
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.data.repo.MedicationTemplates
import com.ayla.pcos.data.repo.MedicationTemplates.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class MedsUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val medications: List<MedicationEntity> = emptyList(),
    val todayDoses: List<DoseItem> = emptyList(),
    val weekAdherence: Adherence = Adherence(0, 0),
    val batteryBandDismissed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedsViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val ticker: DateTicker,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<MedsUiState> = ticker.today.flatMapLatest { today ->
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        combine(
            repository.activeMedications,
            repository.observeDoses(today),
            repository.observeAdherence(weekStart, today),
            prefs.preferences,
        ) { meds, doses, adherence, p ->
            MedsUiState(
                loading = false, today = today, medications = meds, todayDoses = doses, weekAdherence = adherence,
                batteryBandDismissed = p.batteryBandDismissed,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedsUiState())

    fun markTaken(item: DoseItem) = viewModelScope.launch { repository.markTaken(item.medication.id, item.date, item.time) }

    fun undo(item: DoseItem) = viewModelScope.launch { repository.markPending(item.medication.id, item.date, item.time) }

    fun dismissBatteryBand() = viewModelScope.launch { prefs.update { it.copy(batteryBandDismissed = true) } }

    /** Şablondan ekler; doğum kontrol hapında [packStart] paketin ilk hapının günüdür. */
    fun addTemplate(template: MedicationTemplates.Template, packStart: LocalDate?) = viewModelScope.launch {
        repository.add(template.toEntity(packStart = packStart ?: ticker.now()))
    }
}
