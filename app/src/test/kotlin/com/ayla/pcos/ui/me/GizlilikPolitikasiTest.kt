package com.ayla.pcos.ui.me

import com.ayla.pcos.ui.components.MdBlok
import com.ayla.pcos.ui.components.kalinVurgulu
import com.ayla.pcos.ui.components.markdownBloklari
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GizlilikPolitikasiTest {

    @Test
    fun `uygulamadaki politika docs klasorundeki metinle birebir ayni`() {
        // Birim testleri app/ modül dizininde çalışır.
        val kaynak = File("../docs/gizlilik-politikasi.md").readText()
        val uygulama = File("src/main/res/raw/gizlilik_politikasi.md").readText()
        assertEquals(kaynak, uygulama, "docs/gizlilik-politikasi.md değişti; res/raw kopyasını güncelle")
    }

    @Test
    fun `politika saglik verisi, Health Connect ve haklar bolumlerini icerir`() {
        val bloklar = markdownBloklari(File("src/main/res/raw/gizlilik_politikasi.md").readText())
        val basliklar = bloklar.filterIsInstance<MdBlok.Baslik>().map { it.metin }
        assertTrue(basliklar.first().startsWith("Ayla Gizlilik Politikası"))
        listOf("6. Health Connect", "11. KVKK kapsamındaki hakların", "13. Tıbbi uyarı").forEach { beklenen ->
            assertTrue(beklenen in basliklar, "eksik bölüm: $beklenen")
        }
    }

    @Test
    fun `markdown basliklari, maddeleri ve paragraflari ayirir`() {
        val bloklar = markdownBloklari(
            """
            # Başlık
            İlk satır
            devamı

            - **Kalın** madde
            - ikinci madde

            ## Alt başlık
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                MdBlok.Baslik(1, "Başlık"),
                MdBlok.Paragraf("İlk satır devamı"),
                MdBlok.Madde("**Kalın** madde"),
                MdBlok.Madde("ikinci madde"),
                MdBlok.Baslik(2, "Alt başlık"),
            ),
            bloklar,
        )
    }

    @Test
    fun `kalin isaretleri yaziya donusur, eksik isaret oldugu gibi kalir`() {
        val metin = kalinVurgulu("**Ne:** adet tarihleri")
        assertEquals("Ne: adet tarihleri", metin.text)
        assertEquals(1, metin.spanStyles.size)
        assertEquals("tek ** işaret", kalinVurgulu("tek ** işaret").text)
    }
}
