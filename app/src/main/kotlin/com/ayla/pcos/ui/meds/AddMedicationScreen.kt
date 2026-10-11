package com.ayla.pcos.ui.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ayla.pcos.R
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.MedicationTemplates
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.saatMetni
import java.time.LocalDate

/**
 * İlaç ekleme: hazır şablonlar tek dokunuşla eklenir (doğum kontrol hapında önce paket başlangıcı
 * sorulur); listede olmayanlar için tam form açılır.
 */
@Composable
fun AddMedicationScreen(onDone: () -> Unit, onCustom: () -> Unit, viewModel: MedsViewModel = hiltViewModel()) {
    AddMedicationContent(
        onBack = onDone,
        onTemplate = { template, packStart ->
            viewModel.addTemplate(template, packStart)
            onDone()
        },
        onCustom = onCustom,
    )
}

/** Durumsuz ilaç ekleme içeriği. */
@Composable
internal fun AddMedicationContent(
    onBack: () -> Unit,
    onTemplate: (MedicationTemplates.Template, LocalDate?) -> Unit,
    onCustom: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    var pillTemplate by remember { mutableStateOf<MedicationTemplates.Template?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.meds_add_title), style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(8.dp))
        Overline(stringResource(R.string.meds_templates))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MedicationTemplates.all.forEach { template ->
                SatirZemini(onClick = {
                    if (template.kind == MedicationKind.CONTRACEPTIVE) pillTemplate = template else onTemplate(template, null)
                }) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text(templateSubtitle(template), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        AylaCard {
            Text(stringResource(R.string.meds_custom_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.meds_custom_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onCustom) {
                Icon(Icons.Rounded.EditNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.meds_custom))
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    pillTemplate?.let { template ->
        PaketBaslangicDialog(
            initial = today,
            today = today,
            onDismiss = { pillTemplate = null },
            onConfirm = { date ->
                pillTemplate = null
                onTemplate(template, date)
            },
        )
    }
}

@Composable
private fun templateSubtitle(template: MedicationTemplates.Template): String {
    val times = template.times.joinToString(" · ") { it.saatMetni() }
    val active = template.pillActiveDays
    return if (template.kind == MedicationKind.CONTRACEPTIVE && active != null) {
        stringResource(R.string.meds_template_pill, template.doseText, times, active, template.pillPlaceboDays ?: 0)
    } else {
        template.doseText + " · " + times
    }
}
