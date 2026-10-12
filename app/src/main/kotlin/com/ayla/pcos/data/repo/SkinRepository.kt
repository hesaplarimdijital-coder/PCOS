package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileDao
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinRoutineCheckDao
import com.ayla.pcos.data.db.SkinRoutineCheckEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cilt/saç profili (tek satır) ve günlük rutin işaretleri (#25). Profil öz-beyanları sağlık verisidir;
 * yalnızca şifreli veritabanında durur. Ürün/ilaç önerisi üretmez; içerik süzgeci arayüzdedir.
 */
@Singleton
class SkinRepository @Inject constructor(
    private val profileDao: SkinProfileDao,
    private val routineDao: SkinRoutineCheckDao,
) {
    /** Aynı dilime iki hızlı dokunuş oku-değiştir-yaz yarışına girmesin. */
    private val toggleLock = Mutex()

    fun observeProfile(): Flow<SkinProfileEntity?> = profileDao.observe()

    suspend fun profile(): SkinProfileEntity? = profileDao.get()

    /** Profili yazar; kimlik her zaman tek satıra ([SkinProfileEntity.SINGLE_ID]) sabitlenir. */
    suspend fun saveProfile(profile: SkinProfileEntity) =
        profileDao.upsert(profile.copy(id = SkinProfileEntity.SINGLE_ID, updatedAt = Instant.now()))

    /**
     * Profili güncel satırdan oku-değiştir-yaz ile günceller: art arda iki hızlı değişiklik birbirini ezmez
     * (ekrandaki durum henüz yenilenmemiş olabilir).
     */
    suspend fun updateProfile(transform: (SkinProfileEntity) -> SkinProfileEntity) = toggleLock.withLock {
        saveProfile(transform(profileDao.get() ?: SkinProfileEntity()))
    }

    suspend fun clearProfile() = profileDao.deleteAll()

    /** Günün sabah/akşam işaretleri (işaretsiz dilim listede yoktur); sıra sabah → akşam (enum sırası, metin sırası değil). */
    fun observeRoutine(date: LocalDate): Flow<List<SkinRoutineCheckEntity>> = routineDao.observe(date).map { it.slotOrder() }

    fun observeRoutineRange(from: LocalDate, to: LocalDate): Flow<List<SkinRoutineCheckEntity>> =
        routineDao.observeRange(from, to).map { it.slotOrder() }

    suspend fun routineRange(from: LocalDate, to: LocalDate): List<SkinRoutineCheckEntity> = routineDao.getRange(from, to).slotOrder()

    private fun List<SkinRoutineCheckEntity>.slotOrder(): List<SkinRoutineCheckEntity> =
        sortedWith(compareBy<SkinRoutineCheckEntity> { it.date }.thenBy { it.slot.ordinal })

    suspend fun routine(date: LocalDate, slot: RoutineSlot): SkinRoutineCheckEntity? = routineDao.get(date, slot)

    /**
     * Bir adımı işaretler ya da işareti kaldırır; yeni durumu (işaretli mi) döndürür. Dilimde hiç adım
     * kalmazsa satır silinir, böylece "hiç dokunulmamış" ile "tümü kaldırılmış" aynı görünür.
     */
    suspend fun toggleStep(date: LocalDate, slot: RoutineSlot, stepKey: String): Boolean = toggleLock.withLock {
        // Adımlar "|" ile birleştirilerek saklanır (Converters); anahtar bu karakteri içeremez.
        require(stepKey.isNotBlank() && '|' !in stepKey) { "Geçersiz adım anahtarı" }
        val current = routineDao.get(date, slot)?.doneSteps ?: emptyList()
        val done = stepKey !in current
        val updated = if (done) current + stepKey else current - stepKey
        if (updated.isEmpty()) {
            routineDao.delete(date, slot)
        } else {
            routineDao.upsert(SkinRoutineCheckEntity(date = date, slot = slot, doneSteps = updated, updatedAt = Instant.now()))
        }
        done
    }

    /** Dilimin tüm işaretlerini tek seferde yazar (ör. "hepsini tamamladım"). Boş liste satırı siler. */
    suspend fun setSteps(date: LocalDate, slot: RoutineSlot, stepKeys: List<String>) = toggleLock.withLock {
        val steps = stepKeys.filter { it.isNotBlank() && '|' !in it }.distinct()
        if (steps.isEmpty()) {
            routineDao.delete(date, slot)
        } else {
            routineDao.upsert(SkinRoutineCheckEntity(date = date, slot = slot, doneSteps = steps, updatedAt = Instant.now()))
        }
    }
}
