package com.ayla.pcos.ekran

import com.ayla.cycle.CycleEngine
import com.ayla.cycle.PeriodRecord
import com.ayla.cycle.PillRegimen
import com.ayla.cycle.Prediction
import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.repo.Adherence
import com.ayla.pcos.data.repo.DoseItem
import com.ayla.pcos.data.repo.SymptomCatalog
import com.ayla.pcos.export.ImportSummary
import com.ayla.pcos.ui.calendar.CalendarUiState
import com.ayla.pcos.ui.calendar.CycleRow
import com.ayla.pcos.ui.calendar.DayActions
import com.ayla.pcos.ui.calendar.buildCalendarWeeks
import com.ayla.pcos.ui.calendar.calendarDayRules
import com.ayla.pcos.ui.log.DailyLogUiState
import com.ayla.pcos.ui.me.CycleTrend
import com.ayla.pcos.ui.me.MeUiState
import com.ayla.pcos.ui.meds.EditMedicationState
import com.ayla.pcos.ui.meds.MedsUiState
import com.ayla.pcos.ui.onboarding.OnboardingState
import com.ayla.pcos.ui.plan.DayCups
import com.ayla.pcos.ui.plan.PlanUiState
import com.ayla.pcos.ui.today.PillModeState
import com.ayla.pcos.ui.today.TodayUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Ekran görüntüleri için gerçekçi örnek veri: düzensiz döngülü (32–44 gün) bir kullanıcı,
 * günde iki doz Inofolic, D vitamini ve nane çayı. Tarihler sabittir; çıktı belirleyicidir.
 */
internal object Ornek {
    val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    val bugun: LocalDate = LocalDate.of(2026, 10, 12)
    private val engine = CycleEngine()

    private fun instant(date: LocalDate, time: LocalTime): Instant = date.atTime(time).atZone(zone).toInstant()

    // --- Döngü ---

    /** Son adet 23 gün önce başladı; önceki döngüler 38, 33, 44, 36, 41, 35 gün. */
    private val baslangiclar: List<LocalDate> = run {
        val uzunluklar = listOf(38, 33, 44, 36, 41, 35)
        val son = bugun.minusDays(22)
        val list = mutableListOf(son)
        uzunluklar.asReversed().forEach { list.add(0, list.first().minusDays(it.toLong())) }
        list
    }

    val adetler: List<PeriodEntity> = baslangiclar.mapIndexed { i, start ->
        PeriodEntity(id = i + 1L, startDate = start, endDate = start.plusDays(if (i % 2 == 0) 4 else 5))
    }

    private val kayitlar: List<PeriodRecord> = adetler.map { PeriodRecord(it.startDate, it.endDate, it.excludedFromStats) }

    val genelBakis = engine.overview(kayitlar, bugun, trackingSince = baslangiclar.first())

    /** 67. gün: uzun döngü uyarısı (nazik). */
    val uzunDonguGenelBakis = run {
        val records = listOf(
            PeriodRecord(bugun.minusDays(66 + 39), bugun.minusDays(66 + 35)),
            PeriodRecord(bugun.minusDays(66), bugun.minusDays(62)),
        )
        engine.overview(records, bugun, trackingSince = bugun.minusDays(200))
    }

    // --- İlaçlar ---

    val inofolic = MedicationEntity(
        id = 1, name = "Inofolic Combi", doseText = "1 kapsül", kind = MedicationKind.SUPPLEMENT,
        form = MedicationForm.CAPSULE, times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)), withFood = true,
        stockCount = 18, createdAt = instant(bugun.minusDays(60), LocalTime.NOON),
    )
    val dVitamini = MedicationEntity(
        id = 2, name = "D vitamini", doseText = "1 damla", kind = MedicationKind.SUPPLEMENT,
        form = MedicationForm.DROPS, times = listOf(LocalTime.of(13, 0)), withFood = true,
        createdAt = instant(bugun.minusDays(40), LocalTime.NOON),
    )
    val naneCayi = MedicationEntity(
        id = 3, name = "Yeşil nane çayı", doseText = "1 fincan", kind = MedicationKind.HERBAL,
        form = MedicationForm.TEA, times = listOf(LocalTime.of(16, 0)), withFood = null,
        createdAt = instant(bugun.minusDays(20), LocalTime.NOON),
    )
    val hap = MedicationEntity(
        id = 4, name = "Doğum kontrol hapı", doseText = "1 tablet", kind = MedicationKind.CONTRACEPTIVE,
        form = MedicationForm.TABLET, times = listOf(LocalTime.of(22, 0)), withFood = null,
        pillActiveDays = 24, pillPlaceboDays = 4, packStartDate = bugun.minusDays(11),
        createdAt = instant(bugun.minusDays(30), LocalTime.NOON),
    )

    private fun alindi(med: MedicationEntity, time: LocalTime, at: LocalTime) = DoseItem(
        medication = med, date = bugun, time = time,
        event = DoseEventEntity(id = med.id * 10 + time.hour, medicationId = med.id, date = bugun, time = time, status = DoseStatus.TAKEN, actedAt = instant(bugun, at)),
    )

    private fun bekliyor(med: MedicationEntity, time: LocalTime) = DoseItem(medication = med, date = bugun, time = time, event = null)

    val dozlar: List<DoseItem> = listOf(
        alindi(inofolic, LocalTime.of(9, 0), LocalTime.of(9, 12)),
        bekliyor(dVitamini, LocalTime.of(13, 0)),
        bekliyor(naneCayi, LocalTime.of(16, 0)),
        bekliyor(inofolic, LocalTime.of(21, 0)),
    )

    // --- Ekran durumları ---

    val bugunDongu = TodayUiState(
        loading = false, today = bugun, displayName = "Elif", overview = genelBakis,
        doses = dozlar, hasMedications = true, waterCups = 5, waterGoal = 8,
    )

    val bugunUzunDongu = bugunDongu.copy(overview = uzunDonguGenelBakis, doses = dozlar.take(2))

    val bugunHap = TodayUiState(
        loading = false, today = bugun, displayName = "Elif", overview = genelBakis,
        pillMode = PillModeState(hap, PillRegimen.R24_4, todayPill = 12, isPlacebo = false, takenPills = (1..11).toSet(), packStart = bugun.minusDays(11)),
        doses = listOf(bekliyor(hap, LocalTime.of(22, 0))), hasMedications = true, waterCups = 3, waterGoal = 8,
    )

    val bugunHapDunKacti = bugunHap.copy(pillMode = bugunHap.pillMode!!.copy(takenPills = (1..10).toSet()))

    val bugunBos = TodayUiState(loading = false, today = bugun, displayName = "", overview = engine.overview(emptyList(), bugun))

    fun takvim(secili: LocalDate? = bugun.minusDays(20)): CalendarUiState {
        val month = YearMonth.from(bugun)
        val prediction = genelBakis.prediction
        val logDates = setOf(bugun.minusDays(1), bugun.minusDays(3), bugun.minusDays(4), bugun.minusDays(8), bugun.minusDays(20), bugun)
        val weeks = buildCalendarWeeks(month, bugun, kayitlar, prediction, logDates, engine)
        val cycles = genelBakis.cycles
        val actions = secili?.let { date ->
            val rules = calendarDayRules(date, kayitlar, cycles, bugun)
            DayActions(
                canMarkStart = rules.canMarkStart, canMarkEnd = rules.canMarkEnd, canUnmark = rules.canUnmark,
                canOpenLog = rules.canOpenLog,
                period = rules.containingStart?.let { s -> adetler.firstOrNull { it.startDate == s } },
                cycle = rules.cycle,
            )
        }
        return CalendarUiState(
            loading = false, month = month, today = bugun, weeks = weeks, selected = secili, actions = actions,
            stats = genelBakis.stats, cycleDay = genelBakis.currentCycleDay,
            history = cycles.filter { it.isComplete }.asReversed().map { c -> CycleRow(c, adetler.firstOrNull { it.startDate == c.start }) },
        )
    }

    val tahminVar: Boolean get() = genelBakis.prediction is Prediction.Window

    val ilaclar = MedsUiState(
        loading = false, today = bugun, medications = listOf(inofolic, dVitamini, naneCayi),
        todayDoses = dozlar, weekAdherence = Adherence(taken = 11, scheduled = 14),
    )

    val plan = PlanUiState(
        loading = false, today = bugun, waterCups = 5, waterGoal = 8, cupMl = 250,
        waterWeek = listOf(6, 8, 9, 4, 8, 7, 5).mapIndexed { i, c -> DayCups(bugun.minusDays(6L - i), c) },
        lastNight = SleepEntity(id = 1, date = bugun, startAt = instant(bugun.minusDays(1), LocalTime.of(23, 40)), endAt = instant(bugun, LocalTime.of(7, 15)), quality = 3),
        sleepWeek = listOf(
            SleepEntity(id = 1, date = bugun, startAt = instant(bugun.minusDays(1), LocalTime.of(23, 40)), endAt = instant(bugun, LocalTime.of(7, 15)), quality = 3),
            SleepEntity(id = 2, date = bugun.minusDays(1), startAt = instant(bugun.minusDays(1), LocalTime.of(0, 30)), endAt = instant(bugun.minusDays(1), LocalTime.of(7, 0)), quality = 2),
            SleepEntity(id = 3, date = bugun.minusDays(2), startAt = instant(bugun.minusDays(3), LocalTime.of(23, 5)), endAt = instant(bugun.minusDays(2), LocalTime.of(6, 50)), quality = 3),
        ),
        eveningRoutineEnabled = true, eveningRoutineTime = LocalTime.of(22, 30),
    )

    val ben = MeUiState(
        loading = false, today = bugun, displayName = "Elif",
        cycleLengths = listOf(38, 33, 44, 36, 41, 35), trend = CycleTrend.Variable(33, 44),
        adherence = Adherence(taken = 11, scheduled = 14), hasMedications = true,
        waterDaysThisWeek = 3, logDaysThisWeek = 5,
        // Bugün Pazartesi: son satırın ilk hücresi bugün, sonrası gelecek.
        logGrid = List(28) { i -> if (i > 21) null else (i % 3 != 1) },
    )

    val yedekOzeti = ImportSummary(
        exportedAt = Instant.parse("2026-10-01T17:42:00Z"),
        periods = 7, dailyLogs = 118, medications = 3, doses = 236, hydrationDays = 64, sleepNights = 41,
    )

    val gunlukKayit = DailyLogUiState(
        date = bugun, loaded = true, mood = 4, energy = 2, flow = 0,
        symptoms = setOf(SymptomCatalog.BLOATING, SymptomCatalog.ACNE, SymptomCatalog.SWEET_CRAVING),
        note = "Öğle yemeğinden sonra 15 dakika yürüdüm, şişkinlik azaldı.",
        hasSavedLog = true,
    )

    val ayarlar = UserPreferences(
        onboardingDone = true, displayName = "Elif", goals = setOf(Goal.CYCLE, Goal.SYMPTOMS),
        trackingSince = baslangiclar.first(),
    )

    val ilacDuzenle = EditMedicationState.from(inofolic, bugun)
    val hapDuzenle = EditMedicationState.from(hap, bugun)

    val karsilama = OnboardingState(step = 0, name = "Elif")
    val karsilamaHedefler = OnboardingState(step = 1, name = "Elif", goals = setOf(Goal.CYCLE, Goal.SYMPTOMS))
    val karsilamaIzinler = OnboardingState(step = 3, name = "Elif")
}
