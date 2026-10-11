package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.SleepDao
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.db.SleepSource
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepRepository @Inject constructor(private val dao: SleepDao) {

    fun observeNight(date: LocalDate): Flow<SleepEntity?> = dao.observeLatestFor(date)

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<SleepEntity>> = dao.observeRange(from, to)

    /** Elle girilen uyku; [wakeDate] uyanılan gündür. */
    suspend fun addManual(start: Instant, end: Instant, quality: Int?, zone: ZoneId = ZoneId.systemDefault()) {
        require(end.isAfter(start)) { "Uyku bitişi başlangıçtan sonra olmalı" }
        val wakeDate = end.atZone(zone).toLocalDate()
        dao.insert(SleepEntity(date = wakeDate, startAt = start, endAt = end, quality = quality, source = SleepSource.MANUAL))
    }

    /** Health Connect'ten gelen oturumları ekler; aynı kimlikli kayıt ikinci kez eklenmez. */
    suspend fun importExternal(sessions: List<SleepEntity>) {
        val known = dao.getExternalIds().toSet()
        sessions.filter { it.externalId != null && it.externalId !in known }.forEach { dao.insert(it) }
    }

    suspend fun delete(entity: SleepEntity) = dao.delete(entity)
}

val SleepEntity.duration: Duration get() = Duration.between(startAt, endAt)
