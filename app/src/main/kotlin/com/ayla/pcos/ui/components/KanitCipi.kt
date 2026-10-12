package com.ayla.pcos.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.ui.theme.LocalAylaColors

/**
 * Kanıt düzeyi (docs/arastirma/pcos-kanit-ozeti.md): PCOS kılavuzunun benimsediği öneri "güçlü"; kılavuzun
 * benimsemediği genel nüfus kanıtı bir basamak düşer; küçük çalışmalar "sınırlı"; kurum önerisi "uzman görüşü".
 */
enum class EvidenceLevel(@StringRes val labelRes: Int, @StringRes val textRes: Int, val icon: ImageVector) {
    STRONG(R.string.evidence_strong_label, R.string.evidence_strong_text, Icons.Rounded.Verified),
    MODERATE(R.string.evidence_moderate_label, R.string.evidence_moderate_text, Icons.Rounded.TaskAlt),
    LIMITED(R.string.evidence_limited_label, R.string.evidence_limited_text, Icons.Rounded.Science),
    EXPERIMENTAL(R.string.evidence_experimental_label, R.string.evidence_experimental_text, Icons.Rounded.Biotech),
    EXPERT_OPINION(R.string.evidence_expert_label, R.string.evidence_expert_text, Icons.Rounded.Groups),
}

/**
 * Kanıt çipi: simge + etiket (renk tek başına anlam taşımaz; her düzey aynı nötr renkte, metinle ayrışır).
 * Dokununca düzeyin ne anlama geldiği açılır. Dokunma alanı en az 48 dp.
 */
@Composable
fun KanitCipi(level: EvidenceLevel, modifier: Modifier = Modifier) {
    val colors = LocalAylaColors.current
    var showInfo by remember { mutableStateOf(false) }
    val label = stringResource(level.labelRes)
    val a11y = stringResource(R.string.evidence_chip_a11y, label)
    Surface(
        onClick = { showInfo = true },
        shape = CircleShape,
        color = colors.lavantaAcik,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = a11y },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(level.icon, contentDescription = null, tint = colors.lavanta, modifier = Modifier.size(yaziOlcekli(14.dp)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.lavanta, maxLines = 1)
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            icon = { Icon(level.icon, contentDescription = null) },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(level.textRes), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.evidence_info_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    Text(stringResource(R.string.evidence_info_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}
