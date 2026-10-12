package com.ayla.pcos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Sürüm geçmişi:
 * - 1: adet, günlük, ilaç/doz, su, uyku (ADR-0002).
 * - 2: doğurganlık kayıtları, cilt/saç profili ve rutin işaretleri, doktora sorular (ADR-0009, [MIGRATION_1_2]).
 */
@Database(
    entities = [
        PeriodEntity::class,
        DailyLogEntity::class,
        MedicationEntity::class,
        DoseEventEntity::class,
        HydrationEntity::class,
        SleepEntity::class,
        FertilityEntryEntity::class,
        SkinProfileEntity::class,
        SkinRoutineCheckEntity::class,
        DoctorQuestionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AylaDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun medicationDao(): MedicationDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun hydrationDao(): HydrationDao
    abstract fun sleepDao(): SleepDao
    abstract fun fertilityEntryDao(): FertilityEntryDao
    abstract fun skinProfileDao(): SkinProfileDao
    abstract fun skinRoutineCheckDao(): SkinRoutineCheckDao
    abstract fun doctorQuestionDao(): DoctorQuestionDao

    companion object {
        const val NAME = "ayla.db"
    }
}
