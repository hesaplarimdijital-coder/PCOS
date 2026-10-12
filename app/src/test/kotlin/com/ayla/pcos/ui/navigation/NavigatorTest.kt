package com.ayla.pcos.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavKey
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Gezgin davranışları; Navigation 3 geçişinin (#22) kabul testleridir (ADR-0003). */
class NavigatorTest {

    private fun navigator(vararg keys: NavKey, tab: Screen.Tab = Screen.Today): AylaNavigator =
        AylaNavigator(mutableListOf(*keys), mutableStateOf(tab))

    @Test
    fun `tum rotalar geri okunur`() {
        val screens = listOf(
            Screen.Today, Screen.Calendar, Screen.Meds, Screen.Plan, Screen.Me,
            Screen.DailyLog(LocalDate.of(2026, 10, 12)), Screen.AddMedication, Screen.EditMedication(42), Screen.EditMedication(0),
            Screen.Settings, Screen.RestoreBackup, Screen.PrivacyPolicy, Screen.Fertility, Screen.Report, Screen.Questions, Screen.Skin,
        )
        screens.forEach { assertEquals(it, Screen.fromRoute(it.route), it.route) }
        assertNull(Screen.fromRoute("meds/edit/abc"))
        assertNull(Screen.fromRoute("bilinmeyen"))
    }

    @Test
    fun `geri once yigini sonra bugun sekmesini bosaltir`() {
        val nav = navigator(TabsRoot)
        nav.selectTab(Screen.Meds)
        nav.push(Screen.AddMedication)
        assertFalse(nav.showsBottomBar)
        assertTrue(nav.canGoBack)
        assertTrue(nav.pop())
        assertEquals(Screen.Meds, nav.current)
        assertTrue(nav.showsBottomBar)
        assertTrue(nav.pop())
        assertEquals(Screen.Today, nav.current)
        assertFalse(nav.canGoBack)
        assertFalse(nav.pop())
        assertEquals(listOf<NavKey>(TabsRoot), nav.backStack.toList(), "kök giriş hiç çıkmaz")
    }

    @Test
    fun `replaceTop ekleme ekranini forma cevirir ve kaydetmeden sonra listeye doner`() {
        val nav = navigator(TabsRoot)
        nav.selectTab(Screen.Meds)
        nav.push(Screen.AddMedication)
        nav.replaceTop(Screen.EditMedication(0))
        assertEquals(listOf<Screen>(Screen.EditMedication(0)), nav.stack)
        assertEquals(listOf<NavKey>(TabsRoot, Screen.EditMedication(0)), nav.backStack.toList())
        nav.pop()
        assertEquals(Screen.Meds, nav.current)
    }

    @Test
    fun `ayni ekran ust uste itilmez`() {
        val nav = navigator(TabsRoot)
        nav.push(Screen.Settings)
        nav.push(Screen.Settings)
        assertEquals(1, nav.stack.size)
        nav.push(Screen.PrivacyPolicy)
        assertEquals(listOf<Screen>(Screen.Settings, Screen.PrivacyPolicy), nav.stack)
    }

    @Test
    fun `sekme secimi yigini bosaltir, sekme itmek sekme secmektir`() {
        val nav = navigator(TabsRoot)
        nav.push(Screen.Settings)
        nav.push(Screen.PrivacyPolicy)
        nav.push(Screen.Plan)
        assertEquals(Screen.Plan, nav.tab)
        assertEquals(Screen.Plan, nav.current)
        assertTrue(nav.stack.isEmpty())
        assertTrue(nav.showsBottomBar)
    }

    @Test
    fun `kaydedilmis yigin ve sekme surec olumunden geri gelir`() {
        val restored = navigator(TabsRoot, Screen.Settings, Screen.PrivacyPolicy, tab = Screen.Me)
        assertEquals(Screen.PrivacyPolicy, restored.current)
        assertEquals(listOf<Screen>(Screen.Settings, Screen.PrivacyPolicy), restored.stack)
        assertEquals(Screen.Me, restored.tab)
        assertEquals(Screen.Me, AylaNavigator.TabSaver.restore(Screen.Me.route))
        assertNull(AylaNavigator.TabSaver.restore("settings"), "sekme olmayan rota sekme olarak okunmaz")
    }

    @Test
    fun `kok girisi olmayan yigin onarilir`() {
        val bozuk = navigator(Screen.Settings)
        assertEquals(listOf<NavKey>(TabsRoot), bozuk.backStack.toList())
        val bos = AylaNavigator(mutableListOf(), mutableStateOf(Screen.Today))
        assertEquals(listOf<NavKey>(TabsRoot), bos.backStack.toList())
        assertEquals(Screen.Today, bos.current)
    }
}
