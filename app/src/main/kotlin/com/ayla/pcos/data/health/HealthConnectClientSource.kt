package com.ayla.pcos.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerçek Health Connect istemcisi. İstemci yalnızca Health Connect kullanılabilirken oluşturulur; kayıt
 * içerikleri hiçbir yere (log dahil) yazılmaz.
 */
@Singleton
class HealthConnectClientSource @Inject constructor(@ApplicationContext private val context: Context) : HealthConnectSource {

    override fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNAVAILABLE
    }

    private fun clientOrNull(): HealthConnectClient? =
        if (availability() == HealthConnectAvailability.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    override suspend fun hasSleepPermission(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(SLEEP_PERMISSIONS)
    }

    override suspend fun readSleepSessions(from: Instant, to: Instant): List<ExternalSleepSession> {
        val client = clientOrNull() ?: return emptyList()
        val filter = TimeRangeFilter.between(from, to)
        val out = mutableListOf<ExternalSleepSession>()
        var pageToken: String? = null
        var pages = 0
        do {
            val response = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, filter, pageToken = pageToken))
            response.records.mapTo(out) { ExternalSleepSession(it.metadata.id, it.startTime, it.endTime, it.endZoneOffset) }
            pageToken = response.pageToken
            pages++
            // Bazı sürümler son sayfada null yerine "" döndürür; sayfa sınırı sonsuz döngüye karşı.
        } while (!pageToken.isNullOrEmpty() && pages < MAX_PAGES)
        return out
    }

    override suspend fun revokeAllPermissions() {
        clientOrNull()?.permissionController?.revokeAllPermissions()
    }

    private companion object {
        const val MAX_PAGES = 20
    }
}
