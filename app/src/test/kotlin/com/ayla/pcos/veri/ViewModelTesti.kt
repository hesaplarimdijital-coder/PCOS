package com.ayla.pcos.veri

import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.duration
import com.ayla.pcos.ui.plan.PlanViewModel
import com.ayla.pcos.ui.today.TodayViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** ViewModel durum birleşimi: gerçek depolar, sabit gün. */
class ViewModelTesti : VeriTestOrtami() {

    private fun today() = TodayViewModel(cycles, meds, water, prefs, ticker, scheduler, notifier, doctorNote)

    @Test
    fun `hap modunda bugunku hap numarasi ve alinan haplar hesaplanir`() {
        calistir {
            val id = meds.add(
                MedicationEntity(
                    name = "Hap", doseText = "1 tablet", kind = MedicationKind.CONTRACEPTIVE,
                    times = listOf(LocalTime.of(22, 0)), pillActiveDays = 24, pillPlaceboDays = 4,
                    packStartDate = bugun.minusDays(11),
                ),
            )
            (1..11).forEach { d ->
                db.doseEventDao().upsert(
                    DoseEventEntity(medicationId = id, date = bugun.minusDays(12L - d), time = LocalTime.of(22, 0), status = DoseStatus.TAKEN),
                )
            }
        }
        val vm = today()
        val state = bekle(vm.uiState) { state -> state.pillMode?.takenPills?.size == 11 }
        val pill = assertNotNull(state.pillMode)
        assertEquals(12, pill.todayPill)
        assertFalse(pill.isPlacebo)
        assertEquals((1..11).toSet(), pill.takenPills)
    }

    @Test
    fun `paketteki gecmis hapa dokunmak o gunu isaretler, gelecek hap degismez`() {
        calistir {
            meds.add(
                MedicationEntity(
                    name = "Hap", doseText = "1 tablet", kind = MedicationKind.CONTRACEPTIVE,
                    times = listOf(LocalTime.of(22, 0)), pillActiveDays = 21, pillPlaceboDays = 7,
                    packStartDate = bugun.minusDays(4),
                ),
            )
        }
        val vm = today()
        bekle(vm.uiState) { it.pillMode?.todayPill == 5 }
        vm.togglePill(3)
        assertEquals(setOf(3), bekle(vm.uiState) { it.pillMode?.takenPills == setOf(3) }.pillMode?.takenPills)
        vm.togglePill(9) // gelecek
        vm.togglePill(3) // geri al
        bekle(vm.uiState) { it.pillMode?.takenPills?.isEmpty() == true }
    }

    @Test
    fun `bildirimler kapaliyken bant gorunur, kapatilinca gizlenir, izin acilinca sifirlanir`() {
        calistir { meds.add(MedicationEntity(name = "D vitamini", doseText = "1", kind = MedicationKind.SUPPLEMENT, times = listOf(LocalTime.of(13, 0)))) }
        bildirimleriAc(false)
        val vm = today()
        bekle(vm.uiState) { !it.loading && it.notificationsOff }
        vm.dismissNotificationBand()
        bekle(vm.uiState) { !it.loading && it.hasMedications && !it.notificationsOff }
        assertTrue(calistir { prefs.current().notificationsBandDismissed })

        bildirimleriAc(true)
        ticker.refresh() // öne geliş
        calistir {
            repeat(50) {
                if (!prefs.current().notificationsBandDismissed) return@calistir
                delay(50)
            }
        }
        assertFalse(calistir { prefs.current().notificationsBandDismissed })
    }

    @Test
    fun `gelecek tarihli ilk adet reddedilir, gecmis tarih kaydedilir`() {
        val vm = today()
        vm.startPeriodAt(bugun.plusDays(2))
        vm.startPeriodAt(bugun.minusDays(9))
        val state = bekle(vm.uiState) { it.overview?.lastPeriodStart != null }
        assertEquals(bugun.minusDays(9), state.overview?.lastPeriodStart)
        assertEquals(10, state.overview?.currentCycleDay)
        assertEquals(1, calistir { db.periodDao().getAll().size })
    }

    @Test
    fun `uyku gece yarisini gecince yatis onceki gune yazilir`() {
        val vm = PlanViewModel(water, sleep, prefs, scheduler, ticker)
        vm.addSleep(LocalTime.of(23, 30), LocalTime.of(7, 0), quality = 3)
        val night = bekle(vm.uiState) { it.lastNight != null }.lastNight!!
        assertEquals(bugun, night.date)
        assertEquals(Duration.ofMinutes(7 * 60 + 30), night.duration)
        assertEquals(bugun.minusDays(1), night.startAt.atZone(zone).toLocalDate())
    }

    @Test
    fun `gece yarisindan sonra yatis ayni gune yazilir`() {
        val vm = PlanViewModel(water, sleep, prefs, scheduler, ticker)
        vm.addSleep(LocalTime.of(1, 0), LocalTime.of(8, 30), quality = null)
        val night = bekle(vm.uiState) { it.lastNight != null }.lastNight!!
        assertEquals(Duration.ofMinutes(7 * 60 + 30), night.duration)
        assertEquals(bugun, night.startAt.atZone(zone).toLocalDate())
    }

    @Test
    fun `su ekleme ve cikarma sifirin altina inmez`() {
        val vm = PlanViewModel(water, sleep, prefs, scheduler, ticker)
        calistir {
            vm.removeCup().join()
            vm.addCup().join()
            vm.addCup().join()
        }
        bekle(vm.uiState) { it.waterCups == 2 }
        calistir { repeat(3) { vm.removeCup().join() } }
        assertEquals(0, bekle(vm.uiState) { it.waterCups == 0 }.waterCups)
    }

    @Test
    fun `es zamanli dokunuslarda bardak kaybolmaz`() {
        calistir {
            kotlinx.coroutines.coroutineScope {
                repeat(20) { launch(kotlinx.coroutines.Dispatchers.Default) { water.addCup(bugun) } }
            }
        }
        assertEquals(20, calistir { water.cups(bugun) })
    }

    @Test
    fun `es zamanli aldim stoğu bir kez dusurur`() {
        val id = calistir {
            meds.add(MedicationEntity(name = "D", doseText = "1", kind = MedicationKind.SUPPLEMENT, times = listOf(LocalTime.of(9, 0)), stockCount = 10))
        }
        calistir {
            kotlinx.coroutines.coroutineScope {
                repeat(10) { launch(kotlinx.coroutines.Dispatchers.Default) { meds.markTaken(id, bugun, LocalTime.of(9, 0)) } }
            }
        }
        assertEquals(9, calistir { meds.get(id)?.stockCount })
    }
}
