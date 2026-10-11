package com.ayla.pcos.ui.log

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.repo.SymptomCatalog
import com.ayla.pcos.ui.components.Overline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Günlük kayıt: ruh hâli, enerji, kanama, semptomlar, not; isteğe bağlı ölçümler. 30 saniyede doldurulur. */
@Composable
fun DailyLogScreen(date: LocalDate, onClose: () -> Unit, viewModel: DailyLogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(date) { viewModel.load(date) }
    if (!state.loaded || state.date != date) return
    DailyLogContent(
        state = state,
        onClose = onClose,
        onMood = { viewModel.setMood(it) },
        onEnergy = { viewModel.setEnergy(it) },
        onFlow = { viewModel.setFlow(it) },
        onSymptom = { viewModel.toggleSymptom(it) },
        onWeight = { viewModel.setWeight(it) },
        onWaist = { viewModel.setWaist(it) },
        onNote = { viewModel.setNote(it) },
        onSave = { viewModel.save(onClose) },
    )
}

/** Durumsuz günlük kayıt içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DailyLogContent(
    state: DailyLogUiState,
    onClose: () -> Unit,
    onMood: (Int) -> Unit,
    onEnergy: (Int) -> Unit,
    onFlow: (Int) -> Unit,
    onSymptom: (String) -> Unit,
    onWeight: (String) -> Unit,
    onWaist: (String) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
) {
    val date = state.date
    val dateText = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("tr")))

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Overline(dateText)
                    Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close)) }
            }

            Bolum(stringResource(R.string.log_mood)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val labels = listOf(R.string.mood_1, R.string.mood_2, R.string.mood_3, R.string.mood_4, R.string.mood_5)
                    labels.forEachIndexed { index, res ->
                        SegmentedButton(
                            selected = state.mood == index + 1,
                            onClick = { onMood(index + 1) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                            label = { Text((index + 1).toString()) },
                        )
                    }
                }
                state.mood?.let { Text(stringResource(listOf(R.string.mood_1, R.string.mood_2, R.string.mood_3, R.string.mood_4, R.string.mood_5)[it - 1]), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            Bolum(stringResource(R.string.log_energy)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val labels = listOf(R.string.energy_1, R.string.energy_2, R.string.energy_3)
                    labels.forEachIndexed { index, res ->
                        SegmentedButton(
                            selected = state.energy == index + 1,
                            onClick = { onEnergy(index + 1) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                            label = { Text(stringResource(res)) },
                        )
                    }
                }
            }

            Bolum(stringResource(R.string.log_flow)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(R.string.flow_0, R.string.flow_1, R.string.flow_2, R.string.flow_3, R.string.flow_4).forEachIndexed { index, res ->
                        FilterChip(
                            selected = state.flow == index,
                            onClick = { onFlow(index) },
                            label = { Text(stringResource(res)) },
                            leadingIcon = if (state.flow == index) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                        )
                    }
                }
            }

            Bolum(stringResource(R.string.log_body)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SymptomCatalog.defaults.forEach { key ->
                        val selected = key in state.symptoms
                        FilterChip(
                            selected = selected,
                            onClick = { onSymptom(key) },
                            label = { Text(stringResource(symptomLabel(key))) },
                            leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                        )
                    }
                }
            }

            if (state.showBodyMeasurements) {
                Bolum(stringResource(R.string.log_measurements)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.weightKg,
                            onValueChange = onWeight,
                            label = { Text(stringResource(R.string.log_weight)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                        )
                        OutlinedTextField(
                            value = state.waistCm,
                            onValueChange = onWaist,
                            label = { Text(stringResource(R.string.log_waist)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
            }

            Bolum(stringResource(R.string.log_note)) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNote,
                    placeholder = { Text(stringResource(R.string.log_note_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text(" " + stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun Bolum(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    content()
}

fun symptomLabel(key: String): Int = when (key) {
    SymptomCatalog.BLOATING -> R.string.symptom_bloating
    SymptomCatalog.BREAST_TENDERNESS -> R.string.symptom_breast_tenderness
    SymptomCatalog.SWEET_CRAVING -> R.string.symptom_sweet_craving
    SymptomCatalog.ACNE -> R.string.symptom_acne
    SymptomCatalog.HAIR_LOSS -> R.string.symptom_hair_loss
    SymptomCatalog.CRAMPS -> R.string.symptom_cramps
    SymptomCatalog.HEADACHE -> R.string.symptom_headache
    SymptomCatalog.LOW_MOOD -> R.string.symptom_low_mood
    SymptomCatalog.FATIGUE -> R.string.symptom_fatigue
    SymptomCatalog.NAUSEA -> R.string.symptom_nausea
    SymptomCatalog.INSOMNIA -> R.string.symptom_insomnia
    SymptomCatalog.SPOTTING -> R.string.symptom_spotting
    else -> R.string.symptom_bloating
}
