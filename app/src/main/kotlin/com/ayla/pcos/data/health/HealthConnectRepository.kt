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

    @Volatile private var lastImportAt: Instant? = null

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
     * Ön plana gelişte: rıza ve izin varsa, son almadan bu yana [minInterval] geçtiyse sessizce alır.
     * Koşullardan biri yoksa hiçbir şey okunmaz ve null döner.
     */
    suspend fun syncIfConnected(minInterval: Duration = SYNC_INTERVAL): SleepImportResult? {
        val now = clock.instant()
        lastImportAt?.let { if (Duration.between(it, now) < minInterval) return null }
        if (prefs.current().healthConnectConsentAt == null) return null
        if (!hasSleepPermission()) return null
        return importNow()
    }

    /** Son [IMPORT_WINDOW] içindeki uyku oturumlarını alır; aynı gece iki kez eklenmez. */
    suspend fun importNow(): SleepImportResult = mutex.withLock { importLocked() }

    private suspend fun importLocked(): SleepImportResult {
        if (availability() != HealthConnectAvailability.AVAILABLE) return SleepImportResult.Unavailable
        if (prefs.current().healthConnectConsentAt == null) return SleepImportResult.NoPermission
        return try {
            if (!source.hasSleepPermission()) {
                SleepImportResult.NoPermission
            } else {
                val now = clock.instant()
                val sessions = source.readSleepSessions(now.minus(IMPORT_WINDOW), now)
                val fresh = selectNewSleep(sleepDao.getAll(), sessions.map { it.toSleepEntity(clock.zone) })
                fresh.forEach { sleepDao.insert(it) }
                lastImportAt = now
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

    /** Health Connect'ten alınan kopyaları siler; Health Connect'teki asıl kayıtlar değişmez. */
    suspend fun deleteImported(): Int = sleepDao.deleteBySource(SleepSource.HEALTH_CONNECT)

    /**
     * Bağlantıyı keser: rıza silinir ve Health Connect izinleri bırakılır. Alınmış geceler kullanıcı silene
     * kadar kalır. Health Connect bu değişikliği Ayla yeniden açılana dek göstermeyebilir.
     */
    suspend fun disconnect() {
        prefs.update { it.copy(healthConnectConsentAt = null, healthConnectPermissionCancels = 0) }
        revokePermissions()
        lastImportAt = null
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
        /** Health Connect varsayılan olarak ilk izinden en fazla 30 gün öncesini açar. */
        val IMPORT_WINDOW: Duration = Duration.ofDays(30)

        /** Ön plana gelişte en sık bu aralıkla okunur. */
        val SYNC_INTERVAL: Duration = Duration.ofHours(1)
    }
}
