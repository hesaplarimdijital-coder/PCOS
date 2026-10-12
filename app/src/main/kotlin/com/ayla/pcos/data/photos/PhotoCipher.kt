package com.ayla.pcos.data.photos

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Fotoğraf dosyalarının ve dizinin ortak şifre biçimi (ADR-0010):
 *
 * ```
 * [12 bayt rastgele IV][AES/GCM/NoPadding şifreli içerik][128 bit doğrulama etiketi]
 * ```
 *
 * Dosyanın mantıksal adı (`<id>.enc` ya da `index.json`) ek doğrulanmış veridir (AAD): bir dosyanın
 * içeriği başka bir adın altına taşınırsa şifre çözme etiket hatasıyla başarısız olur. Akış şifreleme
 * yapılır; içerik belleğe toptan alınmaz. (GCM çözmede sağlayıcı etiketi doğrulamak için şifreli
 * içeriği tamponlar; dosyalar en uzun kenarı 1600 px olan JPEG'lerle sınırlı olduğu için bu kabul edilir.)
 */
internal object PhotoCipher {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128

    /** IV'yi [out]'a yazar ve şifreleyen akışı döndürür. Akış **kapatılmalıdır**; etiket kapanışta yazılır. */
    fun encrypt(key: SecretKey, name: String, out: OutputStream): OutputStream {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(name.toByteArray())
        out.write(iv)
        return CipherOutputStream(out, cipher)
    }

    /**
     * IV'yi [input]'tan okur ve çözen akışı döndürür. Etiket tutmuyorsa (yanlış anahtar, bozuk ya da yer
     * değiştirilmiş dosya) okuma sırasında [IOException] fırlatılır.
     */
    fun decrypt(key: SecretKey, name: String, input: InputStream): InputStream {
        val iv = ByteArray(IV_LENGTH)
        var read = 0
        while (read < IV_LENGTH) {
            val n = input.read(iv, read, IV_LENGTH - read)
            if (n < 0) throw IOException("Şifreli dosya çok kısa")
            read += n
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(name.toByteArray())
        return CipherInputStream(input, cipher)
    }
}
