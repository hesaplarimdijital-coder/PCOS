package com.ayla.pcos.ui.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.PermissionController
import com.ayla.pcos.R
import com.ayla.pcos.data.health.SLEEP_PERMISSIONS
import com.ayla.pcos.data.health.openHealthConnectInPlayStore
import com.ayla.pcos.data.health.openHealthConnectSettings
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.SimgeZemini
import com.ayla.pcos.ui.me.GizlilikPolitikasiDialog
import com.ayla.pcos.ui.theme.LocalAylaColors

/**
 * Health Connect akışının diyalogları (#11): gerekçe ve açık rıza, Health Connect'in izin penceresi,
 * güncelleme gerekli, iki kez iptal. Planım ve Ayarlar aynı akışı [HealthConnectViewModel] ile kullanır.
 */
@Composable
fun HealthConnectAkisi(viewModel: HealthConnectViewModel, step: HcStep?) {
    HealthConnectAkisi(
        step = step,
        onAcceptRationale = viewModel::acceptRationale,
        onDismiss = viewModel::dismissStep,
        onPermissionLaunched = viewModel::permissionRequestLaunched,
        onPermissionLaunchFailed = viewModel::permissionRequestFailed,
        onPermissionResult = viewModel::onPermissionResult,
    )
}

@Composable
internal fun HealthConnectAkisi(
    step: HcStep?,
    onAcceptRationale: () -> Unit,
    onDismiss: () -> Unit,
    onPermissionLaunched: () -> Unit,
    onPermissionLaunchFailed: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val contract = remember { PermissionController.createRequestPermissionResultContract() }
    val launcher = rememberLauncherForActivityResult(contract) { granted -> onPermissionResult(granted.containsAll(SLEEP_PERMISSIONS)) }

    when (step) {
        HcStep.RATIONALE -> HcGerekceDialog(onAccept = onAcceptRationale, onDecline = onDismiss)
        HcStep.REQUEST_PERMISSION -> LaunchedEffect(Unit) {
            runCatching { launcher.launch(SLEEP_PERMISSIONS) }
                .onSuccess { onPermissionLaunched() }
                .onFailure { onPermissionLaunchFailed() }
        }
        HcStep.UPDATE_REQUIRED -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.hc_update_title)) },
            text = { Text(stringResource(R.string.hc_update_required)) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    openHealthConnectInPlayStore(context)
                }) { Text(stringResource(R.string.hc_update_action)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        HcStep.CANCELLED_TWICE -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.hc_cancelled_twice_title)) },
            text = { Text(stringResource(R.string.hc_cancelled_twice_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    openHealthConnectSettings(context)
                }) { Text(stringResource(R.string.hc_open_settings)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        null -> Unit
    }
}

/** Gerekçe ekranı tam ekran diyalogdur; "Gizlilik politikasının tamamı" üstüne açılır. */
@Composable
private fun HcGerekceDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDecline, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            HcGerekceIcerik(onAccept = onAccept, onDecline = onDecline, onOpenPolicy = { showPolicy = true })
        }
    }
    if (showPolicy) GizlilikPolitikasiDialog(onDismiss = { showPolicy = false })
}

/**
 * Açık rıza adımı (KVKK): ne okunur, neden, nerede saklanır, paylaşılmaz, nasıl geri alınır. Rıza cümlesi
 * düğmenin hemen üstündedir ve önceden işaretli bir şey yoktur; onay "Onaylıyorum, devam et" dokunuşudur.
 */
@Composable
internal fun HcGerekceIcerik(onAccept: () -> Unit, onDecline: () -> Unit, onOpenPolicy: () -> Unit) {
    val colors = LocalAylaColors.current
    // Büyük yazıda sabit alt çubuk ekranın yarısını kaplardı; rıza ve düğmeler metnin sonuna, kaydırılan alana iner.
    val stacked = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecline) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close)) }
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.hc_rationale_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            GerekceBolumu(Icons.Rounded.Bedtime, R.string.hc_rationale_what_title, R.string.hc_rationale_what)
            GerekceBolumu(Icons.Rounded.Schedule, R.string.hc_rationale_why_title, R.string.hc_rationale_why)
            GerekceBolumu(Icons.Rounded.Lock, R.string.hc_rationale_where_title, R.string.hc_rationale_where)
            GerekceBolumu(Icons.Rounded.VisibilityOff, R.string.hc_rationale_sharing_title, R.string.hc_rationale_sharing)
            GerekceBolumu(Icons.Rounded.ToggleOff, R.string.hc_rationale_revoke_title, R.string.hc_rationale_revoke)
            TextButton(onClick = onOpenPolicy, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Policy, contentDescription = null, tint = colors.lavanta)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.hc_rationale_policy))
            }
            if (stacked) {
                HorizontalDivider()
                RizaVeDugmeler(onAccept, onDecline, Modifier.padding(bottom = 16.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
        }
        if (!stacked) {
            HorizontalDivider()
            RizaVeDugmeler(onAccept, onDecline, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        }
    }
}

/** Rıza cümlesi düğmenin hemen üstünde; önceden işaretli bir şey yok, onay düğmeye dokunmaktır. */
@Composable
private fun RizaVeDugmeler(onAccept: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.hc_rationale_consent), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.hc_rationale_accept))
        }
        TextButton(onClick = onDecline, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.hc_rationale_decline))
        }
    }
}

@Composable
private fun GerekceBolumu(icon: ImageVector, @StringRes titleRes: Int, @StringRes textRes: Int) {
    val colors = LocalAylaColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        SimgeZemini(icon = icon, background = colors.lavantaAcik, tint = colors.lavanta)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(textRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Sonucun kullanıcıya gösterilen metni; null: gösterilecek bir şey yok. */
@Composable
fun hcMessageText(message: HcMessage?): String? = when (message) {
    null -> null
    is HcMessage.Added -> stringResource(R.string.hc_import_done, message.count)
    HcMessage.NothingNew -> stringResource(R.string.hc_import_nothing_new)
    HcMessage.Failed -> stringResource(R.string.hc_import_failed)
    HcMessage.PermissionDenied -> stringResource(R.string.hc_permission_denied)
    HcMessage.PermissionRevoked -> stringResource(R.string.hc_revoked_text)
    HcMessage.Disconnected -> stringResource(R.string.hc_disconnected)
    is HcMessage.Deleted -> stringResource(R.string.hc_deleted, message.count)
}
