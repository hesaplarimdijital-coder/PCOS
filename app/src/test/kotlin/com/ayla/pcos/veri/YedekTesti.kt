package com.ayla.pcos.veri

import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.DailyLogEntity
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.repo.SymptomCatalog
import com.ayla.pcos.di.AppModule
import com.ayla.pcos.export.BACKUP_SCHEMA_VERSION
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.export.DataImporter
import com.ayla.pcos.export.ImportMode
import com.ayla.pcos.export.InvalidBackupException
import com.ayla.pcos.export.normalizedName
import kotlinx.serialization.encodeToString
import org.junit.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Yedekten geri yükleme: gerçek Room, gerçek DataStore; dışa aktarılan JSON aynen geri okunur. */
class YedekTesti : VeriTestOrtami() {

    private val json = AppModule.provideJson()
    private val rizaZamani = java.time.Instant.parse("2026-10-12T07:00:00Z")
    private val exporter by lazy { DataExporter(context, db, json, prefs) }
    private val importer by lazy { DataImporter(context, db, json, prefs, scheduler) }

    private fun inofolic(name: String = "Inofolic Combi") = MedicationEntity(
        name = name, doseText = "1 kapsül", kind = MedicationKind.SUPPLEMENT,
        times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
        createdAt = bugun.minusDays(30).atStartOfDay(zone).toInstant(),
    )

    @Test
    fun `uzerine yazinca kayitlar ve tercihler geri gelir, dozlar yeni ilac kimligine baglanir`() {
        val text = calistir {
            cycles.addPeriodStart(bugun.minusDays(40))
            cycles.endPeriod(bugun.minusDays(40), bugun.minusDays(36))
            cycles.addPeriodStart(bugun.minusDays(5))
            logs.save(DailyLogEntity(date = bugun.minusDays(1), mood = 4, symptoms = listOf(SymptomCatalog.BLOATING)))
            val id = meds.add(inofolic())
            meds.markTaken(id, bugun.minusDays(1), LocalTime.of(9, 0))
            meds.markTaken(id, bugun, LocalTime.of(9, 0))
            water.setCups(bugun, 5)
            sleep.addManual(bugun.minusDays(1).atTime(23, 30).atZone(zone).toInstant(), bugun.atTime(7, 0).atZone(zone).toInstant(), 3)
            prefs.update { it.copy(displayName = "Elif", waterGoalCups = 10) }
            json.encodeToString(exporter.buildBundle())
        }

        // Yeni cihaz: veri yok, rızalar farklı.
        calistir {
            exporter.wipeAll()
            prefs.update { UserPreferences(onboardingDone = true, consentPersonalization = true, healthDataConsentAt = rizaZamani) }
        }

        val bundle = importer.parse(text)
        val result = calistir { importer.apply(bundle, ImportMode.REPLACE) }

        calistir {
            val periods = db.periodDao().getAll()
            assertEquals(listOf(bugun.minusDays(40), bugun.minusDays(5)), periods.map { it.startDate })
            assertEquals(bugun.minusDays(36), periods.first().endDate)
            assertEquals(4, logs.get(bugun.minusDays(1))?.mood)
            val med = db.medicationDao().getAll().single()
            assertEquals("Inofolic Combi", med.name)
            assertEquals(bugun.minusDays(30).atStartOfDay(zone).toInstant(), med.createdAt)
            val doses = db.doseEventDao().getAll()
            assertEquals(2, doses.size)
            assertTrue(doses.all { it.medicationId == med.id })
            assertEquals(5, water.cups(bugun))
            assertEquals(1, db.sleepDao().getAll().size)
            assertEquals(2 + 1 + 1 + 2 + 1 + 1, result.added)
            assertEquals(0, result.skipped)

            val p = prefs.current()
            assertEquals("Elif", p.displayName)
            assertEquals(10, p.waterGoalCups)
            assertEquals(bugun.minusDays(40), p.trackingSince)
            // Rızalar ve karşılama durumu cihazdaki gibi kalır.
            assertTrue(p.consentPersonalization)
            assertEquals(rizaZamani, p.healthDataConsentAt)
            assertTrue(p.onboardingDone)
        }
    }

    @Test
    fun `birlestirmede cihazdaki kayit kazanir, ayni ilac yeniden eklenmez, acik adet tamamlanir`() {
        val text = calistir {
            cycles.addPeriodStart(bugun.minusDays(40))
            cycles.endPeriod(bugun.minusDays(40), bugun.minusDays(36))
            logs.save(DailyLogEntity(date = bugun.minusDays(2), mood = 2))
            logs.save(DailyLogEntity(date = bugun.minusDays(3), mood = 3))
            water.setCups(bugun.minusDays(1), 6)
            val id = meds.add(inofolic())
            meds.markTaken(id, bugun.minusDays(1), LocalTime.of(9, 0))
            json.encodeToString(exporter.buildBundle())
        }

        // Cihaz: aynı adet bitişsiz, aynı günün farklı kaydı, daha az su, aynı ilaç farklı yazımla.
        val deviceMedId = calistir {
            exporter.wipeAll()
            cycles.addPeriodStart(bugun.minusDays(40))
            logs.save(DailyLogEntity(date = bugun.minusDays(2), mood = 5))
            water.setCups(bugun.minusDays(1), 3)
            meds.add(inofolic(name = " inofolic combi "))
        }

        val result = calistir { importer.apply(importer.parse(text), ImportMode.MERGE) }

        calistir {
            assertEquals(bugun.minusDays(36), db.periodDao().getByStart(bugun.minusDays(40))?.endDate)
            assertEquals(5, logs.get(bugun.minusDays(2))?.mood, "aynı günün kaydı cihazdaki gibi kalmalı")
            assertEquals(3, logs.get(bugun.minusDays(3))?.mood, "eksik gün eklenmeli")
            assertEquals(6, water.cups(bugun.minusDays(1)), "suda büyük sayı alınır")
            assertEquals(1, db.medicationDao().getAll().size)
            assertEquals(deviceMedId, db.doseEventDao().getAll().single().medicationId)
            // eklenen: 1 günlük + 1 doz; atlanan: adet, günlük, su, ilaç
            assertEquals(2, result.added)
            assertEquals(4, result.skipped)
        }
    }

    @Test
    fun `surum 1 yedegi okunur, olusturulma zamani ilk dozdan cikarilir, sahipsiz doz atlanir`() {
        val v1 = """
            {"app":"Ayla","schemaVersion":1,"exportedAt":"2026-10-01T08:00:00Z",
             "periods":[{"start":"2026-09-10","end":"2026-09-14","excludedFromStats":false}],
             "dailyLogs":[],"hydration":[],"sleep":[],
             "medications":[{"id":7,"name":"D vitamini","doseText":"1 damla","kind":"SUPPLEMENT","form":"DROPS",
               "times":["13:00"],"withFood":true,"active":true,"stockCount":null,
               "pillActiveDays":null,"pillPlaceboDays":null,"packStartDate":null}],
             "doses":[
               {"medicationId":7,"date":"2026-09-03","time":"13:00","status":"TAKEN","actedAt":"2026-09-03T10:05:00Z"},
               {"medicationId":7,"date":"2026-09-01","time":"13:00","status":"TAKEN","actedAt":null},
               {"medicationId":99,"date":"2026-09-02","time":"08:00","status":"TAKEN","actedAt":null}]}
        """.trimIndent()

        val bundle = importer.parse(v1)
        val summary = importer.summarize(bundle)
        assertEquals(3, summary.doses)
        assertEquals(1, summary.medications)

        val result = calistir { importer.apply(bundle, ImportMode.MERGE) }
        calistir {
            val med = db.medicationDao().getAll().single()
            assertEquals(java.time.LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant(), med.createdAt)
            assertEquals(5, med.stockWarnDays)
            assertEquals(2, db.doseEventDao().getAll().size)
            assertEquals(1, result.skipped, "ilacı olmayan doz atlanır")
            assertEquals(java.time.LocalDate.of(2026, 9, 10), prefs.current().trackingSince)
        }
    }

    @Test
    fun `gecersiz dosyalar nedeniyle reddedilir ve hicbir sey yazilmaz`() {
        fun neden(text: String) = assertFailsWith<InvalidBackupException> { importer.parse(text) }.reason

        assertEquals(InvalidBackupException.Reason.CORRUPT, neden("bu bir json değil"))
        assertEquals(InvalidBackupException.Reason.NOT_AYLA, neden("[1, 2, 3]"))
        assertEquals(InvalidBackupException.Reason.NOT_AYLA, neden("""{"name":"başka uygulama","items":[]}"""))
        assertEquals(InvalidBackupException.Reason.TOO_NEW, neden("""{"app":"Ayla","schemaVersion":99,"exportedAt":"2026-10-01T08:00:00Z"}"""))
        assertEquals(
            InvalidBackupException.Reason.CORRUPT,
            neden("""{"app":"Ayla","schemaVersion":2,"exportedAt":"2026-10-01T08:00:00Z","periods":[{"start":"2026-13-45","end":null,"excludedFromStats":false}]}"""),
        )
        calistir { assertTrue(db.periodDao().getAll().isEmpty()) }
        assertNull(calistir { prefs.current().trackingSince })
    }

    @Test
    fun `ilac adi eslesmesi buyuk kucuk harf, bosluk ve Turkce I farkini yok sayar`() {
        val ad = "Inofolic Combi".normalizedName()
        listOf(" inofolic  combi ", "INOFOLIC COMBI", "İNOFOLİC COMBİ", "ınofolıc combı").forEach { yazim ->
            assertEquals(ad, yazim.normalizedName(), yazim)
        }
        assertTrue("Inofolic Plus".normalizedName() != ad)
    }

    // --- Sürüm 3 (ADR-0009): doğurganlık, cilt/saç, doktora sorular ---

    private suspend fun mvp3VerisiYaz() {
        fertility.save(FertilityEntryEntity(date = bugun.minusDays(2), bbtCelsius = 36.41, bbtTime = LocalTime.of(6, 30), lhResult = LhResult.NEGATIVE))
        fertility.save(FertilityEntryEntity(date = bugun.minusDays(1), bbtCelsius = 36.72, bbtDisturbed = true, lhResult = LhResult.POSITIVE, mucus = CervicalMucus.EGG_WHITE))
        skin.saveProfile(SkinProfileEntity(skinType = SkinType.OILY, acne = AcneSelfReport.MODERATE, acanthosis = true, morningReminder = LocalTime.of(8, 0)))
        skin.setSteps(bugun, RoutineSlot.MORNING, listOf("skin_step_cleanse", "skin_step_spf"))
        skin.setSteps(bugun.minusDays(1), RoutineSlot.EVENING, listOf("skin_step_cleanse"))
        questions.add("Metformin dozum uygun mu?")
        questions.add("İnositol ne kadar süre?")
    }

    @Test
    fun `surum 3 yedegi tam tur gider gelir ve tam silme dort tabloyu da bosaltir`() {
        val text = calistir {
            cycles.addPeriodStart(bugun.minusDays(20))
            mvp3VerisiYaz()
            val id = questions.getAll().first().id
            questions.markAsked(id, true, java.time.Instant.parse("2026-10-11T09:00:00Z"))
            json.encodeToString(exporter.buildBundle())
        }
        assertTrue("\"schemaVersion\": $BACKUP_SCHEMA_VERSION" in text)
        assertTrue("\"skinProfile\"" in text && "\"fertility\"" in text)

        calistir {
            exporter.wipeAll()
            assertTrue(db.fertilityEntryDao().getAll().isEmpty())
            assertNull(db.skinProfileDao().get())
            assertTrue(db.skinRoutineCheckDao().getAll().isEmpty())
            assertTrue(db.doctorQuestionDao().getAll().isEmpty())
            assertTrue(db.periodDao().getAll().isEmpty())
        }

        val bundle = importer.parse(text)
        val summary = importer.summarize(bundle)
        assertEquals(2, summary.fertilityDays)
        assertTrue(summary.hasSkinProfile)
        assertEquals(2, summary.skinRoutineSlots)
        assertEquals(2, summary.doctorQuestions)
        assertFalse(summary.isEmpty)

        val result = calistir { importer.apply(bundle, ImportMode.REPLACE) }
        calistir {
            val kayitlar = db.fertilityEntryDao().getAll()
            assertEquals(listOf(bugun.minusDays(2), bugun.minusDays(1)), kayitlar.map { it.date })
            assertEquals(36.41, kayitlar[0].bbtCelsius)
            assertEquals(LocalTime.of(6, 30), kayitlar[0].bbtTime)
            assertEquals(LhResult.NEGATIVE, kayitlar[0].lhResult)
            assertTrue(kayitlar[1].bbtDisturbed)
            assertEquals(CervicalMucus.EGG_WHITE, kayitlar[1].mucus)

            val profil = db.skinProfileDao().get()
            assertNotNull(profil)
            assertEquals(SkinType.OILY, profil.skinType)
            assertEquals(AcneSelfReport.MODERATE, profil.acne)
            assertTrue(profil.acanthosis)
            assertFalse(profil.hairThinning)
            assertEquals(LocalTime.of(8, 0), profil.morningReminder)
            assertNull(profil.eveningReminder)

            val rutin = db.skinRoutineCheckDao().getAll()
            assertEquals(2, rutin.size)
            assertEquals(listOf("skin_step_cleanse", "skin_step_spf"), rutin.last().doneSteps)
            assertEquals(RoutineSlot.MORNING, rutin.last().slot)

            val sorular = db.doctorQuestionDao().getAll()
            assertEquals(listOf("Metformin dozum uygun mu?", "İnositol ne kadar süre?"), sorular.map { it.text })
            assertEquals(java.time.Instant.parse("2026-10-11T09:00:00Z"), sorular[0].askedAt)
            assertNull(sorular[1].askedAt)

            // adet + 2 doğurganlık + profil + 2 rutin + 2 soru
            assertEquals(1 + 2 + 1 + 2 + 2, result.added)
            assertEquals(0, result.skipped)
        }
    }

    @Test
    fun `birlestirmede surum 3 kayitlarinda cihazdaki kazanir, eksikler eklenir`() {
        val text = calistir {
            mvp3VerisiYaz()
            json.encodeToString(exporter.buildBundle())
        }

        // Cihaz: aynı günün farklı doğurganlık kaydı, farklı profil, aynı dilimde farklı adımlar, aynı soru farklı yazımla.
        calistir {
            exporter.wipeAll()
            fertility.save(FertilityEntryEntity(date = bugun.minusDays(1), bbtCelsius = 36.9))
            skin.saveProfile(SkinProfileEntity(skinType = SkinType.DRY))
            skin.setSteps(bugun, RoutineSlot.MORNING, listOf("skin_step_moisturize"))
            questions.add("metformin  dozum uygun mu?")
        }

        val result = calistir { importer.apply(importer.parse(text), ImportMode.MERGE) }

        calistir {
            assertEquals(36.9, fertility.get(bugun.minusDays(1))?.bbtCelsius, "aynı günün kaydı cihazdaki gibi kalır")
            assertEquals(36.41, fertility.get(bugun.minusDays(2))?.bbtCelsius, "eksik gün eklenir")
            assertEquals(SkinType.DRY, skin.profile()?.skinType, "profil cihazdaki gibi kalır")
            assertEquals(listOf("skin_step_moisturize"), skin.routine(bugun, RoutineSlot.MORNING)?.doneSteps)
            assertEquals(listOf("skin_step_cleanse"), skin.routine(bugun.minusDays(1), RoutineSlot.EVENING)?.doneSteps)
            // Yedekteki soru oluşturulma zamanını korur, bu yüzden listede cihazdakinden önce gelir; küme karşılaştırılır.
            assertEquals(setOf("metformin  dozum uygun mu?", "İnositol ne kadar süre?"), questions.getAll().map { it.text }.toSet())
            // eklenen: 1 doğurganlık + 1 rutin + 1 soru; atlanan: doğurganlık, profil, rutin, soru
            assertEquals(3, result.added)
            assertEquals(4, result.skipped)
        }
    }

    @Test
    fun `surum 2 yedegi aynen okunur, yeni alanlar bos kalir`() {
        val v2 = """
            {"app":"Ayla","schemaVersion":2,"exportedAt":"2026-10-01T08:00:00Z",
             "periods":[{"start":"2026-09-10","end":null,"excludedFromStats":false}],
             "dailyLogs":[],"medications":[],"doses":[],"hydration":[{"date":"2026-09-10","cups":4}],"sleep":[],
             "preferences":{"displayName":"Elif","goals":["CYCLE"]}}
        """.trimIndent()

        val bundle = importer.parse(v2)
        val summary = importer.summarize(bundle)
        assertEquals(1, summary.periods)
        assertEquals(0, summary.fertilityDays)
        assertFalse(summary.hasSkinProfile)
        assertEquals(0, summary.skinRoutineSlots)
        assertEquals(0, summary.doctorQuestions)

        val result = calistir { importer.apply(bundle, ImportMode.REPLACE) }
        calistir {
            assertEquals(2, result.added)
            assertEquals(1, db.periodDao().getAll().size)
            assertTrue(db.fertilityEntryDao().getAll().isEmpty())
            assertNull(db.skinProfileDao().get())
            assertEquals("Elif", prefs.current().displayName)
        }
    }

    @Test
    fun `bilinmeyen enum adi tasiyan surum 3 kaydi atlanir, dosya reddedilmez`() {
        val v3 = """
            {"app":"Ayla","schemaVersion":3,"exportedAt":"2026-10-01T08:00:00Z",
             "fertility":[
               {"date":"2026-09-20","bbtCelsius":36.456,"lh":"MAYBE"},
               {"date":"2026-09-21","bbtCelsius":36.5,"mucus":"EGG_WHITE","lh":"NEGATIVE"},
               {"date":"2026-09-22","mucus":"FOAMY"}],
             "skinProfile":{"skinType":"GLASS","darkSpots":true},
             "skinRoutine":[
               {"date":"2026-09-21","slot":"NOON","doneSteps":["skin_step_spf"]},
               {"date":"2026-09-21","slot":"EVENING","doneSteps":["skin_step_cleanse","","skin_step_cleanse"]},
               {"date":"2026-09-22","slot":"MORNING","doneSteps":[]}],
             "doctorQuestions":[{"text":"   ","createdAt":"2026-09-21T10:00:00Z"},{"text":" Soru ","createdAt":"2026-09-21T10:00:00Z","askedAt":null}]}
        """.trimIndent()

        val bundle = importer.parse(v3)
        val result = calistir { importer.apply(bundle, ImportMode.MERGE) }
        calistir {
            val kayitlar = db.fertilityEntryDao().getAll()
            assertEquals(listOf(java.time.LocalDate.of(2026, 9, 21)), kayitlar.map { it.date }, "bilinmeyen LH ve mukus adı taşıyan günler atlanır")
            assertEquals(CervicalMucus.EGG_WHITE, kayitlar.single().mucus)
            assertNull(db.skinProfileDao().get(), "bilinmeyen cilt tipi: profil atlanır")
            val rutin = db.skinRoutineCheckDao().getAll()
            assertEquals(1, rutin.size, "bilinmeyen dilim ve boş adım listesi atlanır")
            assertEquals(RoutineSlot.EVENING, rutin.single().slot)
            assertEquals(listOf("skin_step_cleanse"), rutin.single().doneSteps)
            assertEquals(listOf("Soru"), db.doctorQuestionDao().getAll().map { it.text })
            assertEquals(3, result.added)
        }
    }

    @Test
    fun `isi degeri ice aktarmada da iki ondaliga yuvarlanir`() {
        val v3 = """
            {"app":"Ayla","schemaVersion":3,"exportedAt":"2026-10-01T08:00:00Z",
             "fertility":[{"date":"2026-09-20","bbtCelsius":36.456,"bbtTime":"06:30"}]}
        """.trimIndent()
        calistir { importer.apply(importer.parse(v3), ImportMode.MERGE) }
        val kayit = calistir { fertility.get(java.time.LocalDate.of(2026, 9, 20)) }
        assertNotNull(kayit)
        assertEquals(36.46, kayit.bbtCelsius)
        assertEquals(LocalTime.of(6, 30), kayit.bbtTime)
    }
}
