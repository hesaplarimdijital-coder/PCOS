package com.ayla.pcos.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.cycle.DayKind
import com.ayla.cycle.ExclusionReason
import com.ayla.pcos.R
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.Overline
import com.ayla.pcos.ui.components.SatirZemini
import com.ayla.pcos.ui.components.SectionHeader
import com.ayla.pcos.ui.components.toTurkishUpper
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import com.ayla.pcos.ui.theme.LocalReducedMotion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val trLocale: Locale = Locale.forLanguageTag("tr")
private val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", trLocale)
private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", trLocale)
private val dayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", trLocale)

/**
 * Takvim: ay ızgarası (kayıtlı adet, tahmini aralık, günlük kayıt izi), seçili gün eylemleri
 * ve döngü geçmişi. Tahmin gizliyse aralık hiç çizilmez.
 */
@Composable
fun CalendarScreen(onOpenLog: (LocalDate) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Box(Modifier.fillMaxSize())
        return
    }
    CalendarContent(
        state = state,
        onPrevious = { viewModel.previousMonth() },
        onNext = { viewModel.nextMonth() },
        onToday = { viewModel.goToToday() },
        onSelect = { viewModel.select(it) },
        onOpenLog = onOpenLog,
        onMarkStart = { viewModel.markPeriodStart(it) },
        onMarkEnd = { viewModel.markPeriodEnd(it) },
        onUnmark = { viewModel.unmarkPeriod(it) },
        onExcluded = { period, excluded -> viewModel.setExcluded(period, excluded) },
    )
}

/** Durumsuz takvim içeriği; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun CalendarContent(
    state: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onOpenLog: (LocalDate) -> Unit,
    onMarkStart: (LocalDate) -> Unit,
    onMarkEnd: (LocalDate) -> Unit,
    onUnmark: (PeriodEntity) -> Unit,
    onExcluded: (PeriodEntity, Boolean) -> Unit,
) {
    // Ay geçişi sırasında eski ay da çizilebilsin diye hücreler ay bazında hatırlanır.
    val cache = remember { mutableStateMapOf<YearMonth, List<List<CalendarDay>>>() }
    SideEffect { cache[state.month] = state.weeks }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Overline(state.month.format(monthYear))
                Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = onToday) {
                Icon(Icons.Rounded.Today, contentDescription = stringResource(R.string.label_today))
            }
        }

        Spacer(Modifier.height(8.dp))
        AyGezinme(month = state.month, onPrevious = onPrevious, onNext = onNext)
        Spacer(Modifier.height(8.dp))
        HaftaBasliklari()

        val reduced = LocalReducedMotion.current
        AnimatedContent(
            targetState = state.month,
            transitionSpec = {
                if (reduced) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    val forward = targetState > initialState
                    (slideInHorizontally(AylaMotion.enter(360)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(140)))
                }
            },
            label = "ayGecisi",
        ) { targetMonth ->
            val weeks = cache[targetMonth] ?: if (targetMonth == state.month) state.weeks else emptyList()
            AyIzgarasi(weeks = weeks, selected = state.selected, onSelect = onSelect)
        }

        Spacer(Modifier.height(10.dp))
        Lejant(showPredicted = !state.hidePrediction)

        AnimatedVisibility(
            visible = state.selected != null && state.actions != null,
            enter = expandVertically(AylaMotion.spatial()) + fadeIn(),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
        ) {
            val date = state.selected
            val actions = state.actions
            if (date != null && actions != null) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    SeciliGunKarti(
                        date = date,
                        day = state.weeks.flatten().firstOrNull { it.date == date },
                        actions = actions,
                        onOpenLog = { onOpenLog(date) },
                        onMarkStart = { onMarkStart(date) },
                        onMarkEnd = { onMarkEnd(date) },
                        onUnmark = { actions.period?.let(onUnmark) },
                        onExcluded = { excluded -> actions.period?.let { onExcluded(it, excluded) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader(title = stringResource(R.string.calendar_history_title))
        Spacer(Modifier.height(8.dp))
        val stats = state.stats
        val mean = stats?.mean
        if (stats == null || mean == null) {
            Text(
                stringResource(R.string.calendar_stats_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(
                    R.string.calendar_stats,
                    mean.roundToInt(),
                    stats.shortest ?: 0,
                    stats.longest ?: 0,
                    stats.periodsInTrailingYear,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.history.forEach { row ->
                DonguSatiri(
                    row = row,
                    onExcluded = { excluded -> row.period?.let { onExcluded(it, excluded) } },
                )
            }
        }
    }
}

@Composable
private fun AyGezinme(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.calendar_prev_month))
        }
        Text(
            month.format(monthYear).replaceFirstChar { it.titlecase(trLocale) },
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.calendar_next_month))
        }
    }
}

@Composable
private fun HaftaBasliklari() {
    Row(modifier = Modifier.fillMaxWidth()) {
        (1..7).map(DayOfWeek::of).forEach { dow ->
            Text(
                dow.getDisplayName(TextStyle.SHORT, trLocale).trimEnd('.').toTurkishUpper(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AyIzgarasi(weeks: List<List<CalendarDay>>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    GunHucresi(
                        day = day,
                        selected = day.date == selected,
                        onClick = { onSelect(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Gün hücresi. Renk tek başına anlam taşımaz: adet günleri dolu daire, tahmin günleri kesik
 * çizgili daire, kayıtlı günler altta nokta; erişilebilirlik metni de aynı bilgiyi verir.
 */
@Composable
private fun GunHucresi(day: CalendarDay, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val dateText = day.date.format(dayMonth)
    val a11y = when (day.kind) {
        DayKind.PERIOD -> stringResource(R.string.calendar_day_a11y_period, dateText)
        DayKind.PREDICTED -> stringResource(R.string.calendar_day_a11y_predicted, dateText)
        DayKind.NONE -> dateText
    }
    val fill = when (day.kind) {
        DayKind.PERIOD -> colors.fazAdet
        DayKind.PREDICTED -> colors.fazAdetAcik
        DayKind.NONE -> Color.Transparent
    }
    val textColor = when {
        day.kind == DayKind.PERIOD -> scheme.onPrimary
        day.isFuture -> scheme.onSurfaceVariant
        day.isToday -> scheme.primary
        else -> scheme.onSurface
    }
    val outline = scheme.primary
    val selectedRing = scheme.onSurface
    val dashed = colors.fazAdet

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .alpha(if (day.inMonth) 1f else 0.38f)
            .selectable(selected = selected, onClick = onClick, role = Role.Button)
            .semantics { contentDescription = a11y },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .drawBehind {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (fill != Color.Transparent) drawCircle(fill, radius = radius, center = center)
                    if (day.kind == DayKind.PREDICTED) {
                        drawCircle(
                            dashed,
                            radius = radius - 1.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))),
                        )
                    }
                    if (day.isToday && day.kind != DayKind.PERIOD) {
                        drawCircle(outline, radius = radius - 0.75.dp.toPx(), center = center, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    if (selected) {
                        drawCircle(selectedRing, radius = radius + 1.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || day.isPeriodStart) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
            )
            if (day.hasLog) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .background(if (day.kind == DayKind.PERIOD) scheme.onPrimary else colors.lavanta, CircleShape),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Lejant(showPredicted: Boolean) {
    val colors = LocalAylaColors.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LejantOgesi(stringResource(R.string.calendar_legend_period)) {
            Box(Modifier.size(12.dp).background(colors.fazAdet, CircleShape))
        }
        if (showPredicted) {
            LejantOgesi(stringResource(R.string.calendar_legend_predicted)) {
                val dashed = colors.fazAdet
                Box(
                    Modifier.size(12.dp).background(colors.fazAdetAcik, CircleShape).drawBehind {
                        drawCircle(dashed, radius = size.minDimension / 2f - 1f, style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))))
                    },
                )
            }
        }
        LejantOgesi(stringResource(R.string.calendar_legend_log)) {
            Box(Modifier.size(6.dp).background(colors.lavanta, CircleShape))
        }
    }
}

@Composable
private fun LejantOgesi(label: String, marker: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) { marker() }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeciliGunKarti(
    date: LocalDate,
    day: CalendarDay?,
    actions: DayActions,
    onOpenLog: () -> Unit,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onUnmark: () -> Unit,
    onExcluded: (Boolean) -> Unit,
) {
    val title = date.dayOfWeek.getDisplayName(TextStyle.FULL, trLocale).replaceFirstChar { it.titlecase(trLocale) } + ", " + date.format(dayMonth)
    val subtitle = when (day?.kind) {
        DayKind.PERIOD -> stringResource(R.string.calendar_legend_period)
        DayKind.PREDICTED -> stringResource(R.string.calendar_legend_predicted)
        else -> null
    }
    AylaCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (actions.canOpenLog) {
                Button(onClick = onOpenLog) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.calendar_open_log))
                }
            }
            if (actions.canMarkStart) {
                FilledTonalButton(onClick = onMarkStart) { Text(stringResource(R.string.calendar_mark_period_start)) }
            }
            if (actions.canMarkEnd) {
                FilledTonalButton(onClick = onMarkEnd) { Text(stringResource(R.string.calendar_mark_period_end)) }
            }
            if (actions.canUnmark) {
                TextButton(onClick = onUnmark) {
                    Text(stringResource(R.string.calendar_unmark_period), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        val cycle = actions.cycle
        val period = actions.period
        if (cycle != null && period != null && cycle.isComplete) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(if (period.excludedFromStats) R.string.calendar_include_cycle else R.string.calendar_exclude_cycle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = period.excludedFromStats, onCheckedChange = onExcluded)
            }
        }
    }
}

/** Döngü geçmişi satırı: başlangıç, uzunluk, hesaba katılma durumu. */
@Composable
private fun DonguSatiri(row: CycleRow, onExcluded: (Boolean) -> Unit) {
    val colors = LocalAylaColors.current
    val cycle = row.cycle
    val length = cycle.lengthDays ?: return
    val excluded = cycle.exclusion != null
    SatirZemini {
        Column(modifier = Modifier.weight(1f)) {
            Text(cycle.start.format(dayMonthYear), style = MaterialTheme.typography.titleMedium)
            val badge = when (cycle.exclusion) {
                ExclusionReason.USER -> stringResource(R.string.calendar_excluded_user)
                ExclusionReason.IMPLAUSIBLY_SHORT -> stringResource(R.string.calendar_excluded_short)
                null -> null
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.calendar_cycle_length, length),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (badge != null) {
                    Surface(shape = CircleShape, color = colors.uyariAcik) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.uyari,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        if (row.period != null) {
            Switch(
                checked = !excluded,
                onCheckedChange = { include -> onExcluded(!include) },
                enabled = cycle.exclusion != ExclusionReason.IMPLAUSIBLY_SHORT,
            )
        }
    }
}
