package com.ayla.pcos.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ayla.pcos.R

/**
 * Sağlık verisi açık rıza penceresi: rıza hiç kaydedilmemişse genel metin, eski kapsamdaysa yeni veri türlerini
 * anlatan metin (#20). Kapatılamaz; politika okunabilir.
 */
@Composable
internal fun AcikRizaPenceresi(scopeUpdated: Boolean, onAccept: () -> Unit, onReadPolicy: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(if (scopeUpdated) R.string.consent_update_title else R.string.consent_dialog_title)) },
        text = { Text(stringResource(if (scopeUpdated) R.string.consent_update_text else R.string.consent_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.consent_dialog_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onReadPolicy) { Text(stringResource(R.string.privacy_policy_read)) }
        },
    )
}
