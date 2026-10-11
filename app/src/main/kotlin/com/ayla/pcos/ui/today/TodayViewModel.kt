package com.ayla.pcos.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.CycleOverview
import com.ayla.cycle.PillRegimen
import com.ayla.cycle.statusOn
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.DoseItem
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.reminders.ReminderNotifier
import com.ayla.pcos.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Hap modunda halkanın yerine gösterilen paket durumu. */
data class PillModeState(
    val medication: MedicationEntity,
    val regimen: PillRegimen,
    val todayPill: Int,
    val isPlacebo: Boolean,
    val takenPills: Set<Int>,
)

data class TodayUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val displayName: String = "",
    val overview: CycleOverview? = null,
    val hidePrediction: Boolean = false,
    val pillMode: PillModeState? = null,
    val doses: List<DoseItem> = emptyList(),
    val hasMedications: Boolean = false,
    val waterCups: Int = 0,
    val waterGoal: Int = 8,
    val exactAlarmMissing: Boolean = false,
    /** İlaç var ama uygulamanın bildirimleri kapalı ve kullanıcı bandı kapatmadı. */
    val notificationsOff: Boolean = false,
    /** Bugün başlayan açık bir adet var mı (bitişi işaretlenebilir). */
    val openPeriod: PeriodEntity? = null,
)

/** Öne gelişte yeniden okunan sistem izinleri. */
private data class PermissionSnapshot(val exactAlarmsAllowed: Boolean, val notificationsEnabled: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val medicationRepository: MedicationRepository,
    private val hydrationRepository: HydrationRepository,
    private val prefs: UserPreferencesRepository,
    private val ticker: DateTicker,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) : ViewModel() {

    private val todayFlow: Flow<LocalDate> = ticker.today

    private val dosesFlow: Flow<List<DoseItem>> = todayFlow.flatMapLatest { medicationRepository.observeDoses(it) }
    private val waterFlow: Flow<Int> = todayFlow.flatMapLatest { hydrationRepository.observeCups(it) }
    private val pillFlow: Flow<PillModeState?> = combine(todayFlow, medicationRepository.activeMedications) { today, meds ->
        meds.firstOrNull { it.kind == MedicationKind.CONTRACEPTIVE && it.packStartDate != null && it.pillActiveDays != null }
            ?.let { med -> Triple(today, med, PillRegimen(med.pillActiveDays!!, med.pillPlaceboDays ?: 0)) }
    }.flatMapLatest { triple ->
        if (triple == null) {
            flowOf(null)
        } else {
            val (today, med, regimen) = triple
            val packStart = med.packStartDate!!
            if (today.isBefore(packStart)) {
                flowOf(null)
            } else {
                val status = regimen.statusOn(packStart, today)
                val currentPackStart = packStart.plusDays(((status.packNumber - 1) * regimen.packLength).toLong())
                medicationRepository.observeTakenDates(med.id, currentPackStart, today).map { takenDates ->
                    val takenPills = takenDates.map { ChronoUnit.DAYS.between(currentPackStart, it).toInt() + 1 }.toSet()
                    PillModeState(med, regimen, status.pillNumber, status.isPlacebo, takenPills)
                }
            }
        }
    }
    private val openPeriodFlow: Flow<PeriodEntity?> = combine(cycleRepository.periods, todayFlow) { periods, today ->
        periods.lastOrNull { it.endDate == null && !it.startDate.isAfter(today) && ChronoUnit.DAYS.between(it.startDate, today) <= 10 }
    }

    /** İzinler ayarlardan değiştirilip geri dönülünce yeniden okunur (öne gelme sinyali). */
    private val permissionFlow: Flow<PermissionSnapshot> = ticker.foreground.map {
        PermissionSnapshot(scheduler.exactAlarmsAllowed(), notifier.notificationsEnabled())
    }

    init {
        // Bildirimler yeniden açıldıysa kapatılmış bandı sıfırla; ileride tekrar kapanırsa bant yine görünsün.
        viewModelScope.launch {
            permissionFlow.collect { snapshot ->
                if (snapshot.notificationsEnabled && prefs.current().notificationsBandDismissed) {
                    prefs.update { it.copy(notificationsBandDismissed = false) }
                }
            }
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        combine(todayFlow, cycleRepository.overview, prefs.preferences) { today, overview, p -> Triple(today, overview, p) },
        combine(dosesFlow, medicationRepository.activeMedications) { doses, meds -> doses to meds.isNotEmpty() },
        combine(waterFlow, permissionFlow) { water, permissions -> water to permissions },
        pillFlow,
        openPeriodFlow,
    ) { (today, overview, p), (doses, hasMeds), (water, permissions), pill, openPeriod ->
        TodayUiState(
            loading = false,
            today = today,
            displayName = p.displayName,
            overview = overview,
            hidePrediction = p.hidePrediction,
            pillMode = pill,
            doses = doses,
            hasMedications = hasMeds,
            waterCups = water,
            waterGoal = p.waterGoalCups,
            exactAlarmMissing = hasMeds && permissions.notificationsEnabled && !permissions.exactAlarmsAllowed,
            notificationsOff = hasMeds && !permissions.notificationsEnabled && !p.notificationsBandDismissed,
            openPeriod = openPeriod,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun addWater() = viewModelScope.launch { hydrationRepository.addCup(ticker.now()) }

    fun dismissNotificationBand() = viewModelScope.launch { prefs.update { it.copy(notificationsBandDismissed = true) } }

    fun markTaken(item: DoseItem) = viewModelScope.launch {
        if (item.status == DoseStatus.TAKEN) return@launch
        medicationRepository.markTaken(item.medication.id, item.date, item.time)
        scheduler.cancelSnooze(item.medication.id, item.time)
    }

    fun undoTaken(item: DoseItem) = viewModelScope.launch {
        medicationRepository.markPending(item.medication.id, item.date, item.time)
    }

    fun startPeriodToday() = viewModelScope.launch { cycleRepository.startPeriod(ticker.now()) }

    /** İlk kayıt: kullanıcının seçtiği geçmiş bir gün (gelecek tarih reddedilir). */
    fun startPeriodAt(date: LocalDate) = viewModelScope.launch {
        if (!date.isAfter(ticker.now())) cycleRepository.addPeriodStart(date)
    }

    fun endPeriodToday() = viewModelScope.launch {
        val open = uiState.value.openPeriod ?: return@launch
        cycleRepository.endPeriod(open.startDate, ticker.now())
    }
}
