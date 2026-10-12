package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.DoctorQuestionDao
import com.ayla.pcos.data.db.DoctorQuestionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Doktora sorulacak sorular (US-103). Sorular raporun "sorular/sorulanlar" bölümüne düşer; silinebilir. */
@Singleton
class DoctorQuestionRepository @Inject constructor(private val dao: DoctorQuestionDao) {

    /** Oluşturulma sırasıyla (eskiden yeniye). */
    fun observeAll(): Flow<List<DoctorQuestionEntity>> = dao.observeAll()

    suspend fun getAll(): List<DoctorQuestionEntity> = dao.getAll()

    suspend fun get(id: Long): DoctorQuestionEntity? = dao.get(id)

    /**
     * Soruyu kırparak ekler; yalnızca boşluktan oluşan metin reddedilir (null döner). Metin
     * [MAX_LENGTH] karakterle sınırlıdır; arayüz aynı sınırı alanda uygular.
     */
    suspend fun add(text: String): Long? {
        val trimmed = text.trim().take(MAX_LENGTH)
        if (trimmed.isEmpty()) return null
        return dao.insert(DoctorQuestionEntity(text = trimmed))
    }

    /** "Sordum" işaretini koyar ya da kaldırır. */
    suspend fun markAsked(id: Long, asked: Boolean, at: Instant = Instant.now()) =
        dao.setAskedAt(id, if (asked) at else null)

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        const val MAX_LENGTH = 500
    }
}

val DoctorQuestionEntity.isAsked: Boolean get() = askedAt != null
