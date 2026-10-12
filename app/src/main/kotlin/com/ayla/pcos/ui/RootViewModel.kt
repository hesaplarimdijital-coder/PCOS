package com.ayla.pcos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.security.DatabaseKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Kök durum: karşılama tamamlandı mı, uygulama kilitli mi. */
data class RootUiState(
    val loading: Boolean = true,
    val onboardingDone: Boolean = false,
    val locked: Boolean = false,
    /** Önceki veriler açılamadı ve yeni başlangıç yapıldı; kullanıcıya bir kez söylenir. */
    val dataResetNotice: Boolean = false,
    /** Karşılama bitmiş ama sağlık verisine güncel kapsamda açık rıza kaydı yok (önceki sürümden kalan kurulum). */
    val needsHealthConsent: Boolean = false,
    /** Rıza var ama eski kapsamda; pencere yeni veri türlerini anlatır (#20). */
    val consentScopeUpdated: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val keyProvider: DatabaseKeyProvider,
) : ViewModel() {

    private val unlocked = MutableStateFlow(false)

    /** Arka plana gidiş zamanı; bu süreden uzun kalınca kilit yeniden devreye girer. */
    private var backgroundedAt: Long? = null

    val uiState: StateFlow<RootUiState> = combine(
        prefs.preferences,
        unlocked,
        keyProvider.resetNoticePending,
    ) { p, isUnlocked, resetNotice ->
        RootUiState(
            loading = false,
            onboardingDone = p.onboardingDone,
            locked = p.appLockEnabled && !isUnlocked,
            dataResetNotice = resetNotice,
            needsHealthConsent = p.onboardingDone && (p.healthDataConsentAt == null || p.healthConsentOutdated),
            consentScopeUpdated = p.healthConsentOutdated,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState())

    fun onUnlocked() {
        unlocked.value = true
    }

    fun acknowledgeDataReset() = keyProvider.acknowledgeResetNotice()

    fun giveHealthDataConsent() = viewModelScope.launch {
        prefs.update { it.copy(healthDataConsentAt = Instant.now(), healthConsentVersion = UserPreferences.HEALTH_CONSENT_VERSION) }
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
