package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.SleepDao
import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.db.SleepSource
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepRepository @Inject constructor(private val dao: SleepDao, private val prefs: UserPreferencesRepository) {

    /** Uyanılan günün ana uykusu (en uzun oturum). */
    fun observeNight(date: LocalDate): Flow<SleepEntity?> = dao.observeLongestFor(date)

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

    /**
     * Geceyi siler. Health Connect'ten gelen bir geceyse kimliği "gizlenenler"e eklenir; böylece sonraki
     * almada yeniden eklenmez (Health Connect'teki asıl kayıt değişmez).
     */
    suspend fun delete(entity: SleepEntity) {
        dao.delete(entity)
        val id = entity.externalId
        if (entity.source == SleepSource.HEALTH_CONNECT && id != null) {
            prefs.update { it.copy(healthConnectHiddenIds = it.healthConnectHiddenIds + id) }
        }
    }
}

val SleepEntity.duration: Duration get() = Duration.between(startAt, endAt)
