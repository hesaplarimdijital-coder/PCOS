package com.ayla.pcos.veri

import com.ayla.pcos.data.prefs.UserPreferences
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Rıza sürümü DataStore'a yazılır ve okunur; eski kurulumda anahtar yoktur (#20). */
class RizaSurumuDepoTesti : VeriTestOrtami() {

    @Test
    fun `eski kurulumdaki riza yenilenene kadar eski kalir, yenilenince surum saklanir`() {
        val eski = Instant.parse("2026-08-01T08:00:00Z")
        calistir { prefs.update { it.copy(onboardingDone = true, healthDataConsentAt = eski) } }
        val once = calistir { prefs.current() }
        assertEquals(0, once.healthConsentVersion)
        assertTrue(once.healthConsentOutdated)

        val yeni = Instant.parse("2026-10-12T09:00:00Z")
        calistir { prefs.update { it.copy(healthDataConsentAt = yeni, healthConsentVersion = UserPreferences.HEALTH_CONSENT_VERSION) } }
        val sonra = calistir { prefs.current() }
        assertEquals(UserPreferences.HEALTH_CONSENT_VERSION, sonra.healthConsentVersion)
        assertEquals(yeni, sonra.healthDataConsentAt)
        assertFalse(sonra.healthConsentOutdated)
    }
}
