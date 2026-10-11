package com.ayla.pcos.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Köşe ölçeği: radius-xs … radius-xl. Butonlar hap biçimindedir (CircleShape). */
val AylaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Expressive biçimler. Her biçimin tek bir anlamı vardır:
 * Çiçek = adet günü, Çerez = başarı rozeti, Yonca = cilt bakımı, Patlama = kutlama.
 * Kutupsal çizimle üretilir; Material 3 Expressive'in MaterialShapes kararlı sürümü
 * geldiğinde birebir değiştirilebilir.
 */
object AylaExpressiveShapes {
    val Cicek: Shape = polar(lobes = 8, amplitude = 0.14f)
    val Cerez: Shape = polar(lobes = 9, amplitude = 0.07f)
    val Yonca: Shape = polar(lobes = 4, amplitude = 0.20f)
    val Patlama: Shape = polar(lobes = 12, amplitude = 0.10f)

    /** r(θ) = R·(1 + a·cos(nθ)) eğrisini kapalı bir yola çevirir. */
    fun polar(lobes: Int, amplitude: Float, steps: Int = 180): Shape = GenericShape { size, _ ->
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = minOf(cx, cy) / (1f + amplitude)
        for (i in 0 until steps) {
            val t = i.toFloat() / steps * (2.0 * Math.PI).toFloat()
            val r = base * (1f + amplitude * cos(lobes * t))
            val x = cx + r * cos(t - (Math.PI / 2).toFloat())
            val y = cy + r * sin(t - (Math.PI / 2).toFloat())
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}
