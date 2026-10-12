package com.ayla.pcos.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.ayla.pcos.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

/** Widget görünümleri (#14): RemoteViews gerçekten açılır ve doğru metinleri, görünürlüğü taşır. */
@RunWith(RobolectricTestRunner::class)
class WidgetGorunumuTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    private fun ac(state: WidgetState, wide: Boolean): View =
        WidgetGorunumu.views(context, state, wide).apply(context, FrameLayout(context))

    private fun View.metin(id: Int): String = findViewById<TextView>(id).text.toString()

    private fun View.gorunur(id: Int): Boolean = findViewById<View>(id).visibility == View.VISIBLE

    private val hazir = WidgetState.Ready(
        headline = WidgetHeadline.CycleDay(23),
        nextDose = NextDose(1, "Inofolic Combi", LocalDate.of(2026, 10, 12), LocalTime.of(21, 0), overdue = false),
        hasDosesToday = true,
        waterCups = 5,
        waterGoal = 8,
    )

    @Test
    fun `dar duzende gun, siradaki doz ve Aldim dugmesi gorunur, su gizlidir`() {
        val v = ac(hazir, wide = false)
        assertEquals("DÖNGÜ", v.metin(R.id.widget_ust_etiket))
        assertEquals("23. gün", v.metin(R.id.widget_baslik))
        assertEquals("21:00 · Inofolic Combi", v.metin(R.id.widget_doz))
        assertEquals(true, v.gorunur(R.id.widget_aldim))
        assertEquals("21:00 Inofolic Combi dozunu aldım olarak işaretle", v.findViewById<View>(R.id.widget_aldim).contentDescription?.toString())
        assertEquals(false, v.gorunur(R.id.widget_su))
        assertEquals(false, v.gorunur(R.id.widget_doz_etiket))
    }

    @Test
    fun `genis duzende su ve doz etiketi de gorunur, bekleyen doz belirtilir`() {
        val v = ac(hazir.copy(nextDose = hazir.nextDose!!.copy(time = LocalTime.of(9, 0), overdue = true)), wide = true)
        assertEquals("Su 5/8 bardak", v.metin(R.id.widget_su))
        assertEquals("BEKLEYEN DOZ", v.metin(R.id.widget_doz_etiket))
        assertEquals("09:00 · Inofolic Combi bekliyor", v.metin(R.id.widget_doz))
    }

    @Test
    fun `dozlar tamamsa dugme gizlenir, hap gununde plasebo etiketi gosterilir`() {
        val v = ac(hazir.copy(headline = WidgetHeadline.PillDay(26, placebo = true), nextDose = null), wide = false)
        assertEquals("HAP · PLASEBO", v.metin(R.id.widget_ust_etiket))
        assertEquals("26. hap", v.metin(R.id.widget_baslik))
        assertEquals("Bugünkü dozlar tamam", v.metin(R.id.widget_doz))
        assertEquals(false, v.gorunur(R.id.widget_aldim))
    }

    @Test
    fun `notr icerikte saglik verisi yoktur`() {
        listOf(false, true).forEach { wide ->
            val v = ac(WidgetState.Neutral, wide)
            assertEquals("AYLA", v.metin(R.id.widget_ust_etiket))
            assertEquals("Bugün", v.metin(R.id.widget_baslik))
            assertEquals("Görmek için dokun", v.metin(R.id.widget_doz))
            assertEquals(false, v.gorunur(R.id.widget_aldim))
            assertEquals(false, v.gorunur(R.id.widget_su))
        }
    }
}
