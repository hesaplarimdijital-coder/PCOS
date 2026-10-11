package com.ayla.pcos.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import com.ayla.pcos.ui.theme.LocalReducedMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hale halkası: Bugün ekranının kahramanı.
 *
 * - Halkanın tamamı tahmin aralığının üst sınırını temsil eder ([totalDays]); sabit 28 gün değil.
 * - Geçen günler nar dolgu, tahmin aralığı izin üstünde açık bant ve dışında kesik çizgili hale.
 * - Dolgu ekran açılınca yayla bugüne kadar dolar; arkadaki parıltı 7 saniyelik nefes döngüsüyle
 *   büyür küçülür. "Animasyonları kaldır" açıksa ikisi de durur.
 *
 * @param day Döngünün günü (1 tabanlı); kayıt yoksa null.
 * @param totalDays Halkanın temsil ettiği gün sayısı (tahmin aralığının sonu ya da en az 28).
 * @param windowStartDay Tahmin aralığının ilk günü; yoksa null.
 * @param windowEndDay Tahmin aralığının son günü; yoksa null.
 * @param overdue Bugün aralığı geçtiyse true; dolgu uyarı rengine döner.
 */
@Composable
fun HaleHalkasi(
    day: Int?,
    totalDays: Int,
    windowStartDay: Int?,
    windowEndDay: Int?,
    overdue: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 232.dp,
    center: @Composable () -> Unit,
) {
    val colors = LocalAylaColors.current
    val scheme = MaterialTheme.colorScheme
    val reduced = LocalReducedMotion.current
    // Büyük yazıda merkez metin sığsın diye halka da (en fazla %30) büyür.
    val ringSize = size * LocalDensity.current.fontScale.coerceIn(1f, 1.3f)

    val safeTotal = totalDays.coerceAtLeast(1)
    val targetFraction = ((day ?: 0).coerceAtLeast(0).toFloat() / safeTotal).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (reduced) tween(0) else AylaMotion.slow(),
        label = "haleDolgu",
    )

    val breathing = rememberInfiniteTransition(label = "nefes")
    val glowScale by breathing.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced) 1f else 1.035f,
        animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse),
        label = "parilti",
    )

    // Gecikme PCOS'ta sıktır: alarm kırmızısı değil, sıcak kayısı.
    val fillColor = if (overdue) colors.kayisiCanli else scheme.primary
    val glowColor = scheme.primaryContainer
    val trackColor = colors.cukur
    val bandColor = colors.fazAdetAcik
    val haloColor = colors.fazAdet
    val markerFill = colors.kart

    Box(
        modifier = modifier
            .size(ringSize)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringStroke = 16.dp.toPx()
            val haloStroke = 2.5.dp.toPx()
            val outerPad = 10.dp.toPx() // kesik hale için dış pay
            val radius = (this.size.minDimension / 2f) - outerPad - ringStroke
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            val arcTopLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)

            // parıltı
            drawCircle(color = glowColor.copy(alpha = 0.55f), radius = (radius + ringStroke) * glowScale, center = centerOffset)
            // iz
            drawCircle(color = trackColor, radius = radius, center = centerOffset, style = Stroke(ringStroke))

            // tahmin bandı (iz üstünde) ve kesik hale (dışında)
            if (windowStartDay != null && windowEndDay != null && windowEndDay >= windowStartDay) {
                val startAngle = -90f + 360f * ((windowStartDay - 1).toFloat() / safeTotal)
                val sweep = 360f * ((windowEndDay - windowStartDay + 1).toFloat() / safeTotal)
                drawArc(
                    color = bandColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(ringStroke * 0.75f, cap = StrokeCap.Round),
                )
                val haloRadius = radius + ringStroke / 2f + outerPad * 0.6f
                drawArc(
                    color = haloColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - haloRadius, centerOffset.y - haloRadius),
                    size = Size(haloRadius * 2f, haloRadius * 2f),
                    style = Stroke(
                        width = haloStroke,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }

            // dolgu
            if (fraction > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(ringStroke, cap = StrokeCap.Round),
                )
            }

            // bugün işareti
            if (day != null && day > 0) {
                val angle = Math.toRadians((-90f + 360f * fraction).toDouble())
                val marker = Offset(
                    centerOffset.x + radius * cos(angle).toFloat(),
                    centerOffset.y + radius * sin(angle).toFloat(),
                )
                drawCircle(color = markerFill, radius = 8.dp.toPx(), center = marker)
                drawCircle(color = fillColor, radius = 8.dp.toPx(), center = marker, style = Stroke(4.dp.toPx()))
            }
        }
        // Merkez içerik iç dairenin içinde kalır (halka kalınlığı + hale payı düşülür).
        Column(
            modifier = Modifier.widthIn(max = ringSize * 0.62f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { center() }
    }
}

/** Hale halkasının ortasındaki standart içerik: etiket, büyük gün sayısı, alt metin. */
@Composable
fun HaleMerkezi(label: String, day: Int?, subtitle: String) {
    Overline(label)
    Text(
        text = day?.toString() ?: "—",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
