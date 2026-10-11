package com.ayla.pcos.ui.meds

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.DozSatiri
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.UyariBandi
import kotlin.math.roundToInt

@Composable
fun MedsScreen(onAdd: () -> Unit, viewModel: MedsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<MedicationEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.meds_title), style = MaterialTheme.typography.headlineMedium)
                FilledIconButton(
                    onClick = onAdd,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.meds_add_title)) }
            }
        }

        if (state.loading) return@LazyColumn

        if (state.medications.isEmpty()) {
            item {
                AylaCard {
                    Text(stringResource(R.string.today_no_meds), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onAdd) { Text(stringResource(R.string.today_add_med)) }
                }
            }
        } else {
            item { Overline(stringResource(R.string.label_today)) }
            items(state.todayDoses, key = { "${it.medication.id}-${it.time}" }) { dose ->
                DozSatiri(item = dose, onTaken = { viewModel.markTaken(dose) }, onUndo = { viewModel.undo(dose) })
            }

            item {
                val a = state.weekAdherence
                AylaCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Overline(stringResource(R.string.meds_adherence_title))
                            Text(stringResource(R.string.meds_adherence_text, a.scheduled, a.taken), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "%" + (a.ratio * 100).roundToInt(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item { Overline(stringResource(R.string.action_all)) }
            items(state.medications, key = { it.id }) { med ->
                MedicationRow(med, onRemove = { pendingRemoval = med })
            }

            if (state.medications.any { it.name.contains("inofolic", ignoreCase = true) || it.name.contains("inositol", ignoreCase = true) }) {
                item {
                    UyariBandi(
                        tur = BantTuru.BILGI,
                        title = stringResource(R.string.meds_inositol_evidence_title),
                        text = stringResource(R.string.meds_inositol_evidence_text),
                    )
                }
            }
        }
    }

    pendingRemoval?.let { med ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.meds_remove)) },
            text = { Text(stringResource(R.string.meds_remove_confirm, med.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(med)
                    pendingRemoval = null
                }) { Text(stringResource(R.string.meds_remove)) }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun MedicationRow(med: MedicationEntity, onRemove: () -> Unit) {
    SatirZemini {
        Column(modifier = Modifier.weight(1f)) {
            Text(med.name, style = MaterialTheme.typography.titleMedium)
            val times = med.times.joinToString(" · ") { it.toString() }
            Text(
                "${med.doseText} · $times",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            med.stockCount?.let { stock ->
                val perDay = med.times.size.coerceAtLeast(1)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(R.string.meds_stock_days, med.name, stock / perDay),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        TextButton(onClick = onRemove) { Text(stringResource(R.string.meds_remove)) }
    }
}
