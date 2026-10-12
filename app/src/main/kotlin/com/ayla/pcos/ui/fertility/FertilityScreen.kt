package com.ayla.pcos.ui.fertility

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.cycle.CycleFertility
import com.ayla.cycle.FertilityNote
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.EkranBasligi
import com.ayla.pcos.ui.components.EvidenceLevel
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.components.YaziOlcegiSiniri
import com.ayla.pcos.ui.components.yaziOlcekli
import com.ayla.pcos.ui.plan.BaslikVeKanit
import com.ayla.pcos.ui.plan.BilgiKarti
import com.ayla.pcos.ui.plan.BolumBasligi
import com.ayla.pcos.ui.plan.IcerikKarti
import com.ayla.pcos.ui.plan.KaynakSatiri
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Doğurganlık ekranı (#24): ısı grafiği, döngü notları ve kaynaklı bilgi kartları. Kayıt ve paylaşım içindir. */
@Composable
fun FertilityScreen(onBack: () -> Unit, onOpenLog: (LocalDate) -> Unit, viewModel: FertilityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.loaded) return
    FertilityContent(
        state = state,
        onBack = onBack,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onAddToday = { onOpenLog(state.today) },
    )
}

private val tr: Locale = Locale.forLanguageTag("tr")
private val longDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", tr)

/** Durumsuz içerik; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun FertilityContent(
    state: FertilityUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddToday: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EkranBasligi(stringResource(R.string.fertility_title), onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.fertility_not_contraception),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                state.disabled -> UyariBandi(
                    tur = BantTuru.BILGI,
                    title = stringResource(R.string.fertility_disabled_pill_title),
                    text = stringResource(R.string.fertility_disabled_pill_text),
                )
                else -> {
                    if (!state.ttcGoal) {
                        UyariBandi(BantTuru.BILGI, stringResource(R.string.fertility_open_title), stringResource(R.string.fertility_enable_hint))
                    }
                    val cycle = state.cycle
                    if (cycle == null) {
                        UyariBandi(BantTuru.BILGI, stringResource(R.string.fertility_no_period_title), stringResource(R.string.fertility_no_period_text))
                    } else {
                        DonguKarti(state, cycle, onPrevious, onNext, onAddToday)
                        val notes = cycle.notes
                        if (notes.isNotEmpty()) {
                            BolumBasligi(stringResource(R.string.fertility_notes_title))
                            notes.forEach { NotKarti(it) }
                        }
                    }
                }
            }
            BolumBasligi(stringResource(R.string.fertility_info_title))
            BilgiKartlari()
            Text(
                stringResource(R.string.fertility_not_diagnosis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DonguKarti(state: FertilityUiState, cycle: CycleFertility, onPrevious: () -> Unit, onNext: () -> Unit, onAddToday: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AylaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = state.canGoPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.fertility_cycle_previous))
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (cycle.isOpen) stringResource(R.string.fertility_cycle_current) else stringResource(R.string.fertility_cycle_label, cycle.start.format(longDate)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.fertility_cycle_position, state.selected + 1, state.cycles.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onNext, enabled = state.canGoNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.fertility_cycle_next))
            }
        }
        val status = cycle.lutealDay?.let { stringResource(R.string.fertility_luteal_day, it) }
            ?: state.cycleDay?.let { stringResource(R.string.fertility_cycle_day, it) }
        if (status != null) {
            Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        if (cycle.readings.isEmpty()) {
            Text(stringResource(R.string.fertility_chart_empty), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        } else {
            IsiGrafigi(IsiGrafigiOlcegi.of(cycle, state.today), modifier = Modifier.fillMaxWidth().height(200.dp))
            Spacer(Modifier.height(8.dp))
            Lejant(showCoverline = cycle.coverlineCelsius != null, showLh = cycle.lhPositiveDates.isNotEmpty(), showOvulation = cycle.ovulation != null)
        }
        cycle.ovulation?.let { ov ->
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.fertility_ovulation_estimate, ov.ovulationDate.format(longDate)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.fertility_confirmed_on, ov.confirmedOn.format(longDate)), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        if (state.showsCurrent) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAddToday, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(" " + stringResource(R.string.fertility_add_today))
            }
        }
    }
}

/**
 * Isı grafiği: geçerli ölçüm dolu daire, hesaba girmeyen içi boş, örtü çizgisi kesikli, LH pozitif günler eksenin
 * üstünde küçük kare, tahmini yumurtlama günü ince dikey çizgi. Renk tek başına anlam taşımaz (biçim + lejant).
 * Etiket yazısı en fazla 1,3 ölçeğe büyür ki eksen taşmasın; ekran okuyucuya özet metin verilir.
 */
@Composable
private fun IsiGrafigi(olcek: IsiGrafigiOlcegi, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    val excludedCount = olcek.points.count { it.excluded }
    val coverlineText = olcek.coverline?.let { stringResource(R.string.fertility_chart_coverline_a11y, String.format(tr, "%.2f", it)) }
        ?: stringResource(R.string.fertility_chart_a11y_coverline_none)
    val a11y = stringResource(R.string.fertility_chart_a11y, olcek.points.size, excludedCount, coverlineText)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)
    val pointColor = scheme.primary
    val lineColor = scheme.primary.copy(alpha = 0.6f)
    val gridColor = scheme.outlineVariant
    val coverColor = colors.lavanta
    val lhColor = colors.kayisiCanli
    YaziOlcegiSiniri(max = 1.3f) {
        val measurer = rememberTextMeasurer()
        Canvas(modifier = modifier.semantics { contentDescription = a11y }) {
            val yLabels = olcek.yTicks.map { String.format(tr, "%.1f", it) }
            val labelWidth = yLabels.maxOf { measurer.measure(it, labelStyle).size.width }.toFloat()
            val left = labelWidth + 8.dp.toPx()
            val bottomLabelHeight = measurer.measure("1", labelStyle).size.height.toFloat()
            val bottom = size.height - bottomLabelHeight - 6.dp.toPx()
            val top = 6.dp.toPx()
            val right = size.width - 8.dp.toPx()
            fun x(day: Int) = left + olcek.xOf(day) * (right - left)
            fun y(c: Double) = bottom - olcek.yOf(c) * (bottom - top)

            // Izgara ve y etiketleri
            olcek.yTicks.forEachIndexed { i, t ->
                val yy = y(t)
                drawLine(gridColor, Offset(left, yy), Offset(right, yy), strokeWidth = 1f)
                val layout = measurer.measure(yLabels[i], labelStyle)
                drawText(layout, topLeft = Offset(left - 6.dp.toPx() - layout.size.width, yy - layout.size.height / 2f))
            }
            olcek.xTicks.forEach { d ->
                val layout = measurer.measure(d.toString(), labelStyle)
                drawText(layout, topLeft = Offset(x(d) - layout.size.width / 2f, bottom + 4.dp.toPx()))
            }
            // Yumurtlama günü
            olcek.ovulationDay?.let { d -> drawLine(scheme.onSurfaceVariant, Offset(x(d), top), Offset(x(d), bottom), strokeWidth = 1.dp.toPx()) }
            // Örtü çizgisi
            olcek.coverline?.let { c ->
                drawLine(
                    coverColor,
                    Offset(left, y(c)),
                    Offset(right, y(c)),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
                )
            }
            // Çizgi parçaları
            olcek.segments.filter { it.size > 1 }.forEach { seg ->
                val path = Path().apply {
                    moveTo(x(seg.first().day), y(seg.first().celsius))
                    seg.drop(1).forEach { lineTo(x(it.day), y(it.celsius)) }
                }
                drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
            }
            // Noktalar
            val r = 4.dp.toPx()
            olcek.points.forEach { p ->
                val center = Offset(x(p.day), y(p.celsius))
                if (p.excluded) {
                    drawCircle(scheme.surface, r, center)
                    drawCircle(pointColor, r, center, style = Stroke(width = 1.5.dp.toPx()))
                } else {
                    drawCircle(pointColor, r, center)
                }
            }
            // LH pozitif günler: eksenin hemen üstünde kare
            val sq = 7.dp.toPx()
            olcek.lhDays.forEach { d -> drawRect(lhColor, Offset(x(d) - sq / 2, bottom - sq - 2.dp.toPx()), Size(sq, sq)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Lejant(showCoverline: Boolean, showLh: Boolean, showOvulation: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LejantOgesi(stringResource(R.string.fertility_legend_valid)) { drawCircle(scheme.primary, size.minDimension / 3) }
        LejantOgesi(stringResource(R.string.fertility_chart_excluded)) {
            drawCircle(scheme.primary, size.minDimension / 3, style = Stroke(width = 1.5.dp.toPx()))
        }
        if (showCoverline) {
            LejantOgesi(stringResource(R.string.fertility_chart_coverline)) {
                drawLine(
                    colors.lavanta,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f),
                )
            }
        }
        if (showLh) {
            LejantOgesi(stringResource(R.string.fertility_chart_lh_marker)) {
                val s = size.minDimension * 0.6f
                drawRect(colors.kayisiCanli, Offset((size.width - s) / 2, (size.height - s) / 2), Size(s, s))
            }
        }
        if (showOvulation) {
            LejantOgesi(stringResource(R.string.fertility_legend_ovulation)) {
                drawLine(scheme.onSurfaceVariant, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun LejantOgesi(label: String, glyph: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(yaziOlcekli(16.dp)), onDraw = glyph)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Döngü notu: başlık, metin, kanıt düzeyi ve kaynak (docs/arastirma/dogurganlik-icerigi.md). */
@Composable
private fun NotKarti(note: FertilityNote) {
    val (title, text, source, level) = notIcerigi(note)
    AylaCard {
        BaslikVeKanit(stringResource(title), level)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        KaynakSatiri(source)
    }
}

private data class NotIcerigi(val title: Int, val text: Int, val source: Int, val level: EvidenceLevel)

private fun notIcerigi(note: FertilityNote): NotIcerigi = when (note) {
    FertilityNote.OVULATION_CONFIRMED -> NotIcerigi(
        R.string.fertility_note_confirmed_title, R.string.fertility_note_confirmed_text, R.string.fertility_note_confirmed_source, EvidenceLevel.MODERATE,
    )
    FertilityNote.TOO_FEW_READINGS -> NotIcerigi(
        R.string.fertility_note_too_few_title, R.string.fertility_note_too_few_text, R.string.fertility_note_too_few_source, EvidenceLevel.EXPERT_OPINION,
    )
    FertilityNote.MANY_DISTURBED -> NotIcerigi(
        R.string.fertility_note_many_disturbed_title, R.string.fertility_note_many_disturbed_text, R.string.fertility_note_many_disturbed_source,
        EvidenceLevel.EXPERT_OPINION,
    )
    FertilityNote.LH_POSITIVE_WITHOUT_RISE -> NotIcerigi(
        R.string.fertility_note_lh_without_rise_title, R.string.fertility_note_lh_without_rise_text, R.string.fertility_note_lh_without_rise_source,
        EvidenceLevel.LIMITED,
    )
    FertilityNote.RISE_WITHOUT_LH_POSITIVE -> NotIcerigi(
        R.string.fertility_note_rise_without_lh_title, R.string.fertility_note_rise_without_lh_text, R.string.fertility_note_rise_without_lh_source,
        EvidenceLevel.MODERATE,
    )
    FertilityNote.LONG_LUTEAL -> NotIcerigi(
        R.string.fertility_note_long_luteal_title, R.string.fertility_note_long_luteal_text, R.string.fertility_note_long_luteal_source, EvidenceLevel.MODERATE,
    )
}

/** Kaynaklı bilgi kartları; kanıt düzeyleri dogurganlik-icerigi.md tablosundan. */
internal object DogurganlikIcerigi {
    val intro = BilgiKarti(R.string.fertility_intro_title, R.string.fertility_intro_text, EvidenceLevel.STRONG, R.string.fertility_intro_source)
    val bbtHow = BilgiKarti(R.string.fertility_bbt_how_title, R.string.fertility_bbt_step_1, EvidenceLevel.EXPERT_OPINION, R.string.fertility_bbt_how_source)
    val bbtSteps = listOf(R.string.fertility_bbt_step_2, R.string.fertility_bbt_step_3, R.string.fertility_bbt_step_4, R.string.fertility_bbt_step_5)
    val bbtWhy = BilgiKarti(R.string.fertility_bbt_why_title, R.string.fertility_bbt_why_text, EvidenceLevel.MODERATE, R.string.fertility_bbt_why_source)
    val rule = BilgiKarti(R.string.fertility_rule_title, R.string.fertility_rule_text, EvidenceLevel.MODERATE, R.string.fertility_rule_source)
    val imprecise = BilgiKarti(R.string.fertility_bbt_imprecise_title, R.string.fertility_bbt_imprecise_text, EvidenceLevel.MODERATE, R.string.fertility_bbt_imprecise_source)
    val lhHow = BilgiKarti(R.string.fertility_lh_how_title, R.string.fertility_lh_how_text, EvidenceLevel.EXPERT_OPINION, R.string.fertility_lh_how_source)
    val lhPositive = BilgiKarti(
        R.string.fertility_lh_positive_meaning_title, R.string.fertility_lh_positive_meaning_text, EvidenceLevel.MODERATE, R.string.fertility_lh_positive_meaning_source,
    )
    val lhPcos = BilgiKarti(R.string.fertility_lh_pcos_title, R.string.fertility_lh_pcos_text, EvidenceLevel.LIMITED, R.string.fertility_lh_pcos_source)
    val mucus = BilgiKarti(R.string.fertility_mucus_title, R.string.fertility_mucus_text, EvidenceLevel.LIMITED, R.string.fertility_mucus_source)
    val irregular = BilgiKarti(R.string.fertility_irregular_title, R.string.fertility_irregular_text, EvidenceLevel.STRONG, R.string.fertility_irregular_source)
    val regularAnovulation = BilgiKarti(
        R.string.fertility_regular_anovulation_title, R.string.fertility_regular_anovulation_text, EvidenceLevel.STRONG, R.string.fertility_regular_anovulation_source,
    )
    val hope = BilgiKarti(R.string.fertility_hope_title, R.string.fertility_hope_text, EvidenceLevel.STRONG, R.string.fertility_hope_source)
    val doctorWhen = BilgiKarti(R.string.fertility_doctor_when_title, R.string.fertility_doctor_when_text, EvidenceLevel.STRONG, R.string.fertility_doctor_when_source)
    val pregnancyTest = BilgiKarti(
        R.string.fertility_pregnancy_test_title, R.string.fertility_pregnancy_test_text, EvidenceLevel.STRONG, R.string.fertility_pregnancy_test_source,
    )
}

@Composable
private fun ColumnScope.BilgiKartlari() {
    val c = DogurganlikIcerigi
    IcerikKarti(c.intro)
    // Adımlar madde listesi: ilk adım da madde olsun diye kart elle kurulur.
    AylaCard {
        BaslikVeKanit(stringResource(c.bbtHow.title), c.bbtHow.evidence)
        (listOf(c.bbtHow.text) + c.bbtSteps).forEach { step ->
            Text("• " + stringResource(step), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        KaynakSatiri(c.bbtHow.source)
    }
    IcerikKarti(c.rule)
    IcerikKarti(c.bbtWhy)
    IcerikKarti(c.imprecise)
    IcerikKarti(c.lhHow) {
        Text(stringResource(R.string.fertility_lh_how_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
    IcerikKarti(c.lhPcos)
    IcerikKarti(c.lhPositive)
    IcerikKarti(c.mucus) {
        Text(stringResource(R.string.fertility_mucus_pcos_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
    IcerikKarti(c.irregular)
    IcerikKarti(c.regularAnovulation)
    IcerikKarti(c.hope)
    IcerikKarti(c.doctorWhen)
    IcerikKarti(c.pregnancyTest)
}
