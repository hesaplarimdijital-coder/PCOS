package com.ayla.pcos.data.health

import com.ayla.pcos.data.db.SleepEntity
import com.ayla.pcos.data.db.SleepSource
import com.ayla.pcos.data.repo.duration
import java.time.Duration
import java.time.ZoneId

/** 24 saati aşan oturum gerçek bir uyku kaydı değildir (ör. kapatılmayı unutulmuş takip); alınmaz. */
val MAX_SLEEP_SESSION: Duration = Duration.ofHours(24)

/** Uyanılan gün: oturumun bittiği yerel gün; bitiş ofseti yoksa cihazın saat dilimi. */
fun ExternalSleepSession.toSleepEntity(fallbackZone: ZoneId): SleepEntity = SleepEntity(
    date = end.atZone(endOffset ?: fallbackZone).toLocalDate(),
    startAt = start,
    endAt = end,
    quality = null,
    source = SleepSource.HEALTH_CONNECT,
    externalId = id,
)

/**
 * Eklenecek oturumlar (#11). Alınmayanlar: bitişi başlangıçtan önce ya da 24 saatten uzun olan, kimliği zaten
 * bilinen ve var olan bir kayıtla (elle girilen dahil) çakışan oturumlar; cihazdaki kayıt kazanır. Gelenler
 * kendi aralarında çakışırsa (ör. saat ve telefon aynı geceyi ayrı kaydetmiş) uzun olan kalır. Sonuç
 * başlangıca göre sıralıdır.
 */
fun selectNewSleep(existing: List<SleepEntity>, incoming: List<SleepEntity>): List<SleepEntity> {
    val knownIds = existing.mapNotNullTo(HashSet()) { it.externalId }
    val occupied = existing.toMutableList()
    val result = mutableListOf<SleepEntity>()
    incoming
        .filter { it.endAt.isAfter(it.startAt) && it.duration <= MAX_SLEEP_SESSION }
        .filter { candidate -> candidate.externalId.let { id -> id == null || id !in knownIds } }
        .sortedByDescending { it.duration }
        .forEach { candidate ->
            if (occupied.none { it.overlaps(candidate) }) {
                occupied += candidate
                result += candidate
            }
        }
    return result.sortedBy { it.startAt }
}

private fun SleepEntity.overlaps(other: SleepEntity): Boolean = startAt.isBefore(other.endAt) && other.startAt.isBefore(endAt)
