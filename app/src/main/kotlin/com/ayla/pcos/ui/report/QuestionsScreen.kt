package com.ayla.pcos.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.db.DoctorQuestionEntity
import com.ayla.pcos.data.repo.DoctorQuestionRepository
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.CipEtiketi
import com.ayla.pcos.ui.components.EkranBasligi
import com.ayla.pcos.ui.plan.BolumBasligi
import com.ayla.pcos.ui.theme.LocalAylaColors

/** Hazır, nötr soru önerileri (tanı ya da tedavi önermez, ilaç adı vermez). */
internal val soruOnerileri = listOf(
    R.string.questions_suggestion_1,
    R.string.questions_suggestion_2,
    R.string.questions_suggestion_3,
    R.string.questions_suggestion_4,
    R.string.questions_suggestion_5,
    R.string.questions_suggestion_6,
    R.string.questions_suggestion_7,
)

/** Doktora soracaklarım (US-103): açık sorular rapora girer; "sordum" işaretlenenler ayrı listede. */
@Composable
fun QuestionsScreen(onBack: () -> Unit, viewModel: QuestionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.loaded) return
    QuestionsContent(
        state = state,
        onBack = onBack,
        onDraft = viewModel::setDraft,
        onSuggestion = viewModel::useSuggestion,
        onAdd = viewModel::add,
        onAsked = viewModel::setAsked,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuestionsContent(
    state: QuestionsUiState,
    onBack: () -> Unit,
    onDraft: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onAdd: () -> Unit,
    onAsked: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    initialShowAsked: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    var showAsked by rememberSaveable { mutableStateOf(initialShowAsked) }
    var confirmDelete by remember { mutableStateOf<DoctorQuestionEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        EkranBasligi(stringResource(R.string.questions_title), onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.questions_intro), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

            AylaCard {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraft,
                    label = { Text(stringResource(R.string.questions_add_label)) },
                    minLines = 2,
                    supportingText = { Text(stringResource(R.string.questions_counter, state.draft.length, DoctorQuestionRepository.MAX_LENGTH)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                Button(onClick = onAdd, enabled = state.canAdd, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(" " + stringResource(R.string.questions_add))
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.questions_suggestions), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    soruOnerileri.forEach { res ->
                        val text = stringResource(res)
                        AssistChip(onClick = { onSuggestion(text) }, label = { CipEtiketi(text) })
                    }
                }
            }

            BolumBasligi(stringResource(R.string.questions_open))
            if (state.open.isEmpty()) {
                Text(stringResource(R.string.questions_empty), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            } else {
                state.open.forEach { q -> SoruSatiri(q, asked = false, onAsked = onAsked, onDelete = { confirmDelete = q }) }
            }

            if (state.asked.isNotEmpty()) {
                TextButton(onClick = { showAsked = !showAsked }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (showAsked) stringResource(R.string.questions_hide_asked) else stringResource(R.string.questions_show_asked, state.asked.size))
                }
                if (showAsked) {
                    BolumBasligi(stringResource(R.string.questions_asked))
                    state.asked.forEach { q -> SoruSatiri(q, asked = true, onAsked = onAsked, onDelete = { confirmDelete = q }) }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    confirmDelete?.let { q ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.questions_delete_confirm_title)) },
            text = { Text(stringResource(R.string.questions_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(q.id)
                    confirmDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Soru satırı: "sordum" düğmesi (biçim + metinle, renk tek başına değil), metin, silme. */
@Composable
private fun SoruSatiri(q: DoctorQuestionEntity, asked: Boolean, onAsked: (Long, Boolean) -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    val stacked = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI
    val toggleLabel = if (asked) stringResource(R.string.questions_mark_open) else stringResource(R.string.questions_mark_asked)
    AylaCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { onAsked(q.id, !asked) }) {
                Icon(
                    if (asked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = toggleLabel,
                    tint = if (asked) colors.basari else scheme.onSurfaceVariant,
                )
            }
            Text(
                q.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (asked) scheme.onSurfaceVariant else scheme.onSurface,
                modifier = Modifier.weight(1f).padding(top = 12.dp),
            )
            if (!stacked) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.questions_delete), tint = scheme.onSurfaceVariant)
                }
            }
        }
        if (stacked) {
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.questions_delete), color = scheme.error)
            }
        }
    }
}
