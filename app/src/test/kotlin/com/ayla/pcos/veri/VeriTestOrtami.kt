package com.ayla.pcos.veri

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.ayla.cycle.CycleEngine
import com.ayla.pcos.data.db.AylaDatabase
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.CycleRepository
import com.ayla.pcos.data.repo.DailyLogRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.HydrationRepository
import com.ayla.pcos.data.repo.MedicationRepository
import com.ayla.pcos.data.repo.SleepRepository
import com.ayla.pcos.reminders.ReminderNotifier
import com.ayla.pcos.reminders.ReminderScheduler
import com.ayla.pcos.report.DoctorNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

/** Sabit bir güne kilitli "bugün"; gece yarısı döngüsü çalışmaz. */
class SabitGun(baslangic: LocalDate, zone: ZoneId) : DateTicker(Clock.fixed(baslangic.atTime(10, 0).atZone(zone).toInstant(), zone)) {
    private val gun = MutableStateFlow(baslangic)
    private val oneGelis = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1).apply { tryEmit(Unit) }
    override val today: Flow<LocalDate> = gun
    override val foreground: Flow<Unit> = oneGelis
    override fun now(): LocalDate = gun.value
    override fun refresh() {
        oneGelis.tryEmit(Unit)
    }
    fun ayarla(date: LocalDate) {
        gun.value = date
    }
}

/**
 * Gerçek Room (bellek içi, şifresiz), gerçek DataStore (geçici dosya) ve Robolectric'in AlarmManager /
 * NotificationManager gölgeleriyle depo ve ViewModel testleri. Ana dağıtıcı arka plan iş parçacığına
 * yönlendirilir; testler akışlardan beklenen değeri zaman aşımıyla bekler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
abstract class VeriTestOrtami {
    @get:Rule
    val tmp = TemporaryFolder()

    protected val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    protected val bugun: LocalDate = LocalDate.of(2026, 10, 12) // Pazartesi
    protected lateinit var context: Context
    protected lateinit var db: AylaDatabase
    protected lateinit var prefs: UserPreferencesRepository
    protected lateinit var ticker: SabitGun
    protected lateinit var scheduler: ReminderScheduler
    protected lateinit var notifier: ReminderNotifier
    protected lateinit var cycles: CycleRepository
    protected lateinit var meds: MedicationRepository
    protected lateinit var logs: DailyLogRepository
    protected lateinit var water: HydrationRepository
    protected lateinit var sleep: SleepRepository
    protected lateinit var doctorNote: DoctorNoteRepository
    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before
    fun ortamiKur() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
        Dispatchers.setMain(Dispatchers.Default)
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AylaDatabase::class.java).allowMainThreadQueries().build()
        prefs = UserPreferencesRepository(
            PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(tmp.root, "ayla.preferences_pb") },
        )
        ticker = SabitGun(bugun, zone)
        val clock = Clock.fixed(bugun.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant(), zone)
        scheduler = ReminderScheduler(context, db.medicationDao(), prefs, clock)
        notifier = ReminderNotifier(context)
        cycles = CycleRepository(db.periodDao(), prefs, CycleEngine(), ticker)
        meds = MedicationRepository(db.medicationDao(), db.doseEventDao(), scheduler)
        logs = DailyLogRepository(db.dailyLogDao())
        water = HydrationRepository(db.hydrationDao())
        sleep = SleepRepository(db.sleepDao())
        doctorNote = DoctorNoteRepository(context, cycles, logs, db.medicationDao(), db.doseEventDao(), ticker)
    }

    @After
    fun ortamiKapat() {
        db.close()
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    /** Akıştan koşulu sağlayan ilk değeri bekler (gerçek zaman, en fazla [timeoutMs]). */
    protected fun <T> bekle(flow: Flow<T>, timeoutMs: Long = 5_000, kosul: (T) -> Boolean = { true }): T =
        runBlocking { withTimeout(timeoutMs) { flow.first(kosul) } }

    protected fun <T> calistir(block: suspend () -> T): T = runBlocking { block() }

    protected fun bildirimleriAc(acik: Boolean) {
        val nm = context.getSystemService(NotificationManager::class.java)
        shadowOf(nm).setNotificationsEnabled(acik)
    }

    /** Kurulu alarmların veri URI'leri (ör. ayla://reminder/dose/1/09:00). */
    protected fun kuruluAlarmlar(): List<String> {
        val am = context.getSystemService(AlarmManager::class.java)
        return shadowOf(am).scheduledAlarms.mapNotNull { alarm ->
            alarm.operation?.let { shadowOf(it).savedIntent.dataString }
        }
    }
}
