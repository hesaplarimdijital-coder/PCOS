package com.ayla.pcos.ekran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.photos.PhotoKind
import com.ayla.pcos.data.photos.PhotoRecord
import com.ayla.pcos.data.skin.RoutineStep
import com.ayla.pcos.data.skin.SkinRoutineInput
import com.ayla.pcos.data.skin.SkinRoutineRules
import com.ayla.pcos.ui.skin.PhotoStatus
import com.ayla.pcos.ui.skin.PhotoUi
import com.ayla.pcos.ui.skin.SkinUiState

/** Cilt ve saç ekran görüntüleri için örnek veri (#25). Fotoğraflar kodla üretilmiş degradelerdir; gerçek yüz yok. */
internal object OrnekCilt {
    private val bugun = Ornek.bugun

    private val profil = SkinProfileEntity(skinType = SkinType.COMBINATION, acne = AcneSelfReport.MILD, darkSpots = true, acanthosis = true)

    private fun plan(pregnancy: Boolean) = SkinRoutineRules.plan(
        SkinRoutineInput(
            skinType = profil.skinType, acne = profil.acne, darkSpots = profil.darkSpots, acanthosis = profil.acanthosis,
            pregnancyPlan = pregnancy,
        ),
    )

    val rutin = SkinUiState(
        loaded = true,
        today = bugun,
        profile = profil,
        plan = plan(pregnancy = false),
        checks = mapOf(RoutineSlot.MORNING to setOf(RoutineStep.CLEANSER.key, RoutineStep.NIACINAMIDE.key)),
        weekMorning = 4,
        weekEvening = 3,
    )

    val rutinGebelik = rutin.copy(pregnancyPlan = true, plan = plan(pregnancy = true))

    val bosProfil = SkinUiState(loaded = true, today = bugun)

    private val an = bugun.atStartOfDay(Ornek.zone).toInstant()

    private val fotograflar = listOf(56L, 28L, 14L, 0L).mapIndexed { i, gun ->
        PhotoRecord(
            id = "0000000$i-0000-4000-8000-00000000000$i",
            kind = PhotoKind.SKIN,
            area = "çene",
            date = bugun.minusDays(gun),
            createdAt = an.minusSeconds(gun * 86_400),
            byteSize = 250_000,
            width = 1200,
            height = 1600,
        )
    }

    val fotografRiza = rutin.copy(photoConsent = false)

    val fotograflarDurumu = rutin.copy(photoConsent = true, photos = fotograflar.reversed(), photo = PhotoUi(status = PhotoStatus.AVAILABLE))

    val karsilastirmaSecimi: Set<String> = setOf(fotograflar.first().id, fotograflar.last().id)

    /** Fotoğraf yerine degrade; her kayıt farklı tonla. */
    fun kucukResim(record: PhotoRecord, px: Int): ImageBitmap {
        val w = px.coerceAtMost(240)
        val h = w * 4 / 3
        val index = fotograflar.indexOfFirst { it.id == record.id }.coerceAtLeast(0)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                android.graphics.Color.rgb(232 - index * 10, 180 - index * 6, 160 - index * 4),
                android.graphics.Color.rgb(150 - index * 12, 100, 120 + index * 8),
                Shader.TileMode.CLAMP,
            )
        }
        Canvas(bmp).drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return bmp.asImageBitmap()
    }
}
