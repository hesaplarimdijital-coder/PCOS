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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.DoseItem
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.DozSatiri
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.components.saatMetni
import com.ayla.pcos.ui.theme.LocalAylaColors
import kotlin.math.roundToInt

@Composable
fun MedsScreen(onAdd: () -> Unit, onEdit: (Long) -> Unit, viewModel: MedsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MedsContent(
        state = state,
        onAdd = onAdd,
        onTaken = { viewModel.markTaken(it) },
        onUndo = { viewModel.undo(it) },
        onEdit = { onEdit(it.id) },
    )
}

/** Durumsuz İlaçlar içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun MedsContent(
    state: MedsUiState,
    onAdd: () -> Unit,
    onTaken: (DoseItem) -> Unit,
    onUndo: (DoseItem) -> Unit,
    onEdit: (MedicationEntity) -> Unit,
) {
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
            return@LazyColumn
        }

        item { Overline(stringResource(R.string.label_today)) }
        items(state.todayDoses, key = { "${it.medication.id}-${it.time}" }) { dose ->
            DozSatiri(item = dose, onTaken = { onTaken(dose) }, onUndo = { onUndo(dose) })
        }

        item {
            val a = state.weekAdherence
            AylaCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
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

        item {
            Spacer(Modifier.height(4.dp))
            Overline(stringResource(R.string.meds_list_title))
        }
        if (state.medications.any { isInositol(it.name) }) {
            item {
                UyariBandi(
                    tur = BantTuru.BILGI,
                    title = stringResource(R.string.meds_inositol_evidence_title),
                    text = stringResource(R.string.meds_inositol_evidence_text),
                )
            }
        }
        items(state.medications, key = { it.id }) { med ->
            MedicationRow(med, onClick = { onEdit(med) })
        }
    }
}

@Composable
private fun MedicationRow(med: MedicationEntity, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    val limitedEvidence = isInositol(med.name) || med.kind == MedicationKind.HERBAL
    val badge: @Composable () -> Unit = {
        Surface(shape = CircleShape, color = colors.lavantaAcik) {
            Text(
                stringResource(R.string.evidence_limited_label),
                style = MaterialTheme.typography.labelSmall,
                color = colors.lavanta,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
    SatirZemini(onClick = onClick) {
        Column(modifier = Modifier.weight(1f)) {
            if (LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI) {
                // Büyük yazıda rozet adın altına iner; ad "Nane / çayı" diye bölünmez.
                Text(med.name, style = MaterialTheme.typography.titleMedium)
                if (limitedEvidence) badge()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(med.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    if (limitedEvidence) badge()
                }
            }
            val times = med.times.joinToString(" · ") { it.saatMetni() }
            Text("${med.doseText} · $times", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            med.stockCount?.let { stock ->
                val daysLeft = stock / med.times.size.coerceAtLeast(1)
                val low = daysLeft <= med.stockWarnDays
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = if (low) colors.uyari else scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(if (low) R.string.meds_stock_low else R.string.meds_stock_left, daysLeft),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (low) colors.uyari else scheme.onSurfaceVariant,
                    )
                }
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.action_edit), tint = scheme.onSurfaceVariant)
    }
}
