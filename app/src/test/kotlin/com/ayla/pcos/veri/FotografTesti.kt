package com.ayla.pcos.veri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ayla.pcos.data.photos.PhotoCipher
import com.ayla.pcos.data.photos.PhotoKeyProvider
import com.ayla.pcos.data.photos.PhotoKind
import com.ayla.pcos.data.photos.PhotoRecord
import com.ayla.pcos.data.photos.PhotoStore
import com.ayla.pcos.data.photos.PhotoStoreAvailability
import com.ayla.pcos.data.photos.PhotoStoreUnavailableException
import com.ayla.pcos.di.AppModule
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Keystore yerine bellekte duran yazılımsal AES anahtarı: Robolectric'te Android Keystore yoktur
 * (veritabanı testleri de SQLCipher'sız bellek içi Room kullanır). Anahtar sabittir; [hata] verilirse
 * Keystore kaybı taklit edilir.
 */
class YazilimsalFotografAnahtari : PhotoKeyProvider {
    var anahtar: SecretKey = anahtarUret(1)
    var sifirlamaSayisi = 0
    var hata: Throwable? = null
    var sonPhotosExist: Boolean? = null

    override fun key(photosExist: Boolean): SecretKey {
        sonPhotosExist = photosExist
        hata?.let { throw PhotoStoreUnavailableException("Keystore anahtarı açılamadı", it) }
        return anahtar
    }

    override fun reset() {
        sifirlamaSayisi++
        anahtar = anahtarUret(sifirlamaSayisi + 1)
    }

    companion object {
        fun anahtarUret(tohum: Int): SecretKey = SecretKeySpec(ByteArray(32) { (it * 7 + tohum).toByte() }, "AES")
    }
}

/**
 * Şifreli fotoğraf deposu (#25 / US-093, ADR-0010): gerçek JPEG kodlama için Robolectric yerel grafik kipi.
 * Kaynaklar geçici klasörde üretilir; depo uygulamanın filesDir/photos ve cacheDir/camera dizinlerini kullanır.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FotografTesti {

    @get:Rule
    val tmp = TemporaryFolder()

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")
    private val bugun: LocalDate = LocalDate.of(2026, 10, 12)
    private val saat: Clock = Clock.fixed(bugun.atTime(10, 0).atZone(zone).toInstant(), zone)
    private val json = AppModule.provideJson()
    private lateinit var context: Context
    private lateinit var anahtar: YazilimsalFotografAnahtari
    private lateinit var depo: PhotoStore

    private val fotografDizini: File get() = File(context.filesDir, "photos")
    private val kameraDizini: File get() = File(context.cacheDir, "camera")
    private val dizinDosyasi: File get() = File(fotografDizini, "index.json")

    @Before
    fun kur() {
        context = RuntimeEnvironment.getApplication()
        fotografDizini.deleteRecursively()
        kameraDizini.deleteRecursively()
        anahtar = YazilimsalFotografAnahtari()
        depo = PhotoStore(context, anahtar, json, saat)
    }

    private fun <T> calistir(block: suspend () -> T): T = runBlocking { block() }

    private fun sifreliDosya(record: PhotoRecord): File = File(fotografDizini, record.id + ".enc")

    /** Gerçek bir JPEG kaynak: degrade dolgu, böylece sıkıştırılmış dosya anlamlı boyutta olur. */
    private fun jpegUret(genislik: Int, yukseklik: Int): File {
        val bmp = Bitmap.createBitmap(genislik, yukseklik, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, genislik.toFloat(), yukseklik.toFloat(),
                Color.rgb(230, 170, 150), Color.rgb(60, 40, 90), Shader.TileMode.CLAMP,
            )
        }
        Canvas(bmp).drawRect(0f, 0f, genislik.toFloat(), yukseklik.toFloat(), paint)
        val file = File.createTempFile("kaynak", ".jpg", tmp.root)
        FileOutputStream(file).use { assertTrue(bmp.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        bmp.recycle()
        return file
    }

    private fun kaydet(kaynak: File, kind: PhotoKind = PhotoKind.SKIN, area: String? = "çene", date: LocalDate = bugun): PhotoRecord =
        calistir { FileInputStream(kaynak).use { depo.save(it, kind, area, date) } }

    private fun jpegImzasi(bytes: ByteArray, offset: Int = 0): Boolean =
        bytes.size >= offset + 3 &&
            bytes[offset] == 0xFF.toByte() && bytes[offset + 1] == 0xD8.toByte() && bytes[offset + 2] == 0xFF.toByte()

    private fun dizaAktar(id: String): ByteArray {
        val uri = Uri.parse("content://com.ayla.test/disa/$id.jpg")
        val out = ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(uri, out)
        assertTrue(calistir { depo.exportTo(uri, id) })
        return out.toByteArray()
    }

    @Test
    fun `kaydet listele yukle, cozulen goruntunun en uzun kenari 1600 pikseli gecmez`() {
        val kaynak = jpegUret(3000, 1500)

        val record = kaydet(kaynak, area = " çene ")

        assertEquals(PhotoKind.SKIN, record.kind)
        assertEquals("çene", record.area, "bölge kırpılır")
        assertEquals(bugun, record.date)
        assertEquals(saat.instant(), record.createdAt)
        assertEquals(1600, maxOf(record.width, record.height))
        assertTrue(record.height in 799..800, "en-boy oranı korunur: ${record.width}x${record.height}")
        assertEquals(sifreliDosya(record).length(), record.byteSize)
        assertTrue(record.byteSize > 1_000)

        assertEquals(listOf(record), calistir { depo.list() })
        assertEquals(listOf(record), depo.records.value)
        assertIs<PhotoStoreAvailability.Available>(calistir { depo.availability() })

        val bitmap = assertNotNull(calistir { depo.load(record.id) })
        assertEquals(1600, maxOf(bitmap.width, bitmap.height))
        assertEquals(record.width, bitmap.width)
        assertEquals(record.height, bitmap.height)
        assertNull(calistir { depo.load("olmayan") })
        assertTrue(kameraDizini.listFiles().isNullOrEmpty(), "kaynak kopyası geçici dizinde kalmaz")
    }

    @Test
    fun `diskteki dosya duz JPEG degildir`() {
        val record = kaydet(jpegUret(640, 480))

        val bytes = sifreliDosya(record).readBytes()
        assertTrue(bytes.size > 12 + 16, "IV ve etiketten büyük olmalı")
        assertFalse(jpegImzasi(bytes), "dosya başı JPEG imzası taşımaz")
        assertFalse(jpegImzasi(bytes, offset = 12), "IV sonrası da düz JPEG değildir")
        // Çözülen içerik gerçek JPEG'dir.
        val cozulmus = PhotoCipher.decrypt(anahtar.anahtar, record.id + ".enc", FileInputStream(sifreliDosya(record))).use { it.readBytes() }
        assertTrue(jpegImzasi(cozulmus))
    }

    @Test
    fun `dizin dosyasi sifrelidir ve ayni anahtarla cozulur`() {
        val record = kaydet(jpegUret(300, 300), kind = PhotoKind.HAIR, area = "tepe")

        assertTrue(dizinDosyasi.isFile)
        val ham = dizinDosyasi.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(ham.contains("schemaVersion"), "dizin düz metin JSON olmamalı")
        assertFalse(ham.contains(record.id))
        assertFalse(ham.contains("tepe"))

        val metin = PhotoCipher.decrypt(anahtar.anahtar, "index.json", FileInputStream(dizinDosyasi)).use { it.readBytes() }.decodeToString()
        assertTrue(metin.contains("\"schemaVersion\": 1"), metin)
        assertTrue(metin.contains(record.id))
        assertTrue(metin.contains("\"kind\": \"HAIR\""))
        assertTrue(metin.contains("\"date\": \"2026-10-12\""))
    }

    @Test
    fun `yeniden acilista dizin okunur ve fotograf yuklenir`() {
        val record = kaydet(jpegUret(500, 400), kind = PhotoKind.HAIR)

        val yeniDepo = PhotoStore(context, anahtar, json, saat)
        assertTrue(yeniDepo.records.value.isEmpty(), "yüklenmeden önce boş")
        assertEquals(listOf(record), calistir { yeniDepo.list() })
        assertEquals(listOf(record), yeniDepo.records.value)
        val bitmap = assertNotNull(calistir { yeniDepo.load(record.id) })
        assertEquals(500, bitmap.width)
        assertEquals(400, bitmap.height)
        assertEquals(true, anahtar.sonPhotosExist, "şifreli dosya varken anahtar sağlayıcıya söylenir")
    }

    @Test
    fun `tur filtresi ve siralama, yeni tarih once`() {
        val eski = kaydet(jpegUret(200, 200), kind = PhotoKind.SKIN, date = bugun.minusDays(30))
        val sac = kaydet(jpegUret(200, 200), kind = PhotoKind.HAIR, date = bugun.minusDays(7))
        val yeni = kaydet(jpegUret(200, 200), kind = PhotoKind.SKIN, date = bugun)

        assertEquals(listOf(yeni, sac, eski), calistir { depo.list() })
        assertEquals(listOf(yeni, eski), calistir { depo.list(PhotoKind.SKIN) })
        assertEquals(listOf(sac), calistir { depo.list(PhotoKind.HAIR) })
    }

    @Test
    fun `silme dosyayi ve dizin kaydini kaldirir`() {
        val birinci = kaydet(jpegUret(300, 200))
        val ikinci = kaydet(jpegUret(300, 200), date = bugun.minusDays(1))

        calistir { depo.delete(birinci.id) }

        assertEquals(listOf(ikinci), calistir { depo.list() })
        assertFalse(sifreliDosya(birinci).exists())
        assertTrue(sifreliDosya(ikinci).exists())
        assertNull(calistir { depo.load(birinci.id) })
        val metin = PhotoCipher.decrypt(anahtar.anahtar, "index.json", FileInputStream(dizinDosyasi)).use { it.readBytes() }.decodeToString()
        assertFalse(metin.contains(birinci.id))
        assertTrue(metin.contains(ikinci.id))
        calistir { depo.delete("olmayan") } // sessizce biter
        assertEquals(1, calistir { depo.list() }.size)
    }

    @Test
    fun `tam silme hicbir iz birakmaz ve anahtari sifirlar`() {
        kaydet(jpegUret(300, 200))
        kaydet(jpegUret(300, 200), kind = PhotoKind.HAIR)
        val hedef = depo.cameraTarget()
        jpegUret(100, 100).copyTo(hedef.file, overwrite = true)
        assertTrue(fotografDizini.isDirectory && kameraDizini.isDirectory)

        calistir { depo.deleteAll() }

        assertFalse(fotografDizini.exists())
        assertFalse(kameraDizini.exists())
        assertTrue(depo.records.value.isEmpty())
        assertEquals(1, anahtar.sifirlamaSayisi)
        // Depo yeniden kullanılabilir: yeni anahtar, boş dizin.
        assertTrue(calistir { depo.list() }.isEmpty())
        val record = kaydet(jpegUret(200, 100))
        assertEquals(listOf(record), calistir { depo.list() })
    }

    @Test
    fun `kamera gecici dosyasi kayittan sonra her durumda silinir`() {
        val hedef = depo.cameraTarget()
        assertTrue(hedef.file.parentFile == kameraDizini)
        assertEquals(hedef.file, depo.capture(hedef.id).file, "kimlikten yeniden kurulur")
        jpegUret(400, 200).copyTo(hedef.file, overwrite = true)

        val record = calistir { depo.saveCapture(hedef, PhotoKind.SKIN, null, bugun) }

        assertFalse(hedef.file.exists(), "düz dosya silinir")
        assertNull(record.area)
        assertEquals(400, record.width)
        assertEquals(listOf(record), calistir { depo.list() })

        // Boş hedef: kayıt başarısız olur ama dosya yine silinir.
        val bos = depo.cameraTarget()
        bos.file.writeBytes(ByteArray(0))
        assertFailsWith<java.io.IOException> { calistir { depo.saveCapture(bos, PhotoKind.SKIN, null, bugun) } }
        assertFalse(bos.file.exists())

        // İptal: discardCapture siler.
        val iptal = depo.cameraTarget()
        iptal.file.writeBytes(byteArrayOf(1, 2, 3))
        calistir { depo.discardCapture(iptal) }
        assertFalse(iptal.file.exists())

        assertFailsWith<IllegalArgumentException> { depo.capture("../../gizli") }
    }

    @Test
    fun `kamera hedefi uygulamanin kendi FileProvider adresini verir`() {
        val hedef = depo.cameraTarget()

        assertEquals("content", hedef.uri.scheme)
        assertEquals(context.packageName + ".photos", hedef.uri.authority)
        assertEquals("/camera/${hedef.id}.jpg", hedef.uri.path)
    }

    @Test
    fun `temizlik eski gecici dosyalari siler, yenileri birakir`() {
        kameraDizini.mkdirs()
        val eski = File(kameraDizini, "eski.jpg").apply { writeBytes(byteArrayOf(1)) }
        val yeni = File(kameraDizini, "yeni.jpg").apply { writeBytes(byteArrayOf(1)) }
        assertTrue(eski.setLastModified(System.currentTimeMillis() - Duration.ofHours(2).toMillis()))

        calistir { depo.cleanupTemp() }
        assertFalse(eski.exists())
        assertTrue(yeni.exists(), "çekim sürüyor olabilir; yeni dosya korunur")

        calistir { depo.cleanupTemp(Duration.ZERO) }
        assertFalse(yeni.exists())
    }

    @Test
    fun `kaynaktaki EXIF konum ve cihaz bilgisi saklanan fotografa gecmez`() {
        val kaynak = jpegUret(400, 200)
        ExifInterface(kaynak.path).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "41/1,1/1,30/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "28/1,58/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:12 10:00:00")
            setAttribute(ExifInterface.TAG_MAKE, "TestMarka")
            saveAttributes()
        }
        val kaynakExif = ExifInterface(kaynak.path)
        assertNotNull(kaynakExif.latLong, "kaynakta konum olmalı ki test anlamlı olsun")
        assertEquals("TestMarka", kaynakExif.getAttribute(ExifInterface.TAG_MAKE))

        val record = kaydet(kaynak)
        val cikti = dizaAktar(record.id)

        assertTrue(jpegImzasi(cikti), "dışa aktarılan dosya düz JPEG")
        val ciktiExif = ExifInterface(ByteArrayInputStream(cikti))
        assertNull(ciktiExif.latLong, "konum silinmiş olmalı")
        assertNull(ciktiExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(ciktiExif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertNull(ciktiExif.getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun `EXIF yon etiketi uygulanir, meta veri olarak tasinmaz`() {
        val kaynak = jpegUret(400, 200)
        ExifInterface(kaynak.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val record = kaydet(kaynak)

        assertEquals(200, record.width)
        assertEquals(400, record.height)
        val bitmap = assertNotNull(calistir { depo.load(record.id) })
        assertEquals(200, bitmap.width)
        assertEquals(400, bitmap.height)
        // Yeniden kodlanan JPEG'de yön etiketi ya yoktur ya da "tanımsız/normal"dır (Robolectric'in kodlayıcısı
        // boş bir EXIF bloğu yazabiliyor); döndürülmüş değer taşımaz.
        val ciktiExif = ExifInterface(ByteArrayInputStream(dizaAktar(record.id)))
        val yon = ciktiExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        assertTrue(yon == ExifInterface.ORIENTATION_NORMAL || yon == ExifInterface.ORIENTATION_UNDEFINED, "yön: $yon")
    }

    @Test
    fun `kucuk onizleme istenen boyutu asmaz`() {
        val record = kaydet(jpegUret(3200, 1600))
        assertEquals(1600, record.width)

        val onizleme = assertNotNull(calistir { depo.thumbnail(record.id, 200) })
        assertTrue(maxOf(onizleme.width, onizleme.height) <= 200, "${onizleme.width}x${onizleme.height}")
        assertTrue(onizleme.width >= 150, "örnekleme çok küçültmez: ${onizleme.width}")
        assertNull(calistir { depo.thumbnail("olmayan", 200) })
    }

    @Test
    fun `disa aktarma duz JPEG yazar, olmayan kayit icin false doner`() {
        val record = kaydet(jpegUret(300, 300))

        val cikti = dizaAktar(record.id)
        assertTrue(jpegImzasi(cikti))
        assertTrue(cikti.size > 1_000)

        val uri = Uri.parse("content://com.ayla.test/disa/yok.jpg")
        shadowOf(context.contentResolver).registerOutputStream(uri, ByteArrayOutputStream())
        assertFalse(calistir { depo.exportTo(uri, "olmayan") })
    }

    @Test
    fun `anahtar acilamiyorsa depo erisilemez der, sessizce yeni anahtar uretmez, dosyalara dokunmaz`() {
        val record = kaydet(jpegUret(300, 200))
        val dosyaSayisi = fotografDizini.listFiles()!!.size

        anahtar.hata = IllegalStateException("Keystore anahtarı geçersiz")
        val kayipDepo = PhotoStore(context, anahtar, json, saat)

        val durum = calistir { kayipDepo.availability() }
        assertIs<PhotoStoreAvailability.Unavailable>(durum)
        assertIs<PhotoStoreUnavailableException>(durum.cause)
        assertFailsWith<PhotoStoreUnavailableException> { calistir { kayipDepo.list() } }
        assertFailsWith<PhotoStoreUnavailableException> {
            calistir { FileInputStream(jpegUret(100, 100)).use { kayipDepo.save(it, PhotoKind.SKIN, null, bugun) } }
        }
        assertFailsWith<PhotoStoreUnavailableException> { calistir { kayipDepo.load(record.id) } }
        assertEquals(0, anahtar.sifirlamaSayisi, "sessizce yeni anahtar üretilmez")
        assertEquals(dosyaSayisi, fotografDizini.listFiles()!!.size, "dosyalara dokunulmaz")
        assertTrue(sifreliDosya(record).exists())

        // Karar kullanıcının: tam silme anahtar açılamazken de çalışır ve yeni başlangıç verir.
        calistir { kayipDepo.deleteAll() }
        assertFalse(fotografDizini.exists())
        assertEquals(1, anahtar.sifirlamaSayisi)
        anahtar.hata = null
        assertIs<PhotoStoreAvailability.Available>(calistir { kayipDepo.availability() })
    }

    @Test
    fun `dizin bozuksa sifreli fotograf varken erisilemez, yokken yenilenir`() {
        val record = kaydet(jpegUret(300, 200))
        dizinDosyasi.writeBytes(ByteArray(64) { 0x5A })

        val bozuk = PhotoStore(context, anahtar, json, saat)
        val durum = calistir { bozuk.availability() }
        assertIs<PhotoStoreAvailability.Unavailable>(durum)
        assertTrue(sifreliDosya(record).exists(), "fotoğraf silinmez")

        // Şifreli fotoğraf yoksa bozuk dizin kayıp sayılmaz; temiz dizinle devam edilir.
        sifreliDosya(record).delete()
        val temiz = PhotoStore(context, anahtar, json, saat)
        assertIs<PhotoStoreAvailability.Available>(calistir { temiz.availability() })
        assertTrue(calistir { temiz.list() }.isEmpty())
        assertTrue(dizinDosyasi.isFile)
    }

    @Test
    fun `dizinde olmayan yarim kalmis dosyalar acilista temizlenir`() {
        val record = kaydet(jpegUret(300, 200))
        val yarim = File(fotografDizini, "00000000-0000-4000-8000-000000000000.enc").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val gecici = File(fotografDizini, record.id + ".enc.tmp").apply { writeBytes(byteArrayOf(1)) }

        val yeniDepo = PhotoStore(context, anahtar, json, saat)
        assertEquals(listOf(record), calistir { yeniDepo.list() })

        assertFalse(yarim.exists())
        assertFalse(gecici.exists())
        assertTrue(sifreliDosya(record).exists())
    }

    @Test
    fun `ornekleme orani en uzun kenari sinirin altina dusurmeyen en buyuk ikinin kuvvetidir`() {
        assertEquals(1, PhotoStore.sampleSize(3000, 1600))
        assertEquals(2, PhotoStore.sampleSize(3200, 1600))
        assertEquals(2, PhotoStore.sampleSize(4000, 1600))
        assertEquals(4, PhotoStore.sampleSize(6400, 1600))
        assertEquals(8, PhotoStore.sampleSize(1600, 200))
        assertEquals(1, PhotoStore.sampleSize(100, 1600))
        assertEquals("ayla-cilt-2026-10-12-abcdef12.jpg", PhotoStore.suggestedFileName(ornekKayit("abcdef12-0000-4000-8000-000000000000")))
        assertEquals("ayla-sac-2026-10-12-abcdef12.jpg", PhotoStore.suggestedFileName(ornekKayit("abcdef12-0000-4000-8000-000000000000", PhotoKind.HAIR)))
    }

    private fun ornekKayit(id: String, kind: PhotoKind = PhotoKind.SKIN) =
        PhotoRecord(id, kind, null, bugun, saat.instant(), 10, 100, 100)
}
