package com.ayla.pcos.data.photos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/** Fotoğrafın neyi izlediği. */
enum class PhotoKind { SKIN, HAIR }

/** Şifreli dizindeki (`photos/index.json`) bir fotoğrafın meta verisi. Görüntünün kendisi `photos/<id>.enc`. */
@Serializable
data class PhotoRecord(
    val id: String,
    val kind: PhotoKind,
    /** Bölge (ör. çene, saç çizgisi); kullanıcı metni, boş olabilir. */
    val area: String? = null,
    /** Fotoğrafın ait olduğu gün (kullanıcı seçer). */
    @Serializable(with = LocalDateIso::class) val date: LocalDate,
    @Serializable(with = InstantIso::class) val createdAt: Instant,
    /** Şifreli dosyanın boyutu (IV ve etiket dâhil). */
    val byteSize: Long,
    /** Saklanan (küçültülmüş, yönü düzeltilmiş) görüntünün piksel boyutu; önizleme örnekleme oranı buradan hesaplanır. */
    val width: Int,
    val height: Int,
)

/** Dizin dosyasının biçimi; sürümlüdür, alan eklenirken varsayılan verilir ki eski dizinler okunsun. */
@Serializable
internal data class PhotoIndex(val schemaVersion: Int = PhotoStore.INDEX_SCHEMA_VERSION, val photos: List<PhotoRecord> = emptyList())

/** Deponun açılabilirliği; [Unavailable] ise fotoğraflar kurtarılamaz, karar kullanıcıya bırakılır. */
sealed interface PhotoStoreAvailability {
    data object Available : PhotoStoreAvailability

    data class Unavailable(val cause: Throwable) : PhotoStoreAvailability
}

/**
 * Kamera uygulamasının yazacağı geçici **düz** dosya (`cacheDir/camera/<id>.jpg`). [uri] `ACTION_IMAGE_CAPTURE`
 * niyetine `EXTRA_OUTPUT` olarak verilir; sistem, niyetteki çıktı adresine yazma iznini kamera uygulamasına
 * kendisi verir. Çekim bitince [PhotoStore.saveCapture] ya da [PhotoStore.discardCapture] dosyayı mutlaka siler.
 * Süreç kamera açıkken ölürse [id] saklanıp [PhotoStore.capture] ile yeniden kurulur.
 */
class PendingCapture internal constructor(val id: String, val file: File, val uri: Uri)

/** Toplu dışa aktarma sonucu. */
data class PhotoExportResult(val exported: Int, val failed: Int)

/**
 * Cilt/saç ilerleme fotoğraflarının şifreli, cihaz içi deposu (ADR-0010, #25 / US-093).
 *
 * - Her fotoğraf `filesDir/photos/<uuid>.enc` dosyasıdır: AES-GCM, dosya başına rastgele IV, ad AAD olarak
 *   bağlı ([PhotoCipher]). İçerik, en uzun kenarı [MAX_EDGE_PX] pikseli aşmayan, yeniden kodlanmış JPEG'dir;
 *   yeniden kodlama kaynak dosyadaki EXIF'i (konum, cihaz, zaman) atar, yalnızca yön düzeltmesi uygulanır.
 * - Meta veriler Room'da değil, aynı anahtarla şifreli `photos/index.json` dizinindedir. Dizin önce geçici
 *   dosyaya yazılıp yeniden adlandırılır (atomik); yazma sırası "önce dosya, sonra dizin" olduğundan yarım
 *   kalan kayıt dizine girmez ve bir sonraki açılışta temizlenir.
 * - Anahtar [PhotoKeyProvider]'dan her işlemde alınır, bellekte tutulmaz. Anahtar açılamıyorsa işlemler
 *   [PhotoStoreUnavailableException] fırlatır; [availability] bunu [PhotoStoreAvailability.Unavailable] olarak
 *   döndürür. Sessizce yeni anahtar üretilmez; temiz başlangıç yalnızca [deleteAll] ile.
 * - Galeriye ve MediaStore'a hiçbir şey yazılmaz. `filesDir` ve `cacheDir` zaten yedekleme dışıdır
 *   (`data_extraction_rules.xml`, `allowBackup=false`).
 * - Tüm disk işlemleri IO dağıtıcısında; dizin değişiklikleri tek [Mutex] altında.
 */
@Singleton
class PhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyProvider: PhotoKeyProvider,
    private val json: Json,
    private val clock: Clock,
) {
    private val photosDir: File get() = File(context.filesDir, PHOTOS_DIR)
    private val cameraDir: File get() = File(context.cacheDir, CAMERA_DIR)

    private val mutex = Mutex()

    @Volatile
    private var loaded = false

    private val _records = MutableStateFlow<List<PhotoRecord>>(emptyList())

    /**
     * Bellekteki dizin: tarihe göre yeniden eskiye. İlk başarılı yükleme ([availability] ya da herhangi bir
     * işlem) öncesinde boştur.
     */
    val records: StateFlow<List<PhotoRecord>> get() = _records

    /** Anahtarı açar ve dizini yükler. Ekran açılırken bir kez sorulur; [Unavailable][PhotoStoreAvailability.Unavailable] ise kullanıcıya seçenek sunulur. */
    suspend fun availability(): PhotoStoreAvailability = withContext(Dispatchers.IO) {
        try {
            mutex.withLock { openLocked() }
            PhotoStoreAvailability.Available
        } catch (e: PhotoStoreUnavailableException) {
            PhotoStoreAvailability.Unavailable(e)
        }
    }

    /** Kayıtlar (isteğe bağlı türe göre), tarihe göre yeniden eskiye. */
    suspend fun list(kind: PhotoKind? = null): List<PhotoRecord> = withContext(Dispatchers.IO) {
        mutex.withLock { openLocked() }
        _records.value.filter { kind == null || it.kind == kind }
    }

    /**
     * Akıştaki görüntüyü küçültüp şifreli olarak kaydeder. Akış kapatılmaz; çağıran kapatır. Görüntü
     * çözülemezse [IllegalArgumentException].
     */
    suspend fun save(source: InputStream, kind: PhotoKind, area: String?, date: LocalDate): PhotoRecord = withContext(Dispatchers.IO) {
        val staging = newStagingFile()
        try {
            FileOutputStream(staging).use { source.copyTo(it) }
            saveFile(staging, kind, area, date)
        } finally {
            staging.delete()
        }
    }

    /** Galeri/fotoğraf seçiciden gelen içerik adresini kaydeder (`ContentResolver.openInputStream`). */
    suspend fun save(uri: Uri, kind: PhotoKind, area: String?, date: LocalDate): PhotoRecord = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Kaynak açılamadı")
        input.use { save(it, kind, area, date) }
    }

    /** Kamera için yeni geçici hedef üretir. */
    fun cameraTarget(): PendingCapture = capture(UUID.randomUUID().toString())

    /** Daha önce üretilen hedefi kimliğinden yeniden kurar (süreç kamera açıkken yeniden başlamış olabilir). */
    fun capture(id: String): PendingCapture {
        require(ID_PATTERN.matches(id)) { "Geçersiz fotoğraf kimliği" }
        val file = File(cameraDir.apply { mkdirs() }, "$id.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        return PendingCapture(id, file, uri)
    }

    /** Kameranın yazdığı geçici dosyayı şifreli depoya alır ve geçici dosyayı **her durumda** siler. */
    suspend fun saveCapture(capture: PendingCapture, kind: PhotoKind, area: String?, date: LocalDate): PhotoRecord = withContext(Dispatchers.IO) {
        try {
            if (!capture.file.isFile || capture.file.length() == 0L) throw IOException("Kamera dosyası yok")
            saveFile(capture.file, kind, area, date)
        } finally {
            capture.file.delete()
        }
    }

    /** Çekim iptal edildi ya da başarısız oldu: geçici düz dosya silinir. */
    suspend fun discardCapture(capture: PendingCapture) {
        withContext(Dispatchers.IO) { capture.file.delete() }
    }

    /** Fotoğrafı çözerek tam boyutta (en uzun kenar ≤ [MAX_EDGE_PX]) döndürür; kayıt yoksa ya da dosya açılamıyorsa null. */
    suspend fun load(id: String): Bitmap? = withContext(Dispatchers.IO) {
        val (key, record) = find(id)
        if (record == null) null else decodeEncrypted(key, record, BitmapFactory.Options())
    }

    /** En uzun kenarı [maxPx] pikseli aşmayan önizleme; örnekleme oranı dizindeki boyuttan hesaplanır, tam çözme yapılmaz. */
    suspend fun thumbnail(id: String, maxPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        require(maxPx > 0) { "maxPx pozitif olmalı" }
        val (key, record) = find(id)
        if (record == null) {
            null
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(maxOf(record.width, record.height), maxPx) }
            decodeEncrypted(key, record, options)?.let { normalize(it, maxPx, ExifInterface.ORIENTATION_NORMAL) }
        }
    }

    /** Tek fotoğrafı siler: önce dizinden, sonra diskten. Yoksa sessizce biter. */
    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = openLocked()
            val current = _records.value
            if (current.any { it.id == id }) {
                val updated = current.filterNot { it.id == id }
                writeIndex(key, updated)
                _records.value = updated
                fileOf(id).delete()
            }
        }
    }

    /**
     * Tam silme: şifreli dosyalar, dizin, geçici kamera dosyaları ve anahtar (sarmalıyla birlikte) gider;
     * hiçbir iz kalmaz. Anahtar açılamıyor olsa da çalışır; "yeni başlangıç" da budur.
     */
    suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            photosDir.deleteRecursively()
            cameraDir.deleteRecursively()
            keyProvider.reset()
            _records.value = emptyList()
            loaded = false
        }
    }

    /** Fotoğrafı düz JPEG olarak kullanıcının seçtiği adrese (SAF) yazar. Kayıt yoksa false. */
    suspend fun exportTo(uri: Uri, id: String): Boolean = withContext(Dispatchers.IO) {
        val (key, record) = find(id)
        if (record == null) {
            false
        } else {
            writeDecrypted(key, record, uri)
            true
        }
    }

    /**
     * Tüm fotoğrafları kullanıcının seçtiği klasöre (`ACTION_OPEN_DOCUMENT_TREE` ağaç adresi) düz JPEG olarak
     * yazar; her dosya `DocumentsContract.createDocument` ile açılır. Yazılamayan belge geri silinir.
     */
    suspend fun exportAllTo(treeUri: Uri): PhotoExportResult = withContext(Dispatchers.IO) {
        val (key, records) = mutex.withLock { openLocked() to _records.value }
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        var exported = 0
        var failed = 0
        for (record in records) {
            val target = try {
                DocumentsContract.createDocument(resolver, parent, MIME_JPEG, suggestedFileName(record))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (target == null) {
                failed++
                continue
            }
            try {
                writeDecrypted(key, record, target)
                exported++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed++
                runCatching { DocumentsContract.deleteDocument(resolver, target) }
            }
        }
        PhotoExportResult(exported, failed)
    }

    /**
     * Artık geçici dosyaları (kamera hedefleri, kaynak kopyaları) siler. Uygulama açılışında çağrılır.
     * Yeni dosyalar korunur: süreç kamera uygulaması öndeyken öldürülmüş ve çekim henüz alınmamış olabilir.
     */
    suspend fun cleanupTemp(olderThan: Duration = TEMP_MAX_AGE) {
        withContext(Dispatchers.IO) {
            val threshold = System.currentTimeMillis() - olderThan.toMillis()
            cameraDir.listFiles()?.forEach { if (it.lastModified() <= threshold) it.delete() }
        }
    }

    // --- iç işler ---

    /**
     * Kilit altında çağrılır: anahtarı açar, dizin yüklü değilse yükler. Döndürülen anahtar aynı kilitli
     * bölümde (ya da hemen ardından tek okuma için) kullanılır; alanda saklanmaz.
     */
    private fun openLocked(): SecretKey {
        val dir = photosDir
        val encryptedFilesExist = hasEncryptedFiles(dir)
        val key = keyProvider.key(photosExist = encryptedFilesExist)
        if (loaded) return key
        val indexFile = File(dir, INDEX_NAME)
        val records: List<PhotoRecord> = if (indexFile.isFile) {
            try {
                readIndex(key, indexFile)
            } catch (e: Exception) {
                if (encryptedFilesExist) throw PhotoStoreUnavailableException("Fotoğraf dizini açılamadı", e)
                // Kaybedilecek fotoğraf yok: bozuk ya da eski anahtarla yazılmış dizin yenilenir.
                emptyList<PhotoRecord>().also { writeIndex(key, it) }
            }
        } else if (encryptedFilesExist) {
            throw PhotoStoreUnavailableException("Fotoğraf dizini yok ama şifreli dosyalar var")
        } else {
            emptyList<PhotoRecord>().also { writeIndex(key, it) }
        }
        // Dosyası kaybolmuş kayıt gösterilmez; dizinde olmayan dosyalar (yarım kalmış kayıt, eski geçici) erişilemez, temizlenir.
        val present = records.filter { fileOf(it.id).isFile }
        val known = present.mapTo(HashSet()) { fileOf(it.id).name }
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name != INDEX_NAME && f.name !in known) f.delete()
        }
        _records.value = present.sortedWith(ORDER)
        loaded = true
        return key
    }

    private fun readIndex(key: SecretKey, indexFile: File): List<PhotoRecord> {
        val text = PhotoCipher.decrypt(key, INDEX_NAME, FileInputStream(indexFile)).use { it.readBytes() }.decodeToString()
        return json.decodeFromString<PhotoIndex>(text).photos
    }

    /** Dizini şifreleyip geçici dosyaya yazar, diske eşitler ve yerine koyar (atomik). */
    private fun writeIndex(key: SecretKey, records: List<PhotoRecord>) {
        val dir = photosDir.apply { mkdirs() }
        val buffer = ByteArrayOutputStream()
        PhotoCipher.encrypt(key, INDEX_NAME, buffer).use {
            it.write(json.encodeToString(PhotoIndex(photos = records)).encodeToByteArray())
        }
        val tmp = File(dir, INDEX_NAME + TMP_SUFFIX)
        FileOutputStream(tmp).use { out ->
            out.write(buffer.toByteArray())
            out.fd.sync()
        }
        if (!tmp.renameTo(File(dir, INDEX_NAME))) throw IOException("Fotoğraf dizini yazılamadı")
    }

    private suspend fun find(id: String): Pair<SecretKey, PhotoRecord?> = mutex.withLock {
        val key = openLocked()
        key to _records.value.firstOrNull { it.id == id }
    }

    /** Düz kaynak dosyayı küçültür, yönünü düzeltir, şifreleyip yazar ve dizine ekler. */
    private suspend fun saveFile(source: File, kind: PhotoKind, area: String?, date: LocalDate): PhotoRecord {
        val bitmap = decodeScaled(source, MAX_EDGE_PX)
        try {
            return mutex.withLock {
                val key = openLocked()
                val id = UUID.randomUUID().toString()
                val dir = photosDir.apply { mkdirs() }
                val target = fileOf(id)
                val tmp = File(dir, target.name + TMP_SUFFIX)
                try {
                    FileOutputStream(tmp).use { out ->
                        PhotoCipher.encrypt(key, target.name, out).use { encrypted ->
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, encrypted)) throw IOException("JPEG yazılamadı")
                        }
                    }
                    if (!tmp.renameTo(target)) throw IOException("Fotoğraf dosyası yazılamadı")
                    val record = PhotoRecord(
                        id = id,
                        kind = kind,
                        area = area?.trim()?.takeIf { it.isNotEmpty() },
                        date = date,
                        createdAt = Instant.now(clock),
                        byteSize = target.length(),
                        width = bitmap.width,
                        height = bitmap.height,
                    )
                    val updated = (_records.value + record).sortedWith(ORDER)
                    writeIndex(key, updated)
                    _records.value = updated
                    record
                } catch (e: Exception) {
                    tmp.delete()
                    target.delete()
                    throw e
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Kaynağı en uzun kenarı [maxEdge] pikseli aşmayacak biçimde çözer: önce sınırlar okunur, `inSampleSize` ile
     * bellek sınırlı tutulur, sonra kalan ölçek ve EXIF yönü tek bir dönüşümle uygulanır. Yön dışındaki hiçbir
     * EXIF alanı okunmaz; yeniden kodlanan JPEG'e meta veri yazılmaz.
     */
    @SuppressLint("ExifInterface") // Yeni bağımlılık eklenmez; yalnızca yön etiketi okunur.
    private fun decodeScaled(file: File, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("Görüntü çözülemedi")
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(maxOf(bounds.outWidth, bounds.outHeight), maxEdge) }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: throw IllegalArgumentException("Görüntü çözülemedi")
        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        return normalize(decoded, maxEdge, orientation)
    }

    /** Kalan ölçeği ve EXIF yönünü uygular; dönüşüm gerekmiyorsa kaynağı aynen döndürür. */
    private fun normalize(source: Bitmap, maxEdge: Int, orientation: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        val matrix = Matrix()
        if (longest > maxEdge) {
            val scale = maxEdge.toFloat() / longest
            matrix.postScale(scale, scale)
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        if (matrix.isIdentity) return source
        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result !== source) source.recycle()
        return result
    }

    private fun decodeEncrypted(key: SecretKey, record: PhotoRecord, options: BitmapFactory.Options): Bitmap? {
        val file = fileOf(record.id)
        if (!file.isFile) return null
        return try {
            PhotoCipher.decrypt(key, file.name, FileInputStream(file)).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: IOException) {
            null
        }
    }

    private fun writeDecrypted(key: SecretKey, record: PhotoRecord, target: Uri) {
        val file = fileOf(record.id)
        val out = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Hedef açılamadı")
        out.use { sink ->
            PhotoCipher.decrypt(key, file.name, FileInputStream(file)).use { it.copyTo(sink) }
        }
    }

    private fun fileOf(id: String): File = File(photosDir, id + ENC_SUFFIX)

    private fun hasEncryptedFiles(dir: File): Boolean = dir.listFiles()?.any { it.isFile && it.name.endsWith(ENC_SUFFIX) } == true

    private fun newStagingFile(): File = File(cameraDir.apply { mkdirs() }, UUID.randomUUID().toString() + STAGING_SUFFIX)

    companion object {
        /** Dizin biçimi sürümü; alan eklenirken artırılır, eski dizin varsayılanlarla okunur. */
        const val INDEX_SCHEMA_VERSION = 1

        /** Saklanan görüntünün en uzun kenarı. Karşılaştırma görünümü için yeter; dosya ~200–500 KB kalır. */
        const val MAX_EDGE_PX = 1600

        /** Geçici kamera dosyaları bu süreden eskiyse açılışta silinir. */
        val TEMP_MAX_AGE: Duration = Duration.ofHours(1)

        /** Dışa aktarılan dosya adı: `ayla-cilt-2026-10-12-1a2b3c4d.jpg`. Dosya adıdır, arayüz metni değil. */
        fun suggestedFileName(record: PhotoRecord): String {
            val slug = when (record.kind) {
                PhotoKind.SKIN -> "cilt"
                PhotoKind.HAIR -> "sac"
            }
            return "ayla-$slug-${record.date}-${record.id.take(8)}.jpg"
        }

        /** En uzun kenar [maxEdge]'in altına düşmeden önceki en büyük 2'nin kuvveti (`inSampleSize`). */
        internal fun sampleSize(longest: Int, maxEdge: Int): Int {
            var sample = 1
            while (longest / (sample * 2) >= maxEdge) sample *= 2
            return sample
        }

        private const val JPEG_QUALITY = 85
        private const val PHOTOS_DIR = "photos"
        private const val CAMERA_DIR = "camera"
        private const val INDEX_NAME = "index.json"
        private const val ENC_SUFFIX = ".enc"
        private const val TMP_SUFFIX = ".tmp"
        private const val STAGING_SUFFIX = ".src"

        /** Manifestteki FileProvider yetkisi: `<applicationId>.photos`. */
        private const val AUTHORITY_SUFFIX = ".photos"
        private const val MIME_JPEG = "image/jpeg"
        private val ID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val ORDER = compareByDescending<PhotoRecord> { it.date }.thenByDescending { it.createdAt }
    }
}

/** ISO-8601 gün (`2026-10-12`). */
internal object LocalDateIso : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

/** ISO-8601 an (`2026-10-12T07:00:00Z`). */
internal object InstantIso : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
