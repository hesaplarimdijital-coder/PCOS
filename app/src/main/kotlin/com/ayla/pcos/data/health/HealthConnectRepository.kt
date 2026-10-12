package com.ayla.pcos.data.health

import com.ayla.pcos.data.db.SleepDao
import com.ayla.pcos.data.db.SleepSource
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Health Connect'ten uyku alma sonucu. */
sealed interface SleepImportResult {
    /** [count] yeni gece eklendi (0: eklenecek yeni gece yok). */
    data class Added(val count: Int) : SleepImportResult

    /** İzin yok ya da okuma sırasında geri alındı. */
    data object NoPermission : SleepImportResult

    data object Unavailable : SleepImportResult

    /** Servis yanıt vermedi, kota ya da beklenmeyen hata; "sonra dene". */
    data object Failed : SleepImportResult
}

/**
 * Health Connect bağlantısı (#11): yalnızca uyku okunur, yalnızca uygulama açıkken ve yalnızca kullanıcının
 * açık rızası ([com.ayla.pcos.data.prefs.UserPreferences.healthConnectConsentAt]) ile Health Connect izni
 * birlikte varken. Alınan geceler şifreli veritabanına `source = HEALTH_CONNECT` ile yazılır.
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val source: HealthConnectSource,
    private val sleepDao: SleepDao,
    private val prefs: UserPreferencesRepository,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    // Yalnızca kilit altında okunur ve yazılır.
    private var lastAttemptAt: Instant? = null
    private var lastResult: SleepImportResult? = null

    fun availability(): HealthConnectAvailability = runCatching { source.availability() }.getOrDefault(HealthConnectAvailability.UNAVAILABLE)

    suspend fun hasSleepPermission(): Boolean = try {
        availability() == HealthConnectAvailability.AVAILABLE && source.hasSleepPermission()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    val importedCount: Flow<Int> = sleepDao.observeCount(SleepSource.HEALTH_CONNECT)

    /** Gerekçe ekranında "Onaylıyorum, devam et". */
    suspend fun giveConsent() = prefs.update { it.copy(healthConnectConsentAt = clock.instant()) }

    /** İzin penceresinin sonucu: verilince sayaç sıfırlanır, kapatılınca bir artar. */
    suspend fun recordPermissionResult(granted: Boolean) = prefs.update {
        it.copy(healthConnectPermissionCancels = if (granted) 0 else it.healthConnectPermissionCancels + 1)
    }

    /**
     * Ön plana gelişte: rıza ve izin varsa, son denemeden bu yana [minInterval] geçtiyse sessizce alır (başarısız
     * denemeden sonra da aynı aralık beklenir, Health Connect'i her öne gelişte zorlamamak için). Koşullardan
     * biri yoksa hiçbir şey okunmaz ve null döner. Aralık denetimi kilit altındadır: aynı anda gelen elle alma
     * ve ön plan eşitlemesi tek okumaya iner.
     */
    suspend fun syncIfConnected(minInterval: Duration = SYNC_INTERVAL): SleepImportResult? = mutex.withLock {
        val now = clock.instant()
        val last = lastAttemptAt
        when {
            last != null && Duration.between(last, now) < minInterval -> null
            prefs.current().healthConnectConsentAt == null -> null
            !hasSleepPermission() -> null
            else -> importLocked(now)
        }
    }

    /**
     * Son [IMPORT_WINDOW] içindeki uyku oturumlarını alır; aynı gece iki kez eklenmez, Ayla'dan silinen gece
     * yeniden eklenmez. Birkaç saniye içindeki ikinci çağrı (çift dokunuş, izin dönüşü + ön plan eşitlemesi)
     * yeniden okumaz, ilk sonucu döndürür.
     */
    suspend fun importNow(): SleepImportResult = mutex.withLock {
        val now = clock.instant()
        val last = lastAttemptAt
        val previous = lastResult
        if (last != null && previous != null && Duration.between(last, now) < DEDUP_WINDOW) previous else importLocked(now)
    }

    private suspend fun importLocked(now: Instant): SleepImportResult {
        val result = readAndStore(now)
        lastAttemptAt = now
        lastResult = result
        return result
    }

    private suspend fun readAndStore(now: Instant): SleepImportResult {
        if (availability() != HealthConnectAvailability.AVAILABLE) return SleepImportResult.Unavailable
        val p = prefs.current()
        if (p.healthConnectConsentAt == null) return SleepImportResult.NoPermission
        return try {
            if (!source.hasSleepPermission()) {
                SleepImportResult.NoPermission
            } else {
                val sessions = source.readSleepSessions(now.minus(IMPORT_WINDOW), now)
                val ids = sessions.mapTo(HashSet()) { it.id }
                val hidden = p.healthConnectHiddenIds
                val visible = sessions.filter { it.id !in hidden }
                val fresh = selectNewSleep(sleepDao.getAll(), visible.map { it.toSleepEntity(clock.zone) })
                if (fresh.isNotEmpty()) sleepDao.insertAll(fresh)
                // Pencereden çıkan (ya da Health Connect'te silinen) kayıtların kimliği artık gerekmez.
                val stillNeeded = hidden.intersect(ids)
                if (stillNeeded.size != hidden.size) prefs.update { it.copy(healthConnectHiddenIds = it.healthConnectHiddenIds.intersect(ids)) }
                SleepImportResult.Added(fresh.size)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            SleepImportResult.NoPermission
        } catch (_: Exception) {
            // IllegalStateException (servis yok, kota), RemoteException, IOException: içerik loglanmaz.
            SleepImportResult.Failed
        }
    }

    /**
     * Health Connect'ten alınan kopyaları siler; kimlikleri "gizlenenler"e eklenir ki bağlantı açık kalsa da
     * yeniden alınmasınlar. Health Connect'teki asıl kayıtlar değişmez.
     */
    suspend fun deleteImported(): Int = mutex.withLock {
        val ids = sleepDao.getExternalIds()
        if (ids.isNotEmpty()) prefs.update { it.copy(healthConnectHiddenIds = it.healthConnectHiddenIds + ids) }
        sleepDao.deleteBySource(SleepSource.HEALTH_CONNECT)
    }

    /**
     * Bağlantıyı keser: rıza silinir ve Health Connect izinleri bırakılır. Alınmış geceler kullanıcı silene
     * kadar kalır. Health Connect bu değişikliği Ayla yeniden açılana dek göstermeyebilir.
     */
    suspend fun disconnect() {
        prefs.update { it.copy(healthConnectConsentAt = null, healthConnectPermissionCancels = 0) }
        revokePermissions()
        mutex.withLock {
            lastAttemptAt = null
            lastResult = null
        }
    }

    /** Tam silmede de çağrılır; Health Connect yoksa ya da izin hiç verilmediyse sessizce geçer. */
    suspend fun revokePermissions() {
        try {
            if (availability() == HealthConnectAvailability.AVAILABLE) source.revokeAllPermissions()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // izin yoksa ya da servis yanıt vermezse yapılacak bir şey yok
        }
    }

    companion object {
        /**
         * Uyku kartı dün geceyi ve son 7 geceyi gösterir; yalnızca bu kadarı okunur (veri en aza indirme). Bir
         * günlük pay gece yarısını aşan oturumlar içindir. Health Connect daha eskisini zaten açmaz (30 gün).
         */
        val IMPORT_WINDOW: Duration = Duration.ofDays(8)

        /** Ön plana gelişte en sık bu aralıkla okunur. */
        val SYNC_INTERVAL: Duration = Duration.ofHours(1)

        /** Bu süre içindeki ikinci elle alma yeniden okumaz. */
        val DEDUP_WINDOW: Duration = Duration.ofSeconds(10)
    }
}
