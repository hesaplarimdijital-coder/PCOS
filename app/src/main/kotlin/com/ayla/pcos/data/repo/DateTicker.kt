package com.ayla.pcos.data.repo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Bugün" akışı. Gece yarısında ve uygulama öne geldiğinde yeniden değerlendirilir; böylece
 * ekranlar tarih değişince kendiliğinden güncellenir. Testler sabit bir gün için alt sınıf kullanır
 * (gece yarısı döngüsü sanal zamanda sonsuza dek ilerlemesin diye).
 */
@Singleton
open class DateTicker @Inject constructor(private val clock: Clock) {

    private val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Her öne gelişte (ve ilk abonelikte) tetiklenir; tarihten bağımsız olarak yeniden okunması gereken
     * sistem durumları için (ör. tam alarm izni). [today] aynı gün içinde tekrar yaymaz, bu yayar.
     */
    private val foregroundSignal = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1).apply { tryEmit(Unit) }
    open val foreground: Flow<Unit> = foregroundSignal

    private val midnightTicker: Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDate.now(clock)
            emit(now)
            val nextMidnight = now.plusDays(1).atStartOfDay(clock.zone).toInstant()
            val wait = Duration.between(Instant.now(clock), nextMidnight).toMillis().coerceAtLeast(1_000L)
            delay(wait)
        }
    }

    open val today: Flow<LocalDate> = merge(midnightTicker, refresh.map { LocalDate.now(clock) }).distinctUntilChanged()

    /** Etkinlik öne geldiğinde çağrılır; uzun uykudan sonra tarihi tazeler. */
    open fun refresh() {
        refresh.tryEmit(Unit)
        foregroundSignal.tryEmit(Unit)
    }

    open fun now(): LocalDate = LocalDate.now(clock)
}
