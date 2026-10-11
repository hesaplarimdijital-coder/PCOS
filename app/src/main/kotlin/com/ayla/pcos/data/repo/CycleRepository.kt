package com.ayla.pcos.data.repo

import com.ayla.cycle.CycleEngine
import com.ayla.cycle.CycleOverview
import com.ayla.cycle.PeriodRecord
import com.ayla.pcos.data.db.PeriodDao
import com.ayla.pcos.data.db.PeriodEntity
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CycleRepository @Inject constructor(
    private val periodDao: PeriodDao,
    private val prefs: UserPreferencesRepository,
    private val engine: CycleEngine,
    private val ticker: DateTicker,
) {
    /** Tüm döngü hesabı; adet kaydı, ayar ya da tarih değişince yeniden üretilir. */
    val overview: Flow<CycleOverview> = combine(
        periodDao.observeAll(),
        prefs.preferences,
        ticker.today,
    ) { periods, preferences, today ->
        engine.overview(periods.map { it.toRecord() }, today, preferences.trackingSince)
    }

    val periods: Flow<List<PeriodEntity>> = periodDao.observeAll()

    suspend fun snapshot(): CycleOverview = overview.first()

    /**
     * Adet başlangıcı kaydeder. Son adet hâlâ "açık"sa (bitişi yok) ve yeni tarih ona
     * [SAME_PERIOD_WINDOW_DAYS] içinde ise yeni kayıt açmaz; bu, aynı adetin iki kez
     * kaydedilmesini önler.
     */
    suspend fun startPeriod(date: LocalDate) {
        val latest = periodDao.latestOnOrBefore(date)
        if (latest != null) {
            val gap = ChronoUnit.DAYS.between(latest.startDate, date)
            if (gap in 0 until SAME_PERIOD_WINDOW_DAYS && latest.endDate == null) return
            if (gap == 0L) return
        }
        periodDao.upsert(PeriodEntity(startDate = date))
        ensureTrackingSince(date)
    }

    suspend fun endPeriod(startDate: LocalDate, endDate: LocalDate) {
        val period = periodDao.getByStart(startDate) ?: return
        periodDao.setEnd(period.id, endDate)
    }

    /**
     * Takvimden açık işaretleme: o gün başlayan kayıt yoksa ekler. Bugün ekranındaki
     * "aynı adet iki kez" koruması burada uygulanmaz; kullanıcı tarihi bilinçli seçmiştir.
     * Mantıksız kısa döngüler motor tarafından zaten istatistik dışı bırakılır.
     */
    suspend fun addPeriodStart(date: LocalDate) {
        if (periodDao.getByStart(date) != null) return
        periodDao.upsert(PeriodEntity(startDate = date))
        ensureTrackingSince(date)
    }

    /** [date] gününü, ondan önce başlayan son adetin bitişi yapar; adet yoksa bir şey yapmaz. */
    suspend fun endPeriodAt(date: LocalDate) {
        val period = periodDao.latestOnOrBefore(date) ?: return
        periodDao.setEnd(period.id, date)
    }

    /** Bitiş tarihini kaldırır (adet "açık" kalır). */
    suspend fun clearEnd(period: PeriodEntity) = periodDao.setEnd(period.id, null)

    suspend fun deletePeriod(period: PeriodEntity) = periodDao.delete(period)

    suspend fun setExcluded(period: PeriodEntity, excluded: Boolean) = periodDao.setExcluded(period.id, excluded)

    suspend fun upsert(period: PeriodEntity) {
        periodDao.upsert(period)
        ensureTrackingSince(period.startDate)
    }

    private suspend fun ensureTrackingSince(date: LocalDate) {
        val current = prefs.current().trackingSince
        if (current == null || date.isBefore(current)) {
            prefs.update { it.copy(trackingSince = minOf(date, ticker.now())) }
        }
    }

    private fun PeriodEntity.toRecord() = PeriodRecord(
        start = startDate,
        end = endDate,
        excludedFromStats = excludedFromStats,
    )

    private companion object {
        const val SAME_PERIOD_WINDOW_DAYS = 7L
    }
}
