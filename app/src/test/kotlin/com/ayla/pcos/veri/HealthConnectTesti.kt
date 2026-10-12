package com.ayla.pcos.veri

import com.ayla.pcos.data.db.SleepSource
import com.ayla.pcos.data.health.ExternalSleepSession
import com.ayla.pcos.data.health.HealthConnectAvailability
import com.ayla.pcos.data.health.HealthConnectRepository
import com.ayla.pcos.data.health.HealthConnectSource
import com.ayla.pcos.data.health.SleepImportResult
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.ui.health.HcMessage
import com.ayla.pcos.ui.health.HcStep
import com.ayla.pcos.ui.health.HealthConnectViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Health Connect'in yerine geçen sahte kaynak: okunan aralığı ve çağrıları kaydeder. */
class SahteHealthConnect : HealthConnectSource {
    var durum = HealthConnectAvailability.AVAILABLE
    var izin = true
    var oturumlar: List<ExternalSleepSession> = emptyList()
    var hata: Exception? = null
    var okumaSayisi = 0
    var geriAlindi = false

    override fun availability() = durum

    override suspend fun hasSleepPermission() = izin

    override suspend fun readSleepSessions(from: Instant, to: Instant): List<ExternalSleepSession> {
        okumaSayisi++
        hata?.let { throw it }
        return oturumlar.filter { it.end.isAfter(from) && it.start.isBefore(to) }
    }

    override suspend fun revokeAllPermissions() {
        geriAlindi = true
        izin = false
    }
}

/** İleri sarılabilen saat: aynı andaki ikinci alma birleştirildiği için testler zamanı elle ilerletir. */
class OynakSaat(var an: Instant, private val dilim: ZoneId) : Clock() {
    override fun getZone(): ZoneId = dilim

    override fun withZone(zone: ZoneId): Clock = OynakSaat(an, zone)

    override fun instant(): Instant = an

    fun ilerlet(dakika: Long) {
        an = an.plusSeconds(dakika * 60)
    }
}

/** Health Connect'ten uyku alma (#11): rıza ve izin kapıları, yineleme, hatalar ve akış adımları. */
class HealthConnectTesti : VeriTestOrtami() {

    private val sahte = SahteHealthConnect()
    private val saat by lazy { OynakSaat(bugun.atTime(10, 0).atZone(zone).toInstant(), zone) }
    private val depo by lazy { HealthConnectRepository(sahte, db.sleepDao(), prefs, saat) }

    /** [gunOnce] gün önce [kalkis]'ta biten gece; yatış kalkıştan sonraysa önceki akşamdır. */
    private fun gece(id: String, gunOnce: Long, yatis: LocalTime, kalkis: LocalTime): ExternalSleepSession {
        val uyanis = bugun.minusDays(gunOnce)
        val yatisGunu = if (yatis.isAfter(kalkis)) uyanis.minusDays(1) else uyanis
        val start = yatisGunu.atTime(yatis).atZone(zone).toInstant()
        val end = uyanis.atTime(kalkis).atZone(zone).toInstant()
        return ExternalSleepSession(id, start, end, ZoneOffset.ofHours(3))
    }

    /** Ön plana geliş sinyali olmayan gün kaynağı: başlangıçtaki sessiz eşitleme testleri karıştırmasın. */
    private fun vm() = kaydet(
        HealthConnectViewModel(
            depo,
            prefs,
            object : DateTicker(saat) {
                override val foreground: Flow<Unit> = emptyFlow()
            },
        ),
    )

    private fun rizaVer() = calistir { depo.giveConsent() }

    @Test
    fun `riza yokken izin verilmis olsa bile hicbir sey okunmaz`() {
        sahte.oturumlar = listOf(gece("a", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)))
        assertEquals(SleepImportResult.NoPermission, calistir { depo.importNow() })
        assertNull(calistir { depo.syncIfConnected() })
        assertEquals(0, sahte.okumaSayisi)
        assertTrue(calistir { db.sleepDao().getAll() }.isEmpty())
    }

    @Test
    fun `riza ve izinle son 30 gun alinir, ikinci almada ayni gece eklenmez`() {
        rizaVer()
        sahte.oturumlar = listOf(
            gece("a", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)),
            gece("b", 1, LocalTime.of(0, 15), LocalTime.of(6, 45)),
            gece("eski", 9, LocalTime.of(23, 0), LocalTime.of(7, 0)), // son 7 gecenin dışı
        )
        assertEquals(SleepImportResult.Added(2), calistir { depo.importNow() })
        saat.ilerlet(1)
        assertEquals(SleepImportResult.Added(0), calistir { depo.importNow() })
        val kayitlar = calistir { db.sleepDao().getAll() }
        assertEquals(setOf("a", "b"), kayitlar.mapNotNull { it.externalId }.toSet())
        assertTrue(kayitlar.all { it.source == SleepSource.HEALTH_CONNECT && it.quality == null })
        assertEquals(2, calistir { depo.importedCount.first() })
    }

    @Test
    fun `izin kalkmissa ya da okuma sirasinda geri alinirsa izin yok sonucu doner`() {
        rizaVer()
        sahte.izin = false
        assertEquals(SleepImportResult.NoPermission, calistir { depo.importNow() })
        saat.ilerlet(1)
        sahte.izin = true
        sahte.hata = SecurityException("izin geri alındı")
        assertEquals(SleepImportResult.NoPermission, calistir { depo.importNow() })
        assertEquals(1, sahte.okumaSayisi)
    }

    @Test
    fun `servis hatasi sessizce yutulmaz, kullanilamiyorsa okunmaz`() {
        rizaVer()
        sahte.hata = IllegalStateException("kota")
        assertEquals(SleepImportResult.Failed, calistir { depo.importNow() })
        // Başarısız denemeden sonra ön plan eşitlemesi saat dolmadan yeniden zorlamaz.
        saat.ilerlet(5)
        assertNull(calistir { depo.syncIfConnected() })
        sahte.hata = null
        sahte.durum = HealthConnectAvailability.UPDATE_REQUIRED
        assertEquals(SleepImportResult.Unavailable, calistir { depo.importNow() })
    }

    @Test
    fun `on plana geliste en sik saatte bir okunur`() {
        rizaVer()
        sahte.oturumlar = listOf(gece("a", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)))
        assertEquals(SleepImportResult.Added(1), calistir { depo.syncIfConnected() })
        saat.ilerlet(30)
        assertNull(calistir { depo.syncIfConnected() })
        saat.ilerlet(31)
        assertEquals(SleepImportResult.Added(0), calistir { depo.syncIfConnected() })
        assertEquals(2, sahte.okumaSayisi)
    }

    @Test
    fun `ayni anda gelen ikinci alma yeniden okumaz, ilk sonucu dondurur`() {
        rizaVer()
        sahte.oturumlar = listOf(gece("a", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)))
        assertEquals(SleepImportResult.Added(1), calistir { depo.importNow() })
        assertEquals(SleepImportResult.Added(1), calistir { depo.importNow() }, "izin dönüşü + ön plan eşitlemesi")
        assertNull(calistir { depo.syncIfConnected() })
        assertEquals(1, sahte.okumaSayisi)
    }

    @Test
    fun `aylada silinen health connect gecesi yeniden alinmaz, pencereden cikinca kimligi duser`() {
        rizaVer()
        sahte.oturumlar = listOf(
            gece("a", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)),
            gece("b", 1, LocalTime.of(23, 0), LocalTime.of(6, 30)),
        )
        calistir { depo.importNow() }
        val a = calistir { db.sleepDao().getAll() }.single { it.externalId == "a" }
        calistir { sleep.delete(a) }
        assertEquals(setOf("a"), calistir { prefs.current().healthConnectHiddenIds })

        saat.ilerlet(1)
        assertEquals(SleepImportResult.Added(0), calistir { depo.importNow() })
        assertEquals(listOf("b"), calistir { db.sleepDao().getAll() }.mapNotNull { it.externalId })

        // "a" Health Connect penceresinden çıktı (ya da orada silindi): kimliği artık tutulmaz.
        sahte.oturumlar = sahte.oturumlar.filter { it.id != "a" }
        saat.ilerlet(1)
        calistir { depo.importNow() }
        assertTrue(calistir { prefs.current().healthConnectHiddenIds }.isEmpty())
    }

    @Test
    fun `alinan geceleri silmek elle girilenlere dokunmaz, baglanti kesmek rizayi siler`() {
        rizaVer()
        sahte.oturumlar = listOf(gece("a", 1, LocalTime.of(23, 30), LocalTime.of(7, 0)))
        calistir {
            depo.importNow()
            sleep.addManual(
                bugun.minusDays(1).atTime(23, 0).atZone(zone).toInstant(),
                bugun.atTime(6, 30).atZone(zone).toInstant(),
                quality = 3,
                zone = zone,
            )
        }
        assertEquals(1, calistir { depo.deleteImported() })
        assertEquals(listOf(SleepSource.MANUAL), calistir { db.sleepDao().getAll() }.map { it.source })
        // Bağlantı açık kalsa da silinenler geri gelmez.
        saat.ilerlet(1)
        assertEquals(SleepImportResult.Added(0), calistir { depo.importNow() })
        assertEquals(listOf(SleepSource.MANUAL), calistir { db.sleepDao().getAll() }.map { it.source })

        calistir { depo.disconnect() }
        assertNull(calistir { prefs.current().healthConnectConsentAt })
        assertTrue(sahte.geriAlindi)
    }

    @Test
    fun `dun gece ogle uykusu degil en uzun oturumdur`() {
        rizaVer()
        sahte.oturumlar = listOf(
            gece("gece", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)),
            ExternalSleepSession(
                "ogle",
                bugun.atTime(8, 30).atZone(zone).toInstant(),
                bugun.atTime(9, 10).atZone(zone).toInstant(),
                ZoneOffset.ofHours(3),
            ),
        )
        calistir { depo.importNow() }
        assertEquals("gece", bekle(sleep.observeNight(bugun)) { it != null }?.externalId)
    }

    @Test
    fun `izin penceresi iki kez kapatilinca bir daha acilmaz, Health Connect ayarlari onerilir`() {
        sahte.izin = false
        val vm = vm()
        vm.start()
        assertEquals(HcStep.RATIONALE, bekle(vm.uiState) { it.step != null }.step)

        vm.acceptRationale()
        assertEquals(HcStep.REQUEST_PERMISSION, bekle(vm.uiState) { it.step == HcStep.REQUEST_PERMISSION }.step)
        assertNotNull(calistir { prefs.current().healthConnectConsentAt })
        vm.permissionRequestLaunched()
        vm.onPermissionResult(false)
        assertEquals(HcMessage.PermissionDenied, bekle(vm.uiState) { it.message != null }.message)

        vm.start()
        bekle(vm.uiState) { it.step == HcStep.REQUEST_PERMISSION }
        vm.permissionRequestLaunched()
        vm.onPermissionResult(false)
        calistir {
            repeat(50) {
                if (prefs.current().healthConnectPermissionCancels == 2) return@calistir
                kotlinx.coroutines.delay(20)
            }
        }
        vm.start()
        assertEquals(HcStep.CANCELLED_TWICE, bekle(vm.uiState) { it.step == HcStep.CANCELLED_TWICE }.step)
    }

    @Test
    fun `izin verilince sayac sifirlanir ve geceler alinir, guncelleme gerekiyorsa Play adimi acilir`() {
        sahte.oturumlar = listOf(gece("a", 0, LocalTime.of(23, 30), LocalTime.of(7, 0)))
        sahte.izin = false
        calistir { prefs.update { it.copy(healthConnectPermissionCancels = 1) } }
        rizaVer()
        val vm = vm()
        sahte.izin = true
        vm.onPermissionResult(true)
        assertEquals(HcMessage.Added(1), bekle(vm.uiState) { it.message is HcMessage.Added }.message)
        assertEquals(0, calistir { prefs.current().healthConnectPermissionCancels })
        assertTrue(bekle(vm.uiState) { it.connected }.connected)

        sahte.durum = HealthConnectAvailability.UPDATE_REQUIRED
        vm.start()
        assertEquals(HcStep.UPDATE_REQUIRED, bekle(vm.uiState) { it.step != null }.step)
        assertFalse(bekle(vm.uiState) { !it.connected }.connected)
    }
}
