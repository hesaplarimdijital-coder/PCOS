package com.ayla.pcos.ui.me

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.BuildConfig
import com.ayla.pcos.R
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.UyariBandi
import java.time.format.DateTimeFormatter

/** Gizlilik ve güvenlik: koruma, KVKK rızaları, bildirimler, görünüm, veri. */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmWipe by remember { mutableStateOf(false) }
    val exportDone = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(if (event is SettingsEvent.ExportDone) exportDone else exportFailed)
        }
    }

    val p = prefs ?: return
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            }
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.settings_privacy_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Grup(stringResource(R.string.settings_section_protection)) {
                AyarSatiri(R.string.settings_app_lock, R.string.settings_app_lock_text, p.appLockEnabled) { v -> viewModel.update { it.copy(appLockEnabled = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_block_screenshots, R.string.settings_block_screenshots_text, p.blockScreenshots) { v -> viewModel.update { it.copy(blockScreenshots = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_hidden_notifications, R.string.settings_hidden_notifications_text, p.hideNotificationContent) { v -> viewModel.update { it.copy(hideNotificationContent = v) } }
            }

            Grup(stringResource(R.string.settings_section_consent)) {
                AyarSatiri(R.string.consent_personalization_title, R.string.consent_personalization_text, p.consentPersonalization) { v -> viewModel.update { it.copy(consentPersonalization = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.consent_analytics_title, R.string.consent_analytics_text, p.consentAnalytics) { v -> viewModel.update { it.copy(consentAnalytics = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.consent_research_title, R.string.consent_research_text, p.consentResearch) { v -> viewModel.update { it.copy(consentResearch = v) } }
            }

            Grup(stringResource(R.string.settings_section_notifications)) {
                if (!viewModel.exactAlarmsAllowed()) {
                    UyariBandi(tur = BantTuru.BILGI, title = stringResource(R.string.onboarding_exact_alarm_title), text = stringResource(R.string.settings_exact_alarm_missing))
                    Spacer(Modifier.height(8.dp))
                }
                AyarSatiri(R.string.settings_water_reminders, null, p.waterRemindersEnabled) { v -> viewModel.update { it.copy(waterRemindersEnabled = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_cycle_notifications, null, p.cycleNotificationsEnabled) { v -> viewModel.update { it.copy(cycleNotificationsEnabled = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_progress_notifications, null, p.progressNotificationsEnabled) { v -> viewModel.update { it.copy(progressNotificationsEnabled = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.evening_routine_title, null, p.eveningRoutineEnabled) { v -> viewModel.update { it.copy(eveningRoutineEnabled = v) } }
                HorizontalDivider()
                val fmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
                AyarSatiriMetin(
                    R.string.settings_quiet_hours,
                    stringResource(R.string.settings_quiet_hours_text, p.quietHoursStart.format(fmt), p.quietHoursEnd.format(fmt)),
                    p.quietHoursEnabled,
                ) { v -> viewModel.update { it.copy(quietHoursEnabled = v) } }
            }

            Grup(stringResource(R.string.settings_section_display)) {
                AyarSatiri(R.string.settings_hide_prediction, R.string.settings_hide_prediction_text, p.hidePrediction) { v -> viewModel.update { it.copy(hidePrediction = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_show_body, R.string.settings_show_body_text, p.showBodyMeasurements) { v -> viewModel.update { it.copy(showBodyMeasurements = v) } }
            }

            Grup(stringResource(R.string.settings_section_data)) {
                EylemSatiri(Icons.Rounded.Download, R.string.settings_export, R.string.settings_export_text) {
                    exportLauncher.launch(DataExporter.suggestedFileName())
                }
                HorizontalDivider()
                EylemSatiri(Icons.Rounded.Delete, R.string.settings_delete_all, null, destructive = true) { confirmWipe = true }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
        SnackbarHost(hostState = snackbar)
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    viewModel.wipeAll()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Grup(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Overline(title, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    AylaCard { content() }
}

@Composable
private fun AyarSatiri(titleRes: Int, textRes: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    AyarSatiriMetin(titleRes, textRes?.let { stringResource(it) }, checked, onChange)
}

@Composable
private fun AyarSatiriMetin(titleRes: Int, text: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EylemSatiri(icon: ImageVector, titleRes: Int, textRes: Int?, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (textRes != null) Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
