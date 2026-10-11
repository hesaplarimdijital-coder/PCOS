package com.ayla.pcos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Renk tokenları Ayla Tasarım Sistemi'nden (tokens.json) birebir aktarılmıştır.
 * Her metin/zemin çifti iki temada da en az 4,5:1 kontrast taşır.
 */

// Açık tema
private val Zemin = Color(0xFFFAF7FB)
private val Yuzey = Color(0xFFFFFFFF)
private val YuzeyCukur = Color(0xFFF3EEF5)
private val Cizgi = Color(0xFFE4DCE8)
private val CizgiKontrol = Color(0xFF8B7993)
private val Murekkep = Color(0xFF2B1830)
private val MurekkepSoluk = Color(0xFF6C5A72)
private val Nar = Color(0xFFB02E5A)
private val NarAcik = Color(0xFFFCE4EC)
private val NarUzeri = Color(0xFFFFFFFF)
private val Adacayi = Color(0xFF2D7366)
private val AdacayiAcik = Color(0xFFDCEFE9)
private val Kayisi = Color(0xFF9C4E1C)
private val KayisiAcik = Color(0xFFFCE8D8)
private val Lavanta = Color(0xFF5E4BAE)
private val LavantaAcik = Color(0xFFECE7FA)
private val Basari = Color(0xFF276F45)
private val BasariAcik = Color(0xFFDDF2E4)
private val Uyari = Color(0xFF8A5800)
private val UyariAcik = Color(0xFFFFF1D6)
private val Tehlike = Color(0xFFB3261E)
private val TehlikeAcik = Color(0xFFFCE4E2)
private val FazAdet = Color(0xFFCF3A65)
private val FazAdetAcik = Color(0xFFFBE0E8)
private val FazFolikuler = Color(0xFFD9742F)
private val FazYumurtlama = Color(0xFF7C64CC)
private val FazLuteal = Color(0xFFA3608F)

// Koyu tema
private val ZeminK = Color(0xFF16101A)
private val YuzeyK = Color(0xFF211827)
private val YuzeyCukurK = Color(0xFF110C14)
private val CizgiK = Color(0xFF3B2F43)
private val CizgiKontrolK = Color(0xFF85748D)
private val MurekkepK = Color(0xFFF6EEF7)
private val MurekkepSolukK = Color(0xFFC2B2C9)
private val NarK = Color(0xFFFF8FB2)
private val NarAcikK = Color(0xFF4B1B2F)
private val NarUzeriK = Color(0xFF3A0A1E)
private val AdacayiK = Color(0xFF80D1C0)
private val AdacayiAcikK = Color(0xFF173A33)
private val KayisiK = Color(0xFFFFB585)
private val KayisiAcikK = Color(0xFF4A2916)
private val LavantaK = Color(0xFFBBAAFF)
private val LavantaAcikK = Color(0xFF2C2452)
private val BasariK = Color(0xFF7DD8A0)
private val BasariAcikK = Color(0xFF15392A)
private val UyariK = Color(0xFFF3C266)
private val UyariAcikK = Color(0xFF3E2D0B)
private val TehlikeK = Color(0xFFFF8A80)
private val TehlikeAcikK = Color(0xFF4A1714)
private val FazAdetK = Color(0xFFFF7A9C)
private val FazAdetAcikK = Color(0xFF4A1A2C)
private val FazFolikulerK = Color(0xFFF7B27E)
private val FazYumurtlamaK = Color(0xFFA895F0)
private val FazLutealK = Color(0xFFD9A1CB)

// Her iki temada aynı
private val KayisiCanli = Color(0xFFF29A63)
private val KayisiCanliUzeri = Color(0xFF2B1830)

val AylaLightColorScheme: ColorScheme = lightColorScheme(
    primary = Nar,
    onPrimary = NarUzeri,
    primaryContainer = NarAcik,
    onPrimaryContainer = Murekkep,
    secondary = Adacayi,
    onSecondary = Color.White,
    secondaryContainer = AdacayiAcik,
    onSecondaryContainer = Murekkep,
    tertiary = Kayisi,
    onTertiary = Color.White,
    tertiaryContainer = KayisiAcik,
    onTertiaryContainer = Murekkep,
    error = Tehlike,
    onError = Color.White,
    errorContainer = TehlikeAcik,
    onErrorContainer = Murekkep,
    background = Zemin,
    onBackground = Murekkep,
    surface = Zemin,
    onSurface = Murekkep,
    surfaceVariant = YuzeyCukur,
    onSurfaceVariant = MurekkepSoluk,
    surfaceContainerLowest = Yuzey,
    surfaceContainerLow = Yuzey,
    surfaceContainer = Yuzey,
    surfaceContainerHigh = YuzeyCukur,
    surfaceContainerHighest = YuzeyCukur,
    outline = CizgiKontrol,
    outlineVariant = Cizgi,
    inverseSurface = Murekkep,
    inverseOnSurface = Zemin,
    inversePrimary = NarK,
)

val AylaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = NarK,
    onPrimary = NarUzeriK,
    primaryContainer = NarAcikK,
    onPrimaryContainer = MurekkepK,
    secondary = AdacayiK,
    onSecondary = Color(0xFF0F2A25),
    secondaryContainer = AdacayiAcikK,
    onSecondaryContainer = MurekkepK,
    tertiary = KayisiK,
    onTertiary = Color(0xFF3A1E0C),
    tertiaryContainer = KayisiAcikK,
    onTertiaryContainer = MurekkepK,
    error = TehlikeK,
    onError = Color(0xFF3A0B08),
    errorContainer = TehlikeAcikK,
    onErrorContainer = MurekkepK,
    background = ZeminK,
    onBackground = MurekkepK,
    surface = ZeminK,
    onSurface = MurekkepK,
    surfaceVariant = YuzeyCukurK,
    onSurfaceVariant = MurekkepSolukK,
    surfaceContainerLowest = YuzeyCukurK,
    surfaceContainerLow = YuzeyK,
    surfaceContainer = YuzeyK,
    surfaceContainerHigh = YuzeyCukurK,
    surfaceContainerHighest = CizgiK,
    outline = CizgiKontrolK,
    outlineVariant = CizgiK,
    inverseSurface = MurekkepK,
    inverseOnSurface = ZeminK,
    inversePrimary = Nar,
)

/** Material rolleriyle ifade edilemeyen alan ve durum renkleri. */
@Immutable
data class AylaColors(
    val lavanta: Color,
    val lavantaAcik: Color,
    val basari: Color,
    val basariAcik: Color,
    val uyari: Color,
    val uyariAcik: Color,
    val kayisiCanli: Color,
    val kayisiCanliUzeri: Color,
    val fazAdet: Color,
    val fazAdetAcik: Color,
    val fazFolikuler: Color,
    val fazYumurtlama: Color,
    val fazLuteal: Color,
    /** Kart zemini: açık temada beyaz, koyu temada yükseltilmiş yüzey. */
    val kart: Color,
    /** Gömülü alan: giriş kutuları, halka izi. */
    val cukur: Color,
)

val AylaLightColors = AylaColors(
    lavanta = Lavanta,
    lavantaAcik = LavantaAcik,
    basari = Basari,
    basariAcik = BasariAcik,
    uyari = Uyari,
    uyariAcik = UyariAcik,
    kayisiCanli = KayisiCanli,
    kayisiCanliUzeri = KayisiCanliUzeri,
    fazAdet = FazAdet,
    fazAdetAcik = FazAdetAcik,
    fazFolikuler = FazFolikuler,
    fazYumurtlama = FazYumurtlama,
    fazLuteal = FazLuteal,
    kart = Yuzey,
    cukur = YuzeyCukur,
)

val AylaDarkColors = AylaColors(
    lavanta = LavantaK,
    lavantaAcik = LavantaAcikK,
    basari = BasariK,
    basariAcik = BasariAcikK,
    uyari = UyariK,
    uyariAcik = UyariAcikK,
    kayisiCanli = KayisiCanli,
    kayisiCanliUzeri = KayisiCanliUzeri,
    fazAdet = FazAdetK,
    fazAdetAcik = FazAdetAcikK,
    fazFolikuler = FazFolikulerK,
    fazYumurtlama = FazYumurtlamaK,
    fazLuteal = FazLutealK,
    kart = YuzeyK,
    cukur = YuzeyCukurK,
)

val LocalAylaColors = staticCompositionLocalOf { AylaLightColors }
