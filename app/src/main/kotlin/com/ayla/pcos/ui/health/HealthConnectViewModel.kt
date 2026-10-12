package com.ayla.pcos.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.health.HealthConnectAvailability
import com.ayla.pcos.data.health.HealthConnectRepository
import com.ayla.pcos.data.health.SleepImportResult
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DateTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Akışın kullanıcıdan beklediği adım. */
enum class HcStep { UPDATE_REQUIRED, RATIONALE, REQUEST_PERMISSION, CANCELLED_TWICE }

/** Kullanıcıya gösterilen son sonuç. */
sealed interface HcMessage {
    data class Added(val count: Int) : HcMessage
    data object NothingNew : HcMessage
    data object Failed : HcMessage
    data object PermissionDenied : HcMessage
    data object PermissionRevoked : HcMessage
    data object Disconnected : HcMessage
    data class Deleted(val count: Int) : HcMessage
}

data class HealthConnectUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    /** Gerekçe ekranında açık rıza verildi (bağlantı kesilince silinir). */
    val consentGiven: Boolean = false,
    val permissionGranted: Boolean = false,
    val importing: Boolean = false,
    /** Ayla'daki Health Connect kopyası gece sayısı. */
    val importedCount: Int = 0,
    val step: HcStep? = null,
    val message: HcMessage? = null,
) {
    /** Desteklenmeyen cihazda giriş noktaları gizlenir; elle giriş her zaman açık. */
    val visible: Boolean get() = availability != HealthConnectAvailability.UNAVAILABLE

    val connected: Boolean get() = availability == HealthConnectAvailability.AVAILABLE && consentGiven && permissionGranted
}

private data class LocalState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val permissionGranted: Boolean = false,
    val importing: Boolean = false,
    val step: HcStep? = null,
    val message: HcMessage? = null,
)

/**
 * Health Connect akışı (#11). Planım ve Ayarlar aynı örneği paylaşır. Sıra: güncelleme gerekiyorsa Play'e
 * yönlendirme → açık rıza yoksa gerekçe ekranı → izin yoksa Health Connect'in izin penceresi (art arda iki
 * kez kapatıldıysa Health Connect ayarları) → alma. Ön plana gelişte, bağlıysa yeni geceler sessizce alınır.
 */
@HiltViewModel
class HealthConnectViewModel @Inject constructor(
    private val repository: HealthConnectRepository,
    private val prefs: UserPreferencesRepository,
    ticker: DateTicker,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState())

    val uiState: StateFlow<HealthConnectUiState> = combine(local, prefs.preferences, repository.importedCount) { l, p, count ->
        HealthConnectUiState(
            availability = l.availability,
            consentGiven = p.healthConnectConsentAt != null,
            permissionGranted = l.permissionGranted,
            importing = l.importing,
            importedCount = count,
            step = l.step,
            message = l.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthConnectUiState())

    init {
        // Kullanıcı Play'den kurup ya da Health Connect ayarlarından izni değiştirip dönebilir.
        viewModelScope.launch {
            ticker.foreground.collect {
                refresh()
                val result = repository.syncIfConnected()
                if (result is SleepImportResult.Added && result.count > 0) {
                    local.update { it.copy(message = HcMessage.Added(result.count)) }
                }
            }
        }
    }

    private suspend fun refresh() {
        val availability = repository.availability()
        val granted = availability == HealthConnectAvailability.AVAILABLE && repository.hasSleepPermission()
        local.update { it.copy(availability = availability, permissionGranted = granted) }
    }

    /** "Health Connect'ten al" ya da Ayarlar'da "Bağlan": eksik adımı açar, her şey tamamsa alır. */
    fun start() {
        viewModelScope.launch {
            refresh()
            val p = prefs.current()
            val s = local.value
            when {
                s.availability == HealthConnectAvailability.UNAVAILABLE -> Unit
                s.availability == HealthConnectAvailability.UPDATE_REQUIRED -> setStep(HcStep.UPDATE_REQUIRED)
                p.healthConnectConsentAt == null -> setStep(HcStep.RATIONALE)
                s.permissionGranted -> runImport()
                else -> askPermission(p.healthConnectPermissionCancels)
            }
        }
    }

    /** Gerekçe ekranında "Onaylıyorum, devam et": rıza zamanı saklanır, sonra izin istenir. */
    fun acceptRationale() {
        viewModelScope.launch {
            setStep(null)
            repository.giveConsent()
            refresh()
            if (local.value.permissionGranted) runImport() else askPermission(prefs.current().healthConnectPermissionCancels)
        }
    }

    private fun askPermission(cancels: Int) =
        setStep(if (cancels >= MAX_PERMISSION_CANCELS) HcStep.CANCELLED_TWICE else HcStep.REQUEST_PERMISSION)

    /** İzin penceresi açıldı; adım temizlenir ki yeniden açılmasın. */
    fun permissionRequestLaunched() = setStep(null)

    /** Health Connect izin penceresi açılamadı (sağlayıcı yok ya da yanıt vermedi). */
    fun permissionRequestFailed() = local.update { it.copy(step = null, message = HcMessage.Failed) }

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            repository.recordPermissionResult(granted)
            refresh()
            if (granted) runImport() else local.update { it.copy(message = HcMessage.PermissionDenied) }
        }
    }

    fun dismissStep() = setStep(null)

    fun importNow() {
        viewModelScope.launch { runImport() }
    }

    private suspend fun runImport() {
        local.update { it.copy(importing = true, message = null) }
        val message = when (val result = repository.importNow()) {
            is SleepImportResult.Added -> if (result.count > 0) HcMessage.Added(result.count) else HcMessage.NothingNew
            SleepImportResult.NoPermission -> HcMessage.PermissionRevoked
            SleepImportResult.Unavailable, SleepImportResult.Failed -> HcMessage.Failed
        }
        local.update { it.copy(importing = false, message = message) }
        refresh()
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
            refresh()
            local.update { it.copy(message = HcMessage.Disconnected) }
        }
    }

    fun deleteImported() {
        viewModelScope.launch {
            val count = repository.deleteImported()
            local.update { it.copy(message = HcMessage.Deleted(count)) }
        }
    }

    fun clearMessage() = local.update { it.copy(message = null) }

    private fun setStep(step: HcStep?) = local.update { it.copy(step = step) }

    companion object {
        /** Health Connect, izin penceresi art arda iki kez kapatılınca onu bir daha göstermez. */
        const val MAX_PERMISSION_CANCELS = 2
    }
}
