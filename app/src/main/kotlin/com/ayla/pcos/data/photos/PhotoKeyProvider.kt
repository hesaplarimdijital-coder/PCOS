package com.ayla.pcos.data.photos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fotoğraf deposu açılamıyor: Keystore anahtarı kaybolmuş, sarmalı anahtar yok ya da dizin bozuk.
 * Şifreli fotoğraflar bu durumda kurtarılamaz; karar (kullanıcıya bildirip [PhotoStore.deleteAll] ile
 * temiz başlangıç) çağırana bırakılır. Asla sessizce yeni anahtar üretilmez.
 */
class PhotoStoreUnavailableException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/**
 * Fotoğraf dosyalarını şifreleyen AES anahtarının (DEK) kaynağı. Üretimde [KeystorePhotoKeyProvider];
 * Robolectric'te Android Keystore olmadığı için testler yazılımsal bir anahtar verir.
 */
interface PhotoKeyProvider {
    /**
     * 256 bitlik AES dosya anahtarı; ilk kullanımda üretilir.
     *
     * @param photosExist Diskte şifreli fotoğraf dosyası (`photos/<id>.enc`) var mı. Varsa ve mevcut anahtar
     *   açılamıyorsa dosyalar bir daha okunamaz demektir; sessizce yenisi üretilmez,
     *   [PhotoStoreUnavailableException] fırlatılır. Yoksa kaybedilecek bir şey de yoktur: temiz anahtarla başlanır.
     */
    fun key(photosExist: Boolean): SecretKey

    /**
     * Anahtarı ve sarmalını siler; sonraki [key] çağrısı yenisini üretir. Yalnızca tüm şifreli dosyalar
     * silindikten sonra çağrılır (tam silme ya da anahtar kaybı sonrası yeni başlangıç).
     */
    fun reset()
}

/**
 * Fotoğraf anahtarını [DatabaseKeyProvider][com.ayla.pcos.security.DatabaseKeyProvider] kalıbıyla yönetir:
 * 32 rastgele bayt (DEK), Android Keystore'daki amaç başına tek AES-GCM anahtarıyla (`ayla.photo.wrap.v1`)
 * sarılıp ayrı bir SharedPreferences dosyasında saklanır. DEK hiçbir zaman düz metin olarak diske yazılmaz,
 * günlüğe düşmez. Veritabanı anahtarına ve onun tercih dosyasına dokunulmaz.
 *
 * DEK'in yazılımsal (Keystore dışı) bir anahtar olmasının nedeni akış şifrelemedir: dosya başına rastgele IV
 * ve ek doğrulanmış veri (AAD) ile `CipherOutputStream` kullanılır; Keystore anahtarları açık IV'ye izin
 * vermez. Keystore burada yalnızca sarmal görevi görür.
 */
@Singleton
class KeystorePhotoKeyProvider @Inject constructor(@ApplicationContext private val context: Context) : PhotoKeyProvider {

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    @Synchronized
    override fun key(photosExist: Boolean): SecretKey {
        val stored = prefs.getString(KEY_WRAPPED, null)
        if (stored != null) {
            try {
                return SecretKeySpec(unwrap(stored), KeyProperties.KEY_ALGORITHM_AES)
            } catch (e: Exception) {
                if (photosExist) throw PhotoStoreUnavailableException("Sarmalı fotoğraf anahtarı açılamadı", e)
                // Kaybedilecek fotoğraf yok: bozuk sarmal atılır, temiz başlanır.
                reset()
            }
        } else if (photosExist) {
            throw PhotoStoreUnavailableException("Şifreli fotoğraf var ama sarmalı anahtar yok")
        }
        return createFresh()
    }

    @Synchronized
    override fun reset() {
        runCatching { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS) }
        prefs.edit(commit = true) { remove(KEY_WRAPPED) }
    }

    private fun createFresh(): SecretKey {
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // Eşzamanlı yazım: sarmal diske inmeden şifreli dosya yazılmasın (süreç ölürse dosyalar anahtarsız kalır).
        prefs.edit(commit = true) { putString(KEY_WRAPPED, wrap(fresh)) }
        return SecretKeySpec(fresh, KeyProperties.KEY_ALGORITHM_AES)
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun wrap(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    private fun unwrap(stored: String): ByteArray {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        require(bytes.size > IV_LENGTH) { "Sarmalı anahtar bozuk" }
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val encrypted = bytes.copyOfRange(IV_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /** Amaç başına tek sarmal anahtar; veritabanınınkinden (`ayla.db.wrap.v1`) ayrıdır. */
        const val ALIAS = "ayla.photo.wrap.v1"

        /** Veritabanı anahtarının dosyasından (`ayla_keys`) ayrı tercih dosyası. */
        const val PREFS = "ayla_photo_keys"
        const val KEY_WRAPPED = "photo_dek_wrapped"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
