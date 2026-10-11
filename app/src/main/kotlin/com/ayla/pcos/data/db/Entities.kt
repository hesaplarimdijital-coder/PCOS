package com.ayla.pcos.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Kayıtlı adet. Döngüler core:cycle motorunda bu kayıtlardan türetilir. */
@Entity(tableName = "periods", indices = [Index(value = ["start_date"], unique = true)])
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "end_date") val endDate: LocalDate? = null,
    /** Kullanıcı bu adetle başlayan döngüyü istatistik dışı bıraktı. */
    @ColumnInfo(name = "excluded_from_stats") val excludedFromStats: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
)

/** Bir güne ait kayıt: ruh hâli, enerji, kanama, semptomlar, not, isteğe bağlı ölçümler. */
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: LocalDate,
    /** 1 (çok düşük) … 5 (çok iyi) */
    val mood: Int? = null,
    /** 1 (düşük) … 3 (yüksek) */
    val energy: Int? = null,
    /** 0 yok, 1 lekelenme, 2 hafif, 3 orta, 4 yoğun */
    val flow: Int? = null,
    /** Semptom anahtarları (bkz. SymptomCatalog). */
    val symptoms: List<String> = emptyList(),
    val note: String? = null,
    /** Varsayılan olarak gizli; kullanıcı açarsa kaydedilir. */
    @ColumnInfo(name = "weight_kg") val weightKg: Double? = null,
    @ColumnInfo(name = "waist_cm") val waistCm: Double? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
)

enum class MedicationKind { SUPPLEMENT, PRESCRIPTION, CONTRACEPTIVE, HERBAL }

enum class MedicationForm { CAPSULE, TABLET, SACHET, DROPS, TEA, OTHER }

/**
 * İlaç ya da takviye. [times] günlük doz saatleri; doğum kontrol hapında paket bilgisi de tutulur.
 */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** "1 kapsül", "500 mg" gibi serbest metin. */
    @ColumnInfo(name = "dose_text") val doseText: String,
    val kind: MedicationKind,
    val form: MedicationForm = MedicationForm.CAPSULE,
    val times: List<LocalTime>,
    /** true: yemekle, false: aç karna, null: fark etmez. */
    @ColumnInfo(name = "with_food") val withFood: Boolean? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    /** Kalan doz sayısı; null ise stok takibi kapalı. */
    @ColumnInfo(name = "stock_count") val stockCount: Int? = null,
    @ColumnInfo(name = "stock_warn_days") val stockWarnDays: Int = 5,
    /** Doğum kontrol hapı düzeni (yalnızca CONTRACEPTIVE). */
    @ColumnInfo(name = "pill_active_days") val pillActiveDays: Int? = null,
    @ColumnInfo(name = "pill_placebo_days") val pillPlaceboDays: Int? = null,
    @ColumnInfo(name = "pack_start_date") val packStartDate: LocalDate? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
)

enum class DoseStatus { PENDING, TAKEN, SKIPPED, MISSED }

/**
 * Planlanmış tek bir doz. (medication, date, time) üçlüsü benzersizdir; hatırlatma çaldığında
 * PENDING satır oluşur, kullanıcı "Aldım" deyince TAKEN olur.
 */
@Entity(
    tableName = "dose_events",
    indices = [Index(value = ["medication_id", "date", "time"], unique = true), Index(value = ["date"])],
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "medication_id") val medicationId: Long,
    val date: LocalDate,
    val time: LocalTime,
    val status: DoseStatus = DoseStatus.PENDING,
    @ColumnInfo(name = "acted_at") val actedAt: Instant? = null,
)

/** Günlük su: bardak sayısı. Bardak hacmi ayarlardan gelir. */
@Entity(tableName = "hydration")
data class HydrationEntity(
    @PrimaryKey val date: LocalDate,
    val cups: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
)

enum class SleepSource { MANUAL, HEALTH_CONNECT }

/** Bir gece uykusu. [date] uyanılan gündür. */
@Entity(tableName = "sleep_sessions", indices = [Index(value = ["date"])])
data class SleepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    @ColumnInfo(name = "start_at") val startAt: Instant,
    @ColumnInfo(name = "end_at") val endAt: Instant,
    /** 1 (kötü) … 3 (iyi); Health Connect'ten gelende null. */
    val quality: Int? = null,
    val source: SleepSource = SleepSource.MANUAL,
    /** Health Connect kayıt kimliği; yinelenen içe aktarmayı önler. */
    @ColumnInfo(name = "external_id") val externalId: String? = null,
)
