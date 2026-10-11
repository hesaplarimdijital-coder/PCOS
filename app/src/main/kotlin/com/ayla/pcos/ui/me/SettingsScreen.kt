package com.ayla.pcos.ui.me

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.BuildConfig
import com.ayla.pcos.R
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.ui.components.AdimSayaci
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.DegerSatiri
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SaatSeciciDialog
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.components.openExactAlarmSettings
import com.ayla.pcos.ui.components.openNotificationSettings
import com.ayla.pcos.ui.components.saatMetni
import java.time.LocalTime

/** Ayar sınırları; arayüz ve testler aynı değerleri kullanır. */
object AyarSinirlari {
    val waterGoalRange = 4..16
    val cupMlRange = 150..500
    const val CUP_ML_STEP = 50
    val waterIntervals = listOf(60, 90, 120, 180)
}

/** Düzenlenebilir saat alanları; tek bir diyalog hangisinin seçildiğini bilir. */
enum class SaatAlani { ACTIVE_START, ACTIVE_END, QUIET_START, QUIET_END, DAILY_CHECK, EVENING }

/** Ayarlar: profil, su, bildirimler, koruma, KVKK rızaları, görünüm, veri. */
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val exportDone = stringResource(R.string.export_done)
    val exportFailed = stringResource(R.string.export_failed)
    var exactAllowed by remember { mutableStateOf(viewModel.exactAlarmsAllowed()) }
    var notificationsEnabled by remember { mutableStateOf(viewModel.notificationsEnabled()) }

    // Sistem ayarlarından dönünce izinleri yeniden oku.
    LifecycleResumeEffect(Unit) {
        exactAllowed = viewModel.exactAlarmsAllowed()
        notificationsEnabled = viewModel.notificationsEnabled()
        onPauseOrDispose { }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(if (event is SettingsEvent.ExportDone) exportDone else exportFailed)
        }
    }

    val p = prefs ?: return
    SettingsContent(
        prefs = p,
        exactAlarmsAllowed = exactAllowed,
        notificationsEnabled = notificationsEnabled,
        snackbar = snackbar,
        onBack = onBack,
        onUpdate = { transform -> viewModel.update(transform) },
        onExport = { exportLauncher.launch(DataExporter.suggestedFileName()) },
        onWipe = { viewModel.wipeAll() },
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onOpenExactAlarmSettings = { openExactAlarmSettings(context) },
    )
}

/** Durumsuz ayarlar içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun SettingsContent(
    prefs: UserPreferences,
    exactAlarmsAllowed: Boolean,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onUpdate: ((UserPreferences) -> UserPreferences) -> Unit,
    onExport: () -> Unit,
    onWipe: () -> Unit,
    notificationsEnabled: Boolean = true,
    onOpenNotificationSettings: () -> Unit = {},
    onOpenExactAlarmSettings: () -> Unit = {},
) {
    val p = prefs
    var confirmWipe by remember { mutableStateOf(false) }
    var editingName by rememberSaveable { mutableStateOf(false) }
    var editingTime by rememberSaveable { mutableStateOf<SaatAlani?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            }

            Grup(stringResource(R.string.settings_section_profile)) {
                DegerSatiri(
                    title = stringResource(R.string.settings_name),
                    value = p.displayName.ifBlank { stringResource(R.string.settings_name_empty) },
                    onClick = { editingName = true },
                )
                HorizontalDivider()
                Text(stringResource(R.string.settings_goals), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.height(8.dp))
                HedefSecimi(selected = p.goals) { goal ->
                    onUpdate { it.copy(goals = if (goal in it.goals) it.goals - goal else it.goals + goal) }
                }
                Spacer(Modifier.height(4.dp))
            }

            Grup(stringResource(R.string.settings_section_water)) {
                AdimSayaci(
                    title = stringResource(R.string.settings_water_goal),
                    valueText = stringResource(R.string.settings_water_goal_value, p.waterGoalCups),
                    canDecrease = p.waterGoalCups > AyarSinirlari.waterGoalRange.first,
                    canIncrease = p.waterGoalCups < AyarSinirlari.waterGoalRange.last,
                    onDecrease = { onUpdate { it.copy(waterGoalCups = (it.waterGoalCups - 1).coerceIn(AyarSinirlari.waterGoalRange)) } },
                    onIncrease = { onUpdate { it.copy(waterGoalCups = (it.waterGoalCups + 1).coerceIn(AyarSinirlari.waterGoalRange)) } },
                    text = stringResource(R.string.settings_water_goal_text, litre(p.waterGoalCups * p.cupMl)),
                )
                HorizontalDivider()
                AdimSayaci(
                    title = stringResource(R.string.settings_cup_size),
                    valueText = stringResource(R.string.settings_cup_size_value, p.cupMl),
                    canDecrease = p.cupMl > AyarSinirlari.cupMlRange.first,
                    canIncrease = p.cupMl < AyarSinirlari.cupMlRange.last,
                    onDecrease = { onUpdate { it.copy(cupMl = (it.cupMl - AyarSinirlari.CUP_ML_STEP).coerceIn(AyarSinirlari.cupMlRange)) } },
                    onIncrease = { onUpdate { it.copy(cupMl = (it.cupMl + AyarSinirlari.CUP_ML_STEP).coerceIn(AyarSinirlari.cupMlRange)) } },
                )
                HorizontalDivider()
                AyarSatiri(R.string.settings_water_reminders, null, p.waterRemindersEnabled) { v -> onUpdate { it.copy(waterRemindersEnabled = v) } }
                if (p.waterRemindersEnabled) {
                    Text(stringResource(R.string.settings_water_interval), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    AralikSecimi(selected = p.waterIntervalMinutes) { minutes -> onUpdate { it.copy(waterIntervalMinutes = minutes) } }
                    Spacer(Modifier.height(4.dp))
                    DegerSatiri(
                        title = stringResource(R.string.settings_active_start),
                        value = p.activeHoursStart.saatMetni(),
                        onClick = { editingTime = SaatAlani.ACTIVE_START },
                    )
                    DegerSatiri(
                        title = stringResource(R.string.settings_active_end),
                        value = p.activeHoursEnd.saatMetni(),
                        onClick = { editingTime = SaatAlani.ACTIVE_END },
                    )
                }
            }

            Grup(stringResource(R.string.settings_section_notifications)) {
                if (!notificationsEnabled) {
                    UyariBandi(
                        tur = BantTuru.DIKKAT,
                        title = stringResource(R.string.today_notifications_off_title),
                        text = stringResource(R.string.today_notifications_off_text),
                        actionLabel = stringResource(R.string.action_open_settings),
                        onAction = onOpenNotificationSettings,
                    )
                    Spacer(Modifier.height(8.dp))
                } else if (!exactAlarmsAllowed) {
                    UyariBandi(
                        tur = BantTuru.BILGI,
                        title = stringResource(R.string.onboarding_exact_alarm_title),
                        text = stringResource(R.string.settings_exact_alarm_missing),
                        actionLabel = stringResource(R.string.onboarding_allow_exact_alarm),
                        onAction = onOpenExactAlarmSettings,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                AyarSatiri(R.string.settings_cycle_notifications, R.string.settings_cycle_notifications_text, p.cycleNotificationsEnabled) { v -> onUpdate { it.copy(cycleNotificationsEnabled = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_progress_notifications, R.string.settings_progress_notifications_text, p.progressNotificationsEnabled) { v -> onUpdate { it.copy(progressNotificationsEnabled = v) } }
                HorizontalDivider()
                DegerSatiri(
                    title = stringResource(R.string.settings_daily_check),
                    text = stringResource(R.string.settings_daily_check_text),
                    value = p.dailyCheckTime.saatMetni(),
                    onClick = { editingTime = SaatAlani.DAILY_CHECK },
                )
                HorizontalDivider()
                AyarSatiri(R.string.evening_routine_title, R.string.settings_evening_text, p.eveningRoutineEnabled) { v -> onUpdate { it.copy(eveningRoutineEnabled = v) } }
                if (p.eveningRoutineEnabled) {
                    DegerSatiri(
                        title = stringResource(R.string.settings_evening_time),
                        value = p.eveningRoutineTime.saatMetni(),
                        onClick = { editingTime = SaatAlani.EVENING },
                    )
                }
                HorizontalDivider()
                AyarSatiri(R.string.settings_quiet_hours, R.string.settings_quiet_hours_short, p.quietHoursEnabled) { v -> onUpdate { it.copy(quietHoursEnabled = v) } }
                if (p.quietHoursEnabled) {
                    DegerSatiri(
                        title = stringResource(R.string.settings_quiet_start),
                        value = p.quietHoursStart.saatMetni(),
                        onClick = { editingTime = SaatAlani.QUIET_START },
                    )
                    DegerSatiri(
                        title = stringResource(R.string.settings_quiet_end),
                        value = p.quietHoursEnd.saatMetni(),
                        onClick = { editingTime = SaatAlani.QUIET_END },
                    )
                }
            }

            Grup(stringResource(R.string.settings_section_protection)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.settings_privacy_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                AyarSatiri(R.string.settings_app_lock, R.string.settings_app_lock_text, p.appLockEnabled) { v -> onUpdate { it.copy(appLockEnabled = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_block_screenshots, R.string.settings_block_screenshots_text, p.blockScreenshots) { v -> onUpdate { it.copy(blockScreenshots = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_hidden_notifications, R.string.settings_hidden_notifications_text, p.hideNotificationContent) { v -> onUpdate { it.copy(hideNotificationContent = v) } }
            }

            Grup(stringResource(R.string.settings_section_consent)) {
                AyarSatiri(R.string.consent_personalization_title, R.string.consent_personalization_text, p.consentPersonalization) { v -> onUpdate { it.copy(consentPersonalization = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.consent_analytics_title, R.string.consent_analytics_text, p.consentAnalytics) { v -> onUpdate { it.copy(consentAnalytics = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.consent_research_title, R.string.consent_research_text, p.consentResearch) { v -> onUpdate { it.copy(consentResearch = v) } }
            }

            Grup(stringResource(R.string.settings_section_display)) {
                AyarSatiri(R.string.settings_hide_prediction, R.string.settings_hide_prediction_text, p.hidePrediction) { v -> onUpdate { it.copy(hidePrediction = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_show_body, R.string.settings_show_body_text, p.showBodyMeasurements) { v -> onUpdate { it.copy(showBodyMeasurements = v) } }
            }

            Grup(stringResource(R.string.settings_section_data)) {
                EylemSatiri(Icons.Rounded.Download, R.string.settings_export, R.string.settings_export_text, onClick = onExport)
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

    if (editingName) {
        AdDialog(
            initial = p.displayName,
            onDismiss = { editingName = false },
            onSave = { name ->
                editingName = false
                onUpdate { it.copy(displayName = name.trim().take(40)) }
            },
        )
    }

    editingTime?.let { field ->
        val (titleRes, initial) = when (field) {
            SaatAlani.ACTIVE_START -> R.string.settings_active_start to p.activeHoursStart
            SaatAlani.ACTIVE_END -> R.string.settings_active_end to p.activeHoursEnd
            SaatAlani.QUIET_START -> R.string.settings_quiet_start to p.quietHoursStart
            SaatAlani.QUIET_END -> R.string.settings_quiet_end to p.quietHoursEnd
            SaatAlani.DAILY_CHECK -> R.string.settings_daily_check to p.dailyCheckTime
            SaatAlani.EVENING -> R.string.settings_evening_time to p.eveningRoutineTime
        }
        SaatSeciciDialog(
            title = stringResource(titleRes),
            initial = initial,
            onDismiss = { editingTime = null },
            onConfirm = { time ->
                editingTime = null
                onUpdate { it.withTime(field, time) }
            },
        )
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    onWipe()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Seçilen saat alanını günceller; saf fonksiyon (birim testli). */
internal fun UserPreferences.withTime(field: SaatAlani, time: LocalTime): UserPreferences = when (field) {
    SaatAlani.ACTIVE_START -> copy(activeHoursStart = time)
    SaatAlani.ACTIVE_END -> copy(activeHoursEnd = time)
    SaatAlani.QUIET_START -> copy(quietHoursStart = time)
    SaatAlani.QUIET_END -> copy(quietHoursEnd = time)
    SaatAlani.DAILY_CHECK -> copy(dailyCheckTime = time)
    SaatAlani.EVENING -> copy(eveningRoutineTime = time)
}

/** 2000 ml → "2", 2250 ml → "2,25" (Türkçe ondalık virgül). */
internal fun litre(ml: Int): String {
    val whole = ml / 1000
    val rest = ml % 1000
    if (rest == 0) return whole.toString()
    val decimals = rest.toString().padStart(3, '0').trimEnd('0')
    return "$whole,$decimals"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HedefSecimi(selected: Set<Goal>, onToggle: (Goal) -> Unit) {
    val items = listOf(
        Goal.CYCLE to R.string.goal_cycle_title,
        Goal.SYMPTOMS to R.string.goal_symptoms_title,
        Goal.TTC to R.string.goal_ttc_title,
        Goal.PILL to R.string.goal_pill_title,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (goal, labelRes) ->
            val isSelected = goal in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(goal) },
                label = { Text(stringResource(labelRes)) },
                leadingIcon = if (isSelected) ({ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
            )
        }
    }
}

@Composable
private fun AralikSecimi(selected: Int, onSelect: (Int) -> Unit) {
    val options = AyarSinirlari.waterIntervals
    val labels = listOf(R.string.settings_interval_60, R.string.settings_interval_90, R.string.settings_interval_120, R.string.settings_interval_180)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, minutes ->
            SegmentedButton(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(stringResource(labels[index]), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun AdDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.onboarding_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun Grup(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Overline(title, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    AylaCard { content() }
}

@Composable
private fun AyarSatiri(titleRes: Int, textRes: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            if (textRes != null) Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Satırın tamamı anahtarı değiştirir; anahtarın kendi dokunuşu çift tetiklemesin diye null.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun EylemSatiri(icon: ImageVector, titleRes: Int, textRes: Int?, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
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
