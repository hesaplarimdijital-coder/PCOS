package com.ayla.pcos.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.EvidenceLevel
import com.ayla.pcos.ui.components.KanitCipi
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.theme.LocalAylaColors

/**
 * Başlık + kanıt çipi satırı. Büyük yazıda çip başlığın altına iner; başlık sütunu her zaman ağırlıklıdır
 * (sağdaki çip başlığı harf harf sıkıştırmaz).
 */
@Composable
internal fun BaslikVeKanit(title: String, evidence: EvidenceLevel?, titleColor: Color = MaterialTheme.colorScheme.onSurface) {
    val stacked = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI
    if (stacked || evidence == null) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor, modifier = Modifier.semantics { heading() })
        if (evidence != null) {
            Spacer(Modifier.height(6.dp))
            KanitCipi(evidence)
        }
    } else {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                modifier = Modifier.weight(1f).padding(top = 2.dp).semantics { heading() },
            )
            KanitCipi(evidence)
        }
    }
}

/** "Kaynak: …" satırı; künye kısaltmasıdır, tam künye kanıt özetindedir. */
@Composable
internal fun KaynakSatiri(source: Int?) {
    if (source == null) return
    Text(
        stringResource(R.string.plan_source_format, stringResource(source)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Kaynaklı bilgi kartı; [extra] başlık-metin ile kaynak arasına girer. */
@Composable
internal fun IcerikKarti(card: BilgiKarti, extra: (@Composable ColumnScope.() -> Unit)? = null) {
    AylaCard {
        BaslikVeKanit(stringResource(card.title), card.evidence)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(card.text), style = MaterialTheme.typography.bodyMedium)
        extra?.invoke(this)
        if (card.source != null) {
            Spacer(Modifier.height(8.dp))
            KaynakSatiri(card.source)
        }
    }
}

/** Sekme içi bölüm başlığı (büyük harf, ekran okuyucuya başlık). */
@Composable
internal fun BolumBasligi(text: String) {
    Overline(text, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

/** Sekmenin sonundaki genel uyarı. */
@Composable
internal fun IcerikUyarisi() {
    Text(
        stringResource(R.string.plan_content_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Kartın içindeki etiketli alan ("Ne denendi?", "Alerji", "Dikkat"). */
@Composable
internal fun EtiketliAlan(label: String, texts: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = LocalAylaColors.current.lavanta)
        texts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
