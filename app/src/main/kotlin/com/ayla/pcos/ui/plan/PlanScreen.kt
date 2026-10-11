package com.ayla.pcos.ui.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.repo.duration
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.SectionHeader
import com.ayla.pcos.ui.components.SimgeZemini
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val trLocale: Locale = Locale.forLanguageTag("tr")
private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", trLocale)
private val hourMinute: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Planım: su ve uyku (MVP 1); beslenme ve hareket sekmeleri MVP 2'de dolar. */
@Composable
fun PlanScreen(viewModel: PlanViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Box(Modifier.fillMaxSize())
        return
    }
    PlanContent(
        state = state,
        onAddCup = { viewModel.addCup() },
        onRemoveCup = { viewModel.removeCup() },
        onAddSleep = { bed, wake, quality -> viewModel.addSleep(bed, wake, quality) },
        onDeleteSleep = { viewModel.deleteSleep(it) },
        onEveningRoutine = { viewModel.setEveningRoutine(it) },
    )
}

/** Durumsuz Planım içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun PlanContent(
    state: PlanUiState,
    onAddCup: () -> Unit,
    onRemoveCup: () -> Unit,
    onAddSleep: (LocalTime, LocalTime, Int?) -> Unit,
    onDeleteSleep: (SleepEntity) -> Unit,
    onEveningRoutine: (Boolean) -> Unit,
    initialTab: Int = 0,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var showSleepDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            Overline(state.today.dayOfWeek.getDisplayName(TextStyle.FULL, trLocale) + " · " + state.today.format(dayMonth))
            Text(stringResource(R.string.plan_title), style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(8.dp))
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
            listOf(R.string.plan_tab_water_sleep, R.string.plan_tab_nutrition, R.string.plan_tab_movement).forEachIndexed { index, res ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(res), maxLines = 1) })
            }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(AylaMotion.enter(240)) togetherWith fadeOut(AylaMotion.enter(120)) },
            label = "planSekme",
        ) { selectedTab ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (selectedTab) {
                    0 -> {
                        SuKarti(state = state, onAdd = onAddCup, onRemove = onRemoveCup)
                        UykuKarti(state = state, onAdd = { showSleepDialog = true }, onDelete = onDeleteSleep)
                        AksamRutiniSatiri(
                            enabled = state.eveningRoutineEnabled,
                            time = state.eveningRoutineTime,
                            onChange = onEveningRoutine,
                        )
                    }
                    1 -> YakindaKarti(stringResource(R.string.plan_nutrition_preview))
                    else -> YakindaKarti(stringResource(R.string.plan_movement_preview))
                }
            }
        }
    }

    if (showSleepDialog) {
        UykuEkleDialog(
            onDismiss = { showSleepDialog = false },
            onSave = { bed, wake, quality ->
                showSleepDialog = false
                onAddSleep(bed, wake, quality)
            },
        )
    }
}

/** Su kartı: büyük sayaç, yayla dolan bardak, son 7 gün çubukları, − / + düğmeleri. */
@Composable
private fun SuKarti(state: PlanUiState, onAdd: () -> Unit, onRemove: () -> Unit) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val litres = String.format(trLocale, "%.1f", state.waterCups * state.cupMl / 1000.0)
    val goalLitres = String.format(trLocale, "%.1f", state.waterGoal * state.cupMl / 1000.0)
    val fraction by animateFloatAsState(
        targetValue = (state.waterCups.toFloat() / state.waterGoal.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = AylaMotion.slow(),
        label = "suDolum",
    )
    val progressText = stringResource(R.string.today_water_progress, state.waterCups, state.waterGoal)

    AylaCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Bardak: dışı ince çizgi, içi aşağıdan yukarı dolar.
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(colors.cukur)
                    .semantics { contentDescription = progressText },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .background(scheme.secondary.copy(alpha = 0.85f)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.water_title), style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.waterCups.toString(), style = MaterialTheme.typography.displaySmall, color = scheme.onSurface)
                    Text(
                        stringResource(R.string.water_of_goal, state.waterGoal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text(
                    if (state.waterCups >= state.waterGoal) stringResource(R.string.today_water_goal_done) else stringResource(R.string.water_litres, litres, goalLitres),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.waterCups >= state.waterGoal) colors.basari else scheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledIconButton(
                    onClick = onAdd,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.secondary, contentColor = scheme.onSecondary),
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.today_water_add)) }
                IconButton(onClick = onRemove, enabled = state.waterCups > 0) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.water_remove_cup))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Overline(stringResource(R.string.plan_last_7_days))
        Spacer(Modifier.height(8.dp))
        HaftalikCubuklar(values = state.waterWeek.map { it.cups }, goal = state.waterGoal, labels = state.waterWeek.map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, trLocale).trimEnd('.') })
    }
}

/** Yedi küçük çubuk; hedefe ulaşan günler dolu renkte. Renk tek başına bilgi taşımaz: altta sayı var. */
@Composable
private fun HaftalikCubuklar(values: List<Int>, goal: Int, labels: List<String>) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val max = maxOf(goal, values.maxOrNull() ?: 0, 1)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { index, value ->
            val reached = value >= goal
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.cukur),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fraction by animateFloatAsState(targetValue = value.toFloat() / max, animationSpec = AylaMotion.slow(), label = "cubuk$index")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                            .background(if (reached) scheme.secondary else scheme.secondary.copy(alpha = 0.45f)),
                    )
                }
                Text(value.toString(), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(labels.getOrNull(index) ?: "", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun UykuKarti(state: PlanUiState, onAdd: () -> Unit, onDelete: (SleepEntity) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    AylaCard {
        SectionHeader(
            title = stringResource(R.string.sleep_title),
            action = { FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.sleep_add)) } },
        )
        Spacer(Modifier.height(8.dp))
        val night = state.lastNight
        if (night == null) {
            Text(stringResource(R.string.sleep_none), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        } else {
            val zone = ZoneId.systemDefault()
            val d = night.duration
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SimgeZemini(icon = Icons.Rounded.Bedtime, background = colors.lavantaAcik, tint = colors.lavanta)
                Column {
                    Text(
                        stringResource(R.string.sleep_duration, d.toHours().toInt(), (d.toMinutes() % 60).toInt()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(
                            R.string.sleep_last_night,
                            night.startAt.atZone(zone).toLocalTime().format(hourMinute),
                            night.endAt.atZone(zone).toLocalTime().format(hourMinute),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        val older = state.sleepWeek.filter { it.id != night?.id }
        if (older.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Overline(stringResource(R.string.plan_last_7_days))
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                older.forEach { s -> UykuSatiri(s, onDelete = { onDelete(s) }) }
            }
        }
    }
}

@Composable
private fun UykuSatiri(s: SleepEntity, onDelete: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val d = s.duration
    SatirZemini {
        Column(modifier = Modifier.weight(1f)) {
            Text(s.date.format(dayMonth), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.sleep_duration, d.toHours().toInt(), (d.toMinutes() % 60).toInt()) +
                    " · " + s.startAt.atZone(zone).toLocalTime().format(hourMinute) + "–" + s.endAt.atZone(zone).toLocalTime().format(hourMinute),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AksamRutiniSatiri(enabled: Boolean, time: LocalTime, onChange: (Boolean) -> Unit) {
    SatirZemini {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.evening_routine_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.evening_routine_text, time.format(hourMinute)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun YakindaKarti(preview: String) {
    AylaCard {
        Text(stringResource(R.string.plan_coming_soon), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Üç adımlı tek diyalog: yatış saati → kalkış saati → kalite. */
@Composable
private fun UykuEkleDialog(onDismiss: () -> Unit, onSave: (LocalTime, LocalTime, Int?) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val bedState = rememberTimePickerState(initialHour = 23, initialMinute = 0, is24Hour = true)
    val wakeState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = true)
    var quality by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    0 -> stringResource(R.string.sleep_bedtime)
                    1 -> stringResource(R.string.sleep_wake)
                    else -> stringResource(R.string.sleep_quality)
                },
            )
        },
        text = {
            when (step) {
                0 -> TimePicker(state = bedState)
                1 -> TimePicker(state = wakeState)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val labels = listOf(R.string.sleep_quality_1, R.string.sleep_quality_2, R.string.sleep_quality_3)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        labels.forEachIndexed { index, res ->
                            SegmentedButton(
                                selected = quality == index + 1,
                                onClick = { quality = if (quality == index + 1) null else index + 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                                label = { Text(stringResource(res)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        LocalTime.of(bedState.hour, bedState.minute).format(hourMinute) + " – " + LocalTime.of(wakeState.hour, wakeState.minute).format(hourMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (step < 2) {
                    step++
                } else {
                    onSave(LocalTime.of(bedState.hour, bedState.minute), LocalTime.of(wakeState.hour, wakeState.minute), quality)
                }
            }) { Text(stringResource(if (step < 2) R.string.action_continue else R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = { if (step > 0) step-- else onDismiss() }) {
                Text(stringResource(if (step > 0) R.string.action_back else R.string.action_cancel))
            }
        },
    )
}
