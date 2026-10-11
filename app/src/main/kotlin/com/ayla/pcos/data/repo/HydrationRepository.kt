package com.ayla.pcos.data.repo

import com.ayla.pcos.data.db.HydrationDao
import com.ayla.pcos.data.db.HydrationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationRepository @Inject constructor(private val dao: HydrationDao) {

    fun observeCups(date: LocalDate): Flow<Int> = dao.observe(date).map { it?.cups ?: 0 }

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<HydrationEntity>> = dao.observeRange(from, to)

    suspend fun cups(date: LocalDate): Int = dao.get(date)?.cups ?: 0

    suspend fun addCup(date: LocalDate) = setCups(date, cups(date) + 1)

    suspend fun removeCup(date: LocalDate) = setCups(date, (cups(date) - 1).coerceAtLeast(0))

    suspend fun setCups(date: LocalDate, cups: Int) =
        dao.upsert(HydrationEntity(date = date, cups = cups.coerceAtLeast(0), updatedAt = Instant.now()))
}
