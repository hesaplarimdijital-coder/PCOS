package com.ayla.pcos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PeriodEntity::class,
        DailyLogEntity::class,
        MedicationEntity::class,
        DoseEventEntity::class,
        HydrationEntity::class,
        SleepEntity::class,
    ],
    version = 1,
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

    companion object {
        const val NAME = "ayla.db"
    }
}
