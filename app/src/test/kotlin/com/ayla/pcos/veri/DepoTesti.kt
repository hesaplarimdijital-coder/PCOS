package com.ayla.pcos.veri

import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Depoların gerçek Room sorgularıyla davranışı. */
class DepoTesti : VeriTestOrtami() {

    private fun inofolic(stock: Int? = null) = MedicationEntity(
        name = "Inofolic Combi", doseText = "1 kapsül", kind = MedicationKind.SUPPLEMENT,
        times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)), withFood = true, stockCount = stock,
    )

    @Test
    fun `acik adet varken bir hafta icinde yeni baslangic acilmaz`() = calistir {
        cycles.startPeriod(bugun.minusDays(40))
        cycles.startPeriod(bugun.minusDays(37)) // aynı adet: 3 gün sonra, bitiş girilmemiş
        assertEquals(1, db.periodDao().getAll().size)
        cycles.endPeriod(bugun.minusDays(40), bugun.minusDays(36))
        cycles.startPeriod(bugun.minusDays(5)) // yeni döngü
        assertEquals(2, db.periodDao().getAll().size)
    }

    @Test
    fun `takvimden isaretleme ayni gunu iki kez eklemez ve takip baslangicini gunceller`() = calistir {
        cycles.addPeriodStart(bugun.minusDays(40))
        cycles.addPeriodStart(bugun.minusDays(40))
        cycles.addPeriodStart(bugun.minusDays(3))
        assertEquals(2, db.periodDao().getAll().size)
        assertEquals(bugun.minusDays(40), prefs.current().trackingSince)
        cycles.endPeriodAt(bugun.minusDays(36))
        assertEquals(bugun.minusDays(36), db.periodDao().getByStart(bugun.minusDays(40))?.endDate)
        assertNull(db.periodDao().getByStart(bugun.minusDays(3))?.endDate)
    }

    @Test
    fun `genel bakis kayitlardan dongu gununu ve tahmini uretir`() {
        calistir {
            listOf(100L, 64L, 30L).forEach { cycles.addPeriodStart(bugun.minusDays(it)) }
        }
        val overview = bekle(cycles.overview) { it.lastPeriodStart != null }
        assertEquals(31, overview.currentCycleDay)
        assertEquals(listOf(36, 34), overview.stats.lengths)
    }

    @Test
    fun `aldim stoğu bir kez azaltir, geri alma geri ekler`() = calistir {
        val id = meds.add(inofolic(stock = 10))
        meds.markTaken(id, bugun, LocalTime.of(9, 0))
        meds.markTaken(id, bugun, LocalTime.of(9, 0))
        assertEquals(9, meds.get(id)?.stockCount)
        assertEquals(DoseStatus.TAKEN, meds.status(id, bugun, LocalTime.of(9, 0)))
        meds.markPending(id, bugun, LocalTime.of(9, 0))
        assertEquals(10, meds.get(id)?.stockCount)
        assertEquals(DoseStatus.PENDING, meds.status(id, bugun, LocalTime.of(9, 0)))
    }

    @Test
    fun `gunluk kontrol dunku bekleyen dozlari kacti yapar, bugunkulere dokunmaz`() = calistir {
        val id = meds.add(inofolic())
        db.doseEventDao().insertIgnore(DoseEventEntity(medicationId = id, date = bugun.minusDays(1), time = LocalTime.of(21, 0)))
        db.doseEventDao().insertIgnore(DoseEventEntity(medicationId = id, date = bugun, time = LocalTime.of(9, 0)))
        meds.markOldPendingAsMissed(bugun)
        assertEquals(DoseStatus.MISSED, meds.status(id, bugun.minusDays(1), LocalTime.of(21, 0)))
        assertEquals(DoseStatus.PENDING, meds.status(id, bugun, LocalTime.of(9, 0)))
    }

    @Test
    fun `gunun dozlari saat sirasiyla gelir`() {
        calistir {
            meds.add(inofolic())
            meds.add(inofolic().copy(name = "D vitamini", times = listOf(LocalTime.of(13, 0))))
        }
        val doses = bekle(meds.observeDoses(bugun)) { it.size == 3 }
        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(13, 0), LocalTime.of(21, 0)), doses.map { it.time })
    }

    @Test
    fun `ilac eklenince doz alarmlari kurulur, kaldirilinca iptal edilir`() = calistir {
        val id = meds.add(inofolic())
        val doseAlarms = kuruluAlarmlar().filter { it.contains("/dose/") }
        assertEquals(2, doseAlarms.size, doseAlarms.toString())
        assertTrue(kuruluAlarmlar().any { it.contains("/daily_check/") })
        meds.deactivate(id)
        assertEquals(0, kuruluAlarmlar().count { it.contains("/dose/") })
    }
}
