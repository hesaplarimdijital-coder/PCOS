package com.ayla.pcos.veri

import android.graphics.Bitmap
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.photos.PhotoKind
import com.ayla.pcos.data.photos.PhotoStore
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.skin.AvoidItem
import com.ayla.pcos.data.skin.RoutineStep
import com.ayla.pcos.di.AppModule
import com.ayla.pcos.ui.skin.PhotoStatus
import com.ayla.pcos.ui.skin.SkinViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Test
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cilt ve saç ekranı (#25): profil → rutin, adım işaretleme, fotoğraf rızası ve geri alma. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CiltEkraniTesti : VeriTestOrtami() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val anahtar = YazilimsalFotografAnahtari()
    private val depo by lazy { PhotoStore(context, anahtar, AppModule.provideJson(), Clock.systemDefaultZone()) }

    @After
    fun kapsamiKapat() = appScope.cancel()

    private fun vm() = kaydet(SkinViewModel(skin, prefs, depo, ticker, appScope))

    private fun jpeg(): ByteArrayInputStream {
        val bmp = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFE0A090.toInt()) }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return ByteArrayInputStream(out.toByteArray())
    }

    @Test
    fun `profil kaydedilince rutin ona gore kurulur, gebelik plani tercihlerden gelir`() {
        val vm = vm()
        val ilk = bekle(vm.state) { it.loaded }
        assertNull(ilk.profile)
        assertEquals(listOf(RoutineStep.CLEANSER, RoutineStep.MOISTURIZER), ilk.plan.evening)

        vm.updateProfile { it.copy(skinType = SkinType.OILY, acne = AcneSelfReport.MILD) }
        val yagli = bekle(vm.state) { it.profile?.acne == AcneSelfReport.MILD }
        assertTrue(RoutineStep.SALICYLIC in yagli.plan.evening)
        assertTrue(yagli.plan.avoid.isEmpty())

        calistir { prefs.update { it.copy(goals = it.goals + Goal.TTC) } }
        val gebe = bekle(vm.state) { it.pregnancyPlan }
        assertFalse(RoutineStep.SALICYLIC in gebe.plan.evening)
        assertEquals(AvoidItem.KEPT, gebe.plan.avoid.last())
    }

    @Test
    fun `adim isaretlenir, geri alinir ve haftalik sayac guncellenir`() {
        val vm = vm()
        bekle(vm.state) { it.loaded }
        vm.toggleStep(RoutineSlot.MORNING, RoutineStep.CLEANSER.key)
        val isaretli = bekle(vm.state) { it.isDone(RoutineSlot.MORNING, RoutineStep.CLEANSER.key) }
        assertEquals(1, isaretli.weekMorning)
        assertEquals(0, isaretli.weekEvening)
        vm.toggleStep(RoutineSlot.MORNING, RoutineStep.CLEANSER.key)
        assertEquals(0, bekle(vm.state) { !it.isDone(RoutineSlot.MORNING, RoutineStep.CLEANSER.key) }.weekMorning)
    }

    @Test
    fun `riza yokken fotograf listesi bostur, riza geri alininca fotograflar silinir`() {
        calistir { depo.save(jpeg(), PhotoKind.SKIN, "çene", bugun) } // rıza öncesi kalmış bir kayıt (ör. eski sürüm)
        val vm = vm()
        val riza = bekle(vm.state) { it.loaded }
        assertFalse(riza.photoConsent)
        assertTrue(riza.photos.isEmpty(), "rıza yokken fotoğraflar gösterilmez")

        vm.giveConsent()
        val acik = bekle(vm.state) { it.photoConsent && it.photo.status == PhotoStatus.AVAILABLE && it.photos.isNotEmpty() }
        assertEquals("çene", acik.photos.single().area)

        vm.revokeConsent()
        val kapali = bekle(vm.state) { !it.photoConsent }
        assertTrue(kapali.photos.isEmpty())
        assertTrue(calistir { depo.list() }.isEmpty(), "fotoğraflar ve anahtar silindi")
        assertNull(calistir { prefs.current() }.photoConsentAt)
    }

    @Test
    fun `art arda iki profil degisikligi birbirini ezmez`() {
        val vm = vm()
        bekle(vm.state) { it.loaded }
        vm.updateProfile { it.copy(skinType = SkinType.OILY) }
        vm.updateProfile { it.copy(acne = AcneSelfReport.MILD) }
        val son = bekle(vm.state) { it.profile?.acne == AcneSelfReport.MILD && it.profile.skinType == SkinType.OILY }
        assertEquals(SkinType.OILY, calistir { skin.profile() }?.skinType)
        assertEquals(AcneSelfReport.MILD, son.profile?.acne)
    }

    @Test
    fun `kayit surerken riza geri alinmissa yazilan fotograf da silinir`() {
        val vm = vm()
        bekle(vm.state) { it.loaded }
        vm.giveConsent()
        bekle(vm.state) { it.photoConsent && it.photo.status == PhotoStatus.AVAILABLE }
        // Ayarlar'dan geri alma: bu ViewModel'in süren kaydından habersiz.
        calistir { prefs.update { it.copy(photoConsentAt = null) } }
        val hedef = vm.cameraTarget()
        hedef.file.writeBytes(jpeg().readBytes())
        vm.saveCapture(hedef, success = true, kind = PhotoKind.SKIN, area = null)
        // Kamera hedefi diskte olduğu için dosya var; kayıt bitip rıza denetimi silince hiçbir şey kalmaz.
        val sinir = System.currentTimeMillis() + 5_000
        while (calistir { depo.hasAnyFiles() } && System.currentTimeMillis() < sinir) Thread.sleep(20)
        assertFalse(calistir { depo.hasAnyFiles() }, "rıza yokken diskte fotoğraf kalmadı")
    }

    @Test
    fun `kamera uygulamasi yoksa uyari gosterilir ve bos hedef silinir`() {
        val vm = vm()
        bekle(vm.state) { it.loaded }
        val hedef = vm.cameraTarget()
        vm.cameraUnavailable(hedef)
        assertTrue(bekle(vm.state) { it.photo.cameraMissing }.photo.cameraMissing)
    }
}
