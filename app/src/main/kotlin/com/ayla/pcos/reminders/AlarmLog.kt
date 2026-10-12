package com.ayla.pcos.reminders

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Bir alarmın planlanan ve gerçekten çaldığı an (#17: cihazda alarm güvenilirliği). */
data class AlarmLogEntry(
    val kind: ReminderKind,
    val scheduledAt: Instant,
    val firedAt: Instant,
    /** Tam zamanlı alarm mı kuruldu (yoksa 10 dk pencereli). */
    val exact: Boolean,
) {
    val delay: Duration get() = Duration.between(scheduledAt, firedAt)
}

/** Son [window] içindeki kayıtların özeti; gecikme negatifse (erken çalma) sıfır sayılır. */
data class AlarmLogSummary(val count: Int, val averageDelay: Duration, val maxDelay: Duration, val lateCount: Int)

/** Pencereli alarmın 10 dakikası normal kabul edilir; bunu aşan gecikme "geç" sayılır. */
val LATE_THRESHOLD: Duration = Duration.ofMinutes(10)

fun summarizeAlarmLog(entries: List<AlarmLogEntry>, now: Instant, window: Duration = Duration.ofHours(48)): AlarmLogSummary {
    val recent = entries.filter { !it.firedAt.isBefore(now.minus(window)) }
    if (recent.isEmpty()) return AlarmLogSummary(0, Duration.ZERO, Duration.ZERO, 0)
    val delays = recent.map { if (it.delay.isNegative) Duration.ZERO else it.delay }
    return AlarmLogSummary(
        count = recent.size,
        averageDelay = Duration.ofMillis(delays.sumOf { it.toMillis() } / delays.size),
        maxDelay = delays.max(),
        lateCount = delays.count { it > LATE_THRESHOLD },
    )
}

/**
 * Alarm günlüğü: yalnızca tür ve zaman (ilaç adı ya da kimliği yok); son [MAX_ENTRIES] kayıt cihazda,
 * uygulamanın özel alanında. Kullanıcı Ayarlar'dan görür, paylaşır ya da temizler; tam silme de temizler.
 */
@Singleton
class AlarmLog @Inject constructor(@ApplicationContext context: Context) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    @Synchronized
    fun record(entry: AlarmLogEntry) {
        val lines = (read().map { it.encode() } + entry.encode()).takeLast(MAX_ENTRIES)
        prefs.edit { putString(KEY, lines.joinToString("\n")) }
    }

    @Synchronized
    fun read(): List<AlarmLogEntry> =
        prefs.getString(KEY, null).orEmpty().lineSequence().mapNotNull { decode(it) }.toList()

    @Synchronized
    fun clear() = prefs.edit { remove(KEY) }

    private fun AlarmLogEntry.encode() = "${kind.name}|${scheduledAt.toEpochMilli()}|${firedAt.toEpochMilli()}|${if (exact) 1 else 0}"

    private fun decode(line: String): AlarmLogEntry? {
        val p = line.split("|")
        if (p.size != 4) return null
        val kind = ReminderKind.entries.firstOrNull { it.name == p[0] } ?: return null
        val scheduled = p[1].toLongOrNull() ?: return null
        val fired = p[2].toLongOrNull() ?: return null
        return AlarmLogEntry(kind, Instant.ofEpochMilli(scheduled), Instant.ofEpochMilli(fired), p[3] == "1")
    }

    companion object {
        const val MAX_ENTRIES = 200
        private const val PREFS = "ayla_alarm_log"
        private const val KEY = "entries"
    }
}
