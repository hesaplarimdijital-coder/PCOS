package com.ayla.pcos.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.ayla.cycle.CycleEngine
import com.ayla.pcos.data.db.ALL_MIGRATIONS
import com.ayla.pcos.data.db.AylaDatabase
import com.ayla.pcos.data.db.DailyLogDao
import com.ayla.pcos.data.db.DoctorQuestionDao
import com.ayla.pcos.data.db.DoseEventDao
import com.ayla.pcos.data.db.FertilityEntryDao
import com.ayla.pcos.data.db.HydrationDao
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.db.PeriodDao
import com.ayla.pcos.data.db.SkinProfileDao
import com.ayla.pcos.data.db.SkinRoutineCheckDao
import com.ayla.pcos.data.db.SleepDao
import com.ayla.pcos.export.DataExporter
import com.ayla.pcos.security.DatabaseKeyProvider
import com.ayla.pcos.security.DatabaseKeyUnavailableException
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
import java.io.File
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

    /** Dilim değişince (seyahat) alarmlar ve widget yeni dilimle hesaplansın diye dilim her çağrıda okunur. */
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemZoneClock

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
        val dbFile = context.getDatabasePath(AylaDatabase.NAME)
        val passphrase = try {
            keyProvider.passphrase(databaseExists = dbFile.exists() && dbFile.length() > 0L)
        } catch (e: DatabaseKeyUnavailableException) {
            // Keystore anahtarı kaybolmuş: eski dosya bu anahtarla bir daha açılamaz. Çökme döngüsü
            // yerine dosyayı karantinaya al, yeni anahtarla temiz başla ve kullanıcıya bir kez söyle.
            Log.e(TAG, "Veritabanı anahtarı kullanılamıyor; eski dosya karantinaya alınıyor", e)
            quarantine(dbFile)
            keyProvider.rotateAfterLoss()
        }
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, AylaDatabase::class.java, AylaDatabase.NAME)
            .openHelperFactory(factory)
            // Sürüm artışları yalnızca tablo/sütun ekler (data/db/Migrations.kt); kullanıcı verisi korunur.
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }

    /** ayla.db ve yardımcı dosyalarını (-wal, -shm, -journal) zaman damgalı adla kenara koyar. */
    private fun quarantine(dbFile: File) {
        val stamp = System.currentTimeMillis()
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            val f = File(dbFile.path + suffix)
            if (f.exists()) f.renameTo(File(dbFile.path + DataExporter.QUARANTINE_MARKER + stamp + suffix))
        }
    }

    private const val TAG = "AppModule"

    @Provides fun providePeriodDao(db: AylaDatabase): PeriodDao = db.periodDao()
    @Provides fun provideDailyLogDao(db: AylaDatabase): DailyLogDao = db.dailyLogDao()
    @Provides fun provideMedicationDao(db: AylaDatabase): MedicationDao = db.medicationDao()
    @Provides fun provideDoseEventDao(db: AylaDatabase): DoseEventDao = db.doseEventDao()
    @Provides fun provideHydrationDao(db: AylaDatabase): HydrationDao = db.hydrationDao()
    @Provides fun provideSleepDao(db: AylaDatabase): SleepDao = db.sleepDao()
    @Provides fun provideFertilityEntryDao(db: AylaDatabase): FertilityEntryDao = db.fertilityEntryDao()
    @Provides fun provideSkinProfileDao(db: AylaDatabase): SkinProfileDao = db.skinProfileDao()
    @Provides fun provideSkinRoutineCheckDao(db: AylaDatabase): SkinRoutineCheckDao = db.skinRoutineCheckDao()
    @Provides fun provideDoctorQuestionDao(db: AylaDatabase): DoctorQuestionDao = db.doctorQuestionDao()
}
