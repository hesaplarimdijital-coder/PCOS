package com.ayla.pcos.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.data.repo.SleepRepository
import com.ayla.pcos.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    /** Hareket sekmesinin döngü kartı; kişiselleştirme rızası kapalıysa ya da hap modundaysa genel. */
    val movementCycleCard: MovementCycleCard = MovementCycleCard.GENERAL,
)

/** Açık (bitişi girilmemiş) adetin kaç gün sayılacağı; takvimdeki kuralla aynı. */
private const val OPEN_PERIOD_DAYS = 5L

/**
 * Bugün kaydedilmiş bir adetin içinde mi ya da bugünkü kanama kaydı "hafif" ve üstü mü (B4).
 * Tahmini aralık kullanılmaz; yalnızca kullanıcının kendi kaydı.
 */
fun isRecordedPeriodDay(today: LocalDate, periods: List<PeriodEntity>, todayFlow: Int?): Boolean {
    if ((todayFlow ?: 0) >= 2) return true
    return periods.any { p ->
        val end = p.endDate ?: minOf(p.startDate.plusDays(OPEN_PERIOD_DAYS - 1), today)
        !today.isBefore(p.startDate) && !today.isAfter(end)
    }
}

/** Rıza kapalı ya da hap modu açık: herkese aynı genel kart; aksi hâlde adet günü / diğer gün. */
fun movementCycleCard(consentPersonalization: Boolean, pillMode: Boolean, periodDay: Boolean): MovementCycleCard = when {
    !consentPersonalization || pillMode -> MovementCycleCard.GENERAL
    periodDay -> MovementCycleCard.PERIOD
    else -> MovementCycleCard.OTHER
}

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val hydration: HydrationRepository,
    private val sleep: SleepRepository,
    private val prefs: UserPreferencesRepository,
    private val scheduler: ReminderScheduler,
    private val ticker: DateTicker,
    private val cycles: CycleRepository,
    private val logs: DailyLogRepository,
    private val medications: MedicationRepository,
) : ViewModel() {

    /**
     * Kişiselleştirme rızası kapalıyken kanama kaydı ve ilaçlar hiç okunmaz; kart genel kalır.
     */
    private fun movementCard(today: LocalDate): Flow<MovementCycleCard> =
        prefs.preferences.map { it.consentPersonalization }.distinctUntilChanged().flatMapLatest { consent ->
            if (!consent) {
                flowOf(MovementCycleCard.GENERAL)
            } else {
                combine(medications.activeMedications, cycles.periods, logs.observe(today)) { active, periods, log ->
                    movementCycleCard(
                        consentPersonalization = true,
                        pillMode = active.any { it.kind == MedicationKind.CONTRACEPTIVE },
                        periodDay = isRecordedPeriodDay(today, periods, log?.flow),
                    )
                }
            }
        }

    val uiState: StateFlow<PlanUiState> = ticker.today
        .flatMapLatest { today ->
            val from = today.minusDays(6)
            combine(
                hydration.observeRange(from, today),
                sleep.observeNight(today),
                sleep.observeRange(from, today),
                prefs.preferences,
                movementCard(today),
            ) { cups, lastNight, nights, p, movement ->
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
                    movementCycleCard = movement,
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
