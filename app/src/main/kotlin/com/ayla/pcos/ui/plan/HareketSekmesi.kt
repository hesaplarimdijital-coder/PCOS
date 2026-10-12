package com.ayla.pcos.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalReducedMotion

/**
 * Hareket sekmesi (#12): kılavuzdaki haftalık ritim, güvenlik notu ve dört kısa rutin. Döngü kartı evre
 * tahmini yapmaz; kişiselleştirme rızası açık ve hap modu kapalıyken yalnızca kaydedilmiş adet günlerine bakar.
 */
@Composable
internal fun HareketSekmesi(cycleCard: MovementCycleCard, expandedRoutine: String?, onToggleRoutine: (String) -> Unit) {
    IcerikKarti(PlanIcerigi.movementIntro)
    DonguKarti(cycleCard)

    BolumBasligi(stringResource(R.string.plan_section_weekly))
    PlanIcerigi.movementWeekly.forEach { IcerikKarti(it) }

    BolumBasligi(stringResource(R.string.plan_section_routines))
    IcerikKarti(PlanIcerigi.movementSafety)
    PlanIcerigi.routines.forEach { routine ->
        RutinKarti(routine, expanded = routine.key == expandedRoutine, onToggle = { onToggleRoutine(routine.key) })
    }
    IcerikUyarisi()
}

@Composable
private fun DonguKarti(card: MovementCycleCard) {
    var showWhy by rememberSaveable { mutableStateOf(false) }
    IcerikKarti(PlanIcerigi.cycleCard(card)) {
        if (card == MovementCycleCard.OTHER) {
            TextButton(onClick = { showWhy = !showWhy }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.movement_cycle_why_title))
                Icon(if (showWhy) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            }
            Acilir(visible = showWhy) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.movement_cycle_why_text), style = MaterialTheme.typography.bodyMedium)
                    KaynakSatiri(PlanIcerigi.cycleWhy.source)
                }
            }
        }
    }
}

/** Rutin kartı: başlık, süre, kısa açıklama; adımlar isteğe bağlı açılır (numaralı, her biri ayrı satır). */
@Composable
internal fun RutinKarti(routine: Rutin, expanded: Boolean, onToggle: () -> Unit) {
    AylaCard {
        BaslikVeKanit(stringResource(routine.card.title), routine.card.evidence)
        Spacer(Modifier.height(8.dp))
        SureCipi(routine.minutes)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(routine.card.text), style = MaterialTheme.typography.bodyMedium)
        Acilir(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                routine.steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        AdimNumarasi(index + 1)
                        Text(stringResource(step), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (expanded) R.string.routine_steps_hide else R.string.routine_steps_show))
            Spacer(Modifier.width(4.dp))
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
        }
        KaynakSatiri(routine.card.source)
    }
}

/** "10 dk" rozeti; ekran okuyucu "10 dakika" okur. */
@Composable
private fun SureCipi(minutes: Int) {
    val a11y = stringResource(R.string.movement_duration_a11y, minutes)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.clearAndSetSemantics { contentDescription = a11y },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(stringResource(R.string.movement_duration_short, minutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun AdimNumarasi(number: Int) {
    Box(
        modifier = Modifier.size(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(26.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

/** Açılır içerik; "Animasyonları kaldır" açıkken anında. */
@Composable
private fun Acilir(visible: Boolean, content: @Composable () -> Unit) {
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = if (reduced) EnterTransition.None else expandVertically(AylaMotion.spatial()) + fadeIn(),
        exit = if (reduced) ExitTransition.None else shrinkVertically() + fadeOut(),
    ) {
        content()
    }
}
