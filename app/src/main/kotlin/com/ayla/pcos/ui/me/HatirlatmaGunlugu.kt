package com.ayla.pcos.ui.me

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.reminders.AlarmLogEntry
import com.ayla.pcos.reminders.AlarmLogSummary
import com.ayla.pcos.reminders.LATE_THRESHOLD
import com.ayla.pcos.reminders.ReminderKind
import com.ayla.pcos.reminders.summarizeAlarmLog
import com.ayla.pcos.ui.components.yaziOlcekli
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Diyalogda gösterilen en yeni kayıt sayısı; paylaşılan metin günlüğün tamamını içerir. */
private const val DIALOG_ENTRY_LIMIT = 50

/** Hatırlatma günlüğü metinleri (#17); diyalog ve paylaşım aynı biçimi kullanır. */
internal object AlarmLogText {
    private val tr = Locale.forLanguageTag("tr")
    private val planned = DateTimeFormatter.ofPattern("d MMM HH:mm", tr)
    private val fired = DateTimeFormatter.ofPattern("HH:mm:ss", tr)

    /** 59 sn'ye kadar saniye, sonrası tam dakika; erken çalma (saat değişimi) sıfır sayılır. */
    fun delay(resources: Resources, delay: Duration): String {
        val seconds = delay.seconds.coerceAtLeast(0)
        return if (seconds < 60) {
            resources.getString(R.string.alarm_log_delay_seconds, seconds.toInt())
        } else {
            resources.getString(R.string.alarm_log_delay_minutes, (seconds / 60).toInt())
        }
    }

    fun kind(resources: Resources, kind: ReminderKind): String = resources.getString(
        when (kind) {
            ReminderKind.DOSE -> R.string.alarm_kind_dose
            ReminderKind.DAILY_CHECK -> R.string.alarm_kind_daily_check
            ReminderKind.WATER -> R.string.alarm_kind_water
            ReminderKind.EVENING -> R.string.alarm_kind_evening
            ReminderKind.WEEKLY -> R.string.alarm_kind_weekly
        },
    )

    fun entry(resources: Resources, entry: AlarmLogEntry, zone: ZoneId): String = resources.getString(
        R.string.alarm_log_entry,
        kind(resources, entry.kind),
        planned.format(entry.scheduledAt.atZone(zone)),
        fired.format(entry.firedAt.atZone(zone)),
        delay(resources, entry.delay),
        resources.getString(if (entry.exact) R.string.alarm_log_exact else R.string.alarm_log_window),
    )

    fun summary(resources: Resources, summary: AlarmLogSummary): List<String> {
        if (summary.count == 0) return listOf(resources.getString(R.string.alarm_log_summary_none))
        return listOf(
            resources.getString(
                R.string.alarm_log_summary,
                summary.count,
                delay(resources, summary.averageDelay),
                delay(resources, summary.maxDelay),
            ),
            if (summary.lateCount > 0) {
                resources.getString(R.string.alarm_log_late, summary.lateCount)
            } else {
                resources.getString(R.string.alarm_log_on_time)
            },
        )
    }

    /** Paylaşım metni: başlık, cihaz ve izin satırları, 48 saatlik özet, ardından en yeniden eskiye kayıtlar. */
    fun shareText(resources: Resources, entries: List<AlarmLogEntry>, deviceLines: List<String>, now: Instant, zone: ZoneId): String =
        buildList {
            add(resources.getString(R.string.alarm_log_share_subject))
            addAll(deviceLines)
            add("")
            addAll(summary(resources, summarizeAlarmLog(entries, now)))
            add("")
            entries.asReversed().forEach { e ->
                val line = entry(resources, e, zone)
                add(if (e.delay > LATE_THRESHOLD) resources.getString(R.string.alarm_log_late_marker, line) else line)
            }
        }.joinToString("\n").trimEnd()
}

/**
 * Hatırlatma günlüğü diyaloğu: son 48 saatin özeti ve en yeni kayıtlar. Paylaşım kullanıcının seçtiği
 * uygulamaya gider; günlükte ilaç adı yoktur.
 */
@Composable
internal fun HatirlatmaGunluguDialog(
    entries: List<AlarmLogEntry>,
    onShare: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    now: Instant = remember { Instant.now() },
    zone: ZoneId = remember { ZoneId.systemDefault() },
) {
    val resources = LocalResources.current
    val summary = remember(entries, now) { summarizeAlarmLog(entries, now) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
        title = { Text(stringResource(R.string.alarm_log_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (entries.isEmpty()) {
                    Text(stringResource(R.string.alarm_log_empty), style = MaterialTheme.typography.bodyMedium)
                } else {
                    AlarmLogText.summary(resources, summary).forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                    entries.asReversed().take(DIALOG_ENTRY_LIMIT).forEach { entry -> GunlukSatiri(entry, zone) }
                    HorizontalDivider()
                }
                Text(
                    stringResource(R.string.alarm_log_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entries.isNotEmpty()) {
                    TextButton(onClick = onClear, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.alarm_log_clear), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (entries.isNotEmpty()) {
                TextButton(onClick = onShare) { Text(stringResource(R.string.alarm_log_share)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/** Günlük satırı; 10 dakikayı aşan gecikme simge ve kalın yazıyla işaretlenir (renk tek başına değil). */
@Composable
private fun GunlukSatiri(entry: AlarmLogEntry, zone: ZoneId) {
    val resources = LocalResources.current
    val late = entry.delay > LATE_THRESHOLD
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        if (late) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = stringResource(R.string.alarm_log_late_a11y),
                tint = LocalAylaColors.current.uyari,
                modifier = Modifier.size(yaziOlcekli(16.dp)),
            )
        }
        Text(
            AlarmLogText.entry(resources, entry, zone),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (late) FontWeight.SemiBold else null,
            color = if (late) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
