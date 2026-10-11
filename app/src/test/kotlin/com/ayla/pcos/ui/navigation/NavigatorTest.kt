package com.ayla.pcos.ui.navigation

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigatorTest {

    @Test
    fun `tum rotalar geri okunur`() {
        val screens = listOf(
            Screen.Today, Screen.Calendar, Screen.Meds, Screen.Plan, Screen.Me,
            Screen.DailyLog(LocalDate.of(2026, 10, 12)), Screen.AddMedication, Screen.EditMedication(42), Screen.EditMedication(0),
            Screen.Settings,
        )
        screens.forEach { assertEquals(it, Screen.fromRoute(it.route), it.route) }
        assertNull(Screen.fromRoute("meds/edit/abc"))
        assertNull(Screen.fromRoute("bilinmeyen"))
    }

    @Test
    fun `geri once yigini sonra bugun sekmesini bosaltir`() {
        val nav = AylaNavigator()
        nav.selectTab(Screen.Meds)
        nav.push(Screen.AddMedication)
        assertFalse(nav.showsBottomBar)
        assertTrue(nav.pop())
        assertEquals(Screen.Meds, nav.current)
        assertTrue(nav.pop())
        assertEquals(Screen.Today, nav.current)
        assertFalse(nav.pop())
    }

    @Test
    fun `replaceTop ekleme ekranini forma cevirir ve kaydetmeden sonra listeye doner`() {
        val nav = AylaNavigator()
        nav.selectTab(Screen.Meds)
        nav.push(Screen.AddMedication)
        nav.replaceTop(Screen.EditMedication(0))
        assertEquals(listOf<Screen>(Screen.EditMedication(0)), nav.stack.toList())
        nav.pop()
        assertEquals(Screen.Meds, nav.current)
    }

    @Test
    fun `ayni ekran ust uste itilmez`() {
        val nav = AylaNavigator()
        nav.push(Screen.Settings)
        nav.push(Screen.Settings)
        assertEquals(1, nav.stack.size)
    }
}
