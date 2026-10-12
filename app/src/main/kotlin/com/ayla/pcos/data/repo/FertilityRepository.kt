package com.ayla.pcos.data.repo

import com.ayla.cycle.FertilityEngine
import com.ayla.cycle.FertilityEntry
import com.ayla.cycle.FertilityOverview
import com.ayla.pcos.data.db.FertilityEntryDao
import com.ayla.pcos.data.db.FertilityEntryEntity
import com.ayla.pcos.data.db.MedicationDao
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.db.PeriodDao
import com.ayla.pcos.data.prefs.Goal
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Doğurganlık kayıtları (LH/BBT/mukus, #24) ve core:cycle doğurganlık motorunun çağrısı.
 *
 * Hap modunda ([onPill]: tercihlerde PILL hedefi ya da aktif doğum kontrol hapı) motor hiçbir hesap
 * yapmaz ve [FertilityOverview.Disabled] döner (ADR-0005); kayıtlar yine de saklanır, kullanıcı
 * silmedikçe silinmez. Hiçbir sonuç tanı ya da doğum kontrol kararı değildir.
 */
@Singleton
class FertilityRepository @Inject constructor(
    private val dao: FertilityEntryDao,
    private val periodDao: PeriodDao,
    private val medicationDao: MedicationDao,
    private val prefs: UserPreferencesRepository,
    private val ticker: DateTicker,
) {
    /** Hap modu; hedef ya da aktif ilaç listesi değişince yeniden değerlendirilir. */
    val onPill: Flow<Boolean> = combine(prefs.preferences, medicationDao.observeActive()) { p, meds ->
        isOnPill(p, meds)
    }.distinctUntilChanged()

    /** Tüm doğurganlık görünümü; kayıt, adet, hap durumu ya da tarih değişince yeniden üretilir. */
    val overview: Flow<FertilityOverview> = combine(
        dao.observeAll(),
        periodDao.observeAll(),
        onPill,
        ticker.today,
    ) { entries, periods, pill, today ->
        FertilityEngine.analyze(entries.map { it.toEntry() }, periods.map { it.startDate }, today, pill)
    }.distinctUntilChanged()

    fun observeOverview(): Flow<FertilityOverview> = overview

    fun observe(date: LocalDate): Flow<FertilityEntryEntity?> = dao.observe(date)

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<FertilityEntryEntity>> = dao.observeRange(from, to)

    suspend fun getRange(from: LocalDate, to: LocalDate): List<FertilityEntryEntity> = dao.getRange(from, to)

    suspend fun get(date: LocalDate): FertilityEntryEntity? = dao.get(date)

    suspend fun getAll(): List<FertilityEntryEntity> = dao.getAll()

    /**
     * Günün kaydını yazar (aynı güne ikinci kayıt öncekinin yerine geçer). Isı 2 ondalığa yuvarlanır;
     * akla yatkın aralık (35,0–38,5 °C) dışındaki değer reddedilmez, olduğu gibi saklanır — motor
     * grafikte içi boş gösterir ve hesaba katmaz.
     */
    suspend fun save(entry: FertilityEntryEntity) = dao.upsert(entry.normalized().copy(updatedAt = Instant.now()))

    suspend fun delete(date: LocalDate) = dao.delete(date)

    /** Tek seferlik hesap (rapor, bildirim). Akış için [overview]. */
    suspend fun analyze(today: LocalDate = ticker.now()): FertilityOverview {
        val pill = isOnPill(prefs.current(), medicationDao.getActive())
        return FertilityEngine.analyze(
            entries = dao.getAll().map { it.toEntry() },
            periodStarts = periodDao.getAll().map { it.startDate },
            today = today,
            onPill = pill,
        )
    }

    /** Hap modu tek tanım: tercihlerde PILL hedefi ya da aktif doğum kontrol hapı (ADR-0005; rapor da bunu kullanır). */
    suspend fun isOnPill(): Boolean = isOnPill(prefs.current(), medicationDao.getActive())

    private fun isOnPill(p: UserPreferences, activeMedications: List<MedicationEntity>): Boolean =
        Goal.PILL in p.goals || activeMedications.any { it.kind == MedicationKind.CONTRACEPTIVE }
}

/** Motor girdisine dönüşüm; alan adları birebirdir. */
fun FertilityEntryEntity.toEntry(): FertilityEntry = FertilityEntry(
    date = date,
    bbtCelsius = bbtCelsius,
    bbtTime = bbtTime,
    bbtDisturbed = bbtDisturbed,
    lh = lhResult,
    mucus = mucus,
)

/** Isıyı 2 ondalığa yuvarlar (36,575 → 36,58); sonlu olmayan değer atılır. İçe aktarma da bunu kullanır. */
internal fun FertilityEntryEntity.normalized(): FertilityEntryEntity = copy(bbtCelsius = bbtCelsius?.roundedCelsius())

internal fun Double.roundedCelsius(): Double? =
    takeIf { it.isFinite() }?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP)?.toDouble()
