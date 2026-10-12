package com.ayla.pcos.di

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Sistem saati; `Clock.systemDefaultZone()` dilimi oluşturulduğu anda sabitler, bu saat ise her çağrıda
 * [ZoneId.systemDefault] okur. Android dilim değişince süreç içindeki varsayılan dilimi günceller; böylece
 * İstanbul'dan Londra'ya uçan birinin 21:00 dozu yerel 21:00'de çalar.
 */
object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
