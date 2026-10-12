package com.ayla.pcos.ekran

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.ui.AppLockContent
import com.ayla.pcos.ui.calendar.CalendarContent
import com.ayla.pcos.ui.components.AltGezinme
import com.ayla.pcos.ui.components.BatteryStatus
import com.ayla.pcos.ui.log.DailyLogContent
import com.ayla.pcos.ui.me.BackupImportState
import com.ayla.pcos.ui.me.GizlilikPolitikasiIcerik
import com.ayla.pcos.ui.me.HatirlatmaGunluguDialog
import com.ayla.pcos.ui.me.MeContent
import com.ayla.pcos.ui.me.SettingsContent
import com.ayla.pcos.ui.meds.AddMedicationContent
import com.ayla.pcos.ui.meds.EditMedicationContent
import com.ayla.pcos.ui.meds.EditMedicationState
import com.ayla.pcos.ui.meds.MedsContent
import com.ayla.pcos.ui.navigation.Screen
import com.ayla.pcos.ui.onboarding.OnboardingContent
import com.ayla.pcos.ui.plan.BitkiselKart
import com.ayla.pcos.ui.plan.ListeDisiKarti
import com.ayla.pcos.ui.plan.MovementCycleCard
import com.ayla.pcos.ui.plan.PlanContent
import com.ayla.pcos.ui.plan.PlanIcerigi
import com.ayla.pcos.ui.plan.PlanUiState
import com.ayla.pcos.ui.plan.RutinKarti
import com.ayla.pcos.ui.theme.AylaTheme
import com.ayla.pcos.ui.today.TodayContent
import com.ayla.pcos.ui.today.TodayUiState
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime
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

    @OptIn(ExperimentalRoborazziApi::class)
    private fun ekran(
        ad: String,
        sekme: Screen.Tab? = null,
        koyu: Boolean = false,
        yaziOlcegi: Float = 1f,
        sistemYaziOlcegi: Float? = null,
        tumEkran: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            AylaTheme(darkTheme = koyu, reducedMotion = true) {
                val density = LocalDensity.current
                val context = LocalContext.current
                val configuration = LocalConfiguration.current
                // yaziOlcegi: doğrusal ölçek (sınama için abartılı). sistemYaziOlcegi: cihazdaki gibi
                // yapılandırmadan okunur; Android 14+ büyük metni doğrusal olmayan biçimde daha az büyütür.
                val olcekli = remember(context, configuration, yaziOlcegi, sistemYaziOlcegi) {
                    if (sistemYaziOlcegi != null) {
                        val cfg = Configuration(configuration).apply { fontScale = sistemYaziOlcegi }
                        Density(context.createConfigurationContext(cfg))
                    } else {
                        Density(density.density, yaziOlcegi)
                    }
                }
                CompositionLocalProvider(LocalDensity provides olcekli) {
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
        // Diyaloglar ayrı pencerede çizilir; tumEkran tüm pencereleri tek görüntüde birleştirir.
        if (tumEkran) captureScreenRoboImage("build/ekran/$ad.png") else compose.onRoot().captureRoboImage("build/ekran/$ad.png")
    }

    @Composable
    private fun Bugun(state: TodayUiState, hour: Int = 10, minute: Int = 0) = TodayContent(
        state = state, onOpenLog = {}, onOpenMeds = {}, onOpenSettings = {}, onAddWater = {},
        onTaken = {}, onUndo = {}, onPeriodStart = {}, onPeriodEnd = {}, now = LocalTime.of(hour, minute),
    )

    @Composable
    private fun Takvim(secili: Boolean = true) = CalendarContent(
        state = Ornek.takvim(if (secili) Ornek.bugun.minusDays(4) else null),
        onPrevious = {}, onNext = {}, onToday = {}, onSelect = {}, onOpenLog = {},
        onMarkStart = {}, onMarkEnd = {}, onUnmark = {}, onExcluded = { _, _ -> },
    )

    @Composable
    private fun Ilaclar(pil: BatteryStatus = BatteryStatus.UNRESTRICTED, uretici: Int? = null) = MedsContent(
        state = Ornek.ilaclar, onAdd = {}, onTaken = {}, onUndo = {}, onEdit = {}, batteryStatus = pil, oemNote = uretici,
    )

    @Composable
    private fun IlacFormu(state: EditMedicationState) = EditMedicationContent(
        state = state, onBack = {}, onName = {}, onDose = {}, onKind = {}, onAddTime = {}, onRemoveTime = {},
        onFood = {}, onStock = {}, onStockWarnDays = {}, onRegimen = {}, onPackStart = {}, onSave = {}, onRemove = {},
    )

    @Composable
    private fun Plan(sekme: Int = 0, durum: PlanUiState = Ornek.plan, acikRutin: String? = null) = PlanContent(
        state = durum, onAddCup = {}, onRemoveCup = {}, onAddSleep = { _, _, _ -> }, onDeleteSleep = {}, onEveningRoutine = {},
        initialTab = sekme, initialExpandedRoutine = acikRutin,
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
        OnboardingContent(Ornek.karsilama, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun t12_karsilama_hedefler() = ekran("12-karsilama-hedefler") {
        OnboardingContent(Ornek.karsilamaHedefler, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun t13_karsilama_izinler() = ekran("13-karsilama-izinler") {
        OnboardingContent(Ornek.karsilamaIzinler, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun t19_bugun_bildirim_kapali() = ekran("19-bugun-bildirim-kapali", sekme = Screen.Today) {
        Bugun(Ornek.bugunDongu.copy(notificationsOff = true))
    }

    @Test fun t20_simge() = ekran("20-simge") { SimgeOnizleme() }

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

    @Test fun t26_bugun_hap_dun_isaretlenmedi() = ekran("26-bugun-hap-dun", sekme = Screen.Today) { Bugun(Ornek.bugunHapDunKacti, hour = 9) }

    @Test fun t21_koyu_ben() = ekran("21-koyu-ben", sekme = Screen.Me, koyu = true) { MeContent(state = Ornek.ben, onOpenSettings = {}) }

    @Test fun t22_koyu_planim() = ekran("22-koyu-planim", sekme = Screen.Plan, koyu = true) { Plan() }

    @Test fun t23_buyuk_yazi_ilaclar() = ekran("23-buyuk-yazi-ilaclar", sekme = Screen.Meds, yaziOlcegi = 1.6f) { Ilaclar() }

    @Config(qualifiers = UZUN)
    @Test fun t24_buyuk_yazi_ayarlar() = ekran("24-buyuk-yazi-ayarlar", yaziOlcegi = 1.6f) {
        SettingsContent(
            prefs = Ornek.ayarlar, exactAlarmsAllowed = true, snackbar = remember { SnackbarHostState() },
            onBack = {}, onUpdate = {}, onExport = {}, onWipe = {},
        )
    }

    @Test fun t25_koyu_takvim_bos_secim() = ekran("25-takvim-secimsiz", sekme = Screen.Calendar) { Takvim(secili = false) }

    @Config(qualifiers = KUCUK)
    @Test fun t27_kucuk_ekran_takvim() = ekran("27-kucuk-takvim", sekme = Screen.Calendar) { Takvim() }

    @Test fun t29_yedek_onizleme() = ekran("29-yedek-onizleme", tumEkran = true) {
        SettingsContent(
            prefs = Ornek.ayarlar, exactAlarmsAllowed = true, snackbar = remember { SnackbarHostState() },
            onBack = {}, onUpdate = {}, onExport = {}, onWipe = {},
            importState = BackupImportState.Preview(Ornek.yedekOzeti),
        )
    }

    @Config(qualifiers = UZUN)
    @Test fun t30_gizlilik_politikasi() = ekran("30-gizlilik-politikasi") { GizlilikPolitikasiIcerik(onClose = {}, closeIsBack = true) }

    @Test fun t31_planim_beslenme() = ekran("31-planim-beslenme", sekme = Screen.Plan) { Plan(sekme = 1) }

    @Test fun t32_planim_hareket_adet() = ekran("32-planim-hareket-adet", sekme = Screen.Plan) {
        Plan(sekme = 2, durum = Ornek.plan.copy(movementCycleCard = MovementCycleCard.PERIOD))
    }

    @Test fun t33_koyu_beslenme() = ekran("33-koyu-beslenme", sekme = Screen.Plan, koyu = true) { Plan(sekme = 1) }

    @Test fun t34_ilaclar_pil_bandi() = ekran("34-ilaclar-pil-bandi", sekme = Screen.Meds) {
        Ilaclar(pil = BatteryStatus.OPTIMIZED, uretici = R.string.battery_oem_xiaomi)
    }

    @Test fun t35_hatirlatma_gunlugu() = ekran("35-hatirlatma-gunlugu", tumEkran = true) {
        HatirlatmaGunluguDialog(entries = Ornek.alarmGunlugu, onShare = {}, onClear = {}, onDismiss = {}, now = Ornek.alarmSimdi, zone = Ornek.zone)
    }

    // Diyalog kendi penceresinde çizilir ve LocalDensity'yi pencereden yeniden okur; %200 yazı bu yüzden
    // cihaz yapılandırmasıyla (fontScale) verilir.
    @Config(qualifiers = UZUN, fontScale = 2f)
    @Test fun y13_yazi200_hatirlatma_gunlugu() = ekran("y13-yazi200-hatirlatma-gunlugu", tumEkran = true) {
        HatirlatmaGunluguDialog(entries = Ornek.alarmGunlugu, onShare = {}, onClear = {}, onDismiss = {}, now = Ornek.alarmSimdi, zone = Ornek.zone)
    }

    @Config(qualifiers = BESLENME_TUM)
    @Test fun u09_beslenme_tum() = ekran("u09-beslenme-tum", sekme = Screen.Plan) { Plan(sekme = 1) }

    @Config(qualifiers = HAREKET_TUM)
    @Test fun u10_hareket_tum() = ekran("u10-hareket-tum", sekme = Screen.Plan) {
        Plan(sekme = 2, durum = Ornek.plan.copy(movementCycleCard = MovementCycleCard.OTHER), acikRutin = PlanIcerigi.ROUTINE_WALK)
    }

    @Config(qualifiers = UZUN)
    @Test fun y10_yazi200_beslenme() = ekran("y10-yazi200-beslenme", sekme = Screen.Plan, sistemYaziOlcegi = 2f) { Plan(sekme = 1) }

    @Config(qualifiers = UZUN)
    @Test fun y11_yazi200_hareket() = ekran("y11-yazi200-hareket", sekme = Screen.Plan, sistemYaziOlcegi = 2f) {
        Plan(sekme = 2, durum = Ornek.plan.copy(movementCycleCard = MovementCycleCard.OTHER))
    }

    @Config(qualifiers = HAREKET_TUM)
    @Test fun y12_yazi200_bitkisel_ve_rutin() = ekran("y12-yazi200-bitkisel-rutin", sistemYaziOlcegi = 2f) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BitkiselKart(PlanIcerigi.herbal.first())
            RutinKarti(PlanIcerigi.routines[1], expanded = true, onToggle = {})
            ListeDisiKarti()
        }
    }

    @Test fun t28_planim_uyku_bos() = ekran("28-planim-uyku-bos", sekme = Screen.Plan) {
        PlanContent(
            state = Ornek.plan.copy(lastNight = null, sleepWeek = emptyList()),
            onAddCup = {}, onRemoveCup = {}, onAddSleep = { _, _, _ -> }, onDeleteSleep = {}, onEveningRoutine = {},
        )
    }

    // --- Sistem yazı boyutu %200 (Android 14+ en büyük ayar), tüm içerik ---

    @Config(qualifiers = UZUN)
    @Test fun y01_yazi200_bugun() = ekran("y01-yazi200-bugun", sekme = Screen.Today, sistemYaziOlcegi = 2f) { Bugun(Ornek.bugunDongu) }

    @Config(qualifiers = UZUN)
    @Test fun y02_yazi200_takvim() = ekran("y02-yazi200-takvim", sekme = Screen.Calendar, sistemYaziOlcegi = 2f) { Takvim() }

    @Config(qualifiers = UZUN)
    @Test fun y03_yazi200_planim() = ekran("y03-yazi200-planim", sekme = Screen.Plan, sistemYaziOlcegi = 2f) { Plan() }

    @Config(qualifiers = UZUN)
    @Test fun y04_yazi200_ben() = ekran("y04-yazi200-ben", sekme = Screen.Me, sistemYaziOlcegi = 2f) { MeContent(state = Ornek.ben, onOpenSettings = {}) }

    @Config(qualifiers = UZUN)
    @Test fun y05_yazi200_ilaclar() = ekran("y05-yazi200-ilaclar", sekme = Screen.Meds, sistemYaziOlcegi = 2f) {
        Ilaclar(pil = BatteryStatus.OPTIMIZED, uretici = R.string.battery_oem_samsung)
    }

    @Config(qualifiers = UZUN)
    @Test fun y06_yazi200_ayarlar() = ekran("y06-yazi200-ayarlar", sistemYaziOlcegi = 2f) {
        SettingsContent(
            prefs = Ornek.ayarlar, exactAlarmsAllowed = true, snackbar = remember { SnackbarHostState() },
            onBack = {}, onUpdate = {}, onExport = {}, onWipe = {}, hasDoseReminders = true,
        )
    }

    @Config(qualifiers = UZUN)
    @Test fun y07_yazi200_bugun_hap() = ekran("y07-yazi200-bugun-hap", sekme = Screen.Today, sistemYaziOlcegi = 2f) {
        Bugun(Ornek.bugunHap, hour = 21)
    }

    @Config(qualifiers = UZUN)
    @Test fun y09_yazi200_ilac_formu() = ekran("y09-yazi200-ilac-formu", sistemYaziOlcegi = 2f) { IlacFormu(Ornek.ilacDuzenle) }

    @Config(qualifiers = UZUN)
    @Test fun y08_yazi200_gunluk() = ekran("y08-yazi200-gunluk", sistemYaziOlcegi = 2f) {
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
    @Test fun u03_ilaclar_tum() = ekran("u03-ilaclar-tum", sekme = Screen.Meds) { Ilaclar(pil = BatteryStatus.BACKGROUND_RESTRICTED) }

    @Config(qualifiers = UZUN)
    @Test fun u04_planim_tum() = ekran("u04-planim-tum", sekme = Screen.Plan) { Plan() }

    @Config(qualifiers = UZUN)
    @Test fun u05_ben_tum() = ekran("u05-ben-tum", sekme = Screen.Me) { MeContent(state = Ornek.ben, onOpenSettings = {}) }

    @Config(qualifiers = AYARLAR_TUM)
    @Test fun u06_ayarlar_tum() = ekran("u06-ayarlar-tum") {
        SettingsContent(
            prefs = Ornek.ayarlar, exactAlarmsAllowed = false, snackbar = remember { SnackbarHostState() },
            onBack = {}, onUpdate = {}, onExport = {}, onWipe = {},
            hasDoseReminders = true, batteryStatus = BatteryStatus.OPTIMIZED, oemBatteryNote = R.string.battery_oem_samsung,
        )
    }

    @Config(qualifiers = UZUN)
    @Test fun u07_ilac_duzenle_tum() = ekran("u07-ilac-duzenle-tum") { IlacFormu(Ornek.ilacDuzenle) }

    @Config(qualifiers = UZUN)
    @Test fun u08_hap_duzenle_tum() = ekran("u08-hap-duzenle-tum") { IlacFormu(Ornek.hapDuzenle) }
}

internal const val TELEFON = "w393dp-h852dp-xhdpi"
internal const val UZUN = "w393dp-h1800dp-xhdpi"
internal const val KUCUK = "w360dp-h740dp-xhdpi"
internal const val BESLENME_TUM = "w393dp-h5200dp-xhdpi"
internal const val HAREKET_TUM = "w393dp-h3400dp-xhdpi"
internal const val AYARLAR_TUM = "w393dp-h2900dp-xhdpi"
