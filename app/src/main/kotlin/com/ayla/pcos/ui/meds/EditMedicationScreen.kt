package com.ayla.pcos.ui.meds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.cycle.PillRegimen
import com.ayla.pcos.R
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.ui.components.AdimSayaci
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.CipEtiketi
import com.ayla.pcos.ui.components.DegerSatiri
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SaatSeciciDialog
import com.ayla.pcos.ui.components.SegmentEtiketi
import com.ayla.pcos.ui.components.TarihSeciciDialog
import com.ayla.pcos.ui.components.saatMetni
import com.ayla.pcos.ui.components.segmentSimgesi
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val gunAyYil: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr"))

/** İlaç ekleme (id = 0) ve düzenleme ekranı. */
@Composable
fun EditMedicationScreen(id: Long, onDone: () -> Unit, viewModel: EditMedicationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { viewModel.load(id) }
    if (!state.loaded || state.id != id) return
    EditMedicationContent(
        state = state,
        onBack = onDone,
        onName = { viewModel.setName(it) },
        onDose = { viewModel.setDose(it) },
        onKind = { viewModel.setKind(it) },
        onAddTime = { viewModel.addTime(it) },
        onRemoveTime = { viewModel.removeTime(it) },
        onFood = { viewModel.setFood(it) },
        onStock = { viewModel.setStock(it) },
        onStockWarnDays = { viewModel.setStockWarnDays(it) },
        onRegimen = { viewModel.setRegimen(it) },
        onPackStart = { viewModel.setPackStart(it) },
        onSave = { viewModel.save(onDone) },
        onRemove = { viewModel.remove(onDone) },
    )
}

/** Durumsuz ilaç formu; ekran görüntüsü testleri doğrudan bunu çizer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditMedicationContent(
    state: EditMedicationState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onDose: (String) -> Unit,
    onKind: (MedicationKind) -> Unit,
    onAddTime: (LocalTime) -> Unit,
    onRemoveTime: (LocalTime) -> Unit,
    onFood: (YemekTercihi) -> Unit,
    onStock: (String) -> Unit,
    onStockWarnDays: (Int) -> Unit,
    onRegimen: (PillRegimen) -> Unit,
    onPackStart: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                Text(
                    stringResource(if (state.isNew) R.string.meds_new_title else R.string.meds_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = onName,
                label = { Text(stringResource(R.string.meds_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.doseText,
                onValueChange = onDose,
                label = { Text(stringResource(R.string.meds_dose)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )

            Bolum(stringResource(R.string.meds_kind)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        MedicationKind.SUPPLEMENT to R.string.meds_kind_supplement,
                        MedicationKind.PRESCRIPTION to R.string.meds_kind_prescription,
                        MedicationKind.HERBAL to R.string.meds_kind_herbal,
                        MedicationKind.CONTRACEPTIVE to R.string.meds_kind_contraceptive,
                    ).forEach { (kind, labelRes) ->
                        FilterChip(selected = state.kind == kind, onClick = { onKind(kind) }, label = { CipEtiketi(stringResource(labelRes)) })
                    }
                }
            }

            Bolum(stringResource(R.string.meds_times)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.times.forEach { time ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveTime(time) },
                            label = { Text(time.saatMetni()) },
                            trailingIcon = if (state.times.size > 1) {
                                { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.meds_remove_time, time.saatMetni()), modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(stringResource(R.string.meds_add_time)) },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            if (state.isPill) {
                Bolum(stringResource(R.string.meds_regimen)) {
                    val regimens = listOf(PillRegimen.R21_7, PillRegimen.R24_4, PillRegimen.CONTINUOUS_28)
                    val labels = listOf(R.string.meds_regimen_21_7, R.string.meds_regimen_24_4, R.string.meds_regimen_28)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        regimens.forEachIndexed { index, regimen ->
                            SegmentedButton(
                                selected = state.regimen == regimen,
                                onClick = { onRegimen(regimen) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = regimens.size),
                                modifier = Modifier.fillMaxHeight(),
                                icon = segmentSimgesi(state.regimen == regimen),
                                label = { SegmentEtiketi(stringResource(labels[index]), state.regimen == regimen) },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    DegerSatiri(
                        title = stringResource(R.string.meds_pack_start),
                        text = stringResource(R.string.meds_pack_start_text),
                        value = state.packStart?.format(gunAyYil) ?: stringResource(R.string.onboarding_period_pick),
                        onClick = { showDatePicker = true },
                    )
                }
            } else {
                Bolum(stringResource(R.string.meds_food)) {
                    val options = listOf(YemekTercihi.YEMEKLE, YemekTercihi.AC_KARNA, YemekTercihi.FARK_ETMEZ)
                    val labels = listOf(R.string.meds_with_food, R.string.meds_without_food, R.string.meds_food_any)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = state.food == option,
                                onClick = { onFood(option) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                modifier = Modifier.fillMaxHeight(),
                                icon = segmentSimgesi(state.food == option),
                                label = { SegmentEtiketi(stringResource(labels[index]), state.food == option) },
                            )
                        }
                    }
                }
            }

            Bolum(stringResource(R.string.meds_stock_section)) {
                OutlinedTextField(
                    value = state.stock,
                    onValueChange = onStock,
                    label = { Text(stringResource(R.string.meds_stock)) },
                    supportingText = { Text(stringResource(R.string.meds_stock_help)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
                if (state.stock.isNotBlank()) {
                    AdimSayaci(
                        title = stringResource(R.string.meds_stock_warn),
                        valueText = stringResource(R.string.meds_stock_warn_value, state.stockWarnDays),
                        canDecrease = state.stockWarnDays > EditMedicationState.STOCK_WARN_RANGE.first,
                        canIncrease = state.stockWarnDays < EditMedicationState.STOCK_WARN_RANGE.last,
                        onDecrease = { onStockWarnDays(state.stockWarnDays - 1) },
                        onIncrease = { onStockWarnDays(state.stockWarnDays + 1) },
                    )
                }
            }

            if (isInositol(state.name)) {
                Spacer(Modifier.height(16.dp))
                AylaCard(containerColor = LocalAylaColors.current.lavantaAcik, bordered = false) {
                    Text(stringResource(R.string.meds_inositol_evidence_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.meds_inositol_evidence_text), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!state.isNew) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(onClick = { confirmRemove = true }, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.meds_remove), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) { Text(stringResource(R.string.action_save)) }
    }

    if (showTimePicker) {
        SaatSeciciDialog(
            title = stringResource(R.string.meds_add_time),
            initial = state.times.lastOrNull()?.plusHours(12) ?: LocalTime.of(9, 0),
            confirmLabel = stringResource(R.string.action_add),
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                showTimePicker = false
                onAddTime(time)
            },
        )
    }

    if (showDatePicker) {
        PaketBaslangicDialog(
            initial = state.packStart ?: state.today,
            today = state.today,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                onPackStart(date)
            },
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.meds_remove)) },
            text = { Text(stringResource(R.string.meds_remove_confirm, state.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    onRemove()
                }) { Text(stringResource(R.string.meds_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Hap paketinin ilk hapının günü; gelecek tarih seçilemez. */
@Composable
internal fun PaketBaslangicDialog(initial: LocalDate, today: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    TarihSeciciDialog(
        title = stringResource(R.string.meds_pack_start),
        initial = initial,
        latest = today,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun Bolum(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Overline(title)
    Spacer(Modifier.height(8.dp))
    content()
}

/** Inofolic ve benzeri inositol ürünleri: kanıt notu gösterilir. */
internal fun isInositol(name: String): Boolean =
    name.contains("inositol", ignoreCase = true) || name.contains("inofolic", ignoreCase = true)
