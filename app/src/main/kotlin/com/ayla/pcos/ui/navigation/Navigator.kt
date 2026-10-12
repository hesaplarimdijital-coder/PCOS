package com.ayla.pcos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.ayla.pcos.data.LocalDateIso
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Ekranlar. Beş sekme kök ekrandır; diğerleri sekmenin üstüne itilir ve Navigation 3 geri yığınında
 * ([NavKey]) taşınır; bu yüzden hepsi serileştirilebilir ve yalnızca ilkel değer taşır
 * (bkz. docs/adr/0003-navigasyon.md). [route] metni yalnızca sekme kaydı ve testler içindir.
 */
sealed interface Screen : NavKey {
    val route: String

    sealed interface Tab : Screen

    @Serializable
    data object Today : Tab { override val route: String get() = "today" }

    @Serializable
    data object Calendar : Tab { override val route: String get() = "calendar" }

    @Serializable
    data object Meds : Tab { override val route: String get() = "meds" }

    @Serializable
    data object Plan : Tab { override val route: String get() = "plan" }

    @Serializable
    data object Me : Tab { override val route: String get() = "me" }

    @Serializable
    data class DailyLog(@Serializable(with = LocalDateIso::class) val date: LocalDate) : Screen {
        override val route: String get() = "log/$date"
    }

    @Serializable
    data object AddMedication : Screen { override val route: String get() = "meds/add" }

    /** [id] 0 ise yeni ilaç formu. */
    @Serializable
    data class EditMedication(val id: Long) : Screen { override val route: String get() = "meds/edit/$id" }

    @Serializable
    data object Settings : Screen { override val route: String get() = "settings" }

    /** Ayarlar, yedek dosyası seçici açık başlar (anahtar kaybı diyaloğundan gelinir). */
    @Serializable
    data object RestoreBackup : Screen { override val route: String get() = "settings/restore" }

    /** Gizlilik politikası ve KVKK aydınlatma metni. */
    @Serializable
    data object PrivacyPolicy : Screen { override val route: String get() = "privacy" }

    /** Doğurganlık: ısı grafiği ve notlar (#24). */
    @Serializable
    data object Fertility : Screen { override val route: String get() = "fertility" }

    /** Cilt ve saç: rutin, profil, doktor kartları, fotoğraflar (#25). */
    @Serializable
    data object Skin : Screen { override val route: String get() = "skin" }

    /** Doktor raporu: aralık, bölümler, PDF/metin paylaşımı (#26). */
    @Serializable
    data object Report : Screen { override val route: String get() = "report" }

    /** Doktora soracaklarım (#26). */
    @Serializable
    data object Questions : Screen { override val route: String get() = "report/questions" }

    companion object {
        val tabs: List<Tab> = listOf(Today, Calendar, Meds, Plan, Me)

        fun fromRoute(route: String): Screen? = when {
            route == Today.route -> Today
            route == Calendar.route -> Calendar
            route == Meds.route -> Meds
            route == Plan.route -> Plan
            route == Me.route -> Me
            route == AddMedication.route -> AddMedication
            route == Settings.route -> Settings
            route == RestoreBackup.route -> RestoreBackup
            route == PrivacyPolicy.route -> PrivacyPolicy
            route == Fertility.route -> Fertility
            route == Report.route -> Report
            route == Skin.route -> Skin
            route == Questions.route -> Questions
            route.startsWith("log/") -> runCatching { DailyLog(LocalDate.parse(route.removePrefix("log/"))) }.getOrNull()
            route.startsWith("meds/edit/") -> route.removePrefix("meds/edit/").toLongOrNull()?.let { EditMedication(it) }
            else -> null
        }
    }
}

/**
 * Geri yığınının kök girişi: beş sekme bu girişin içinde çizilir ve değişir; yığından hiç çıkmaz.
 * Böylece sekme ViewModel'leri sekme değişiminde sıcak kalır ve sekme değişimi geri yığınına girmez.
 */
@Serializable
data object TabsRoot : NavKey

/**
 * Sekme + üstüne itilen ekranlar, Navigation 3 geri yığını üzerinde. [backStack] her zaman [TabsRoot] ile
 * başlar; sonrası itilen [Screen]'lerdir. Geri: önce yığın, sonra Bugün sekmesi, sonra sistem.
 * Sekme değişimi yığını boşaltır; aynı ekran üst üste itilmez.
 */
class AylaNavigator(val backStack: MutableList<NavKey>, private val tabState: MutableState<Screen.Tab>) {

    init {
        // Kök giriş eksikse (bozuk ya da eski kayıt) yığın sıfırlanır; NavDisplay boş yığın kabul etmez.
        if (backStack.firstOrNull() != TabsRoot) {
            backStack.clear()
            backStack.add(TabsRoot)
        }
        // Kökten sonra yalnızca itilebilen ekranlar kalır: sekme ya da ikinci bir kök entryProvider'da karşılıksızdır.
        // Ardışık kopyalar da temizlenir (push bunu zaten engeller; eski kayıtlara karşı).
        val pushed = backStack.drop(1).filter { it is Screen && it !is Screen.Tab }
        val cleaned = pushed.filterIndexed { index, key -> index == 0 || pushed[index - 1] != key }
        if (cleaned.size != backStack.size - 1) {
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            backStack.addAll(cleaned)
        }
    }

    var tab: Screen.Tab
        get() = tabState.value
        private set(value) {
            tabState.value = value
        }

    /** İtilen ekranlar, alttan üste. */
    val stack: List<Screen> get() = backStack.drop(1).filterIsInstance<Screen>()

    val current: Screen get() = backStack.lastOrNull() as? Screen ?: tab

    val showsBottomBar: Boolean get() = backStack.size <= 1

    val canGoBack: Boolean get() = backStack.size > 1 || tab != Screen.Today

    fun selectTab(target: Screen.Tab) {
        clearStack()
        tab = target
    }

    fun push(screen: Screen) {
        if (screen is Screen.Tab) {
            selectTab(screen)
        } else if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
    }

    /** Üstteki ekranı [screen] ile değiştirir (ör. "ilaç ekle" → "yeni ilaç formu"). */
    fun replaceTop(screen: Screen) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        push(screen)
    }

    fun pop(): Boolean = when {
        backStack.size > 1 -> {
            backStack.removeAt(backStack.lastIndex)
            true
        }
        tab != Screen.Today -> {
            tab = Screen.Today
            true
        }
        else -> false
    }

    private fun clearStack() {
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        /** Seçili sekme süreç ölümünden rota metniyle döner. */
        val TabSaver: Saver<Screen.Tab, String> = Saver(
            save = { it.route },
            restore = { Screen.fromRoute(it) as? Screen.Tab },
        )
    }
}

/**
 * Geri yığını `rememberNavBackStack` ile (Navigation 3; anahtarlar serileştirilerek süreç ölümüne dayanır),
 * seçili sekme ayrı bir kayıtla tutulur.
 */
@Composable
fun rememberAylaNavigator(): AylaNavigator {
    val backStack = rememberNavBackStack(TabsRoot)
    val tab = rememberSaveable(stateSaver = AylaNavigator.TabSaver) { mutableStateOf<Screen.Tab>(Screen.Today) }
    return remember(backStack, tab) { AylaNavigator(backStack, tab) }
}
