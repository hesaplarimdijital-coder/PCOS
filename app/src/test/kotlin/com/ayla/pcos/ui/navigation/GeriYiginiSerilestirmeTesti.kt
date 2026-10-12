package com.ayla.pcos.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * Süreç ölümü: `rememberNavBackStack` geri yığınını `NavBackStackSerializer(NavKeySerializer())` ile SavedState'e
 * yazar (Android'de sınıf adı + `serializer()` yansımasıyla). `@Serializable` unutulmuş bir ekran derlemede değil,
 * uygulama arka plana giderken çöker; bu test onu CI'a taşır (ADR-0003). Yeni `Screen` alt türü [hepsi]'ne eklenir.
 */
@RunWith(RobolectricTestRunner::class)
class GeriYiginiSerilestirmeTesti {

    private val hepsi: List<NavKey> = listOf(
        TabsRoot,
        Screen.Today, Screen.Calendar, Screen.Meds, Screen.Plan, Screen.Me,
        Screen.DailyLog(LocalDate.of(2026, 10, 12)), Screen.AddMedication, Screen.EditMedication(42), Screen.EditMedication(0),
        Screen.Settings, Screen.RestoreBackup, Screen.PrivacyPolicy,
    )

    private val serializer = NavBackStackSerializer(NavKeySerializer<NavKey>())

    private fun gidisDonus(keys: List<NavKey>): List<NavKey> {
        val kayit = encodeToSavedState(serializer, NavBackStack(*keys.toTypedArray()))
        return decodeFromSavedState(serializer, kayit).toList()
    }

    @Test
    fun `her ekran anahtari tek basina yazilip okunur`() {
        hepsi.forEach { key -> assertEquals(listOf(key), gidisDonus(listOf(key)), key.toString()) }
    }

    @Test
    fun `tipik bir yigin sirasiyla geri gelir ve gezgin onu kabul eder`() {
        val yigin = listOf(TabsRoot, Screen.DailyLog(LocalDate.of(2026, 9, 30)), Screen.Settings, Screen.PrivacyPolicy)
        val geri = gidisDonus(yigin)
        assertEquals(yigin, geri)
        val nav = AylaNavigator(geri.toMutableList(), mutableStateOf(Screen.Me))
        assertEquals(Screen.PrivacyPolicy, nav.current)
        assertEquals(3, nav.stack.size)
    }

    @Test
    fun `geri yuklenen yigindaki sekme ve kopya anahtarlar temizlenir`() {
        val nav = AylaNavigator(
            mutableListOf(TabsRoot, Screen.Plan, Screen.Settings, Screen.Settings, TabsRoot, Screen.PrivacyPolicy),
            mutableStateOf(Screen.Today),
        )
        assertEquals(listOf<NavKey>(TabsRoot, Screen.Settings, Screen.PrivacyPolicy), nav.backStack.toList())
    }
}
