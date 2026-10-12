package com.ayla.pcos.data.prefs

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Açık rıza kapsam sürümü (#20): eski rıza 1. sürüm sayılır ve yenilenmesi istenir. */
class RizaSurumuTest {

    private val zaman = Instant.parse("2026-09-01T08:00:00Z")

    @Test
    fun `riza yoksa surum sifirdir ve eski sayilmaz`() {
        val p = UserPreferences()
        assertEquals(0, p.effectiveHealthConsentVersion)
        assertFalse(p.healthConsentOutdated)
    }

    @Test
    fun `surum kaydi olmayan eski riza birinci surumdur ve yenilenmelidir`() {
        val p = UserPreferences(healthDataConsentAt = zaman)
        assertEquals(1, p.effectiveHealthConsentVersion)
        assertTrue(p.healthConsentOutdated)
    }

    @Test
    fun `guncel surumle verilen riza eski sayilmaz`() {
        val p = UserPreferences(healthDataConsentAt = zaman, healthConsentVersion = UserPreferences.HEALTH_CONSENT_VERSION)
        assertEquals(UserPreferences.HEALTH_CONSENT_VERSION, p.effectiveHealthConsentVersion)
        assertFalse(p.healthConsentOutdated)
    }

    @Test
    fun `guncel kapsam dogurganlik ve cilt verisinin eklendigi ikinci surumdur`() {
        assertEquals(2, UserPreferences.HEALTH_CONSENT_VERSION)
    }
}
