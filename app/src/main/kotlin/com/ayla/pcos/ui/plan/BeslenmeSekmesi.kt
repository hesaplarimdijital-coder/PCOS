package com.ayla.pcos.ui.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.theme.LocalAylaColors

/**
 * Beslenme sekmesi (#12) ve bitkisel destekler (#13). Kişiye özel öneri yok: herkes aynı ilkeleri,
 * kanıt düzeyi ve kaynağıyla görür. Kalori, tartı, ölçü ya da "yasak" dili kullanılmaz.
 */
@Composable
internal fun BeslenmeSekmesi(onOpenWalkRoutine: () -> Unit) {
    IcerikKarti(PlanIcerigi.nutritionIntro)
    TabakKarti()

    BolumBasligi(stringResource(R.string.plan_section_principles))
    PlanIcerigi.nutritionPrinciples.forEach { IcerikKarti(it) }
    IcerikKarti(PlanIcerigi.walkTip) {
        TextButton(onClick = onOpenWalkRoutine, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.nutrition_walk_tip_action))
        }
    }

    BolumBasligi(stringResource(R.string.herbal_intro_title))
    Text(stringResource(R.string.herbal_intro_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PlanIcerigi.herbal.forEach { BitkiselKart(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.herbal_tea_iron_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ListeDisiKarti()

    IcerikKarti(PlanIcerigi.eatingSupport)
    IcerikUyarisi()
}

/**
 * Tabak modeli: yarısı sebze, çeyreği protein, çeyreği tam tahıl ya da baklagil, yanında su. Dilimler
 * renkle değil, açıklama listesindeki etiketle anlam taşır; çizimin tamamı tek bir açıklamayla okunur.
 */
@Composable
internal fun TabakKarti() {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val vegetables = colors.basariAcik to colors.basari
    val protein = scheme.tertiaryContainer to scheme.tertiary
    val grains = colors.uyariAcik to colors.uyari
    val water = scheme.secondaryContainer to scheme.secondary
    val rim = colors.cukur
    val separator = colors.kart
    val a11y = stringResource(R.string.nutrition_plate_a11y)

    IcerikKarti(PlanIcerigi.plate) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = a11y },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Canvas(modifier = Modifier.size(136.dp)) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(rim, radius = radius, center = center)
                val inner = radius - 8.dp.toPx()
                val topLeft = Offset(center.x - inner, center.y - inner)
                val arcSize = Size(inner * 2f, inner * 2f)
                // 0° saat üçte, saat yönünde: sol yarı 90°→270°, sağ üst 270°→360°, sağ alt 0°→90°.
                listOf(Triple(90f, 180f, vegetables), Triple(270f, 90f, protein), Triple(0f, 90f, grains)).forEach { (start, sweep, pair) ->
                    drawArc(pair.first, start, sweep, useCenter = true, topLeft = topLeft, size = arcSize)
                    drawArc(pair.second, start, sweep, useCenter = true, topLeft = topLeft, size = arcSize, style = Stroke(1.5.dp.toPx()))
                }
                // Dilim aralarında tabak rengi çizgi: dilimler kenarlarıyla da ayrışır.
                val gap = 3.dp.toPx()
                drawLine(separator, Offset(center.x, center.y - inner), Offset(center.x, center.y + inner), strokeWidth = gap)
                drawLine(separator, center, Offset(center.x + inner, center.y), strokeWidth = gap)
            }
            Spacer(Modifier.width(12.dp))
            Canvas(modifier = Modifier.size(width = 30.dp, height = 48.dp).padding(bottom = 6.dp)) {
                val corner = CornerRadius(6.dp.toPx())
                drawRoundRect(water.first, cornerRadius = corner)
                drawRoundRect(water.second, cornerRadius = corner, style = Stroke(1.5.dp.toPx()))
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TabakAciklamasi(vegetables, R.string.nutrition_plate_slice_vegetables, R.string.nutrition_plate_vegetables_examples)
            TabakAciklamasi(protein, R.string.nutrition_plate_slice_protein, R.string.nutrition_plate_protein_examples)
            TabakAciklamasi(grains, R.string.nutrition_plate_slice_grains, R.string.nutrition_plate_grains_examples)
            TabakAciklamasi(water, R.string.nutrition_plate_slice_water, null)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.nutrition_plate_legume_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TabakAciklamasi(colors: Pair<Color, Color>, labelRes: Int, examplesRes: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(14.dp)
                .background(colors.first, CircleShape)
                .border(1.5.dp, colors.second, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.titleSmall)
            if (examplesRes != null) {
                Text(stringResource(examplesRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Bitkisel destek: "Ne denendi?", kanıt, alerji, dikkat ve "doktoruna söyle" birlikte; biri yoksa kart yok. */
@Composable
internal fun BitkiselKart(item: BitkiselOge) {
    AylaCard {
        BaslikVeKanit(stringResource(item.title), item.evidence)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EtiketliAlan(stringResource(R.string.herbal_field_tried), listOf(stringResource(item.tried)))
            if (item.note != null) {
                Text(stringResource(item.note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            EtiketliAlan(stringResource(R.string.herbal_field_allergen), listOf(stringResource(item.allergen)))
            EtiketliAlan(stringResource(R.string.herbal_field_caution), item.cautions.map { stringResource(it) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.herbal_tell_professional), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        KaynakSatiri(item.source)
    }
}

/**
 * Listeye alınmayanlar: öneri değil uyarı. Kanıt düzeyi gösterilir ama uyarı, kanıt sınırlı olsa bile
 * her zaman görünür; "ekle" düğmesi yoktur.
 */
@Composable
internal fun ListeDisiKarti() {
    val colors = LocalAylaColors.current
    AylaCard(containerColor = colors.uyariAcik, bordered = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.uyari)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.herbal_excluded_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.herbal_excluded_text), style = MaterialTheme.typography.bodyMedium)
            }
        }
        PlanIcerigi.herbalExcluded.forEach { card ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.uyari.copy(alpha = 0.3f))
            BaslikVeKanit(stringResource(card.title), card.evidence)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(card.text), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            KaynakSatiri(card.source)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.uyari.copy(alpha = 0.3f))
        Text(stringResource(R.string.evidence_safety_note), style = MaterialTheme.typography.bodySmall)
    }
}
