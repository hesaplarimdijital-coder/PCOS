package com.ayla.pcos.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Veritabanı parolasını üretir ve Android Keystore'daki bir AES-GCM anahtarıyla sarmalayarak saklar.
 *
 * - Parola 32 rastgele bayttır; hiçbir zaman düz metin olarak diske yazılmaz.
 * - Keystore anahtarı cihazdan dışarı çıkamaz; amaç başına tek anahtar kullanılır
 *   (Android 17'de uygulama başına anahtar sınırı vardır).
 * - Kullanıcı kimlik doğrulaması gerektirmez; böylece hatırlatma alıcıları ekran kilitliyken de
 *   veritabanını açabilir. Uygulama kilidi ayrı bir katmandır (AppLock).
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** Veritabanı parolasını döndürür; ilk çağrıda üretir. */
    @Synchronized
    fun passphrase(): ByteArray {
        prefs.getString(KEY_WRAPPED, null)?.let { stored ->
            runCatching { unwrap(stored) }.getOrNull()?.let { return it }
        }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_WRAPPED, wrap(fresh)).apply()
        return fresh
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
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val encrypted = bytes.copyOfRange(IV_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "ayla.db.wrap.v1"
        const val PREFS = "ayla_keys"
        const val KEY_WRAPPED = "db_passphrase_wrapped"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
