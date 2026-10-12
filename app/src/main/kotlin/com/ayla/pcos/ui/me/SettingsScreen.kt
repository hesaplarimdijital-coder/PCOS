package com.ayla.pcos.ui.me

import android.content.res.Resources
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.BuildConfig
import com.ayla.pcos.R
import com.ayla.pcos.data.health.openHealthConnectInPlayStore
import com.ayla.pcos.data.health.openHealthConnectSettings
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.export.ImportMode
import com.ayla.pcos.export.ImportSummary
import com.ayla.pcos.export.InvalidBackupException
import com.ayla.pcos.reminders.AlarmLogEntry
import com.ayla.pcos.ui.components.AdimSayaci
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.BatteryStatus
import com.ayla.pcos.ui.components.CipEtiketi
import com.ayla.pcos.ui.components.DegerSatiri
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.PilBandi
import com.ayla.pcos.ui.components.SaatSeciciDialog
import com.ayla.pcos.ui.components.SegmentEtiketi
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.components.batteryStatus
import com.ayla.pcos.ui.components.labelRes
import com.ayla.pcos.ui.components.oemBatteryNote
import com.ayla.pcos.ui.components.openAppBatterySettings
import com.ayla.pcos.ui.components.openExactAlarmSettings
import com.ayla.pcos.ui.components.openNotificationSettings
import com.ayla.pcos.ui.components.saatMetni
import com.ayla.pcos.ui.components.segmentSimgesi
import com.ayla.pcos.ui.components.shareText
import com.ayla.pcos.ui.health.HealthConnectAkisi
import com.ayla.pcos.ui.health.HealthConnectUiState
import com.ayla.pcos.ui.health.HealthConnectViewModel
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
fun SettingsScreen(
    onBack: () -> Unit,
    autoStartImport: Boolean = false,
    onOpenPrivacyPolicy: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    healthConnectViewModel: HealthConnectViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val healthConnect by healthConnectViewModel.uiState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    var exactAllowed by remember { mutableStateOf(viewModel.exactAlarmsAllowed()) }
    var notificationsEnabled by remember { mutableStateOf(viewModel.notificationsEnabled()) }
    var battery by remember { mutableStateOf(batteryStatus(context)) }
    val hasDoseReminders by viewModel.hasDoseReminders.collectAsStateWithLifecycle()

    // Sistem ayarlarından dönünce izinleri ve pil durumunu yeniden oku.
    LifecycleResumeEffect(Unit) {
        exactAllowed = viewModel.exactAlarmsAllowed()
        notificationsEnabled = viewModel.notificationsEnabled()
        battery = batteryStatus(context)
        onPauseOrDispose { }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }
    // Bazı dosya yöneticileri .json'u "octet-stream" ya da düz metin olarak etiketler.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readBackup(uri)
    }
    val launchImport = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }

    // Anahtar kaybı diyaloğundan "Yedekten geri yükle" ile gelindiyse dosya seçici bir kez açılır.
    var autoImportDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoStartImport) {
        if (autoStartImport && !autoImportDone) {
            autoImportDone = true
            launchImport()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbar.showSnackbar(settingsEventMessage(resources, event))
        }
    }
    // Health Connect sonucu ve yarım kalan adım bu ekrana aittir; başka ekranda açılmasın.
    DisposableEffect(Unit) {
        onDispose {
            healthConnectViewModel.clearMessage()
            healthConnectViewModel.dismissStep()
        }
    }
    HealthConnectAkisi(healthConnectViewModel, healthConnect.step)

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
        importState = importState,
        onImport = launchImport,
        onImportConfirm = viewModel::applyBackup,
        onImportCancel = viewModel::cancelImport,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onOpenExactAlarmSettings = { openExactAlarmSettings(context) },
        hasDoseReminders = hasDoseReminders,
        batteryStatus = battery,
        oemBatteryNote = oemBatteryNote(Build.MANUFACTURER),
        onOpenBatterySettings = { openAppBatterySettings(context) },
        alarmLogEntries = viewModel::alarmLogEntries,
        onClearAlarmLog = viewModel::clearAlarmLog,
        onShareAlarmLog = { entries ->
            // Cihaz ve izin satırları gecikmenin nedenini anlamaya yarar; ilaç adı günlükte zaten yok.
            val deviceLines = listOf(
                resources.getString(R.string.alarm_log_share_device, "${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.RELEASE),
                resources.getString(R.string.alarm_log_share_exact, resources.getString(if (exactAllowed) R.string.alarm_log_state_on else R.string.alarm_log_state_off)),
                resources.getString(R.string.alarm_log_share_battery, resources.getString(battery.labelRes())),
            )
            val subject = resources.getString(R.string.alarm_log_share_subject)
            shareText(context, subject, AlarmLogText.shareText(resources, entries, deviceLines, Instant.now(), ZoneId.systemDefault()), subject)
        },
        healthConnect = healthConnect,
        healthConnectActions = HealthConnectActions(
            onConnect = healthConnectViewModel::start,
            onImport = healthConnectViewModel::importNow,
            onOpenSettings = { openHealthConnectSettings(context) },
            onOpenPlayStore = { openHealthConnectInPlayStore(context) },
            onDisconnect = healthConnectViewModel::disconnect,
            onDeleteImported = healthConnectViewModel::deleteImported,
        ),
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
    importState: BackupImportState = BackupImportState.Idle,
    onImport: () -> Unit = {},
    onImportConfirm: (ImportMode) -> Unit = {},
    onImportCancel: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    hasDoseReminders: Boolean = false,
    batteryStatus: BatteryStatus = BatteryStatus.UNRESTRICTED,
    oemBatteryNote: Int? = null,
    onOpenBatterySettings: () -> Unit = {},
    alarmLogEntries: () -> List<AlarmLogEntry> = { emptyList() },
    onShareAlarmLog: (List<AlarmLogEntry>) -> Unit = {},
    onClearAlarmLog: () -> Unit = {},
    initialAlarmLog: List<AlarmLogEntry>? = null,
    healthConnect: HealthConnectUiState? = null,
    healthConnectActions: HealthConnectActions = HealthConnectActions(),
) {
    val p = prefs
    var alarmLog by remember { mutableStateOf(initialAlarmLog) }
    var confirmWipe by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
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
                    Text(
                        stringResource(
                            if (LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI) R.string.settings_water_interval_hours else R.string.settings_water_interval,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                // Doz hatırlatması varsa pil durumu her zaman görünür (#17); bildirimler kapalıyken anlamsız.
                if (hasDoseReminders && notificationsEnabled) {
                    if (batteryStatus == BatteryStatus.UNRESTRICTED) {
                        PilDurumuSatiri()
                        HorizontalDivider()
                    } else {
                        PilBandi(status = batteryStatus, oemNote = oemBatteryNote, onOpenSettings = onOpenBatterySettings)
                        Spacer(Modifier.height(8.dp))
                    }
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
                HorizontalDivider()
                EylemSatiri(Icons.Rounded.History, R.string.alarm_log_title, R.string.alarm_log_row_text) { alarmLog = alarmLogEntries() }
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
                // Temel işlemeye verilen açık rıza: tarihiyle gösterilir; geri alma yolu tam silmedir.
                val consentAt = p.healthDataConsentAt
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_health_consent_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (consentAt != null) {
                                stringResource(
                                    R.string.settings_health_consent_given,
                                    DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("tr")).format(consentAt.atZone(java.time.ZoneId.systemDefault())),
                                )
                            } else {
                                stringResource(R.string.settings_health_consent_missing)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
                AyarSatiri(R.string.consent_personalization_title, R.string.consent_personalization_text, p.consentPersonalization) { v -> onUpdate { it.copy(consentPersonalization = v) } }
                HorizontalDivider()
                EylemSatiri(Icons.Rounded.Policy, R.string.privacy_policy_link, null, onClick = onOpenPrivacyPolicy)
            }

            // Desteklenmeyen cihazda bölüm hiç görünmez (#11).
            healthConnect?.takeIf { it.settingsVisible }?.let { hc ->
                Grup(stringResource(R.string.hc_settings_title)) {
                    HealthConnectAyarIcerigi(state = hc, actions = healthConnectActions)
                }
            }

            Grup(stringResource(R.string.settings_section_display)) {
                AyarSatiri(R.string.settings_hide_prediction, R.string.settings_hide_prediction_text, p.hidePrediction) { v -> onUpdate { it.copy(hidePrediction = v) } }
                HorizontalDivider()
                AyarSatiri(R.string.settings_show_body, R.string.settings_show_body_text, p.showBodyMeasurements) { v -> onUpdate { it.copy(showBodyMeasurements = v) } }
            }

            Grup(stringResource(R.string.settings_section_data)) {
                EylemSatiri(Icons.Rounded.Download, R.string.settings_export, R.string.settings_export_text, onClick = onExport)
                HorizontalDivider()
                EylemSatiri(Icons.Rounded.SettingsBackupRestore, R.string.settings_import, R.string.settings_import_text, onClick = onImport)
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

    alarmLog?.let { entries ->
        HatirlatmaGunluguDialog(
            entries = entries,
            onShare = { onShareAlarmLog(entries) },
            onClear = {
                onClearAlarmLog()
                alarmLog = emptyList()
            },
            onDismiss = { alarmLog = null },
        )
    }

    when (importState) {
        is BackupImportState.Preview -> YedekOnizlemeDialog(
            summary = importState.summary,
            onMerge = { onImportConfirm(ImportMode.MERGE) },
            onReplace = { confirmReplace = true },
            onDismiss = onImportCancel,
        )
        BackupImportState.Applying -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.import_working)) },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
        BackupImportState.Idle -> Unit
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text(stringResource(R.string.import_replace_confirm_title)) },
            text = { Text(stringResource(R.string.import_replace_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    onImportConfirm(ImportMode.REPLACE)
                }) { Text(stringResource(R.string.import_replace_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text(stringResource(R.string.action_cancel)) } },
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

/** Ayarlar olayının kullanıcıya gösterilecek metni. */
internal fun settingsEventMessage(resources: Resources, event: SettingsEvent): String = when (event) {
    SettingsEvent.ExportDone -> resources.getString(R.string.export_done)
    SettingsEvent.ExportFailed -> resources.getString(R.string.export_failed)
    is SettingsEvent.ImportDone ->
        if (event.skipped > 0) {
            resources.getString(R.string.import_done_skipped, event.added, event.skipped)
        } else {
            resources.getString(R.string.import_done, event.added)
        }
    is SettingsEvent.ImportFailed -> resources.getString(
        when (event.reason) {
            InvalidBackupException.Reason.NOT_AYLA -> R.string.import_error_not_ayla
            InvalidBackupException.Reason.TOO_NEW -> R.string.import_error_too_new
            InvalidBackupException.Reason.CORRUPT -> R.string.import_error_corrupt
        },
    )
}

/**
 * Yedek önizlemesi: dosyanın tarihi ve içeriği, iki açık seçenek. "Üzerine yaz" ayrıca onay ister;
 * yedek boşsa yalnızca "Vazgeç" kalır.
 */
@Composable
private fun YedekOnizlemeDialog(summary: ImportSummary, onMerge: () -> Unit, onReplace: () -> Unit, onDismiss: () -> Unit) {
    val tr = remember { java.util.Locale.forLanguageTag("tr") }
    val dateText = summary.exportedAt?.let {
        DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", tr).format(it.atZone(java.time.ZoneId.systemDefault()))
    }
    val parts = listOfNotNull(
        summary.periods.takeIf { it > 0 }?.let { stringResource(R.string.import_part_periods, it) },
        summary.dailyLogs.takeIf { it > 0 }?.let { stringResource(R.string.import_part_logs, it) },
        summary.medications.takeIf { it > 0 }?.let { stringResource(R.string.import_part_meds, it) },
        summary.doses.takeIf { it > 0 }?.let { stringResource(R.string.import_part_doses, it) },
        summary.hydrationDays.takeIf { it > 0 }?.let { stringResource(R.string.import_part_water, it) },
        summary.sleepNights.takeIf { it > 0 }?.let { stringResource(R.string.import_part_sleep, it) },
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.SettingsBackupRestore, contentDescription = null) },
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (dateText != null) Text(stringResource(R.string.import_date, dateText), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (summary.isEmpty) stringResource(R.string.import_empty) else stringResource(R.string.import_contents, parts.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!summary.isEmpty) {
                    YedekSecenegi(R.string.import_merge, R.string.import_merge_text, destructive = false, onClick = onMerge)
                    YedekSecenegi(R.string.import_replace, R.string.import_replace_text, destructive = true, onClick = onReplace)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Diyalogdaki seçenek: başlık + ne olacağını anlatan cümle; tamamı tek dokunma hedefi (≥ 48 dp). */
@Composable
private fun YedekSecenegi(titleRes: Int, textRes: Int, destructive: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (destructive) scheme.error.copy(alpha = 0.5f) else scheme.outlineVariant),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, color = if (destructive) scheme.error else scheme.primary)
            Text(stringResource(textRes), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
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
                label = { CipEtiketi(stringResource(labelRes)) },
                leadingIcon = if (isSelected) ({ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
            )
        }
    }
}

@Composable
private fun AralikSecimi(selected: Int, onSelect: (Int) -> Unit) {
    val options = AyarSinirlari.waterIntervals
    // Büyük yazıda "1,5 sa" segmente sığmaz; birim başlığa taşınır ("Hatırlatma aralığı (saat)").
    val labels = if (LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI) {
        listOf(R.string.settings_interval_60_short, R.string.settings_interval_90_short, R.string.settings_interval_120_short, R.string.settings_interval_180_short)
    } else {
        listOf(R.string.settings_interval_60, R.string.settings_interval_90, R.string.settings_interval_120, R.string.settings_interval_180)
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        options.forEachIndexed { index, minutes ->
            SegmentedButton(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.fillMaxHeight(),
                icon = segmentSimgesi(selected == minutes),
                label = { SegmentEtiketi(stringResource(labels[index]), selected == minutes) },
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

/** Pil tasarrufu Ayla'yı kısıtlamıyorsa: simge + kısa durum (renk tek başına anlam taşımaz). */
@Composable
private fun PilDurumuSatiri() {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        Icon(Icons.Rounded.BatteryFull, contentDescription = null, tint = LocalAylaColors.current.basari)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.battery_unrestricted), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.battery_unrestricted_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
internal fun EylemSatiri(icon: ImageVector, titleRes: Int, textRes: Int?, destructive: Boolean = false, onClick: () -> Unit) {
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
