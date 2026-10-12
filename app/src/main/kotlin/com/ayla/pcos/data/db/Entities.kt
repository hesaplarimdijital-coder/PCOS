package com.ayla.pcos.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.LhResult
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

/*
 * MVP 3 varlıkları (ADR-0009, şema sürümü 2). Hepsi sağlık verisidir ve yalnızca şifreli veritabanında
 * durur; DataStore'a yazılmaz. Enum'lar Room tarafından adıyla TEXT olarak saklanır.
 */

/**
 * Bir güne ait doğurganlık kaydı: bazal vücut ısısı, LH testi ve servikal mukus. Alanlar isteğe bağlıdır;
 * core:cycle motorunun [com.ayla.cycle.FertilityEntry] girdisine birebir eşlenir. Isı 2 ondalıkla
 * saklanır; akla yatkın aralık dışı değerler kaydedilir ama motor hesaba katmaz.
 */
@Entity(tableName = "fertility_entries")
data class FertilityEntryEntity(
    @PrimaryKey val date: LocalDate,
    @ColumnInfo(name = "bbt_celsius") val bbtCelsius: Double? = null,
    @ColumnInfo(name = "bbt_time") val bbtTime: LocalTime? = null,
    /** Ateş, alkol, az uyku, geç ölçüm: örtü çizgisi ve yükselme hesabına girmez. */
    @ColumnInfo(name = "bbt_disturbed") val bbtDisturbed: Boolean = false,
    @ColumnInfo(name = "lh_result") val lhResult: LhResult? = null,
    val mucus: CervicalMucus? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
)

/** Cilt tipi öz-beyanı. */
enum class SkinType { DRY, OILY, COMBINATION, NORMAL, SENSITIVE }

/** Sivilce öz-beyanı; tanı ya da derecelendirme değildir, kullanıcının kendi değerlendirmesidir. */
enum class AcneSelfReport { NONE, MILD, MODERATE, SEVERE }

/** Cilt bakım rutini dilimi. */
enum class RoutineSlot { MORNING, EVENING }

/**
 * Cilt/saç profili; tek satırdır ([SINGLE_ID]). Öz-beyanlar ve "doktoruna söyle" işaretleri
 * ([acanthosis], [unwantedHair], [hairThinning]) buradadır. Gebelik planı tercihlerdeki TTC hedefiyle
 * birleşir; [pregnantOrBreastfeeding] içerik süzgeci içindir (bazı adımlar gebelikte gösterilmez).
 */
@Entity(tableName = "skin_profile")
data class SkinProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ID,
    @ColumnInfo(name = "skin_type") val skinType: SkinType? = null,
    val acne: AcneSelfReport? = null,
    @ColumnInfo(name = "dark_spots") val darkSpots: Boolean = false,
    val acanthosis: Boolean = false,
    @ColumnInfo(name = "unwanted_hair") val unwantedHair: Boolean = false,
    @ColumnInfo(name = "hair_thinning") val hairThinning: Boolean = false,
    @ColumnInfo(name = "pregnant_or_breastfeeding") val pregnantOrBreastfeeding: Boolean = false,
    @ColumnInfo(name = "morning_reminder") val morningReminder: LocalTime? = null,
    @ColumnInfo(name = "evening_reminder") val eveningReminder: LocalTime? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
) {
    companion object {
        /** Tablodaki tek satırın kimliği. */
        const val SINGLE_ID = 1
    }
}

/**
 * Bir günün bir dilimindeki (sabah/akşam) tamamlanan rutin adımları. Adım anahtarları içerik
 * tablosundan gelir (`skin_step_*`); metinleri strings.xml'dedir.
 */
@Entity(tableName = "skin_routine_checks", primaryKeys = ["date", "slot"])
data class SkinRoutineCheckEntity(
    val date: LocalDate,
    val slot: RoutineSlot,
    @ColumnInfo(name = "done_steps") val doneSteps: List<String> = emptyList(),
    @ColumnInfo(name = "updated_at") val updatedAt: Instant = Instant.now(),
)

/** Doktora sorulacak bir soru. "Sordum" işaretlenen ([askedAt]) soru raporun "sorulanlar" kısmına düşer. */
@Entity(tableName = "doctor_questions")
data class DoctorQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant = Instant.now(),
    @ColumnInfo(name = "asked_at") val askedAt: Instant? = null,
)
