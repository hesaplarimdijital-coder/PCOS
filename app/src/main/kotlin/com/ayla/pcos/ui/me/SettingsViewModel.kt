package com.ayla.pcos.ui.me

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data object ExportDone : SettingsEvent
    data object ExportFailed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val exporter: DataExporter,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = prefs.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events

    fun exactAlarmsAllowed(): Boolean = scheduler.exactAlarmsAllowed()

    fun update(transform: (UserPreferences) -> UserPreferences) = viewModelScope.launch {
        prefs.update(transform)
        // Bildirimle ilgili bir ayar değişmiş olabilir; alarmlar ucuzca tazelenir.
        runCatching { scheduler.rescheduleAll() }
    }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        val result = exporter.writeTo(uri)
        _events.tryEmit(if (result.isSuccess) SettingsEvent.ExportDone else SettingsEvent.ExportFailed)
    }

    /** Tüm verileri ve tercihleri siler; uygulama karşılamaya döner. */
    fun wipeAll() = viewModelScope.launch {
        exporter.wipeAll()
        prefs.update { UserPreferences() }
        runCatching { scheduler.rescheduleAll() }
    }
}
