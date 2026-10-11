package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.DailyLogDao
import com.ayla.pcos.data.db.DailyLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(private val dao: DailyLogDao) {

    fun observe(date: LocalDate): Flow<DailyLogEntity?> = dao.observe(date)

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyLogEntity>> = dao.observeRange(from, to)

    suspend fun get(date: LocalDate): DailyLogEntity? = dao.get(date)

    suspend fun save(log: DailyLogEntity) = dao.upsert(log.copy(updatedAt = Instant.now()))

    suspend fun delete(date: LocalDate) = dao.delete(date)
}

/** Günlük kayıtta sunulan semptom anahtarları; metinleri strings.xml'de. */
object SymptomCatalog {
    const val BLOATING = "bloating"
    const val BREAST_TENDERNESS = "breast_tenderness"
    const val SWEET_CRAVING = "sweet_craving"
    const val ACNE = "acne"
    const val HAIR_LOSS = "hair_loss"
    const val CRAMPS = "cramps"
    const val HEADACHE = "headache"
    const val LOW_MOOD = "low_mood"
    const val FATIGUE = "fatigue"
    const val NAUSEA = "nausea"
    const val INSOMNIA = "insomnia"
    const val SPOTTING = "spotting"

    val defaults: List<String> = listOf(
        BLOATING, BREAST_TENDERNESS, SWEET_CRAVING, ACNE, HAIR_LOSS, CRAMPS,
        HEADACHE, LOW_MOOD, FATIGUE, NAUSEA, INSOMNIA, SPOTTING,
    )
}
