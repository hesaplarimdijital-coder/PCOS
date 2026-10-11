package com.ayla.pcos.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.ayla.cycle.CycleEngine
import com.ayla.pcos.data.db.AylaDatabase
import com.ayla.pcos.data.db.DailyLogDao
import com.ayla.pcos.data.db.DoseEventDao
import com.ayla.pcos.data.db.HydrationDao
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.db.PeriodDao
import com.ayla.pcos.data.db.SleepDao
import com.ayla.pcos.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

/** Uygulama ömrü boyunca yaşayan arka plan işleri için kapsam. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideCycleEngine(): CycleEngine = CycleEngine()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("ayla_prefs") }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context, keyProvider: DatabaseKeyProvider): AylaDatabase {
        // SQLCipher yerel kütüphanesi Room'dan önce yüklenmeli.
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(keyProvider.passphrase())
        return Room.databaseBuilder(context, AylaDatabase::class.java, AylaDatabase.NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    @Provides fun providePeriodDao(db: AylaDatabase): PeriodDao = db.periodDao()
    @Provides fun provideDailyLogDao(db: AylaDatabase): DailyLogDao = db.dailyLogDao()
    @Provides fun provideMedicationDao(db: AylaDatabase): MedicationDao = db.medicationDao()
    @Provides fun provideDoseEventDao(db: AylaDatabase): DoseEventDao = db.doseEventDao()
    @Provides fun provideHydrationDao(db: AylaDatabase): HydrationDao = db.hydrationDao()
    @Provides fun provideSleepDao(db: AylaDatabase): SleepDao = db.sleepDao()
}
