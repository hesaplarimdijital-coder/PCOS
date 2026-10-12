package com.ayla.pcos.ui.me

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.data.health.HealthConnectAvailability
import com.ayla.pcos.ui.health.HealthConnectUiState
import com.ayla.pcos.ui.health.hcMessageText
import com.ayla.pcos.ui.theme.LocalAylaColors

/** Ayarlar'daki Health Connect bölümünün eylemleri. */
class HealthConnectActions(
    val onConnect: () -> Unit = {},
    val onImport: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenPlayStore: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
    val onDeleteImported: () -> Unit = {},
)

private data class HcDurum(val icon: ImageVector, val tint: Color, @param:StringRes val title: Int, @param:StringRes val text: Int)

/**
 * Ayarlar > Health Connect (#11): bağlantı durumu; bağlan, şimdi al, Health Connect ayarları, bağlantıyı kes ve
 * alınan geceleri silme. Gizlilik politikasının 6. bölümü izni geri almak için bu yolu anar.
 */
@Composable
internal fun HealthConnectAyarIcerigi(state: HealthConnectUiState, actions: HealthConnectActions) {
    var confirmDisconnect by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val colors = LocalAylaColors.current
    val status = when {
        state.availability == HealthConnectAvailability.UNAVAILABLE ->
            HcDurum(Icons.Rounded.WarningAmber, colors.uyari, R.string.hc_status_unavailable, R.string.hc_status_unavailable_text)
        state.availability == HealthConnectAvailability.UPDATE_REQUIRED ->
            HcDurum(Icons.Rounded.SystemUpdate, colors.uyari, R.string.hc_status_update, R.string.hc_update_required)
        state.connected -> HcDurum(Icons.Rounded.CheckCircle, colors.basari, R.string.hc_status_connected, R.string.hc_status_connected_text)
        state.consentGiven -> HcDurum(Icons.Rounded.WarningAmber, colors.uyari, R.string.hc_status_revoked, R.string.hc_revoked_text)
        else -> HcDurum(Icons.Rounded.Bedtime, colors.lavanta, R.string.hc_status_disconnected, R.string.hc_status_disconnected_text)
    }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        Icon(status.icon, contentDescription = null, tint = status.tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(status.title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(status.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    // Eylemler: kullanılabilirliğe göre bağlan / şimdi al / ayarlar; rıza verilmişse her durumda "Bağlantıyı kes"
    // (rıza, Health Connect kaldırılmış olsa da geri alınabilmeli).
    when {
        state.availability == HealthConnectAvailability.UPDATE_REQUIRED -> {
            HorizontalDivider()
            EylemSatiri(Icons.Rounded.SystemUpdate, R.string.hc_update_action, null, onClick = actions.onOpenPlayStore)
        }
        state.connected -> {
            HorizontalDivider()
            EylemSatiri(Icons.Rounded.Sync, R.string.hc_import_now, R.string.hc_import_now_text, onClick = actions.onImport)
        }
        state.visible -> {
            HorizontalDivider()
            EylemSatiri(Icons.Rounded.Link, R.string.hc_connect, null, onClick = actions.onConnect)
        }
    }
    if (state.availability == HealthConnectAvailability.AVAILABLE && state.consentGiven) {
        HorizontalDivider()
        EylemSatiri(Icons.Rounded.Settings, R.string.hc_open_settings, null, onClick = actions.onOpenSettings)
    }
    if (state.consentGiven) {
        HorizontalDivider()
        EylemSatiri(Icons.Rounded.LinkOff, R.string.hc_disconnect, null) { confirmDisconnect = true }
    }
    if (state.importedCount > 0) {
        HorizontalDivider()
        EylemSatiri(Icons.Rounded.Delete, R.string.hc_delete_imported, null, destructive = true) { confirmDelete = true }
    }
    if (state.importing) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    }
    hcMessageText(state.message)?.let { text ->
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.hc_disconnect_confirm_title)) },
            text = { Text(stringResource(R.string.hc_disconnect_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    actions.onDisconnect()
                }) { Text(stringResource(R.string.hc_disconnect)) }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.hc_delete_imported_confirm_title)) },
            text = { Text(stringResource(R.string.hc_delete_imported_confirm, state.importedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDeleteImported()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
