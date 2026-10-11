package com.ayla.pcos.ui.meds

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ayla.pcos.R
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.MedicationTemplates
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import java.time.LocalTime

/**
 * İlaç ekleme: üstte şablonlar (tek dokunuş), altta özel form.
 */
@Composable
fun AddMedicationScreen(onDone: () -> Unit, viewModel: MedsViewModel = hiltViewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var dose by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(MedicationKind.SUPPLEMENT) }
    var withFood by rememberSaveable { mutableStateOf(true) }
    var stock by rememberSaveable { mutableStateOf("") }
    val times = remember { listOf(LocalTime.of(9, 0)).toMutableStateList() }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.meds_add_title), style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(8.dp))
        Overline(stringResource(R.string.meds_templates))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MedicationTemplates.all.forEach { template ->
                SatirZemini(onClick = {
                    viewModel.addTemplate(template)
                    onDone()
                }) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            template.doseText + " · " + template.times.joinToString(" · ") { it.toString() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Overline(stringResource(R.string.meds_custom))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.meds_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = dose,
            onValueChange = { dose = it },
            label = { Text(stringResource(R.string.meds_dose)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindChip(MedicationKind.SUPPLEMENT, R.string.meds_kind_supplement, kind) { kind = it }
            KindChip(MedicationKind.PRESCRIPTION, R.string.meds_kind_prescription, kind) { kind = it }
            KindChip(MedicationKind.HERBAL, R.string.meds_kind_herbal, kind) { kind = it }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.meds_times), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            times.sorted().forEach { time ->
                InputChip(
                    selected = true,
                    onClick = { if (times.size > 1) times.remove(time) },
                    label = { Text(time.toString()) },
                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
            TextButton(onClick = { showTimePicker = true }) { Text(stringResource(R.string.meds_add_time)) }
        }

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.meds_with_food), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = withFood, onCheckedChange = { withFood = it })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = stock,
            onValueChange = { stock = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text(stringResource(R.string.meds_stock)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.addCustom(
                    name = name,
                    doseText = dose,
                    kind = kind,
                    times = times.toList(),
                    withFood = withFood,
                    stock = stock.toIntOrNull(),
                )
                onDone()
            },
            enabled = name.isNotBlank() && times.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) { Text(stringResource(R.string.action_save)) }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = LocalTime.of(pickerState.hour, pickerState.minute)
                    if (picked !in times) times.add(picked)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = pickerState) },
        )
    }
}

@Composable
private fun KindChip(kind: MedicationKind, labelRes: Int, selected: MedicationKind, onSelect: (MedicationKind) -> Unit) {
    FilterChip(
        selected = kind == selected,
        onClick = { onSelect(kind) },
        label = { Text(stringResource(labelRes)) },
    )
}
