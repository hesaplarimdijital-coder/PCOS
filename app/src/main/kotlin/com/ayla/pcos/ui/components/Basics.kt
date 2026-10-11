package com.ayla.pcos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ayla.pcos.R
import com.ayla.pcos.ui.navigation.Screen
import com.ayla.pcos.ui.theme.LocalAylaColors
import java.util.Locale

/** Türkçe büyük harf kuralı: "ilaç" → "İLAÇ". */
fun String.toTurkishUpper(): String = uppercase(Locale.forLanguageTag("tr"))

/** Bölüm üst etiketi (overline); metin Türkçe yerel ayarla büyük harfe çevrilir. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text.toTurkishUpper(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Ana kart: yüzey rengi, ince kenar, radius-lg. */
@Composable
fun AylaCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalAylaColors.current.kart,
    bordered: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (bordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Bölüm başlığı satırı: başlık solda, isteğe bağlı eylem sağda. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        action?.invoke()
    }
}

/** 40 dp yuvarlak simge zemini (liste satırlarında). */
@Composable
fun SimgeZemini(icon: ImageVector, background: Color, tint: Color, modifier: Modifier = Modifier, contentDescription: String? = null) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.size(40.dp).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Alt gezinme: beş hedef; seçili olan dolu ikon ve nar-açık göstergeyle. */
@Composable
fun AltGezinme(selected: Screen.Tab, onSelect: (Screen.Tab) -> Unit) {
    NavigationBar(containerColor = LocalAylaColors.current.kart) {
        Screen.tabs.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(isSelected),
                        contentDescription = null,
                    )
                },
                label = { GezinmeEtiketi(stringResource(tab.labelRes())) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Beş sekme 393 dp'ye sığmalı: sistem yazı boyutu %200'e çıktığında etiket [GEZINME_YAZI_SINIRI]
 * ölçeğinde durur (simge ve ekran okuyucu metni değişmez); aksi hâlde "Bugü", "Takvi" diye kesilir.
 */
@Composable
private fun GezinmeEtiketi(text: String) {
    val density = LocalDensity.current
    val capped = if (density.fontScale > GEZINME_YAZI_SINIRI) Density(density.density, GEZINME_YAZI_SINIRI) else density
    CompositionLocalProvider(LocalDensity provides capped) {
        Text(text, maxLines = 1, softWrap = false)
    }
}

private const val GEZINME_YAZI_SINIRI = 1.5f

private fun Screen.Tab.icon(selected: Boolean): ImageVector = when (this) {
    Screen.Today -> if (selected) Icons.Rounded.Today else Icons.Outlined.Today
    Screen.Calendar -> if (selected) Icons.Rounded.CalendarMonth else Icons.Outlined.CalendarMonth
    Screen.Meds -> if (selected) Icons.Rounded.Medication else Icons.Outlined.Medication
    Screen.Plan -> if (selected) Icons.Rounded.Eco else Icons.Outlined.Eco
    Screen.Me -> if (selected) Icons.Rounded.Person else Icons.Outlined.Person
}

private fun Screen.Tab.labelRes(): Int = when (this) {
    Screen.Today -> R.string.nav_today
    Screen.Calendar -> R.string.nav_calendar
    Screen.Meds -> R.string.nav_meds
    Screen.Plan -> R.string.nav_plan
    Screen.Me -> R.string.nav_me
}
