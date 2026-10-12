package com.ayla.pcos.ui.skin

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.photos.PendingCapture
import com.ayla.pcos.data.photos.PhotoExportResult
import com.ayla.pcos.data.photos.PhotoKind
import com.ayla.pcos.data.photos.PhotoRecord
import com.ayla.pcos.data.photos.PhotoStore
import com.ayla.pcos.data.photos.PhotoStoreAvailability
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.SkinRepository
import com.ayla.pcos.data.skin.RoutinePlan
import com.ayla.pcos.data.skin.SkinRoutineInput
import com.ayla.pcos.data.skin.SkinRoutineRules
import com.ayla.pcos.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Fotoğraf deposunun ekrandaki durumu. */
enum class PhotoStatus { UNKNOWN, AVAILABLE, UNAVAILABLE }

data class PhotoUi(
    val status: PhotoStatus = PhotoStatus.UNKNOWN,
    val busy: Boolean = false,
    val error: Boolean = false,
    /** Cihazda fotoğraf çekecek uygulama yok (ör. iş profilinde kamera kapalı). */
    val cameraMissing: Boolean = false,
    /** "Tümünü klasöre kaydet" sürüyor ([busy] ile birlikte). */
    val exporting: Boolean = false,
    /** Son "tümünü klasöre kaydet" sonucu. */
    val exportResult: PhotoExportResult? = null,
)

/** Cilt ve saç ekranının durumu (#25). Rutin, profil öz-beyanından ve tercihlerdeki gebelik planından türer. */
data class SkinUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val profile: SkinProfileEntity? = null,
    val pregnancyPlan: Boolean = false,
    val plan: RoutinePlan = SkinRoutineRules.plan(SkinRoutineInput()),
    /** Bugün işaretlenen adım anahtarları. */
    val checks: Map<RoutineSlot, Set<String>> = emptyMap(),
    /** Bu hafta (Pazartesiden bugüne) en az bir adımı işaretlenen sabah ve akşam sayısı. */
    val weekMorning: Int = 0,
    val weekEvening: Int = 0,
    val photoConsent: Boolean = false,
    val photos: List<PhotoRecord> = emptyList(),
    val photo: PhotoUi = PhotoUi(),
) {
    fun isDone(slot: RoutineSlot, key: String): Boolean = key in checks[slot].orEmpty()
}

@HiltViewModel
class SkinViewModel @Inject constructor(
    private val skin: SkinRepository,
    private val prefs: UserPreferencesRepository,
    private val photoStore: PhotoStore,
    ticker: DateTicker,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val photoUi = MutableStateFlow(PhotoUi())

    /** Süren fotoğraf kaydı; rıza geri alınırken önce o bitirilir ki silmeden sonra dosya yazılmasın. */
    private var saveJob: Job? = null

    private val routine = ticker.today.flatMapLatest { today ->
        skin.observeRoutineRange(today.with(DayOfWeek.MONDAY), today)
    }

    val state: StateFlow<SkinUiState> = combine(
        combine(skin.observeProfile(), prefs.preferences, ::Pair),
        routine,
        ticker.today,
        photoStore.records,
        photoUi,
    ) { (profile, p), checks, today, photos, photo ->
        val pregnancyPlan = Goal.TTC in p.goals
        val input = SkinRoutineInput(
            skinType = profile?.skinType,
            acne = profile?.acne,
            darkSpots = profile?.darkSpots ?: false,
            acanthosis = profile?.acanthosis ?: false,
            unwantedHair = profile?.unwantedHair ?: false,
            hairThinning = profile?.hairThinning ?: false,
            pregnancyPlan = pregnancyPlan,
            pregnantOrBreastfeeding = profile?.pregnantOrBreastfeeding ?: false,
        )
        SkinUiState(
            loaded = true,
            today = today,
            profile = profile,
            pregnancyPlan = pregnancyPlan,
            plan = SkinRoutineRules.plan(input),
            checks = checks.filter { it.date == today }.associate { it.slot to it.doneSteps.toSet() },
            weekMorning = checks.count { it.slot == RoutineSlot.MORNING && it.doneSteps.isNotEmpty() },
            weekEvening = checks.count { it.slot == RoutineSlot.EVENING && it.doneSteps.isNotEmpty() },
            photoConsent = p.photoConsentAt != null,
            photos = if (p.photoConsentAt != null) photos else emptyList(),
            photo = photo,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkinUiState())

    init {
        viewModelScope.launch { if (prefs.current().photoConsentAt != null) checkPhotos() }
    }

    /** Profil alanı değişince hemen kaydedilir (form düğmesiz); yazma uygulama kapsamında (#27). */
    fun updateProfile(transform: (SkinProfileEntity) -> SkinProfileEntity) {
        appScope.launch { skin.updateProfile(transform) }
    }

    fun toggleStep(slot: RoutineSlot, key: String) {
        val today = state.value.today
        appScope.launch { skin.toggleStep(today, slot, key) }
    }

    // --- Fotoğraflar (ADR-0010) ---

    fun giveConsent() {
        viewModelScope.launch {
            prefs.update { it.copy(photoConsentAt = Instant.now()) }
            checkPhotos()
        }
    }

    /**
     * Rıza geri alınır: süren kayıt beklenir, tüm fotoğraflar ve anahtar silinir, sonra rıza kaydı kalkar.
     * Silme başarısız olsa bile rıza kalkar; kalan dosyalar sonraki açılışta silinir (AylaApp).
     */
    fun revokeConsent() {
        val pending = saveJob
        appScope.launch {
            pending?.cancelAndJoin()
            runCatching { photoStore.deleteAll() }
            prefs.update { it.copy(photoConsentAt = null) }
            photoUi.value = PhotoUi()
        }
    }

    suspend fun checkPhotos() {
        val status = when (photoStore.availability()) {
            PhotoStoreAvailability.Available -> PhotoStatus.AVAILABLE
            is PhotoStoreAvailability.Unavailable -> PhotoStatus.UNAVAILABLE
        }
        photoUi.update { it.copy(status = status) }
    }

    /** Anahtar kaybında temiz başlangıç: fotoğraflar silinir, rıza kalır. */
    fun resetPhotos() {
        appScope.launch {
            photoStore.deleteAll()
            checkPhotos()
        }
    }

    fun cameraTarget(): PendingCapture = photoStore.cameraTarget()

    fun capture(id: String): PendingCapture? = runCatching { photoStore.capture(id) }.getOrNull()

    fun saveCapture(capture: PendingCapture, success: Boolean, kind: PhotoKind, area: String?) = savePhoto {
        if (success) photoStore.saveCapture(capture, kind, area, state.value.today) else photoStore.discardCapture(capture)
    }

    fun savePicked(uri: Uri, kind: PhotoKind, area: String?) = savePhoto { photoStore.save(uri, kind, area, state.value.today) }

    private fun savePhoto(block: suspend () -> Unit) {
        photoUi.update { it.copy(busy = true, error = false, cameraMissing = false, exportResult = null) }
        saveJob = appScope.launch {
            try {
                block()
                // Kayıt sürerken rıza başka yerden (Ayarlar) geri alındıysa yazılan dosya da gider.
                if (prefs.current().photoConsentAt == null) photoStore.deleteAll()
                photoUi.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                photoUi.update { it.copy(busy = false, error = true) }
            }
        }
    }

    /** Kamera uygulaması açılamadı: boş hedef silinir, kullanıcıya galeriden seçmesi söylenir. */
    fun cameraUnavailable(capture: PendingCapture) {
        photoUi.update { it.copy(cameraMissing = true) }
        appScope.launch { runCatching { photoStore.discardCapture(capture) } }
    }

    /** Tüm fotoğrafları seçilen klasöre düz JPEG olarak yazar (telefon değiştirirken; yedeğe girmezler). */
    fun exportAll(treeUri: Uri) {
        photoUi.update { it.copy(busy = true, exporting = true, error = false, cameraMissing = false, exportResult = null) }
        appScope.launch {
            val result = try {
                photoStore.exportAllTo(treeUri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PhotoExportResult(exported = 0, failed = state.value.photos.size.coerceAtLeast(1))
            }
            photoUi.update { it.copy(busy = false, exporting = false, exportResult = result) }
        }
    }

    fun deletePhoto(id: String) {
        appScope.launch { runCatching { photoStore.delete(id) } }
    }

    fun exportPhoto(uri: Uri, id: String) {
        appScope.launch { runCatching { photoStore.exportTo(uri, id) } }
    }

    suspend fun thumbnail(record: PhotoRecord, maxPx: Int): Bitmap? = runCatching { photoStore.thumbnail(record.id, maxPx) }.getOrNull()
}
