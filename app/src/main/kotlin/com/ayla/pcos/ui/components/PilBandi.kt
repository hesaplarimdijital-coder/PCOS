package com.ayla.pcos.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ayla.pcos.R

/**
 * Pil kısıtı bandı (#17). Arka plan kısıtlıysa DİKKAT bandı çıkar ve kapatılamaz (Android bu durumda
 * arka planda alarm çalıştırmaz); pil optimizasyonu açıksa BİLGİ bandı çıkar ve [onDismiss] verilirse
 * kapatılabilir. Ayla muafsa hiçbir şey çizilmez. [oemNote] üreticiye özel ek yönlendirmedir.
 */
@Composable
fun PilBandi(
    status: BatteryStatus,
    @StringRes oemNote: Int?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    if (status == BatteryStatus.UNRESTRICTED) return
    val restricted = status == BatteryStatus.BACKGROUND_RESTRICTED
    val text = stringResource(if (restricted) R.string.battery_restricted_text else R.string.battery_band_text)
    val note = oemNote?.let { stringResource(it) }
    val dismiss = onDismiss.takeIf { !restricted }
    UyariBandi(
        tur = if (restricted) BantTuru.DIKKAT else BantTuru.BILGI,
        title = stringResource(if (restricted) R.string.battery_restricted_title else R.string.battery_band_title),
        text = if (note != null) "$text\n\n$note" else text,
        modifier = modifier,
        actionLabel = stringResource(R.string.battery_open_settings),
        onAction = onOpenSettings,
        dismissLabel = dismiss?.let { stringResource(R.string.action_understood) },
        onDismiss = dismiss,
    )
}
