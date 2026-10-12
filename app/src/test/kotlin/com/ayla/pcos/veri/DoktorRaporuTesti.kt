package com.ayla.pcos.veri

import com.ayla.cycle.LhResult
import com.ayla.pcos.R
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.repo.SymptomCatalog
import com.ayla.pcos.report.DoctorReport
import com.ayla.pcos.report.DoctorReportBuilder
import com.ayla.pcos.report.ReportBlock
import com.ayla.pcos.report.ReportLayout
import com.ayla.pcos.report.ReportOptions
import com.ayla.pcos.report.ReportRange
import com.ayla.pcos.report.TextMeasurer
import com.ayla.pcos.report.TextStyle
import com.ayla.pcos.report.toPlainText
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Doktor raporu modeli (#26, ADR-0009): bölümler kayıtlardan kurulur, yorum içermez, hap modunda doğurganlık yoktur. */
class DoktorRaporuTesti : VeriTestOrtami() {

    private fun builder() = DoctorReportBuilder(
        context, cycles, logs, db.medicationDao(), db.doseEventDao(), sleep, water, fertility, skin, questions, prefs, ticker,
    )

    private fun section(report: DoctorReport, titleRes: Int) = report.sections.firstOrNull { it.title == context.getString(titleRes) }

    private fun DoctorReport.text(): String = toPlainText()

    private suspend fun ornekVeri() {
        listOf(70L, 35L, 5L).forEach { cycles.addPeriodStart(bugun.minusDays(it)) }
        val id = meds.add(
            MedicationEntity(
                name = "Inofolic Combi", doseText = "1 kapsül", kind = MedicationKind.SUPPLEMENT,
                times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
                createdAt = bugun.minusDays(60).atStartOfDay(zone).toInstant(),
            ),
        )
        (0L until 3L).forEach { d -> meds.markTaken(id, bugun.minusDays(d), LocalTime.of(9, 0)) }
        logs.save(DailyLogEntity(date = bugun.minusDays(1), mood = 4, symptoms = listOf(SymptomCatalog.BLOATING, SymptomCatalog.ACNE), weightKg = 61.5))
        logs.save(DailyLogEntity(date = bugun.minusDays(2), mood = 3, flow = 2, symptoms = listOf(SymptomCatalog.BLOATING)))
        water.setCups(bugun.minusDays(1), 6)
        sleep.addManual(
            bugun.minusDays(1).atTime(23, 30).atZone(zone).toInstant(),
            bugun.atTime(7, 0).atZone(zone).toInstant(),
            quality = 2,
            zone = zone,
        )
        skin.saveProfile(SkinProfileEntity(skinType = SkinType.OILY, acne = AcneSelfReport.MILD, acanthosis = true))
        skin.toggleStep(bugun.minusDays(1), RoutineSlot.MORNING, "cleanser")
        questions.add("Metformin bana uygun mu?")
        val sorulmus = questions.add("Kan tahlili ne zaman?")
        questions.markAsked(requireNotNull(sorulmus), asked = true)
    }

    @Test
    fun `rapor kayit bolumlerini kurar, ibareyle baslar ve yorum icermez`() {
        val report = calistir {
            ornekVeri()
            builder().build(ReportOptions.lastDays(ReportRange.DAYS_90, bugun))
        }
        assertEquals(context.getString(R.string.report_title), report.title)
        assertEquals(context.getString(R.string.report_disclaimer), report.disclaimer)
        val text = report.text()
        assertEquals(context.getString(R.string.report_title), text.lines()[0])
        assertEquals(context.getString(R.string.report_disclaimer), text.lines()[2], "ibare en üstte")

        val dongu = assertNotNull(section(report, R.string.report_section_cycle))
        val tablo = dongu.blocks.filterIsInstance<ReportBlock.Table>().single()
        assertEquals(3, tablo.rows.size, "aralıkla kesişen üç döngü")
        assertTrue(tablo.rows.first()[1].contains("6"), "açık döngü 6. gün: ${tablo.rows.first()}")
        assertTrue(tablo.rows[1][1] == context.getString(R.string.report_days_value, 30), tablo.rows[1].toString())
        assertTrue(dongu.blocks.none { it is ReportBlock.Note }, "hap notu yok")

        val gunluk = assertNotNull(section(report, R.string.report_section_log))
        val semptomlar = gunluk.blocks.filterIsInstance<ReportBlock.Table>().single()
        assertEquals(listOf(context.getString(R.string.symptom_bloating), context.getString(R.string.report_days_value, 2)), semptomlar.rows.first())
        val anahtarlar = gunluk.blocks.filterIsInstance<ReportBlock.KeyValues>().single().rows.map { it.first }
        assertTrue(context.getString(R.string.report_log_flow_days) in anahtarlar)
        assertFalse(context.getString(R.string.report_log_weight) in anahtarlar, "kilo varsayılan gizli")
        assertFalse(context.getString(R.string.report_log_mood) in anahtarlar, "beş kayıttan az: ortalama yok")

        val ilac = assertNotNull(section(report, R.string.report_section_meds))
        val ilacSatiri = ilac.blocks.filterIsInstance<ReportBlock.Table>().single().rows.single()
        assertEquals("Inofolic Combi", ilacSatiri[0])
        assertEquals("09:00, 21:00", ilacSatiri[2])
        assertTrue(ilacSatiri[3].startsWith("3 / "), ilacSatiri[3])

        val uyku = assertNotNull(section(report, R.string.report_section_sleep_water))
        val uykuSatirlari = uyku.blocks.filterIsInstance<ReportBlock.KeyValues>().single().rows
        assertEquals(context.getString(R.string.report_sleep_value, 7, 30), uykuSatirlari.first { it.first.startsWith("Ortalama uyku") }.second)

        val cilt = assertNotNull(section(report, R.string.report_section_skin))
        val ciltSatirlari = cilt.blocks.filterIsInstance<ReportBlock.KeyValues>().single().rows
        assertEquals(context.getString(R.string.skin_type_oily), ciltSatirlari.first { it.first == context.getString(R.string.report_skin_type) }.second)
        assertTrue(ciltSatirlari.first { it.first == context.getString(R.string.report_skin_acanthosis) }.second.contains(context.getString(R.string.report_skin_flag)))
        assertEquals(
            context.getString(R.string.report_skin_routine_value, 1, 0, 90),
            ciltSatirlari.first { it.first == context.getString(R.string.report_skin_routine) }.second,
        )

        val sorular = assertNotNull(section(report, R.string.report_section_questions))
        assertEquals(listOf("Metformin bana uygun mu?"), sorular.blocks.filterIsInstance<ReportBlock.Bullets>().single().items)
        assertTrue(sorular.blocks.any { it is ReportBlock.Paragraph && it.style == TextStyle.SMALL }, "sorulan sayısı küçük yazı")

        assertNull(section(report, R.string.report_section_fertility), "hedefte TTC ve kayıt yokken doğurganlık bölümü yok")
        // İbare dışında tanı/tedavi/öneri dili yok (rapor yalnızca kayıt özetidir).
        val govde = text.replace(report.disclaimer, "").lowercase()
        listOf("öner", "tedavi", "tanı").forEach { kelime -> assertFalse(kelime in govde, "raporda yorum dili: $kelime") }
    }

    @Test
    fun `dogurganlik bolumu olcumlerle gelir, grafik ve ibare tasir, hap modunda kaybolur`() {
        val raporlar = calistir {
            cycles.addPeriodStart(bugun.minusDays(20))
            val temps = listOf(36.3, 36.4, 36.2, 36.3, 36.4, 36.3, 36.5, 36.6, 36.7, 36.7)
            temps.forEachIndexed { i, t ->
                fertility.save(FertilityEntryEntity(date = bugun.minusDays(20L - i), bbtCelsius = t, lhResult = if (i == 5) LhResult.POSITIVE else null))
            }
            val acik = builder().build(ReportOptions.lastDays(ReportRange.DAYS_28, bugun))
            prefs.update { it.copy(goals = it.goals + Goal.PILL) }
            val hap = builder().build(ReportOptions.lastDays(ReportRange.DAYS_28, bugun))
            acik to hap
        }
        val bolum = assertNotNull(section(raporlar.first, R.string.report_section_fertility))
        val grafik = bolum.blocks.filterIsInstance<ReportBlock.TemperatureChart>().single()
        assertEquals(10, grafik.points.size)
        assertEquals(1, grafik.points.first().day)
        assertEquals(setOf(6), grafik.lhPositiveDays)
        assertNotNull(grafik.coverlineCelsius, "yumurtlama doğrulandı: örtü çizgisi var")
        assertEquals(context.getString(R.string.report_fertility_disclaimer), bolum.blocks.filterIsInstance<ReportBlock.Note>().single().text)
        val satirlar = bolum.blocks.filterIsInstance<ReportBlock.KeyValues>().single().rows
        assertTrue(satirlar.first { it.first == context.getString(R.string.report_fertility_ovulation) }.second.contains("36,40"), satirlar.toString())

        assertNull(section(raporlar.second, R.string.report_section_fertility), "hap modunda doğurganlık bölümü yazılmaz")
        assertTrue(section(raporlar.second, R.string.report_section_cycle)!!.blocks.any { it is ReportBlock.Note }, "hap notu")
    }

    @Test
    fun `bolum secimi ve bos veri`() {
        val report = calistir {
            builder().build(ReportOptions(from = bugun.minusDays(27), to = bugun, includeMeds = false, includeQuestions = false))
        }
        assertEquals(
            listOf(R.string.report_section_cycle, R.string.report_section_log).map { context.getString(it) },
            report.sections.map { it.title },
        )
        assertEquals(context.getString(R.string.report_cycle_no_period), (section(report, R.string.report_section_cycle)!!.blocks.first() as ReportBlock.Paragraph).text)
        // Sayfalama saf ölçerle: boş rapor tek sayfadır.
        val olcer = object : TextMeasurer {
            override fun width(text: String, style: TextStyle): Float = text.length * style.pointSize * 0.55f
            override fun lineHeight(style: TextStyle): Float = style.pointSize * 1.4f
        }
        assertEquals(1, ReportLayout(olcer).pageCount(report))
        assertTrue(builder().fileName(bugun).endsWith("$bugun.pdf"))
    }
}
