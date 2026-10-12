package com.ayla.pcos.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.FertilityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
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
    /** O gün için kaydedilmiş bir kayıt var; "Bu günün kaydını sil" yalnızca o zaman görünür. */
    val hasSavedLog: Boolean = false,
    /** Doğurganlık bölümü: "hamile kalmayı planlıyorum" açık ve hap modunda değil (ADR-0005, #24). */
    val fertility: FertilityInput? = null,
)

/** Günlük kayıttaki doğurganlık alanları; ısı metin olarak tutulur (Türkçe virgül kabul edilir). */
data class FertilityInput(
    val bbtText: String = "",
    val bbtTime: LocalTime? = null,
    val bbtDisturbed: Boolean = false,
    val lh: LhResult? = null,
    val mucus: CervicalMucus? = null,
) {
    /** Metin boşsa null; sayı değilse [Double.NaN] (alan hatası gösterilir, kaydedilmez). */
    val bbtCelsius: Double? get() = parseCelsius(bbtText)

    val bbtInvalid: Boolean get() = bbtCelsius?.isNaN() == true

    /** 35,0–38,5 dışı: kaydedilir ama motor hesaba katmaz; alanda uyarı görünür. */
    val bbtOutOfRange: Boolean get() = bbtCelsius?.let { !it.isNaN() && (it < 35.0 || it > 38.5) } == true

    val isEmpty: Boolean get() = bbtCelsius == null && lh == null && mucus == null

    companion object {
        fun parseCelsius(text: String): Double? {
            val t = text.trim().replace(',', '.')
            if (t.isEmpty()) return null
            return t.toDoubleOrNull() ?: Double.NaN
        }

        fun from(entity: FertilityEntryEntity?): FertilityInput = FertilityInput(
            bbtText = entity?.bbtCelsius?.let { String.format(java.util.Locale.forLanguageTag("tr"), "%.2f", it) } ?: "",
            bbtTime = entity?.bbtTime,
            bbtDisturbed = entity?.bbtDisturbed ?: false,
            lh = entity?.lhResult,
            mucus = entity?.mucus,
        )
    }
}

@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    private val prefs: UserPreferencesRepository,
    private val ticker: DateTicker,
    private val fertility: FertilityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DailyLogUiState())
    val state: StateFlow<DailyLogUiState> = _state

    /** Ekran açılınca o günün kaydını yükler; aynı ViewModel farklı günler için yeniden kullanılır. */
    fun load(date: LocalDate) {
        viewModelScope.launch {
            val existing = repository.get(date)
            val preferences = prefs.current()
            val showBody = preferences.showBodyMeasurements
            val fertilityVisible = Goal.TTC in preferences.goals && !fertility.isOnPill()
            val fertilityInput = if (fertilityVisible) FertilityInput.from(fertility.get(date)) else null
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
                hasSavedLog = existing != null || fertilityInput?.isEmpty == false,
                fertility = fertilityInput,
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

    fun setBbt(value: String) = updateFertility { it.copy(bbtText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5)) }
    fun setBbtTime(value: LocalTime?) = updateFertility { it.copy(bbtTime = value) }
    fun setBbtDisturbed(value: Boolean) = updateFertility { it.copy(bbtDisturbed = value) }
    fun setLh(value: LhResult?) = updateFertility { it.copy(lh = value) }
    fun setMucus(value: CervicalMucus?) = updateFertility { it.copy(mucus = if (it.mucus == value) null else value) }

    private fun updateFertility(transform: (FertilityInput) -> FertilityInput) = _state.update { s ->
        val current = s.fertility ?: return@update s
        s.copy(fertility = transform(current), saved = false)
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        // Geçersiz ısı metni kaydı durdurur; kullanıcı alandaki hatayı görür.
        if (s.fertility?.bbtInvalid == true) return
        viewModelScope.launch {
            s.fertility?.let { saveFertility(s.date, it) }
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

    private suspend fun saveFertility(date: LocalDate, input: FertilityInput) {
        if (input.isEmpty) {
            fertility.delete(date)
            return
        }
        val celsius = input.bbtCelsius
        fertility.save(
            FertilityEntryEntity(
                date = date,
                bbtCelsius = celsius,
                bbtTime = if (celsius != null) input.bbtTime else null,
                bbtDisturbed = celsius != null && input.bbtDisturbed,
                lhResult = input.lh,
                mucus = input.mucus,
            ),
        )
    }

    /** O günün kaydını tamamen siler (KVKK: tek tek silme her zaman mümkün); doğurganlık kaydı da gider. */
    fun delete(onDone: () -> Unit) {
        val date = _state.value.date
        viewModelScope.launch {
            repository.delete(date)
            fertility.delete(date)
            onDone()
        }
    }

    private fun String.filterNumeric(): String = filter { it.isDigit() || it == '.' || it == ',' }.take(6)
}
