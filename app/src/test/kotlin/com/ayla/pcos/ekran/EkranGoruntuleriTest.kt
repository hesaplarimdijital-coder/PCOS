package com.ayla.pcos.ekran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.ayla.pcos.ui.AppLockContent
import com.ayla.pcos.ui.calendar.CalendarContent
import com.ayla.pcos.ui.components.AltGezinme
import com.ayla.pcos.ui.log.DailyLogContent
import com.ayla.pcos.ui.me.MeContent
import com.ayla.pcos.ui.me.SettingsContent
import com.ayla.pcos.ui.meds.AddMedicationContent
import com.ayla.pcos.ui.meds.EditMedicationContent
import com.ayla.pcos.ui.meds.EditMedicationState
import com.ayla.pcos.ui.meds.MedsContent
import com.ayla.pcos.ui.navigation.Screen
import com.ayla.pcos.ui.onboarding.OnboardingContent
import com.ayla.pcos.ui.plan.PlanContent
import com.ayla.pcos.ui.theme.AylaTheme
import com.ayla.pcos.ui.today.TodayContent
import com.ayla.pcos.ui.today.TodayUiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Ekran görüntüleri: durumsuz içerikler örnek veriyle çizilir ve app/build/ekran altına PNG olarak
 * yazılır (yalnızca -Pekran ile). CI bunları `ekran-goruntuleri` dalına yükler; README ve tasarım
 * incelemesi oradan okur. Her test tek bir görüntü üretir.
 *
 * Boyutlar: TELEFON 393×852 dp (README için "ilk ekran"), UZUN 393×1800 dp (inceleme için tüm içerik).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TELEFON)
class EkranGoruntuleriTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun saatDilimi() {
        // Saatler (doz, uyku) kullanıcının diliminde gösterilir; CI sunucusu UTC'dir.
        TimeZone.setDefault(TimeZone.getTimeZone(Ornek.zone))
    }

    private fun ekran(
        ad: String,
        sekme: Screen.Tab? = null,
        koyu: Boolean = false,
        yaziOlcegi: Float = 1f,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            AylaTheme(darkTheme = koyu, reducedMotion = true) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, yaziOlcegi)) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (sekme != null) {
                            Scaffold(
                                bottomBar = { AltGezinme(selected = sekme, onSelect = {}) },
                                containerColor = MaterialTheme.colorScheme.background,
                            ) { padding ->
                                Box(Modifier.padding(padding)) { content() }
                            }
                        } else {
                            content()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/ekran/$ad.png")
    }

    @Composable
    private fun Bugun(state: TodayUiState, hour: Int = 10) = TodayContent(
        state = state, onOpenLog = {}, onOpenMeds = {}, onOpenSettings = {}, onAddWater = {},
        onTaken = {}, onUndo = {}, onPeriodStart = {}, onPeriodEnd = {}, hour = hour,
    )

    @Composable
    private fun Takvim(secili: Boolean = true) = CalendarContent(
        state = Ornek.takvim(if (secili) Ornek.bugun.minusDays(4) else null),
        onPrevious = {}, onNext = {}, onToday = {}, onSelect = {}, onOpenLog = {},
        onMarkStart = {}, onMarkEnd = {}, onUnmark = {}, onExcluded = { _, _ -> },
    )

    @Composable
    private fun Ilaclar() = MedsContent(state = Ornek.ilaclar, onAdd = {}, onTaken = {}, onUndo = {}, onEdit = {})

    @Composable
    private fun IlacFormu(state: EditMedicationState) = EditMedicationContent(
        state = state, onBack = {}, onName = {}, onDose = {}, onKind = {}, onAddTime = {}, onRemoveTime = {},
        onFood = {}, onStock = {}, onStockWarnDays = {}, onRegimen = {}, onPackStart = {}, onSave = {}, onRemove = {},
    )

    @Composable
    private fun Plan() = PlanContent(
        state = Ornek.plan, onAddCup = {}, onRemoveCup = {}, onAddSleep = { _, _, _ -> }, onDeleteSleep = {}, onEveningRoutine = {},
    )

    // --- README: beş sekme, ilk ekran ---

    @Test fun t01_bugun() = ekran("01-bugun", sekme = Screen.Today) { Bugun(Ornek.bugunDongu) }

    @Test fun t02_takvim() = ekran("02-takvim", sekme = Screen.Calendar) { Takvim() }

    @Test fun t03_ilaclar() = ekran("03-ilaclar", sekme = Screen.Meds) { Ilaclar() }

    @Test fun t04_planim() = ekran("04-planim", sekme = Screen.Plan) { Plan() }

    @Test fun t05_ben() = ekran("05-ben", sekme = Screen.Me) { MeContent(state = Ornek.ben, onOpenSettings = {}) }

    // --- Durumlar ---

    @Test fun t06_bugun_hap() = ekran("06-bugun-hap", sekme = Screen.Today) { Bugun(Ornek.bugunHap, hour = 21) }

    @Test fun t07_bugun_uzun_dongu() = ekran("07-bugun-uzun-dongu", sekme = Screen.Today) { Bugun(Ornek.bugunUzunDongu, hour = 15) }

    @Test fun t08_bugun_bos() = ekran("08-bugun-bos", sekme = Screen.Today) { Bugun(Ornek.bugunBos) }

    @Test fun t09_gunluk_kayit() = ekran("09-gunluk-kayit") {
        DailyLogContent(
            state = Ornek.gunlukKayit, onClose = {}, onMood = {}, onEnergy = {}, onFlow = {}, onSymptom = {},
            onWeight = {}, onWaist = {}, onNote = {}, onSave = {},
        )
    }

    @Test fun t10_ilac_ekle() = ekran("10-ilac-ekle") {
        AddMedicationContent(onBack = {}, onTemplate = { _, _ -> }, onCustom = {}, today = Ornek.bugun)
    }

    @Test fun t11_karsilama() = ekran("11-karsilama") {
        OnboardingContent(Ornek.karsilama, {}, {}, {}, {}, {}, {}, { _, _, _ -> })
    }

    @Test fun t12_karsilama_hedefler() = ekran("12-karsilama-hedefler") {
        OnboardingContent(Ornek.karsilamaHedefler, {}, {}, {}, {}, {}, {}, { _, _, _ -> })
    }

    @Test fun t13_karsilama_izinler() = ekran("13-karsilama-izinler") {
        OnboardingContent(Ornek.karsilamaIzinler, {}, {}, {}, {}, {}, {}, { _, _, _ -> })
    }

    @Test fun t19_bugun_bildirim_kapali() = ekran("19-bugun-bildirim-kapali", sekme = Screen.Today) {
        Bugun(Ornek.bugunDongu.copy(notificationsOff = true))
    }

    @Test fun t14_kilit() = ekran("14-kilit") {
        AppLockContent(title = "Ayla kilitli", subtitle = "Devam etmek için kimliğini doğrula", onUnlock = {})
    }

    // --- Koyu tema ve büyük yazı ---

    @Test fun t15_koyu_bugun() = ekran("15-koyu-bugun", sekme = Screen.Today, koyu = true) { Bugun(Ornek.bugunDongu, hour = 22) }

    @Test fun t16_koyu_takvim() = ekran("16-koyu-takvim", sekme = Screen.Calendar, koyu = true) { Takvim() }

    @Test fun t17_buyuk_yazi_bugun() = ekran("17-buyuk-yazi-bugun", sekme = Screen.Today, yaziOlcegi = 1.6f) { Bugun(Ornek.bugunDongu) }

    @Test fun t18_buyuk_yazi_gunluk() = ekran("18-buyuk-yazi-gunluk", yaziOlcegi = 1.6f) {
        DailyLogContent(
            state = Ornek.gunlukKayit, onClose = {}, onMood = {}, onEnergy = {}, onFlow = {}, onSymptom = {},
            onWeight = {}, onWaist = {}, onNote = {}, onSave = {},
        )
    }

    // --- İnceleme: tüm içerik (uzun) ---

    @Config(qualifiers = UZUN)
    @Test fun u01_bugun_tum() = ekran("u01-bugun-tum", sekme = Screen.Today) { Bugun(Ornek.bugunDongu) }

    @Config(qualifiers = UZUN)
    @Test fun u02_takvim_tum() = ekran("u02-takvim-tum", sekme = Screen.Calendar) { Takvim() }

    @Config(qualifiers = UZUN)
    @Test fun u03_ilaclar_tum() = ekran("u03-ilaclar-tum", sekme = Screen.Meds) { Ilaclar() }

    @Config(qualifiers = UZUN)
    @Test fun u04_planim_tum() = ekran("u04-planim-tum", sekme = Screen.Plan) { Plan() }

    @Config(qualifiers = UZUN)
    @Test fun u05_ben_tum() = ekran("u05-ben-tum", sekme = Screen.Me) { MeContent(state = Ornek.ben, onOpenSettings = {}) }

    @Config(qualifiers = UZUN)
    @Test fun u06_ayarlar_tum() = ekran("u06-ayarlar-tum") {
        SettingsContent(
            prefs = Ornek.ayarlar, exactAlarmsAllowed = false, snackbar = remember { SnackbarHostState() },
            onBack = {}, onUpdate = {}, onExport = {}, onWipe = {},
        )
    }

    @Config(qualifiers = UZUN)
    @Test fun u07_ilac_duzenle_tum() = ekran("u07-ilac-duzenle-tum") { IlacFormu(Ornek.ilacDuzenle) }

    @Config(qualifiers = UZUN)
    @Test fun u08_hap_duzenle_tum() = ekran("u08-hap-duzenle-tum") { IlacFormu(Ornek.hapDuzenle) }
}

internal const val TELEFON = "w393dp-h852dp-xhdpi"
internal const val UZUN = "w393dp-h1800dp-xhdpi"
