package com.ayla.pcos.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.cycle.Confidence
import com.ayla.cycle.CycleOverview
import com.ayla.cycle.LongCycleAlert
import com.ayla.cycle.Prediction
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.TarihSeciciDialog
import com.ayla.pcos.ui.theme.AylaExpressiveShapes
import com.ayla.pcos.R
import com.ayla.pcos.data.repo.DoseItem
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.DozSatiri
import com.ayla.pcos.ui.components.HaleHalkasi
import com.ayla.pcos.ui.components.HaleMerkezi
import com.ayla.pcos.ui.components.HapPaketi
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SectionHeader
import com.ayla.pcos.ui.components.SuSatiri
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val trLocale: Locale = Locale.forLanguageTag("tr")
private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", trLocale)
private val dayMonthShort: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", trLocale)

@Composable
fun TodayScreen(
    onOpenLog: (LocalDate) -> Unit,
    onOpenMeds: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Box(Modifier.fillMaxSize())
        return
    }
    TodayContent(
        state = state,
        onOpenLog = { onOpenLog(state.today) },
        onOpenMeds = onOpenMeds,
        onOpenSettings = onOpenSettings,
        onAddWater = viewModel::addWater,
        onTaken = viewModel::markTaken,
        onUndo = viewModel::undoTaken,
        onPeriodStart = viewModel::startPeriodToday,
        onPeriodEnd = viewModel::endPeriodToday,
        onDismissNotificationBand = { viewModel.dismissNotificationBand() },
        onPeriodStartAt = { viewModel.startPeriodAt(it) },
    )
}

/** Durumsuz içerik; ekran görüntüsü testleri ve önizlemeler doğrudan bunu çizer. */
@Composable
internal fun TodayContent(
    state: TodayUiState,
    onOpenLog: () -> Unit,
    onOpenMeds: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWater: () -> Unit,
    onTaken: (DoseItem) -> Unit,
    onUndo: (DoseItem) -> Unit,
    onPeriodStart: () -> Unit,
    onPeriodEnd: () -> Unit,
    onDismissNotificationBand: () -> Unit = {},
    onPeriodStartAt: (LocalDate) -> Unit = {},
    hour: Int = LocalTime.now().hour,
) {
    val overview = state.overview
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    val dateLine = state.today.dayOfWeek.getDisplayName(TextStyle.FULL, trLocale) + " · " + state.today.format(dayMonth)
                    Overline(dateLine)
                    Text(greeting(state.displayName, hour), style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = stringResource(R.string.me_settings))
                }
            }
        }

        val noPeriodYet = state.pillMode == null && overview?.lastPeriodStart == null
        item {
            when {
                state.pillMode != null -> PillHero(state.pillMode)
                noPeriodYet -> IlkAdetKarti(today = state.today, onPick = onPeriodStartAt)
                else -> CycleHero(overview, hidePrediction = state.hidePrediction, today = state.today)
            }
        }

        item {
            // Hap modunda doğal adet kaydı anlamsızdır (plasebo kanaması kanama olarak günlükte işaretlenir);
            // ilk adet kartı da kendi düğmesini taşır.
            val showPeriodButton = state.pillMode == null && !noPeriodYet
            val stacked = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI
            val logButton: @Composable (Modifier) -> Unit = { m ->
                Button(onClick = onOpenLog, modifier = m) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.today_daily_log))
                }
            }
            val periodButton: @Composable (Modifier) -> Unit = { m ->
                val ended = state.openPeriod != null
                FilledTonalButton(onClick = if (ended) onPeriodEnd else onPeriodStart, modifier = m) {
                    Icon(Icons.Rounded.LocalFlorist, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(if (ended) R.string.today_period_ended else R.string.today_period_started))
                }
            }
            if (stacked || !showPeriodButton) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    logButton(Modifier.fillMaxWidth())
                    if (showPeriodButton) periodButton(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    logButton(Modifier.weight(1f))
                    periodButton(Modifier.weight(1f))
                }
            }
        }

        val alert = overview?.flags?.longCycle ?: LongCycleAlert.NONE
        if (state.pillMode == null && alert != LongCycleAlert.NONE) {
            item {
                val day = overview?.currentCycleDay ?: 0
                UyariBandi(
                    tur = BantTuru.DIKKAT,
                    title = if (alert == LongCycleAlert.SEE_DOCTOR) stringResource(R.string.alert_cycle_90_title, day) else stringResource(R.string.alert_cycle_60_title, day),
                    text = if (alert == LongCycleAlert.SEE_DOCTOR) stringResource(R.string.alert_cycle_90_text) else stringResource(R.string.alert_cycle_60_text),
                )
            }
        }
        if (overview?.flags?.fewPeriodsInTrailingYear == true && state.pillMode == null) {
            item {
                UyariBandi(
                    tur = BantTuru.DIKKAT,
                    title = stringResource(R.string.alert_few_periods_title),
                    text = stringResource(R.string.alert_few_periods_text),
                )
            }
        }

        if (state.notificationsOff) {
            item {
                val context = LocalContext.current
                UyariBandi(
                    tur = BantTuru.DIKKAT,
                    title = stringResource(R.string.today_notifications_off_title),
                    text = stringResource(R.string.today_notifications_off_text),
                    actionLabel = stringResource(R.string.action_open_settings),
                    onAction = { openNotificationSettings(context) },
                    dismissLabel = stringResource(R.string.action_not_now),
                    onDismiss = onDismissNotificationBand,
                )
            }
        }

        if (state.exactAlarmMissing) {
            item {
                val context = LocalContext.current
                UyariBandi(
                    tur = BantTuru.BILGI,
                    title = stringResource(R.string.onboarding_exact_alarm_title),
                    text = stringResource(R.string.settings_exact_alarm_missing),
                    actionLabel = stringResource(R.string.action_open_settings),
                    onAction = {
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            android.net.Uri.parse("package:${context.packageName}"),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.today_plan_title),
                action = { TextButton(onClick = onOpenMeds) { Text(stringResource(R.string.action_all)) } },
            )
        }

        if (!state.hasMedications) {
            item {
                AylaCard {
                    Text(stringResource(R.string.today_no_meds), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOpenMeds) { Text(stringResource(R.string.today_add_med)) }
                }
            }
        } else {
            items(state.doses, key = { "${it.medication.id}-${it.time}" }) { dose ->
                DozSatiri(item = dose, onTaken = { onTaken(dose) }, onUndo = { onUndo(dose) })
            }
        }

        item {
            SuSatiri(cups = state.waterCups, goal = state.waterGoal, onAdd = onAddWater)
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun greeting(name: String, hour: Int): String {
    val hasName = name.isNotBlank()
    return when {
        hour < 12 -> if (hasName) stringResource(R.string.today_greeting_morning, name) else stringResource(R.string.today_greeting_morning_anon)
        hour < 18 -> if (hasName) stringResource(R.string.today_greeting_day, name) else stringResource(R.string.today_greeting_day_anon)
        else -> if (hasName) stringResource(R.string.today_greeting_evening, name) else stringResource(R.string.today_greeting_evening_anon)
    }
}

@Composable
private fun CycleHero(overview: CycleOverview?, hidePrediction: Boolean, today: LocalDate) {
    val day = overview?.currentCycleDay
    val lastStart = overview?.lastPeriodStart
    val prediction = if (hidePrediction) Prediction.NotEnoughData else overview?.prediction ?: Prediction.NotEnoughData

    val window = prediction as? Prediction.Window
    val windowStartDay = if (window != null && lastStart != null) ChronoUnit.DAYS.between(lastStart, window.earliest).toInt() + 1 else null
    val windowEndDay = if (window != null && lastStart != null) ChronoUnit.DAYS.between(lastStart, window.latest).toInt() + 1 else null
    val overdue = prediction is Prediction.PastWindow
    val totalDays = maxOf(28, day ?: 0, windowEndDay ?: 0)

    val confidenceText = when (window?.confidence) {
        Confidence.LOW -> stringResource(R.string.confidence_low)
        Confidence.MEDIUM -> stringResource(R.string.confidence_medium)
        Confidence.HIGH -> stringResource(R.string.confidence_high)
        null -> ""
    }
    val a11y = when {
        day == null -> stringResource(R.string.label_no_period_yet)
        window != null -> stringResource(R.string.today_ring_a11y, day, window.earliest.format(dayMonth), window.latest.format(dayMonth), confidenceText)
        overdue -> stringResource(R.string.today_ring_a11y_past, day)
        else -> stringResource(R.string.today_ring_a11y_no_prediction, day)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HaleHalkasi(
            day = day,
            totalDays = totalDays,
            windowStartDay = windowStartDay,
            windowEndDay = windowEndDay,
            overdue = overdue,
            contentDescription = a11y,
        ) {
            HaleMerkezi(
                label = stringResource(R.string.label_cycle_day),
                day = day,
                subtitle = if (lastStart == null) {
                    stringResource(R.string.label_no_period_yet)
                } else {
                    stringResource(R.string.label_last_period, lastStart.format(dayMonth))
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        when {
            hidePrediction && lastStart != null -> Text(
                stringResource(R.string.label_prediction_hidden),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            window != null -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.label_predicted_period), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.label_date_range, window.earliest.format(dayMonthShort), window.latest.format(dayMonthShort)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                GuvenRozeti(window.confidence, confidenceText)
            }
            prediction is Prediction.PastWindow -> Text(
                stringResource(R.string.label_cycle_longer_than_expected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            lastStart != null -> Text(
                stringResource(R.string.label_prediction_not_enough),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Text(
                stringResource(R.string.empty_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

/** Güven: üç nokta ve sözcük; renk tek başına anlam taşımaz. */
@Composable
private fun GuvenRozeti(confidence: Confidence, text: String) {
    val filled = when (confidence) {
        Confidence.LOW -> 1
        Confidence.MEDIUM -> 2
        Confidence.HIGH -> 3
    }
    Surface(shape = CircleShape, color = LocalAylaColors.current.cukur) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { index ->
                val color: Color = if (index < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                Box(Modifier.size(8.dp).background(color, CircleShape))
            }
            Text(stringResource(R.string.label_confidence, text), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PillHero(pill: PillModeState) {
    val timeText = pill.medication.times.firstOrNull()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
    AylaCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(stringResource(R.string.today_pill_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.today_pill_regimen, pill.regimen.activeDays, pill.regimen.placeboDays, timeText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.today_pill_number, pill.todayPill),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        HapPaketi(
            regimen = pill.regimen,
            todayPill = pill.todayPill,
            takenPills = pill.takenPills,
            contentDescription = stringResource(R.string.today_pill_pack_a11y, pill.regimen.packLength, pill.todayPill),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.today_pill_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Uygulamanın sistem bildirim ayarları (Android 8+). */
internal fun openNotificationSettings(context: android.content.Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/**
 * Henüz adet kaydı yokken halka yerine: sıcak bir karşılama ve son adet tarihini seçme düğmesi.
 * "Adet başladı" yalnızca bugünü kaydeder; ilk kayıt çoğunlukla geçmiş bir gündür.
 */
@Composable
private fun IlkAdetKarti(today: LocalDate, onPick: (LocalDate) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    AylaCard(containerColor = scheme.primaryContainer, bordered = false) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(LocalAylaColors.current.kart, AylaExpressiveShapes.Cicek),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.LocalFlorist, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.today_empty_title), style = MaterialTheme.typography.headlineSmall, color = scheme.onPrimaryContainer)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.empty_today), style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimaryContainer)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { showPicker = true }) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.today_empty_pick))
        }
    }
    if (showPicker) {
        TarihSeciciDialog(
            title = stringResource(R.string.onboarding_period_title),
            initial = today,
            latest = today,
            onDismiss = { showPicker = false },
            onConfirm = { date ->
                showPicker = false
                onPick(date)
            },
        )
    }
}
