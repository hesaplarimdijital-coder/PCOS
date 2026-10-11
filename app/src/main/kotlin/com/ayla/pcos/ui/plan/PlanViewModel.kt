package com.ayla.pcos.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.SleepRepository
import com.ayla.pcos.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class DayCups(val date: LocalDate, val cups: Int)

data class PlanUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val waterCups: Int = 0,
    val waterGoal: Int = 8,
    val cupMl: Int = 250,
    /** Bugün dahil son 7 gün, eskiden yeniye. */
    val waterWeek: List<DayCups> = emptyList(),
    val lastNight: SleepEntity? = null,
    /** Bugün dahil son 7 günün uyku kayıtları, yeniden eskiye. */
    val sleepWeek: List<SleepEntity> = emptyList(),
    val eveningRoutineEnabled: Boolean = false,
    val eveningRoutineTime: LocalTime = LocalTime.of(22, 30),
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val hydration: HydrationRepository,
    private val sleep: SleepRepository,
    private val prefs: UserPreferencesRepository,
    private val scheduler: ReminderScheduler,
    private val ticker: DateTicker,
) : ViewModel() {

    val uiState: StateFlow<PlanUiState> = ticker.today
        .flatMapLatest { today ->
            val from = today.minusDays(6)
            combine(
                hydration.observeRange(from, today),
                sleep.observeNight(today),
                sleep.observeRange(from, today),
                prefs.preferences,
            ) { cups, lastNight, nights, p ->
                val byDate = cups.associate { it.date to it.cups }
                PlanUiState(
                    loading = false,
                    today = today,
                    waterCups = byDate[today] ?: 0,
                    waterGoal = p.waterGoalCups,
                    cupMl = p.cupMl,
                    waterWeek = (0..6).map { i -> from.plusDays(i.toLong()).let { DayCups(it, byDate[it] ?: 0) } },
                    lastNight = lastNight,
                    sleepWeek = nights.sortedByDescending { it.endAt },
                    eveningRoutineEnabled = p.eveningRoutineEnabled,
                    eveningRoutineTime = p.eveningRoutineTime,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun addCup() = viewModelScope.launch { hydration.addCup(ticker.now()) }

    fun removeCup() = viewModelScope.launch { hydration.removeCup(ticker.now()) }

    /**
     * Elle uyku: [bedtime] öğleden sonraysa önceki güne, değilse uyanılan güne ait sayılır;
     * [wake] bugün içindir. Böylece 23:30–07:00 ve 01:00–08:30 ikisi de doğru hesaplanır.
     */
    fun addSleep(bedtime: LocalTime, wake: LocalTime, quality: Int?) = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        val wakeDate = ticker.now()
        val bedDate = if (bedtime.isAfter(wake)) wakeDate.minusDays(1) else wakeDate
        val start = bedDate.atTime(bedtime).atZone(zone).toInstant()
        val end = wakeDate.atTime(wake).atZone(zone).toInstant()
        if (!end.isAfter(start)) return@launch
        sleep.addManual(start, end, quality, zone)
    }

    fun deleteSleep(entity: SleepEntity) = viewModelScope.launch { sleep.delete(entity) }

    fun setEveningRoutine(enabled: Boolean) = viewModelScope.launch {
        prefs.update { it.copy(eveningRoutineEnabled = enabled) }
        runCatching { scheduler.rescheduleAll() }
    }
}
