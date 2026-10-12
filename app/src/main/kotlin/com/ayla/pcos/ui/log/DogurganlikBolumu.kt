package com.ayla.pcos.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.CipEtiketi
import com.ayla.pcos.ui.components.DegerSatiri
import com.ayla.pcos.ui.components.SaatSeciciDialog
import com.ayla.pcos.ui.components.SegmentEtiketi
import com.ayla.pcos.ui.components.saatMetni
import com.ayla.pcos.ui.components.segmentSimgesi
import java.time.LocalTime

/** LH sonucu seçenekleri: "test yok" (null) ilk sırada. */
internal val lhSecenekleri: List<Pair<LhResult?, Int>> = listOf(
    null to R.string.fertility_lh_none,
    LhResult.NEGATIVE to R.string.fertility_lh_negative,
    LhResult.POSITIVE to R.string.fertility_lh_positive,
    LhResult.INVALID to R.string.fertility_lh_invalid,
)

internal fun mucusLabel(mucus: CervicalMucus): Int = when (mucus) {
    CervicalMucus.DRY -> R.string.fertility_mucus_dry
    CervicalMucus.STICKY -> R.string.fertility_mucus_sticky
    CervicalMucus.CREAMY -> R.string.fertility_mucus_creamy
    CervicalMucus.WATERY -> R.string.fertility_mucus_watery
    CervicalMucus.EGG_WHITE -> R.string.fertility_mucus_egg_white
}

/**
 * Günlük kayıttaki doğurganlık bölümü (#24): bazal ısı, ölçüm saati, "bozuk olabilir", LH ve mukus. Yalnızca
 * kayıt alır; yorum yapmaz. Yumurtlama doğrulaması ısı grafiğinde, Doğurganlık ekranındadır.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DogurganlikBolumu(
    input: FertilityInput,
    onBbt: (String) -> Unit,
    onBbtTime: (LocalTime?) -> Unit,
    onDisturbed: (Boolean) -> Unit,
    onLh: (LhResult?) -> Unit,
    onMucus: (CervicalMucus?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var pickTime by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.fertility_section_hint), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

        Text(stringResource(R.string.fertility_bbt_title), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = input.bbtText,
            onValueChange = onBbt,
            label = { Text(stringResource(R.string.fertility_bbt_value_label)) },
            placeholder = { Text(stringResource(R.string.fertility_bbt_value_hint)) },
            singleLine = true,
            isError = input.bbtInvalid,
            supportingText = when {
                input.bbtInvalid -> ({ Text(stringResource(R.string.fertility_bbt_invalid)) })
                input.bbtOutOfRange -> ({ Text(stringResource(R.string.fertility_bbt_out_of_range)) })
                else -> null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        if (input.bbtCelsius != null) {
            DegerSatiri(
                title = stringResource(R.string.fertility_bbt_time_label),
                value = input.bbtTime?.saatMetni() ?: stringResource(R.string.fertility_bbt_time_unset),
                onClick = { pickTime = true },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = input.bbtDisturbed, role = Role.Switch, onValueChange = onDisturbed),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.fertility_bbt_disturbed_label), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.fertility_bbt_disturbed_help), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                // Satırın tamamı anahtar; Switch'in kendi tıklaması kapalı ki iki kez tetiklenmesin.
                Switch(checked = input.bbtDisturbed, onCheckedChange = null)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.fertility_lh_title), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            lhSecenekleri.forEachIndexed { index, (value, res) ->
                val selected = input.lh == value
                SegmentedButton(
                    selected = selected,
                    onClick = { onLh(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = lhSecenekleri.size),
                    modifier = Modifier.fillMaxHeight(),
                    icon = segmentSimgesi(selected),
                    label = { SegmentEtiketi(stringResource(res), selected) },
                )
            }
        }
        if (input.lh == LhResult.INVALID) {
            Text(stringResource(R.string.fertility_lh_invalid_help), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.fertility_mucus_title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CervicalMucus.entries.forEach { mucus ->
                val selected = input.mucus == mucus
                FilterChip(
                    selected = selected,
                    onClick = { onMucus(mucus) },
                    label = { CipEtiketi(stringResource(mucusLabel(mucus))) },
                    leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                )
            }
        }
        Text(
            stringResource(R.string.fertility_mucus_text),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }

    if (pickTime) {
        SaatSeciciDialog(
            title = stringResource(R.string.fertility_bbt_time_label),
            initial = input.bbtTime ?: LocalTime.of(7, 0),
            onDismiss = { pickTime = false },
            onConfirm = {
                onBbtTime(it)
                pickTime = false
            },
        )
    }
}
