package com.ayla.pcos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import com.ayla.pcos.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayla.cycle.PillRegimen
import com.ayla.pcos.ui.theme.LocalAylaColors

/**
 * Hap paketi ızgarası: 7 sütun, her hücre bir hap. Alınan → nar dolgu, bugün → kalın nar kenar,
 * gelecek → ince kenar, plasebo → çukur zemin.
 */
@Composable
fun HapPaketi(
    regimen: PillRegimen,
    todayPill: Int,
    takenPills: Set<Int>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onPillClick: ((Int) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    val rows = (regimen.packLength + 6) / 7
    Column(
        modifier = modifier.fillMaxWidth().semantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0 until 7) {
                    val number = row * 7 + col + 1
                    if (number > regimen.packLength) {
                        Box(Modifier.weight(1f))
                        continue
                    }
                    val placebo = number > regimen.activeDays
                    val taken = number in takenPills
                    val isToday = number == todayPill
                    val label = pillLabel(number, placebo, isToday)
                    val clickable = onPillClick != null && number <= todayPill
                    val base = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .then(
                            if (clickable) {
                                Modifier.toggleable(value = taken, role = Role.Checkbox, onValueChange = { onPillClick(number) })
                            } else {
                                Modifier
                            },
                        )
                        .semantics { this.contentDescription = label }
                    val styled = when {
                        taken -> base.background(scheme.primary, CircleShape)
                        isToday -> base.border(3.dp, scheme.primary, CircleShape)
                        placebo -> base.background(colors.cukur, CircleShape).border(2.dp, scheme.outlineVariant, CircleShape)
                        else -> base.border(2.dp, scheme.outline, CircleShape)
                    }
                    Box(modifier = styled, contentAlignment = Alignment.Center) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                taken -> scheme.onPrimary
                                isToday -> scheme.primary
                                else -> scheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pillLabel(number: Int, placebo: Boolean, isToday: Boolean): String {
    val parts = mutableListOf(stringResource(R.string.today_pill_number, number))
    if (placebo) parts += stringResource(R.string.pill_placebo)
    if (isToday) parts += stringResource(R.string.label_today)
    return parts.joinToString(", ")
}
