package com.ayla.pcos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ayla.pcos.R

/*
 * İki aile: Bricolage Grotesque büyük rakam ve ekran başlıklarında (opsz ekseniyle),
 * Figtree diğer her şeyde. İkisi de değişken yazı tipi ve OFL lisanslı (docs/licenses).
 */

private fun bricolage(weight: Int, opticalSize: Float) = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opticalSize),
    ),
)

private fun figtree(weight: Int) = Font(
    resId = R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Görüntü ailesi: büyük sayılar ve başlıklar. */
val BricolageFamily = FontFamily(
    bricolage(600, 48f),
    bricolage(700, 96f),
)

/** Metin ailesi. */
val FigtreeFamily = FontFamily(
    figtree(400),
    figtree(500),
    figtree(600),
    figtree(700),
)

val AylaTypography = Typography(
    // display-xl: hale halkasının ortasındaki gün sayısı
    displayLarge = TextStyle(
        fontFamily = BricolageFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.02).em,
    ),
    // display-l: karşılama ve kutlama başlıkları
    displayMedium = TextStyle(
        fontFamily = BricolageFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.015).em,
    ),
    displaySmall = TextStyle(
        fontFamily = BricolageFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.015).em,
    ),
    // headline: ekran başlıkları
    headlineMedium = TextStyle(
        fontFamily = BricolageFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = BricolageFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    // title-l: bölüm başlıkları
    titleLarge = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // title-m: kart başlıkları, ilaç adı
    titleMedium = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // body-l: okunacak metin
    bodyLarge = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // body-m: yardımcı metin
    bodyMedium = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // label-l: buton ve sekme etiketleri
    labelLarge = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // label-m: çip ve rozetler
    labelMedium = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.em,
    ),
    // overline: bölüm üst etiketi; metin Türkçe yerel ayarla büyük harfe çevrilir
    labelSmall = TextStyle(
        fontFamily = FigtreeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    ),
)
