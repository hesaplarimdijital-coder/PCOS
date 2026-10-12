package com.ayla.pcos.widget

import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.MedicationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Widget durumunun tek kaynağı: mevcut depolar (şifreli Room ve DataStore). Widget kendi durumunu hiçbir
 * yere yazmaz. Gizlilik ayarı açıkken veritabanı hiç okunmaz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetStateSource @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val cycles: CycleRepository,
    private val medications: MedicationRepository,
    private val hydration: HydrationRepository,
    private val ticker: DateTicker,
    private val clock: Clock,
) {
    fun observe(): Flow<WidgetState> = prefs.preferences.flatMapLatest { p ->
        val neutral = buildWidgetState(p, null, emptyList(), emptyList(), 0, LocalDateTime.now(clock))
        if (neutral == WidgetState.Neutral) {
            flowOf(WidgetState.Neutral)
        } else {
            ticker.today.flatMapLatest { today ->
                combine(
                    cycles.overview,
                    medications.activeMedications,
                    medications.observeDoses(today),
                    hydration.observeCups(today),
                ) { overview, meds, doses, cups ->
                    buildWidgetState(p, overview, meds, doses, cups, LocalDateTime.now(clock))
                }
            }
        }
    }

    suspend fun current(): WidgetState = observe().first()
}
