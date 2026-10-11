package com.ayla.pcos.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Sarmalı parola var ama açılamıyor (Keystore anahtarı kaybolmuş ya da geçersiz). */
class DatabaseKeyUnavailableException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

/**
 * Veritabanı parolasını üretir ve Android Keystore'daki bir AES-GCM anahtarıyla sarmalayarak saklar.
 *
 * - Parola 32 rastgele bayttır; hiçbir zaman düz metin olarak diske yazılmaz.
 * - Keystore anahtarı cihazdan dışarı çıkamaz; amaç başına tek anahtar kullanılır
 *   (Android 17'de uygulama başına anahtar sınırı vardır).
 * - Kullanıcı kimlik doğrulaması gerektirmez; böylece hatırlatma alıcıları ekran kilitliyken de
 *   veritabanını açabilir. Uygulama kilidi ayrı bir katmandır (AppLock).
 * - **Mevcut sarmalı parola açılamazsa asla sessizce yenisi üretilmez**: aksi hâlde şifreli dosya
 *   sonsuza dek okunamaz hâle gelir ve uygulama her açılışta çöker. Bunun yerine
 *   [DatabaseKeyUnavailableException] fırlatılır; karar (karantina + yeni başlangıç + kullanıcıya
 *   bildirim) çağırana bırakılır.
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private val _resetNoticePending = MutableStateFlow(false)

    /** Önceki veriler açılamadığı için yeni başlangıç yapıldı ve kullanıcı henüz bilgilendirilmedi. */
    val resetNoticePending: StateFlow<Boolean> get() = _resetNoticePending

    init {
        _resetNoticePending.value = prefs.getBoolean(KEY_RESET_NOTICE, false)
    }

    /**
     * Veritabanı parolasını döndürür; ilk kurulumda üretir.
     *
     * @param databaseExists Şifreli dosya diskte var mı. Varsa ve sarmalı parola yoksa (ya da
     *   açılamıyorsa) dosya bu anahtarla açılamaz; istisna fırlatılır.
     */
    @Synchronized
    fun passphrase(databaseExists: Boolean): ByteArray {
        val stored = prefs.getString(KEY_WRAPPED, null)
        if (stored != null) {
            return try {
                unwrap(stored)
            } catch (e: Exception) {
                throw DatabaseKeyUnavailableException("Sarmalı veritabanı parolası açılamadı", e)
            }
        }
        if (databaseExists) {
            throw DatabaseKeyUnavailableException("Veritabanı dosyası var ama sarmalı parola yok")
        }
        return createFresh()
    }

    /**
     * Anahtarı ve sarmalı parolayı silip yenisini üretir. Yalnızca eski dosya karantinaya alındıktan
     * sonra çağrılır; kullanıcıya gösterilecek "yeni başlangıç" bildirimi işaretlenir.
     */
    @Synchronized
    fun rotateAfterLoss(): ByteArray {
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        }
        prefs.edit {
            remove(KEY_WRAPPED)
            putBoolean(KEY_RESET_NOTICE, true)
        }
        _resetNoticePending.value = true
        return createFresh()
    }

    fun acknowledgeResetNotice() {
        prefs.edit { remove(KEY_RESET_NOTICE) }
        _resetNoticePending.value = false
    }

    private fun createFresh(): ByteArray {
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit { putString(KEY_WRAPPED, wrap(fresh)) }
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
        require(bytes.size > IV_LENGTH) { "Sarmalı parola bozuk" }
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
        const val KEY_RESET_NOTICE = "db_reset_notice_pending"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
