package com.ayla.pcos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * Ekranlar. Beş sekme kök ekrandır; diğerleri sekmenin üstüne itilir.
 * Navigation 3'e geçiş planlanmıştır (bkz. docs/adr/0003-navigasyon.md); MVP 1'de derlemeyi
 * belirleyici tutmak için küçük bir yığın kullanılır.
 */
sealed interface Screen {
    val route: String

    sealed interface Tab : Screen

    data object Today : Tab { override val route = "today" }
    data object Calendar : Tab { override val route = "calendar" }
    data object Meds : Tab { override val route = "meds" }
    data object Plan : Tab { override val route = "plan" }
    data object Me : Tab { override val route = "me" }

    data class DailyLog(val date: LocalDate) : Screen { override val route = "log/$date" }
    data object AddMedication : Screen { override val route = "meds/add" }

    /** [id] 0 ise yeni ilaç formu. */
    data class EditMedication(val id: Long) : Screen { override val route = "meds/edit/$id" }
    data object Settings : Screen { override val route = "settings" }

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
            route.startsWith("log/") -> runCatching { DailyLog(LocalDate.parse(route.removePrefix("log/"))) }.getOrNull()
            route.startsWith("meds/edit/") -> route.removePrefix("meds/edit/").toLongOrNull()?.let { EditMedication(it) }
            else -> null
        }
    }
}

/** Sekme + üstüne itilen ekranlar. Geri: önce yığın, sonra Bugün sekmesi, sonra sistem. */
class AylaNavigator(initialTab: Screen.Tab = Screen.Today, initialStack: List<Screen> = emptyList()) {
    var tab: Screen.Tab by mutableStateOf(initialTab)
        private set

    val stack = mutableStateListOf<Screen>().apply { addAll(initialStack) }

    val current: Screen get() = stack.lastOrNull() ?: tab

    val showsBottomBar: Boolean get() = stack.isEmpty()

    val canGoBack: Boolean get() = stack.isNotEmpty() || tab != Screen.Today

    fun selectTab(target: Screen.Tab) {
        stack.clear()
        tab = target
    }

    fun push(screen: Screen) {
        if (screen is Screen.Tab) {
            selectTab(screen)
        } else if (stack.lastOrNull() != screen) {
            stack.add(screen)
        }
    }

    /** Üstteki ekranı [screen] ile değiştirir (ör. "ilaç ekle" → "yeni ilaç formu"). */
    fun replaceTop(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        push(screen)
    }

    fun pop(): Boolean = when {
        stack.isNotEmpty() -> {
            stack.removeAt(stack.lastIndex)
            true
        }
        tab != Screen.Today -> {
            tab = Screen.Today
            true
        }
        else -> false
    }

    companion object {
        val Saver = listSaver<AylaNavigator, String>(
            save = { nav -> listOf(nav.tab.route) + nav.stack.map { it.route } },
            restore = { routes ->
                val tab = routes.firstOrNull()?.let { Screen.fromRoute(it) } as? Screen.Tab ?: Screen.Today
                val stack = routes.drop(1).mapNotNull { Screen.fromRoute(it) }
                AylaNavigator(tab, stack)
            },
        )
    }
}

@Composable
fun rememberAylaNavigator(): AylaNavigator = rememberSaveable(saver = AylaNavigator.Saver) { AylaNavigator() }
