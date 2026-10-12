package com.ayla.pcos.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.DegerSatiri
import com.ayla.pcos.ui.components.EkranBasligi
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.SegmentEtiketi
import com.ayla.pcos.ui.components.SimgeZemini
import com.ayla.pcos.ui.components.TarihSeciciDialog
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.components.segmentSimgesi
import com.ayla.pcos.ui.components.sharePdf
import com.ayla.pcos.ui.components.shareText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Doktor raporu (#26): aralık, bölümler, sayfa önizlemesi; PDF ya da metin olarak kullanıcının paylaşımıyla çıkar. */
@Composable
fun ReportScreen(onBack: () -> Unit, onOpenQuestions: () -> Unit, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val subject = stringResource(R.string.report_title)
    val chooser = stringResource(R.string.report_ui_share_chooser)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportEvent.SharePdf -> sharePdf(context, event.file, event.subject, chooser)
                is ReportEvent.ShareText -> shareText(context, event.subject, event.text, chooser)
            }
        }
    }
    if (!state.loaded) return
    ReportContent(
        state = state,
        onBack = onBack,
        onRange = viewModel::setRange,
        onCustomFrom = viewModel::setCustomFrom,
        onCustomTo = viewModel::setCustomTo,
        onToggle = viewModel::toggleSection,
        onOpenQuestions = onOpenQuestions,
        onSharePdf = { viewModel.sharePdf(subject) },
        onShareText = { viewModel.shareText(subject) },
    )
}

private val tarih: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr"))

internal fun ReportSectionChoice.labelRes(): Int = when (this) {
    ReportSectionChoice.CYCLE -> R.string.report_section_cycle
    ReportSectionChoice.LOG -> R.string.report_section_log
    ReportSectionChoice.MEDS -> R.string.report_section_meds
    ReportSectionChoice.SLEEP_WATER -> R.string.report_section_sleep_water
    ReportSectionChoice.FERTILITY -> R.string.report_section_fertility
    ReportSectionChoice.SKIN -> R.string.report_section_skin
    ReportSectionChoice.QUESTIONS -> R.string.report_section_questions
}

private fun RangeChoice.labelRes(): Int = when (this) {
    RangeChoice.DAYS_28 -> R.string.report_ui_range_28
    RangeChoice.DAYS_90 -> R.string.report_ui_range_90
    RangeChoice.DAYS_180 -> R.string.report_ui_range_180
    RangeChoice.CUSTOM -> R.string.report_ui_range_custom
}

/** Durumsuz rapor içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun ReportContent(
    state: ReportUiState,
    onBack: () -> Unit,
    onRange: (RangeChoice) -> Unit,
    onCustomFrom: (LocalDate) -> Unit,
    onCustomTo: (LocalDate) -> Unit,
    onToggle: (ReportSectionChoice) -> Unit,
    onOpenQuestions: () -> Unit,
    onSharePdf: () -> Unit,
    onShareText: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        EkranBasligi(stringResource(R.string.report_ui_title), onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.report_ui_intro), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

            AylaCard {
                Text(stringResource(R.string.report_ui_range), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    RangeChoice.entries.forEachIndexed { index, range ->
                        val selected = state.range == range
                        SegmentedButton(
                            selected = selected,
                            onClick = { onRange(range) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = RangeChoice.entries.size),
                            modifier = Modifier.fillMaxHeight(),
                            icon = segmentSimgesi(selected),
                            label = { SegmentEtiketi(stringResource(range.labelRes()), selected) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (state.range == RangeChoice.CUSTOM) {
                    DegerSatiri(title = stringResource(R.string.report_ui_from), value = state.from.format(tarih), onClick = { pickFrom = true })
                    DegerSatiri(title = stringResource(R.string.report_ui_to), value = state.to.format(tarih), onClick = { pickTo = true })
                } else {
                    Text(
                        stringResource(R.string.report_ui_range_text, state.from.format(tarih), state.to.format(tarih)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            AylaCard {
                Text(stringResource(R.string.report_ui_sections), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                ReportSectionChoice.entries.forEach { section ->
                    val enabled = section != ReportSectionChoice.FERTILITY || state.fertilityAvailable
                    BolumAnahtari(
                        label = stringResource(section.labelRes()),
                        checked = enabled && section in state.sections,
                        enabled = enabled,
                        help = if (!enabled) stringResource(R.string.report_ui_fertility_off) else null,
                        onToggle = { onToggle(section) },
                    )
                }
            }

            SatirZemini(onClick = onOpenQuestions) {
                SimgeZemini(icon = Icons.Rounded.QuestionAnswer, background = scheme.primaryContainer, tint = scheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.report_ui_questions), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.openQuestions > 0) stringResource(R.string.report_ui_questions_count, state.openQuestions) else stringResource(R.string.report_ui_questions_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            }

            if (state.error) {
                UyariBandi(
                    tur = BantTuru.HATA,
                    title = stringResource(R.string.report_ui_error_title),
                    text = stringResource(R.string.report_ui_error_text),
                    actionLabel = stringResource(R.string.report_ui_retry),
                    onAction = onSharePdf,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                state.pageCount?.let { stringResource(R.string.report_ui_pages, it) } ?: stringResource(R.string.report_ui_pages_wait),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Button(onClick = onSharePdf, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(" " + stringResource(R.string.report_ui_busy))
                } else {
                    Icon(Icons.Rounded.IosShare, contentDescription = null)
                    Text(" " + stringResource(R.string.report_ui_share_pdf))
                }
            }
            OutlinedButton(onClick = onShareText, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.report_ui_share_text))
            }
        }
    }

    if (pickFrom) {
        TarihSeciciDialog(
            title = stringResource(R.string.report_ui_from),
            initial = state.from,
            latest = state.today,
            onDismiss = { pickFrom = false },
            onConfirm = {
                onCustomFrom(it)
                pickFrom = false
            },
        )
    }
    if (pickTo) {
        TarihSeciciDialog(
            title = stringResource(R.string.report_ui_to),
            initial = state.to,
            latest = state.today,
            onDismiss = { pickTo = false },
            onConfirm = {
                onCustomTo(it)
                pickTo = false
            },
        )
    }
}

/** Satırın tamamı anahtar (≥ 48 dp); durum metni ekran okuyucuya ayrıca söylenir. */
@Composable
private fun BolumAnahtari(label: String, checked: Boolean, enabled: Boolean, help: String?, onToggle: () -> Unit) {
    val state = if (checked) stringResource(R.string.report_ui_section_on, label) else stringResource(R.string.report_ui_section_off, label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (help != null) Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
