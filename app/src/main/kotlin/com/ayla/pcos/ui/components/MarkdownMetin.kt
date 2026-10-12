package com.ayla.pcos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Basit Markdown blokları: gizlilik politikası gibi uzun metinler için yeterli alt küme. */
sealed interface MdBlok {
    data class Baslik(val seviye: Int, val metin: String) : MdBlok
    data class Paragraf(val metin: String) : MdBlok
    data class Madde(val metin: String) : MdBlok
}

/**
 * Markdown'ı bloklara ayırır: `#` başlıkları, `- ` maddeleri ve boş satırla ayrılan paragraflar.
 * Tablo, kod ve bağlantı desteklenmez (gösterilecek metinler bunları kullanmaz).
 */
fun markdownBloklari(text: String): List<MdBlok> {
    val bloklar = mutableListOf<MdBlok>()
    val paragraf = StringBuilder()
    fun bosalt() {
        if (paragraf.isNotBlank()) bloklar += MdBlok.Paragraf(paragraf.toString().trim())
        paragraf.clear()
    }
    text.lines().forEach { raw ->
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        when {
            trimmed.isEmpty() -> bosalt()
            trimmed.startsWith("#") -> {
                bosalt()
                val seviye = trimmed.takeWhile { it == '#' }.length
                bloklar += MdBlok.Baslik(seviye, trimmed.drop(seviye).trim())
            }
            trimmed.startsWith("- ") -> {
                bosalt()
                bloklar += MdBlok.Madde(trimmed.removePrefix("- ").trim())
            }
            else -> {
                if (paragraf.isNotEmpty()) paragraf.append(' ')
                paragraf.append(trimmed)
            }
        }
    }
    bosalt()
    return bloklar
}

/** `**kalın**` işaretlerini kalın yazıya çevirir; eşi olmayan işaret düz metin kalır. */
fun kalinVurgulu(text: String): AnnotatedString = buildAnnotatedString {
    val parcalar = text.split("**")
    if (parcalar.size % 2 == 0) {
        // Tek sayıda işaret: biçimlendirme belirsiz, metni olduğu gibi göster.
        append(text)
        return@buildAnnotatedString
    }
    parcalar.forEachIndexed { index, parca ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(parca) } else append(parca)
    }
}

/** Markdown metnini Ayla tipografisiyle çizer; başlıklar ekran okuyucuya başlık olarak bildirilir. */
@Composable
fun MarkdownMetin(markdown: String, modifier: Modifier = Modifier) {
    val bloklar = remember(markdown) { markdownBloklari(markdown) }
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bloklar.forEach { blok ->
            when (blok) {
                is MdBlok.Baslik -> {
                    if (blok.seviye > 1) Spacer(Modifier.height(10.dp))
                    Text(
                        kalinVurgulu(blok.metin),
                        style = when (blok.seviye) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        modifier = Modifier.semantics { heading() },
                    )
                }
                is MdBlok.Paragraf -> Text(kalinVurgulu(blok.metin), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                is MdBlok.Madde -> Row(modifier = Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Text(kalinVurgulu(blok.metin), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
            }
        }
    }
}
