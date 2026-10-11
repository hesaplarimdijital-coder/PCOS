package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.HydrationDao
import com.ayla.pcos.data.db.HydrationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Su kaydı. Ekleme/çıkarma oku-değiştir-yaz işlemidir; hızlı art arda dokunuşlarda bardak kaybolmasın
 * diye tüm yazmalar tek bir kilitle sıraya girer.
 */
@Singleton
class HydrationRepository @Inject constructor(private val dao: HydrationDao) {

    private val writeLock = Mutex()

    fun observeCups(date: LocalDate): Flow<Int> = dao.observe(date).map { it?.cups ?: 0 }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<HydrationEntity>> = dao.observeRange(from, to)

    suspend fun cups(date: LocalDate): Int = dao.get(date)?.cups ?: 0

    suspend fun addCup(date: LocalDate) = change(date, +1)

    suspend fun removeCup(date: LocalDate) = change(date, -1)

    suspend fun setCups(date: LocalDate, cups: Int) = writeLock.withLock { write(date, cups) }

    private suspend fun change(date: LocalDate, delta: Int) = writeLock.withLock { write(date, cups(date) + delta) }

    private suspend fun write(date: LocalDate, cups: Int) =
        dao.upsert(HydrationEntity(date = date, cups = cups.coerceAtLeast(0), updatedAt = Instant.now()))
}
