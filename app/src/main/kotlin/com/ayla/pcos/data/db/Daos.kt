package com.ayla.pcos.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY start_date ASC")
    fun observeAll(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods ORDER BY start_date ASC")
    suspend fun getAll(): List<PeriodEntity>

    @Query("SELECT * FROM periods WHERE start_date = :date LIMIT 1")
    suspend fun getByStart(date: LocalDate): PeriodEntity?

    @Query("SELECT * FROM periods WHERE start_date <= :date ORDER BY start_date DESC LIMIT 1")
    suspend fun latestOnOrBefore(date: LocalDate): PeriodEntity?

    @Upsert
    suspend fun upsert(period: PeriodEntity): Long

    @Delete
    suspend fun delete(period: PeriodEntity)

    @Query("UPDATE periods SET excluded_from_stats = :excluded WHERE id = :id")
    suspend fun setExcluded(id: Long, excluded: Boolean)

    @Query("UPDATE periods SET end_date = :end WHERE id = :id")
    suspend fun setEnd(id: Long, end: LocalDate?)

    /** Yedekten "üzerine yaz" için; işlem (transaction) içinde çağrılır. */
    @Query("DELETE FROM periods")
    suspend fun deleteAll()
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun observe(date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun get(date: LocalDate): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY date ASC")
    suspend fun getAll(): List<DailyLogEntity>

    @Upsert
    suspend fun upsert(log: DailyLogEntity)

    @Query("DELETE FROM daily_logs WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAll()
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE is_active = 1 ORDER BY created_at ASC")
    fun observeActive(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE is_active = 1 ORDER BY created_at ASC")
    suspend fun getActive(): List<MedicationEntity>

    @Query("SELECT * FROM medications ORDER BY created_at ASC")
    suspend fun getAll(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun get(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Query("UPDATE medications SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("UPDATE medications SET stock_count = :count WHERE id = :id")
    suspend fun setStock(id: Long, count: Int?)

    @Query("DELETE FROM medications")
    suspend fun deleteAll()
}

@Dao
interface DoseEventDao {
    @Query("SELECT * FROM dose_events WHERE date = :date ORDER BY time ASC")
    fun observeForDate(date: LocalDate): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DoseEventEntity>>

    @Query("SELECT * FROM dose_events WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
    suspend fun getRange(from: LocalDate, to: LocalDate): List<DoseEventEntity>

    @Query("SELECT * FROM dose_events WHERE medication_id = :medicationId AND date = :date AND time = :time LIMIT 1")
    suspend fun find(medicationId: Long, date: LocalDate, time: LocalTime): DoseEventEntity?

    @Query("SELECT * FROM dose_events WHERE id = :id")
    suspend fun get(id: Long): DoseEventEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: DoseEventEntity): Long

    @Upsert
    suspend fun upsert(event: DoseEventEntity)

    @Query("UPDATE dose_events SET status = :status, acted_at = :actedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: DoseStatus, actedAt: Instant?)

    @Query("UPDATE dose_events SET status = 'MISSED' WHERE status = 'PENDING' AND date < :before")
    suspend fun markPendingBeforeAsMissed(before: LocalDate)

    @Query("SELECT * FROM dose_events ORDER BY date ASC, time ASC")
    suspend fun getAll(): List<DoseEventEntity>

    @Query("DELETE FROM dose_events")
    suspend fun deleteAll()
}

@Dao
interface HydrationDao {
    @Query("SELECT * FROM hydration WHERE date = :date")
    fun observe(date: LocalDate): Flow<HydrationEntity?>

    @Query("SELECT * FROM hydration WHERE date = :date")
    suspend fun get(date: LocalDate): HydrationEntity?

    @Query("SELECT * FROM hydration WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<HydrationEntity>>

    @Query("SELECT * FROM hydration ORDER BY date ASC")
    suspend fun getAll(): List<HydrationEntity>

    @Upsert
    suspend fun upsert(entity: HydrationEntity)

    @Query("DELETE FROM hydration")
    suspend fun deleteAll()
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_sessions WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<SleepEntity>>

    /**
     * "Dün gece": uyanılan günün en uzun oturumu. Health Connect'ten gelen öğle uykusu, gecenin yerine
     * geçmesin diye en son başlayan değil en uzun olan seçilir; eşitlikte en son başlayan.
     */
    @Query("SELECT * FROM sleep_sessions WHERE date = :date ORDER BY (end_at - start_at) DESC, start_at DESC LIMIT 1")
    fun observeLongestFor(date: LocalDate): Flow<SleepEntity?>

    @Query("SELECT COUNT(*) FROM sleep_sessions WHERE source = :source")
    fun observeCount(source: SleepSource): Flow<Int>

    /** Health Connect'ten alınan kopyaları siler; Health Connect'teki asıl kayıtlar değişmez. */
    @Query("DELETE FROM sleep_sessions WHERE source = :source")
    suspend fun deleteBySource(source: SleepSource): Int

    @Query("SELECT * FROM sleep_sessions ORDER BY date ASC")
    suspend fun getAll(): List<SleepEntity>

    @Query("SELECT external_id FROM sleep_sessions WHERE external_id IS NOT NULL")
    suspend fun getExternalIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SleepEntity): Long

    @Delete
    suspend fun delete(entity: SleepEntity)

    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAll()
}
