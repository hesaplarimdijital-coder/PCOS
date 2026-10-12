package com.ayla.pcos.ui.me

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.MarkdownMetin

/**
 * Gizlilik politikası ve KVKK aydınlatma metni. Kaynak `docs/gizlilik-politikasi.md`'dir; uygulama içindeki
 * kopya (`res/raw/gizlilik_politikasi.md`) bir birim testiyle onunla aynı tutulur. Play Console'da ve
 * Health Connect'in "Gizlilik politikası" bağlantısında aynı metin gösterilir.
 */
@Composable
fun GizlilikPolitikasiEkrani(onBack: () -> Unit) {
    GizlilikPolitikasiIcerik(onClose = onBack, closeIsBack = true)
}

/** Karşılama ve rıza diyaloğu gibi gezgin dışındaki yerlerden tam ekran açılır. */
@Composable
fun GizlilikPolitikasiDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            GizlilikPolitikasiIcerik(onClose = onDismiss, closeIsBack = false)
        }
    }
}

@Composable
internal fun GizlilikPolitikasiIcerik(onClose: () -> Unit, closeIsBack: Boolean, markdown: String? = null) {
    val resources = LocalResources.current
    val text = markdown ?: remember(resources) {
        resources.openRawResource(R.raw.gizlilik_politikasi).bufferedReader().use { it.readText() }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                if (closeIsBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                } else {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
            Text(stringResource(R.string.privacy_policy_title), style = MaterialTheme.typography.titleLarge)
        }
        MarkdownMetin(
            markdown = text,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        )
    }
}
