package com.ayla.pcos.report

import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.db.DoseEventEntity
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals

class DoctorNoteTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 10, 12)
    private val from = today.minusDays(DOCTOR_NOTE_DAYS - 1)

    private fun med(id: Long, createdDaysAgo: Long, times: Int = 2) = MedicationEntity(
        id = id, name = "İlaç $id", doseText = "1", kind = MedicationKind.SUPPLEMENT,
        times = List(times) { LocalTime.of(9 + it * 12, 0) },
        createdAt = today.minusDays(createdDaysAgo).atStartOfDay(zone).toInstant(),
    )

    private fun taken(medId: Long, date: LocalDate, hour: Int = 9) =
        DoseEventEntity(medicationId = medId, date = date, time = LocalTime.of(hour, 0), status = DoseStatus.TAKEN)

    @Test
    fun `uyum yalnizca ilacin eklendigi gunden itibaren planlanir`() {
        val m = med(1, createdDaysAgo = 9) // 10 gün × 2 doz
        val events = List(15) { taken(1, today.minusDays((it / 2).toLong()), if (it % 2 == 0) 9 else 21) } +
            taken(2, today) + // başka ilaç
            DoseEventEntity(medicationId = 1, date = today, time = LocalTime.of(21, 0), status = DoseStatus.MISSED)
        val a = adherenceFor(m, events, from, today, zone)
        assertEquals(20, a.scheduled)
        assertEquals(15, a.taken)
    }

    @Test
    fun `eski ilacta tum aralik sayilir ve aralik disi dozlar dislanir`() {
        val m = med(1, createdDaysAgo = 100, times = 1)
        val events = listOf(taken(1, from.minusDays(1)), taken(1, from), taken(1, today))
        val a = adherenceFor(m, events, from, today, zone)
        assertEquals(DOCTOR_NOTE_DAYS.toInt(), a.scheduled)
        assertEquals(2, a.taken)
    }

    @Test
    fun `en sik semptomlar gun sayisina gore ve esitlikte alfabetik`() {
        fun log(day: Long, vararg s: String) = DailyLogEntity(date = today.minusDays(day), symptoms = s.toList())
        val logs = listOf(
            log(0, "acne", "bloating"),
            log(1, "bloating", "bloating"), // aynı gün iki kez sayılmaz
            log(2, "cramps", "acne"),
            log(3, "headache"),
            log(4, "bloating"),
        )
        assertEquals(listOf("bloating" to 3, "acne" to 2, "cramps" to 1), topSymptoms(logs))
        assertEquals(emptyList<Pair<String, Int>>(), topSymptoms(emptyList()))
    }
}
