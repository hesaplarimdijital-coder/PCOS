package com.ayla.pcos.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.data.repo.MedicationTemplates
import com.ayla.pcos.data.repo.MedicationTemplates.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 0,
    val name: String = "",
    val goals: Set<Goal> = setOf(Goal.CYCLE, Goal.SYMPTOMS),
    val lastPeriod: LocalDate? = null,
    /** Sağlık verisinin cihazda işlenmesine açık rıza; işaretlenmeden karşılama bitmez. */
    val healthDataConsent: Boolean = false,
    /** İsteğe bağlı; varsayılan kapalı. */
    val consentPersonalization: Boolean = false,
    val saving: Boolean = false,
) {
    val canFinish: Boolean get() = healthDataConsent && !saving

    companion object {
        const val STEP_COUNT = 4
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val cycleRepository: CycleRepository,
    private val medicationRepository: MedicationRepository,
    private val ticker: DateTicker,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun setName(value: String) = _state.update { it.copy(name = value.take(40)) }

    fun toggleGoal(goal: Goal) = _state.update {
        val goals = if (goal in it.goals) it.goals - goal else it.goals + goal
        it.copy(goals = goals)
    }

    fun setLastPeriod(date: LocalDate?) = _state.update { it.copy(lastPeriod = date) }

    fun setHealthDataConsent(value: Boolean) = _state.update { it.copy(healthDataConsent = value) }

    fun setPersonalization(value: Boolean) = _state.update { it.copy(consentPersonalization = value) }

    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(OnboardingState.STEP_COUNT - 1)) }

    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    /** Son adım: tercihleri yazar, varsa son adeti ve hap şablonunu ekler, karşılamayı kapatır. */
    fun finish() {
        val s = _state.value
        if (!s.canFinish) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val today = ticker.now()
            prefs.update {
                it.copy(
                    displayName = s.name.trim(),
                    goals = s.goals,
                    healthDataConsentAt = Instant.now(),
                    consentPersonalization = s.consentPersonalization,
                    trackingSince = it.trackingSince ?: today,
                )
            }
            s.lastPeriod?.let { if (!it.isAfter(today)) cycleRepository.startPeriod(it) }
            if (Goal.PILL in s.goals) {
                medicationRepository.add(MedicationTemplates.PILL_24_4.toEntity(packStart = today))
            }
            prefs.update { it.copy(onboardingDone = true) }
        }
    }
}
