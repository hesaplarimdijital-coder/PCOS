package com.ayla.pcos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Kök durum: karşılama tamamlandı mı, uygulama kilitli mi. */
data class RootUiState(
    val loading: Boolean = true,
    val onboardingDone: Boolean = false,
    val locked: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    private val unlocked = MutableStateFlow(false)

    /** Arka plana gidiş zamanı; bu süreden uzun kalınca kilit yeniden devreye girer. */
    private var backgroundedAt: Long? = null

    val uiState: StateFlow<RootUiState> = combine(
        prefs.preferences.map { it.onboardingDone to it.appLockEnabled },
        unlocked,
    ) { (onboardingDone, lockEnabled), isUnlocked ->
        RootUiState(
            loading = false,
            onboardingDone = onboardingDone,
            locked = lockEnabled && !isUnlocked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState())

    fun onUnlocked() {
        unlocked.value = true
    }

    fun onBackgrounded(now: Long) {
        backgroundedAt = now
    }

    fun onForegrounded(now: Long) {
        val since = backgroundedAt ?: return
        if (now - since > RELOCK_AFTER_MS) unlocked.value = false
        backgroundedAt = null
    }

    private companion object {
        const val RELOCK_AFTER_MS = 60_000L
    }
}
