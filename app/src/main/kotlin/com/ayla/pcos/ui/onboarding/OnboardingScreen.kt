package com.ayla.pcos.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.ui.components.SimgeZemini
import com.ayla.pcos.ui.components.openExactAlarmSettings
import com.ayla.pcos.ui.components.openNotificationSettings
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Karşılama: 1) hoş geldin + ad, 2) öncelikler, 3) son adet, 4) rıza ve izinler.
 */
/** Karşılamanın son adımında gösterilen izin durumu. */
data class OnboardingPermissions(
    val notificationsGranted: Boolean = false,
    /** Kullanıcı sistem diyaloğunda reddetti; artık ayarlar sayfası açılır (Android ikinci reddi sormaz). */
    val notificationsDenied: Boolean = false,
    /** Tam zamanlı alarm izni bu sürümde var mı (Android 12+). */
    val exactAlarmApplicable: Boolean = true,
    val exactAlarmGranted: Boolean = false,
)

private fun readPermissions(context: Context, denied: Boolean): OnboardingPermissions {
    val alarmManager: AlarmManager? = context.getSystemService<AlarmManager>()
    val exactGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager?.canScheduleExactAlarms() == true else true
    return OnboardingPermissions(
        notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        notificationsDenied = denied,
        exactAlarmApplicable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        exactAlarmGranted = exactGranted,
    )
}

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    var permissions by remember { mutableStateOf(readPermissions(context, denied)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        permissions = readPermissions(context, denied)
    }
    // Ayarlardan (bildirim ya da alarm izni) dönünce durumu tazele.
    LifecycleResumeEffect(denied) {
        permissions = readPermissions(context, denied)
        onPauseOrDispose { }
    }

    BackHandler(enabled = state.step > 0) { viewModel.back() }
    OnboardingContent(
        state = state,
        onBack = { viewModel.back() },
        onNext = { viewModel.next() },
        onFinish = { viewModel.finish() },
        onName = { viewModel.setName(it) },
        onToggleGoal = { viewModel.toggleGoal(it) },
        onLastPeriod = { viewModel.setLastPeriod(it) },
        onConsent = { p, a, r -> viewModel.setConsent(p, a, r) },
        permissions = permissions,
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !denied) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
        onOpenExactAlarmSettings = { openExactAlarmSettings(context) },
    )
}

/** Durumsuz karşılama içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun OnboardingContent(
    state: OnboardingState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onName: (String) -> Unit,
    onToggleGoal: (Goal) -> Unit,
    onLastPeriod: (LocalDate?) -> Unit,
    onConsent: (Boolean?, Boolean?, Boolean?) -> Unit,
    permissions: OnboardingPermissions = OnboardingPermissions(),
    onRequestNotifications: () -> Unit = {},
    onOpenExactAlarmSettings: () -> Unit = {},
) {

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (state.step > 0) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            StepDots(current = state.step, total = OnboardingState.STEP_COUNT)
            Spacer(Modifier.size(48.dp))
        }

        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally(AylaMotion.enter()) { if (forward) it / 5 else -it / 5 } + fadeIn()
                val exit = slideOutHorizontally { if (forward) -it / 5 else it / 5 } + fadeOut()
                enter togetherWith exit
            },
            label = "onboardingStep",
        ) { step ->
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(16.dp))
                when (step) {
                    0 -> WelcomeStep(state.name, onName)
                    1 -> GoalsStep(state.goals, onToggleGoal)
                    2 -> LastPeriodStep(state.lastPeriod, onLastPeriod)
                    else -> ConsentStep(state, onConsent, permissions, onRequestNotifications, onOpenExactAlarmSettings)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Button(
            onClick = { if (state.step == OnboardingState.STEP_COUNT - 1) onFinish() else onNext() },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) {
            Text(if (state.step == OnboardingState.STEP_COUNT - 1) stringResource(R.string.onboarding_start) else stringResource(R.string.action_continue))
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val active = index == current
            val done = index < current
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .background(
                        if (active || done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun WelcomeStep(name: String, onName: (String) -> Unit) {
    Text(stringResource(R.string.onboarding_welcome_title), style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onboarding_welcome_text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.onboarding_name_label), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.onboarding_name_hint)) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    )
    Spacer(Modifier.height(24.dp))
    PrivacyNote()
}

@Composable
private fun PrivacyNote() {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.onboarding_privacy_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalsStep(goals: Set<Goal>, onToggle: (Goal) -> Unit) {
    Text(stringResource(R.string.onboarding_goals_title), style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onboarding_goals_text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalRow(Goal.CYCLE, Icons.Rounded.CalendarMonth, R.string.goal_cycle_title, R.string.goal_cycle_text, goals, onToggle)
        GoalRow(Goal.SYMPTOMS, Icons.Rounded.Spa, R.string.goal_symptoms_title, R.string.goal_symptoms_text, goals, onToggle)
        GoalRow(Goal.TTC, Icons.Rounded.Favorite, R.string.goal_ttc_title, R.string.goal_ttc_text, goals, onToggle)
        GoalRow(Goal.PILL, Icons.Rounded.Medication, R.string.goal_pill_title, R.string.goal_pill_text, goals, onToggle)
    }
}

@Composable
private fun GoalRow(goal: Goal, icon: ImageVector, titleRes: Int, textRes: Int, goals: Set<Goal>, onToggle: (Goal) -> Unit) {
    val selected = goal in goals
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onToggle(goal) },
        shape = if (selected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        color = if (selected) scheme.primaryContainer else colors.kart,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimgeZemini(icon = icon, background = if (selected) colors.kart else scheme.primaryContainer, tint = scheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .then(if (selected) Modifier.background(scheme.primary, CircleShape) else Modifier.border(2.dp, scheme.outline, CircleShape)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LastPeriodStep(lastPeriod: LocalDate?, onPick: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr")) }

    Text(stringResource(R.string.onboarding_period_title), style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onboarding_period_text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(lastPeriod?.format(formatter) ?: stringResource(R.string.onboarding_period_pick))
    }
    if (lastPeriod != null) {
        TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.onboarding_period_unknown)) }
    }

    if (showPicker) {
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = lastPeriod?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: todayMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = pickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    if (picked != null && !picked.isAfter(LocalDate.now())) onPick(picked)
                    showPicker = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ConsentStep(
    state: OnboardingState,
    onConsent: (Boolean?, Boolean?, Boolean?) -> Unit,
    permissions: OnboardingPermissions,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Text(stringResource(R.string.onboarding_consent_title), style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onboarding_consent_text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
    ConsentRow(R.string.consent_personalization_title, R.string.consent_personalization_text, state.consentPersonalization) { onConsent(it, null, null) }
    ConsentRow(R.string.consent_analytics_title, R.string.consent_analytics_text, state.consentAnalytics) { onConsent(null, it, null) }
    ConsentRow(R.string.consent_research_title, R.string.consent_research_text, state.consentResearch) { onConsent(null, null, it) }

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.onboarding_notifications_title), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.onboarding_notifications_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    if (permissions.notificationsGranted) {
        IzinVerildi(stringResource(R.string.onboarding_notifications_granted))
    } else {
        OutlinedButton(onClick = onRequestNotifications) {
            Text(
                stringResource(
                    if (permissions.notificationsDenied) R.string.onboarding_open_notification_settings else R.string.onboarding_allow_notifications,
                ),
            )
        }
        if (permissions.notificationsDenied) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onboarding_notifications_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (permissions.exactAlarmApplicable) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.onboarding_exact_alarm_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.onboarding_exact_alarm_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        if (permissions.exactAlarmGranted) {
            IzinVerildi(stringResource(R.string.onboarding_exact_alarm_granted))
        } else {
            OutlinedButton(onClick = onOpenExactAlarmSettings) { Text(stringResource(R.string.onboarding_allow_exact_alarm)) }
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.onboarding_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Verilmiş izin satırı: onay ikonu ve metin (renk tek başına anlam taşımaz). */
@Composable
private fun IzinVerildi(text: String) {
    val colors = LocalAylaColors.current
    Surface(shape = CircleShape, color = colors.basariAcik) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.basari, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = colors.basari)
        }
    }
}

@Composable
private fun ConsentRow(titleRes: Int, textRes: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
