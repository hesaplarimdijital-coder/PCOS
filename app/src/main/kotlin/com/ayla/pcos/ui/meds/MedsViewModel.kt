package com.ayla.pcos.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
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
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class MedsUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val medications: List<MedicationEntity> = emptyList(),
    val todayDoses: List<DoseItem> = emptyList(),
    val weekAdherence: Adherence = Adherence(0, 0),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedsViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val ticker: DateTicker,
) : ViewModel() {

    val uiState: StateFlow<MedsUiState> = ticker.today.flatMapLatest { today ->
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        combine(
            repository.activeMedications,
            repository.observeDoses(today),
            repository.observeAdherence(weekStart, today),
        ) { meds, doses, adherence ->
            MedsUiState(loading = false, today = today, medications = meds, todayDoses = doses, weekAdherence = adherence)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MedsUiState())

    fun markTaken(item: DoseItem) = viewModelScope.launch { repository.markTaken(item.medication.id, item.date, item.time) }

    fun undo(item: DoseItem) = viewModelScope.launch { repository.markPending(item.medication.id, item.date, item.time) }

    fun remove(medication: MedicationEntity) = viewModelScope.launch { repository.deactivate(medication.id) }

    fun addTemplate(template: MedicationTemplates.Template) = viewModelScope.launch {
        repository.add(template.toEntity(packStart = ticker.now()))
    }

    fun addCustom(name: String, doseText: String, kind: MedicationKind, times: List<LocalTime>, withFood: Boolean?, stock: Int?) =
        viewModelScope.launch {
            if (name.isBlank() || times.isEmpty()) return@launch
            repository.add(
                MedicationEntity(
                    name = name.trim(),
                    doseText = doseText.trim().ifBlank { "1 doz" },
                    kind = kind,
                    form = if (kind == MedicationKind.HERBAL) MedicationForm.TEA else MedicationForm.CAPSULE,
                    times = times.sorted(),
                    withFood = withFood,
                    stockCount = stock,
                ),
            )
        }
}
