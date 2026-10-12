package com.ayla.pcos.veri

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
import com.ayla.pcos.data.db.ALL_MIGRATIONS
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.AylaDatabase
import com.ayla.pcos.data.db.DoctorQuestionEntity
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinRoutineCheckEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.repo.SymptomCatalog
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Room şema geçişleri. Sürüm 1 veritabanı, Room'un ürettiği şema dosyasındaki (app/schemas/.../1.json)
 * CREATE cümleleriyle birebir kurulur; sonra Room gerçek geçiş yoluyla (addMigrations) açılır. Room
 * açılışta her tabloyu varlık tanımıyla karşılaştırır: sütun türü, NOT NULL ya da anahtar farkı
 * "Migration didn't properly handle" ile açılışı durdurur; test bu yüzden hem veri korunumunu hem şema
 * eşleşmesini kanıtlar. Şema dosyası sınıf yolundan okunur (build.gradle.kts: test resources).
 */
@RunWith(RobolectricTestRunner::class)
class GecisTesti {
    private lateinit var context: Context
    private lateinit var dosya: File
    private val acikVeritabanlari = mutableListOf<AylaDatabase>()

    @Before
    fun kur() {
        context = RuntimeEnvironment.getApplication()
        dosya = context.getDatabasePath(DB_ADI)
        dosya.parentFile?.mkdirs()
        yardimciDosyalariSil()
    }

    @After
    fun kapat() {
        acikVeritabanlari.forEach { it.close() }
        acikVeritabanlari.clear()
        yardimciDosyalariSil()
    }

    private fun yardimciDosyalariSil() {
        listOf("", "-journal", "-wal", "-shm").forEach { File(dosya.path + it).delete() }
    }

    @Test
    fun `surum 1 veritabani gecisle acilir, eski kayitlar korunur ve yeni tablolar calisir`() {
        val adet = LocalDate.of(2026, 9, 10)
        surum1Kur { db ->
            db.execSQL(
                "INSERT INTO periods (start_date, end_date, excluded_from_stats, created_at) VALUES (?, ?, 0, ?)",
                arrayOf<Any?>(adet.toString(), adet.plusDays(4).toString(), Instant.parse("2026-09-10T06:00:00Z").toEpochMilli()),
            )
            db.execSQL(
                "INSERT INTO daily_logs (date, mood, symptoms, updated_at) VALUES (?, 4, ?, ?)",
                arrayOf<Any?>(adet.toString(), SymptomCatalog.BLOATING + "|" + SymptomCatalog.ACNE, Instant.now().toEpochMilli()),
            )
        }

        val db = gecisleAc()
        runBlocking {
            // Eski tablolar ve veri yerinde; açılış sırasında Room şemayı doğruladı.
            val periods = db.periodDao().getAll()
            assertEquals(listOf(adet), periods.map { it.startDate })
            assertEquals(adet.plusDays(4), periods.single().endDate)
            assertEquals(listOf(SymptomCatalog.BLOATING, SymptomCatalog.ACNE), db.dailyLogDao().get(adet)?.symptoms)
            assertEquals(2, db.openHelper.readableDatabase.version)

            // Yeni tablolar boş başlar ve tam tur okunur.
            assertTrue(db.fertilityEntryDao().getAll().isEmpty())
            val gun = LocalDate.of(2026, 10, 1)
            db.fertilityEntryDao().upsert(
                FertilityEntryEntity(date = gun, bbtCelsius = 36.62, bbtTime = LocalTime.of(6, 45), bbtDisturbed = true, lhResult = LhResult.POSITIVE, mucus = CervicalMucus.WATERY),
            )
            val kayit = db.fertilityEntryDao().get(gun)
            assertNotNull(kayit)
            assertEquals(36.62, kayit.bbtCelsius)
            assertEquals(LocalTime.of(6, 45), kayit.bbtTime)
            assertTrue(kayit.bbtDisturbed)
            assertEquals(LhResult.POSITIVE, kayit.lhResult)
            assertEquals(CervicalMucus.WATERY, kayit.mucus)

            db.skinProfileDao().upsert(SkinProfileEntity(skinType = SkinType.SENSITIVE, acne = AcneSelfReport.MILD, acanthosis = true, eveningReminder = LocalTime.of(22, 0)))
            val profil = db.skinProfileDao().get()
            assertNotNull(profil)
            assertEquals(SkinType.SENSITIVE, profil.skinType)
            assertEquals(AcneSelfReport.MILD, profil.acne)
            assertTrue(profil.acanthosis)
            assertEquals(LocalTime.of(22, 0), profil.eveningReminder)

            db.skinRoutineCheckDao().upsert(SkinRoutineCheckEntity(date = gun, slot = RoutineSlot.EVENING, doneSteps = listOf("skin_step_cleanse", "skin_step_moisturize")))
            db.skinRoutineCheckDao().upsert(SkinRoutineCheckEntity(date = gun, slot = RoutineSlot.EVENING, doneSteps = listOf("skin_step_cleanse")))
            assertEquals(listOf("skin_step_cleanse"), db.skinRoutineCheckDao().get(gun, RoutineSlot.EVENING)?.doneSteps, "bileşik anahtar üzerine yazar")
            assertEquals(1, db.skinRoutineCheckDao().getAll().size)

            val id = db.doctorQuestionDao().insert(DoctorQuestionEntity(text = "Metformin dozum uygun mu?"))
            val ikinci = db.doctorQuestionDao().insert(DoctorQuestionEntity(text = "İnositol?"))
            assertTrue(ikinci > id, "kimlik otomatik artar")
            db.doctorQuestionDao().setAskedAt(id, Instant.parse("2026-10-05T09:00:00Z"))
            assertEquals(Instant.parse("2026-10-05T09:00:00Z"), db.doctorQuestionDao().get(id)?.askedAt)
            assertEquals(listOf(id, ikinci), db.doctorQuestionDao().getAll().map { it.id })
        }
    }

    @Test
    fun `gecis listesi veritabani surumune kadar ulasir`() {
        // Sürüm artırılıp Migration unutulursa burada yakalanır; cihazda "Migration didn't properly handle" çökmesi olmaz.
        val sonSurum = ALL_MIGRATIONS.maxOf { it.endVersion }
        assertEquals(surumOku(), sonSurum, "AylaDatabase sürümü ile son geçiş hedefi aynı olmalı")
        (1 until sonSurum).forEach { from ->
            assertTrue(ALL_MIGRATIONS.any { it.startVersion == from && it.endVersion == from + 1 }, "$from → ${from + 1} geçişi eksik")
        }
    }

    /** `@Database(version = N)` değeri: Room'un sıfırdan kurduğu bellek içi veritabanının user_version'ı. */
    private fun surumOku(): Int {
        val db = Room.inMemoryDatabaseBuilder(context, AylaDatabase::class.java).allowMainThreadQueries().build()
        acikVeritabanlari += db
        return db.openHelper.readableDatabase.version
    }

    @Test
    fun `gecis SQL cumleleri Room tarafindan uretilen sema ile sutun ve indeks duzeyinde birebirdir`() {
        surum1Kur()
        val gecisli = gecisleAc().openHelper.readableDatabase
        val tazeDb = Room.inMemoryDatabaseBuilder(context, AylaDatabase::class.java).allowMainThreadQueries().build()
        acikVeritabanlari += tazeDb
        val taze = tazeDb.openHelper.readableDatabase

        YENI_TABLOLAR.forEach { tablo ->
            assertEquals(taze.sutunlar(tablo), gecisli.sutunlar(tablo), "$tablo sütunları")
            assertEquals(taze.indeksler(tablo), gecisli.indeksler(tablo), "$tablo indeksleri")
        }
        // Geçiş eski tablolara dokunmaz; sürüm 1 şemasındaki tablolar da Room'un beklediğiyle aynıdır.
        ESKI_TABLOLAR.forEach { tablo ->
            assertEquals(taze.sutunlar(tablo), gecisli.sutunlar(tablo), "$tablo sütunları")
        }
    }

    /** Sürüm 1 dosyasını kurar; [doldur] ile eski biçimde satır eklenebilir. */
    private fun surum1Kur(doldur: (SQLiteDatabase) -> Unit = {}) {
        val sema = surumSemasi(1)
        SQLiteDatabase.openOrCreateDatabase(dosya, null).use { db ->
            sema.cumleler.forEach(db::execSQL)
            doldur(db)
            db.version = 1
        }
    }

    private fun gecisleAc(): AylaDatabase =
        Room.databaseBuilder(context, AylaDatabase::class.java, DB_ADI)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { acikVeritabanlari += it }

    private class SurumSemasi(val surum: Int, val cumleler: List<String>)

    /**
     * Room'un dışa aktardığı şema dosyasından CREATE cümlelerini okur (tablolar, indeksler ve
     * room_master_table kurulumu). `${TABLE_NAME}` yer tutucusu tablo adıyla değiştirilir.
     */
    private fun surumSemasi(surum: Int): SurumSemasi {
        val metin = semaMetni(surum)
        val database = Json.parseToJsonElement(metin).jsonObject.getValue("database").jsonObject
        assertEquals(surum, database.getValue("version").jsonPrimitive.content.toInt())
        val cumleler = mutableListOf<String>()
        database.getValue("entities").jsonArray.forEach { entity ->
            val obj = entity.jsonObject
            val tablo = obj.getValue("tableName").jsonPrimitive.content
            cumleler += obj.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", tablo)
            obj["indices"]?.jsonArray?.forEach { index ->
                cumleler += index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", tablo)
            }
        }
        database["setupQueries"]?.jsonArray?.forEach { cumleler += it.jsonPrimitive.content }
        return SurumSemasi(surum, cumleler)
    }

    /** Önce sınıf yolu (test resources), olmazsa proje dizini (Gradle test çalışma dizini app/). */
    private fun semaMetni(surum: Int): String {
        val kaynak = "${AylaDatabase::class.java.name}/$surum.json"
        javaClass.classLoader?.getResourceAsStream(kaynak)?.use { return it.readBytes().decodeToString() }
        val adaylar = listOf(File("schemas", kaynak), File("app/schemas", kaynak))
        val dosya = adaylar.firstOrNull { it.isFile } ?: error("Şema dosyası bulunamadı: $kaynak (${adaylar.map { it.absolutePath }})")
        return dosya.readText()
    }

    /** PRAGMA table_info: ad, tür, NOT NULL, varsayılan, anahtar sırası (Room sütun sırasına bakmaz; ada göre sıralı). */
    private fun SupportSQLiteDatabase.sutunlar(tablo: String): List<String> =
        query("PRAGMA table_info(`$tablo`)").use { c ->
            val adI = c.getColumnIndexOrThrow("name")
            val turI = c.getColumnIndexOrThrow("type")
            val notNullI = c.getColumnIndexOrThrow("notnull")
            val varsayilanI = c.getColumnIndexOrThrow("dflt_value")
            val pkI = c.getColumnIndexOrThrow("pk")
            generateSequence { if (c.moveToNext()) c else null }
                .map { "${it.getString(adI)} ${it.getString(turI)} notnull=${it.getInt(notNullI)} dflt=${it.getString(varsayilanI)} pk=${it.getInt(pkI)}" }
                .toList()
                .sorted()
                .also { assertTrue(it.isNotEmpty(), "$tablo tablosu yok") }
        }

    /** PRAGMA index_list + index_info: ad, benzersizlik ve sütunlar (otomatik anahtar indeksleri dahil). */
    private fun SupportSQLiteDatabase.indeksler(tablo: String): List<String> {
        val liste = query("PRAGMA index_list(`$tablo`)").use { c ->
            val adI = c.getColumnIndexOrThrow("name")
            val uniqueI = c.getColumnIndexOrThrow("unique")
            generateSequence { if (c.moveToNext()) c else null }.map { it.getString(adI) to it.getInt(uniqueI) }.toList()
        }
        return liste.map { (ad, unique) ->
            val sutunlar = query("PRAGMA index_info(`$ad`)").use { c ->
                val adI = c.getColumnIndexOrThrow("name")
                generateSequence { if (c.moveToNext()) c else null }.map { it.getString(adI) }.toList()
            }
            "$ad unique=$unique $sutunlar"
        }.sorted()
    }

    private companion object {
        const val DB_ADI = "gecis-testi.db"
        val YENI_TABLOLAR = listOf("fertility_entries", "skin_profile", "skin_routine_checks", "doctor_questions")
        val ESKI_TABLOLAR = listOf("periods", "daily_logs", "medications", "dose_events", "hydration", "sleep_sessions")
    }
}
