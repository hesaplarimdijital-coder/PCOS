package com.ayla.pcos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val saatBicimi: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.saatMetni(): String = format(saatBicimi)

/**
 * 24 saatlik saat seçici diyaloğu. Ayarlar, ilaç saatleri ve uyku kaydında ortak kullanılır.
 */
@Composable
fun SaatSeciciDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    confirmLabel: String = stringResource(R.string.action_save),
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Ayar satırı: başlık (ve isteğe bağlı açıklama) solda, değer ve ok sağda; satırın tamamı dokunulabilir.
 */
@Composable
fun DegerSatiri(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val labels: @Composable () -> Unit = {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant)
        if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
    val valueAndChevron: @Composable () -> Unit = {
        Text(value, style = MaterialTheme.typography.titleMedium, color = if (enabled) scheme.primary else scheme.onSurfaceVariant)
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
    val rowModifier = modifier
        .fillMaxWidth()
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(vertical = 12.dp)
    if (LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI) {
        // Büyük yazıda başlık tam genişlikte, değer altında: başlık harf harf kırılmaz.
        Column(modifier = rowModifier) {
            labels()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { valueAndChevron() }
        }
    } else {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) { labels() }
            valueAndChevron()
        }
    }
}

/**
 * Bölümlü düğme (SegmentedButton) simgesi. Büyük yazıda onay simgesi etikete yer bıraksın diye gizlenir;
 * seçim o zaman [SegmentEtiketi]'nin kalın yazısıyla da görünür (renk tek başına kalmaz).
 */
@Composable
fun segmentSimgesi(selected: Boolean): @Composable () -> Unit =
    if (LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI) {
        {}
    } else {
        { SegmentedButtonDefaults.Icon(active = selected) }
    }

/**
 * Seçim çipi etiketi. Dikey boşluk normal boyda çip yüksekliğini değiştirmez (20 + 12 = 32 dp); büyük
 * yazıda iki satıra kırılan etiket çipin kenarına yapışmaz.
 */
@Composable
fun CipEtiketi(text: String) {
    Text(text, modifier = Modifier.padding(vertical = 6.dp))
}

/** Bölümlü düğme etiketi: büyük yazıda iki satıra kadar kırılır ve seçiliyse kalınlaşır. */
@Composable
fun SegmentEtiketi(text: String, selected: Boolean) {
    val buyuk = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI
    Text(
        text,
        maxLines = if (buyuk) 2 else 1,
        textAlign = TextAlign.Center,
        fontWeight = if (buyuk && selected) FontWeight.Bold else null,
    )
}

/**
 * Sayı ayarı: − / + düğmeleri ve ortada değer. Sınırlarda ilgili düğme pasifleşir; ekran okuyucu
 * değeri "durum" olarak duyurur.
 */
@Composable
fun AdimSayaci(
    title: String,
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val labels: @Composable () -> Unit = {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (text != null) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val stepper: @Composable () -> Unit = {
        FilledTonalIconButton(onClick = onDecrease, enabled = canDecrease) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.action_decrease))
        }
        Text(
            valueText,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        FilledTonalIconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_increase))
        }
    }
    val rowModifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .semantics(mergeDescendants = false) { stateDescription = valueText }
    if (LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI) {
        // Büyük yazıda düğmeler başlığın altına iner; başlık ve açıklama tam genişliği kullanır.
        Column(modifier = rowModifier) {
            labels()
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { stepper() }
        }
    } else {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) { labels() }
            stepper()
        }
    }
}

/**
 * Tarih seçici diyaloğu; [latest] sonrasındaki günler seçilemez (adet ve paket başlangıcı gelecekte olamaz).
 * DatePicker UTC milisaniye ile çalışır; dönüşüm burada yapılır.
 */
@Composable
fun TarihSeciciDialog(
    title: String,
    initial: LocalDate,
    latest: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val latestMillis = latest.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = minOf(initial, latest).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= latestMillis
            override fun isSelectableYear(year: Int): Boolean = year <= latest.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                if (picked != null && !picked.isAfter(latest)) onConfirm(picked) else onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(
            state = state,
            title = { Text(title, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
        )
    }
}
