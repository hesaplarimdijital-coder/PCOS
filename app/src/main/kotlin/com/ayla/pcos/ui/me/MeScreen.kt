package com.ayla.pcos.ui.me

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.ui.platform.LocalContext
import com.ayla.pcos.ui.components.shareText
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.SimgeZemini
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.format.TextStyle
import java.time.DayOfWeek

/** Ben: döngü düzeni, takviye uyumu, su ve kayıt alışkanlığı; ayarlara giriş. Sayılar yargılamaz, gösterir. */
@Composable
fun MeScreen(onOpenSettings: () -> Unit, viewModel: MeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Box(Modifier.fillMaxSize())
        return
    }
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.note_share)
    MeContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onShareDoctorNote = { viewModel.prepareDoctorNote { title, text -> shareText(context, title, text, shareTitle) } },
    )
}

/** Durumsuz Ben içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun MeContent(state: MeUiState, onOpenSettings: () -> Unit, onShareDoctorNote: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Overline(stringResource(R.string.me_title))
            Text(
                state.displayName.ifBlank { stringResource(R.string.me_title_anon) },
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        // Döngü düzeni
        AylaCard {
            Text(stringResource(R.string.me_cycle_section), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            val trendText = when (val t = state.trend) {
                CycleTrend.NeedData -> stringResource(R.string.me_cycle_need_data)
                is CycleTrend.Shortening -> stringResource(R.string.me_cycle_trend_shorter, t.count) + " " + stringResource(R.string.me_cycle_trend_text)
                is CycleTrend.Stable -> stringResource(R.string.me_cycle_stable, t.mean, t.deviation)
                is CycleTrend.Variable -> stringResource(R.string.me_cycle_variable, t.shortest, t.longest)
            }
            Text(trendText, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            if (state.cycleLengths.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                DonguCubuklari(lengths = state.cycleLengths)
            }
        }

        // Takviye uyumu
        if (state.hasMedications) {
            OlcutKarti(
                icon = Icons.Rounded.Medication,
                background = scheme.primaryContainer,
                tint = scheme.primary,
                title = stringResource(R.string.me_adherence),
                value = stringResource(R.string.meds_adherence_text, state.adherence.scheduled, state.adherence.taken),
                fraction = state.adherence.ratio,
                barColor = scheme.primary,
            )
        }

        // Su hedefi ve günlük kayıt: bu hafta
        OlcutKarti(
            icon = Icons.Rounded.WaterDrop,
            background = scheme.secondaryContainer,
            tint = scheme.secondary,
            title = stringResource(R.string.me_water_days),
            value = stringResource(R.string.me_days_this_week, state.waterDaysThisWeek),
            fraction = state.waterDaysThisWeek / 7f,
            barColor = scheme.secondary,
        )
        OlcutKarti(
            icon = Icons.Rounded.EditNote,
            background = colors.lavantaAcik,
            tint = colors.lavanta,
            title = stringResource(R.string.me_log_days),
            value = stringResource(R.string.me_days_this_week, state.logDaysThisWeek),
            fraction = state.logDaysThisWeek / 7f,
            barColor = colors.lavanta,
        ) {
            Spacer(Modifier.height(12.dp))
            Overline(stringResource(R.string.me_last_4_weeks))
            Spacer(Modifier.height(6.dp))
            KayitIzgarasi(cells = state.logGrid, filled = colors.lavanta, empty = colors.cukur)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.me_log_grid_a11y, state.logGrid.count { it == true }),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        SatirZemini(onClick = onShareDoctorNote) {
            SimgeZemini(icon = Icons.Rounded.Description, background = scheme.primaryContainer, tint = scheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.me_doctor_note), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.me_doctor_note_text), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.Rounded.IosShare, contentDescription = null, tint = scheme.onSurfaceVariant)
        }

        SatirZemini(onClick = onOpenSettings) {
            SimgeZemini(icon = Icons.Rounded.Settings, background = scheme.secondaryContainer, tint = scheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.me_settings), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.me_settings_text), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }

        Text(
            stringResource(R.string.onboarding_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@Composable
private fun OlcutKarti(
    icon: ImageVector,
    background: Color,
    tint: Color,
    title: String,
    value: String,
    fraction: Float,
    barColor: Color,
    extra: (@Composable () -> Unit)? = null,
) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), animationSpec = AylaMotion.slow(), label = "olcut")
    AylaCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimgeZemini(icon = icon, background = background, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = barColor,
            trackColor = LocalAylaColors.current.cukur,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        extra?.invoke()
    }
}

/**
 * Son döngü uzunlukları: 21–35 gün hedef bandı arkada açık yeşil, çubuklar üstünde.
 * Sayılar çubuk altında yazılır; renk tek başına bilgi taşımaz.
 */
@Composable
private fun DonguCubuklari(lengths: List<Int>) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val maxLen = maxOf(45, lengths.max())
    val a11y = stringResource(R.string.me_cycle_lengths_a11y, lengths.joinToString(", "))
    val bar = scheme.primary.copy(alpha = 0.85f)
    val band = colors.basariAcik
    val bandLine = colors.basari
    Column(modifier = Modifier.semantics { contentDescription = a11y }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            val h = size.height
            fun y(days: Int): Float = h - (days.toFloat() / maxLen) * h
            // Hedef bandı 21–35
            drawRect(band, topLeft = Offset(0f, y(35)), size = Size(size.width, y(21) - y(35)))
            drawLine(bandLine.copy(alpha = 0.5f), Offset(0f, y(35)), Offset(size.width, y(35)), strokeWidth = 1f)
            drawLine(bandLine.copy(alpha = 0.5f), Offset(0f, y(21)), Offset(size.width, y(21)), strokeWidth = 1f)
            val slot = size.width / lengths.size
            val barWidth = (slot * 0.34f).coerceAtMost(18.dp.toPx())
            lengths.forEachIndexed { index, len ->
                val left = slot * index + (slot - barWidth) / 2f
                val top = y(len)
                drawRoundRect(
                    color = bar,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, h - top),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            lengths.forEach { len ->
                Text(
                    len.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(width = 18.dp, height = 10.dp).background(band, RoundedCornerShape(3.dp)))
            Text(stringResource(R.string.me_cycle_band), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

/** 4 hafta × 7 gün nokta ızgarası (Pazartesi başlar): kayıt olan günler dolu, gelecek günler boş halka. */
@Composable
private fun KayitIzgarasi(cells: List<Boolean?>, filled: Color, empty: Color) {
    val scheme = MaterialTheme.colorScheme
    val tr = java.util.Locale.forLanguageTag("tr")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            (1..7).map(DayOfWeek::of).forEach { dow ->
                Text(
                    dow.getDisplayName(TextStyle.NARROW, tr),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { on ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when (on) {
                            true -> Box(Modifier.size(14.dp).background(filled, CircleShape))
                            false -> Box(Modifier.size(14.dp).background(empty, CircleShape))
                            null -> Box(Modifier.size(14.dp).border(1.dp, empty, CircleShape))
                        }
                    }
                }
            }
        }
    }
}
