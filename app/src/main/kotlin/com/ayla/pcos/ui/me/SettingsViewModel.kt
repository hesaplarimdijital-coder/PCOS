package com.ayla.pcos.ui.me

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.export.DataImporter
import com.ayla.pcos.export.ExportBundle
import com.ayla.pcos.export.ImportMode
import com.ayla.pcos.export.ImportSummary
import com.ayla.pcos.export.InvalidBackupException
import com.ayla.pcos.reminders.ReminderNotifier
import com.ayla.pcos.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data object ExportDone : SettingsEvent
    data object ExportFailed : SettingsEvent
    data class ImportDone(val added: Int, val skipped: Int) : SettingsEvent
    data class ImportFailed(val reason: InvalidBackupException.Reason) : SettingsEvent
}

/** Yedekten geri yükleme akışı: dosya seçildi → önizleme → kip seçimi → yazılıyor. */
sealed interface BackupImportState {
    data object Idle : BackupImportState
    data class Preview(val summary: ImportSummary) : BackupImportState
    data object Applying : BackupImportState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val exporter: DataExporter,
    private val importer: DataImporter,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events

    private val _importState = MutableStateFlow<BackupImportState>(BackupImportState.Idle)
    val importState: StateFlow<BackupImportState> = _importState

    /** Önizlemesi gösterilen yedek; kullanıcı kip seçene ya da vazgeçene kadar bellekte tutulur. */
    private var pendingBackup: ExportBundle? = null

    fun exactAlarmsAllowed(): Boolean = scheduler.exactAlarmsAllowed()

    fun notificationsEnabled(): Boolean = notifier.notificationsEnabled()

    fun update(transform: (UserPreferences) -> UserPreferences) = viewModelScope.launch {
        prefs.update(transform)
        // Bildirimle ilgili bir ayar değişmiş olabilir; alarmlar ucuzca tazelenir.
        runCatching { scheduler.rescheduleAll() }
    }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        val result = exporter.writeTo(uri)
        _events.tryEmit(if (result.isSuccess) SettingsEvent.ExportDone else SettingsEvent.ExportFailed)
    }

    /** Seçilen dosyayı okur ve doğrular; geçerliyse önizleme gösterilir, hiçbir şey yazılmaz. */
    fun readBackup(uri: Uri) = viewModelScope.launch {
        try {
            val bundle = importer.read(uri)
            pendingBackup = bundle
            _importState.value = BackupImportState.Preview(importer.summarize(bundle))
        } catch (e: InvalidBackupException) {
            _events.tryEmit(SettingsEvent.ImportFailed(e.reason))
        }
    }

    fun applyBackup(mode: ImportMode) = viewModelScope.launch {
        val bundle = pendingBackup ?: return@launch
        _importState.value = BackupImportState.Applying
        val event = try {
            val result = importer.apply(bundle, mode)
            SettingsEvent.ImportDone(result.added, result.skipped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: InvalidBackupException) {
            SettingsEvent.ImportFailed(e.reason)
        } catch (e: Exception) {
            SettingsEvent.ImportFailed(InvalidBackupException.Reason.CORRUPT)
        }
        pendingBackup = null
        _importState.value = BackupImportState.Idle
        _events.tryEmit(event)
    }

    fun cancelImport() {
        pendingBackup = null
        _importState.value = BackupImportState.Idle
    }

    /** Tüm verileri ve tercihleri siler; uygulama karşılamaya döner. */
    fun wipeAll() = viewModelScope.launch {
        exporter.wipeAll()
        prefs.update { UserPreferences() }
        runCatching { scheduler.rescheduleAll() }
    }
}
