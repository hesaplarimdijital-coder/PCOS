package com.ayla.pcos.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DailyLogUiState(
    val date: LocalDate = LocalDate.now(),
    val loaded: Boolean = false,
    val mood: Int? = null,
    val energy: Int? = null,
    val flow: Int? = null,
    val symptoms: Set<String> = emptySet(),
    val note: String = "",
    val weightKg: String = "",
    val waistCm: String = "",
    val showBodyMeasurements: Boolean = false,
    val saved: Boolean = false,
    /** Başlık: bugün için "Bugün nasılsın?", geçmiş gün için "O gün nasıldın?". */
    val isToday: Boolean = true,
)

@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    private val prefs: UserPreferencesRepository,
    private val ticker: DateTicker,
) : ViewModel() {

    private val _state = MutableStateFlow(DailyLogUiState())
    val state: StateFlow<DailyLogUiState> = _state

    /** Ekran açılınca o günün kaydını yükler; aynı ViewModel farklı günler için yeniden kullanılır. */
    fun load(date: LocalDate) {
        viewModelScope.launch {
            val existing = repository.get(date)
            val showBody = prefs.current().showBodyMeasurements
            _state.value = DailyLogUiState(
                date = date,
                loaded = true,
                mood = existing?.mood,
                energy = existing?.energy,
                flow = existing?.flow,
                symptoms = existing?.symptoms?.toSet() ?: emptySet(),
                note = existing?.note ?: "",
                weightKg = existing?.weightKg?.toString() ?: "",
                waistCm = existing?.waistCm?.toString() ?: "",
                showBodyMeasurements = showBody,
                isToday = date == ticker.now(),
            )
        }
    }

    fun setMood(value: Int) = _state.update { it.copy(mood = if (it.mood == value) null else value, saved = false) }
    fun setEnergy(value: Int) = _state.update { it.copy(energy = if (it.energy == value) null else value, saved = false) }
    fun setFlow(value: Int) = _state.update { it.copy(flow = if (it.flow == value) null else value, saved = false) }
    fun toggleSymptom(key: String) = _state.update {
        it.copy(symptoms = if (key in it.symptoms) it.symptoms - key else it.symptoms + key, saved = false)
    }
    fun setNote(value: String) = _state.update { it.copy(note = value.take(2000), saved = false) }
    fun setWeight(value: String) = _state.update { it.copy(weightKg = value.filterNumeric(), saved = false) }
    fun setWaist(value: String) = _state.update { it.copy(waistCm = value.filterNumeric(), saved = false) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            repository.save(
                DailyLogEntity(
                    date = s.date,
                    mood = s.mood,
                    energy = s.energy,
                    flow = s.flow,
                    symptoms = s.symptoms.toList(),
                    note = s.note.trim().ifBlank { null },
                    weightKg = s.weightKg.replace(',', '.').toDoubleOrNull(),
                    waistCm = s.waistCm.replace(',', '.').toDoubleOrNull(),
                ),
            )
            _state.update { it.copy(saved = true) }
            onDone()
        }
    }

    private fun String.filterNumeric(): String = filter { it.isDigit() || it == '.' || it == ',' }.take(6)
}
