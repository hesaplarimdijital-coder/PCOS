package com.ayla.pcos.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.DoseItem
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Liste satırı zemini: yüzey rengi, ince kenar, radius-md. */
@Composable
fun SatirZemini(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = LocalAylaColors.current.kart,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * Doz satırı: saat, biçim simgesi, ad ve doz, durum. "Aldım"a basılınca buton onay rozetine döner.
 */
@Composable
fun DozSatiri(
    item: DoseItem,
    onTaken: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val (icon, bg, tint) = when (item.medication.kind) {
        MedicationKind.HERBAL -> Triple(Icons.Rounded.LocalCafe, scheme.secondaryContainer, scheme.secondary)
        MedicationKind.SUPPLEMENT -> if (item.medication.form == MedicationForm.DROPS) {
            Triple(Icons.Rounded.WbSunny, scheme.tertiaryContainer, scheme.tertiary)
        } else {
            Triple(Icons.Rounded.Medication, scheme.primaryContainer, scheme.primary)
        }
        else -> Triple(Icons.Rounded.Medication, scheme.primaryContainer, scheme.primary)
    }
    val foodText = when (item.medication.withFood) {
        true -> stringResource(R.string.today_dose_with_food)
        false -> stringResource(R.string.today_dose_without_food)
        null -> null
    }

    val subtitle = when (item.status) {
        DoseStatus.TAKEN -> {
            val at = item.event?.actedAt?.atZone(ZoneId.systemDefault())?.toLocalTime()?.format(timeFormatter)
            if (at != null) stringResource(R.string.today_dose_taken_at, at) else stringResource(R.string.action_taken)
        }
        DoseStatus.SKIPPED -> stringResource(R.string.today_dose_skipped)
        DoseStatus.MISSED -> stringResource(R.string.today_dose_missed)
        DoseStatus.PENDING -> listOfNotNull(item.medication.doseText, foodText).joinToString(" · ")
    }
    // Büyük yazıda tek satıra sığmaz: saat ve simge üstte, ad ortada, eylem altta sağda.
    val buyukYazi = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI

    SatirZemini(modifier = modifier) {
        if (!buyukYazi) {
            DozSaati(item.time.format(timeFormatter))
            SimgeZemini(icon = icon, background = bg, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.medication.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DozEylemi(item.status, onTaken = onTaken, onUndo = onUndo, takenColor = colors.basari, takenTint = colors.kart)
        } else {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimgeZemini(icon = icon, background = bg, tint = tint)
                    DozSaati(item.time.format(timeFormatter))
                }
                Text(item.medication.name, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    DozEylemi(item.status, onTaken = onTaken, onUndo = onUndo, takenColor = colors.basari, takenTint = colors.kart)
                }
            }
        }
    }
}

/** Fontun yazı ölçeği bu değeri geçince satırlar dikey düzene geçer (erişilebilirlik). */
const val BUYUK_YAZI_ESIGI = 1.3f

@Composable
private fun DozSaati(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.widthIn(min = 44.dp),
    )
}

/** Doz durumu düğmesi: bekliyor → "Aldım", alındı → yeşil onay (dokununca geri alır), kaçtı/atlandı → rozet. */
@Composable
private fun DozEylemi(status: DoseStatus, onTaken: () -> Unit, onUndo: () -> Unit, takenColor: Color, takenTint: Color) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val takenLabel = stringResource(R.string.today_dose_taken_a11y)
    AnimatedContent(
        targetState = status,
        transitionSpec = {
            (scaleIn(AylaMotion.spatial(), initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.8f) + fadeOut())
        },
        label = "dozDurum",
    ) { current ->
        when (current) {
            DoseStatus.TAKEN -> Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(takenColor, RoundedCornerShape(30))
                    .clickable(onClick = onUndo, onClickLabel = stringResource(R.string.action_undo))
                    .semantics { contentDescription = takenLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = takenTint)
            }
            DoseStatus.MISSED -> DurumRozeti(
                text = stringResource(R.string.today_dose_missed),
                icon = Icons.Outlined.Schedule,
                background = colors.uyariAcik,
                tint = colors.uyari,
                onClick = onTaken,
            )
            DoseStatus.SKIPPED -> DurumRozeti(
                text = stringResource(R.string.today_dose_skipped),
                icon = Icons.Outlined.Schedule,
                background = colors.cukur,
                tint = scheme.onSurfaceVariant,
                onClick = onTaken,
            )
            DoseStatus.PENDING -> Button(
                onClick = onTaken,
                modifier = Modifier.height(48.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text(stringResource(R.string.action_taken), maxLines = 1)
            }
        }
    }
}

@Composable
private fun DurumRozeti(text: String, icon: ImageVector, background: Color, tint: Color, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = background,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

/** Alınmış dozun kısa rozeti (onay ikonu ve saat); Bugün ekranında geçmiş dozlar için. */
@Composable
fun AlindiRozeti(timeText: String) {
    val colors = LocalAylaColors.current
    Surface(shape = CircleShape, color = colors.basariAcik) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.basari, modifier = Modifier.size(18.dp))
            Text(timeText, style = MaterialTheme.typography.labelMedium, color = colors.basari)
        }
    }
}

/** Su satırı: bardak sayısı, ilerleme çubuğu ve tek dokunuşla ekleme. */
@Composable
fun SuSatiri(cups: Int, goal: Int, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    SatirZemini(modifier = modifier) {
        SimgeZemini(icon = Icons.Rounded.WaterDrop, background = scheme.secondaryContainer, tint = scheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.today_water_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (cups >= goal) stringResource(R.string.today_water_goal_done) else stringResource(R.string.today_water_progress, cups, goal),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (cups.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = scheme.secondary,
                trackColor = colors.cukur,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        FilledIconButton(
            onClick = onAdd,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.today_water_add))
        }
    }
}

enum class BantTuru { BILGI, DIKKAT, HATA }

/** Uyarı bandı: ikon, başlık, gövde ve isteğe bağlı eylem. Korku dili yok; ne oldu, ne yapılabilir. */
@Composable
fun UyariBandi(
    tur: BantTuru,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val (bg, accent, icon) = when (tur) {
        BantTuru.BILGI -> Triple(colors.lavantaAcik, colors.lavanta, Icons.Outlined.Info)
        BantTuru.DIKKAT -> Triple(colors.uyariAcik, colors.uyari, Icons.Outlined.WarningAmber)
        BantTuru.HATA -> Triple(scheme.errorContainer, scheme.error, Icons.Outlined.WarningAmber)
    }
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = bg) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                if ((actionLabel != null && onAction != null) || (dismissLabel != null && onDismiss != null)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (actionLabel != null && onAction != null) {
                            TextButton(onClick = onAction, contentPadding = ButtonDefaults.TextButtonContentPadding, modifier = Modifier.widthIn(min = 48.dp)) {
                                Text(actionLabel, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (dismissLabel != null && onDismiss != null) {
                            TextButton(onClick = onDismiss, contentPadding = ButtonDefaults.TextButtonContentPadding, modifier = Modifier.widthIn(min = 48.dp)) {
                                Text(dismissLabel, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
