package com.ayla.pcos.ui.me

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.SimgeZemini
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import com.ayla.pcos.ui.theme.hareket
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** Ben: döngü düzeni, takviye uyumu, su ve kayıt alışkanlığı; ayarlara giriş. Sayılar yargılamaz, gösterir. */
@Composable
fun MeScreen(
    onOpenSettings: () -> Unit,
    onOpenFertility: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    viewModel: MeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Box(Modifier.fillMaxSize())
        return
    }
    MeContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenReport = onOpenReport,
        onOpenFertility = onOpenFertility,
    )
}

/** Durumsuz Ben içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun MeContent(
    state: MeUiState,
    onOpenSettings: () -> Unit,
    onOpenReport: () -> Unit = {},
    onOpenFertility: () -> Unit = {},
) {
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
                is CycleTrend.Shortening -> stringResource(R.string.me_cycle_trend_shorter, t.count)
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
            KayitIzgarasi(
                cells = state.logGrid,
                firstMonday = state.today.with(DayOfWeek.MONDAY).minusWeeks(3),
                filled = colors.lavanta,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.me_log_grid_a11y, state.logGrid.count { it == true }),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        if (state.showFertility) {
            SatirZemini(onClick = onOpenFertility) {
                SimgeZemini(icon = Icons.Rounded.Thermostat, background = colors.lavantaAcik, tint = colors.lavanta)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.fertility_open_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.fertility_open_text), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
            }
        }

        SatirZemini(onClick = onOpenReport) {
            SimgeZemini(icon = Icons.Rounded.Description, background = scheme.primaryContainer, tint = scheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.report_ui_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.report_ui_entry_text), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }

        SatirZemini(onClick = onOpenSettings) {
            SimgeZemini(icon = Icons.Rounded.Settings, background = scheme.secondaryContainer, tint = scheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.me_settings), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.me_settings_text), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0f, 1f), animationSpec = hareket(AylaMotion.slow()), label = "olcut")
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
    // Bant sınırları sağdaki dar sütunda yazılır; çubuklar ve altlarındaki sayılar o sütuna taşmaz.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.basari)
    val upperLabel = remember(measurer, labelStyle) { measurer.measure(TYPICAL_MAX.toString(), labelStyle) }
    val lowerLabel = remember(measurer, labelStyle) { measurer.measure(TYPICAL_MIN.toString(), labelStyle) }
    val density = LocalDensity.current
    val gutterPx = maxOf(upperLabel.size.width, lowerLabel.size.width) + with(density) { 8.dp.roundToPx() }
    val gutter = with(density) { gutterPx.toDp() }
    Column(modifier = Modifier.semantics { contentDescription = a11y }) {
        Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
            val h = size.height
            val plotWidth = size.width - gutterPx
            fun y(days: Int): Float = h - (days.toFloat() / maxLen) * h
            // Tipik aralık bandı 21–35
            drawRect(band, topLeft = Offset(0f, y(TYPICAL_MAX)), size = Size(plotWidth, y(TYPICAL_MIN) - y(TYPICAL_MAX)))
            drawLine(bandLine.copy(alpha = 0.5f), Offset(0f, y(TYPICAL_MAX)), Offset(plotWidth, y(TYPICAL_MAX)), strokeWidth = 1f)
            drawLine(bandLine.copy(alpha = 0.5f), Offset(0f, y(TYPICAL_MIN)), Offset(plotWidth, y(TYPICAL_MIN)), strokeWidth = 1f)
            listOf(upperLabel to TYPICAL_MAX, lowerLabel to TYPICAL_MIN).forEach { (label, days) ->
                val top = (y(days) - label.size.height / 2f).coerceIn(0f, h - label.size.height)
                drawText(label, topLeft = Offset(size.width - label.size.width, top))
            }
            val slot = plotWidth / lengths.size
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
        Row(modifier = Modifier.fillMaxWidth().padding(end = gutter)) {
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

/**
 * 4 hafta × 7 gün nokta ızgarası (Pazartesi başlar): kayıt olan günler dolu, olmayanlar halka.
 * Her satırın solunda o haftanın Pazartesi tarihi yazar; haftalar böylece birbirinden ayrışır.
 */
@Composable
private fun KayitIzgarasi(cells: List<Boolean?>, firstMonday: LocalDate, filled: Color) {
    val scheme = MaterialTheme.colorScheme
    val ring = scheme.outline
    val future = scheme.outlineVariant
    val tr = java.util.Locale.forLanguageTag("tr")
    val weekLabel = remember { DateTimeFormatter.ofPattern("d MMM", tr) }
    val labels = remember(firstMonday, cells.size) {
        (0 until (cells.size + 6) / 7).map { firstMonday.plusWeeks(it.toLong()).format(weekLabel).trimEnd('.') }
    }
    // Etiket sütunu en uzun tarihin ölçülen genişliği kadar: büyük yazıda "28 Ey" diye kesilmez.
    val labelStyle = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidth = remember(labels, labelStyle, density) {
        with(density) { (labels.maxOfOrNull { measurer.measure(it, labelStyle).size.width } ?: 0).toDp() } + 8.dp
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(labelWidth))
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
        cells.chunked(7).forEachIndexed { index, week ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    labels[index],
                    style = labelStyle,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(labelWidth),
                )
                week.forEach { on ->
                    Box(modifier = Modifier.weight(1f).height(16.dp), contentAlignment = Alignment.Center) {
                        when (on) {
                            // Kayıt var: dolu nokta. Kayıt yok: belirgin halka (≥ 3:1). Henüz gelmedi: soluk küçük nokta.
                            true -> Box(Modifier.size(14.dp).background(filled, CircleShape))
                            false -> Box(Modifier.size(14.dp).border(1.5.dp, ring, CircleShape))
                            null -> Box(Modifier.size(6.dp).background(future, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

/** Tipik döngü aralığı (gün); grafikteki bant ve etiketleri. */
private const val TYPICAL_MIN = 21
private const val TYPICAL_MAX = 35

