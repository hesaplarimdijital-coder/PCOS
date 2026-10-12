package com.ayla.pcos.ekran

import com.ayla.cycle.CervicalMucus
import com.ayla.cycle.FertilityEngine
import com.ayla.cycle.FertilityEntry
import com.ayla.cycle.FertilityOverview
import com.ayla.cycle.LhResult
import com.ayla.pcos.ui.fertility.FertilityUiState
import com.ayla.pcos.ui.log.FertilityInput
import java.time.LocalTime

/** Doğurganlık ekran görüntüleri için örnek veri (#24): iki döngü, ikincisinde ısı yükselmesi ve LH pozitif. */
internal object OrnekDogurganlik {
    private val bugun = Ornek.bugun
    private val oncekiBaslangic = bugun.minusDays(52)
    private val baslangic = bugun.minusDays(22)

    private val olcumler: List<FertilityEntry> = buildList {
        // Önceki döngü: düz, birkaç ölçüm (yükselme yok).
        listOf(36.35, 36.4, 36.3, 36.45, 36.4, 36.35, 36.4).forEachIndexed { i, t ->
            add(FertilityEntry(oncekiBaslangic.plusDays(4L + i * 3), bbtCelsius = t))
        }
        // Güncel döngü: 6.–21. günler; 13. günde LH pozitif, 15. günden itibaren yükselme; 10. gün bozuk.
        val temps = listOf(36.30, 36.35, 36.25, 36.30, 36.70, 36.35, 36.30, 36.40, 36.35, 36.55, 36.65, 36.75, 36.70, 36.80, 36.75, 36.70)
        temps.forEachIndexed { i, t ->
            val day = 5L + i
            add(
                FertilityEntry(
                    date = baslangic.plusDays(day),
                    bbtCelsius = t,
                    bbtTime = LocalTime.of(6, 40),
                    bbtDisturbed = day == 9L,
                    lh = when (day) {
                        11L -> LhResult.NEGATIVE
                        12L -> LhResult.POSITIVE
                        else -> null
                    },
                    mucus = if (day in 10L..12L) CervicalMucus.EGG_WHITE else null,
                ),
            )
        }
    }

    private val genelBakis = FertilityEngine.analyze(olcumler, listOf(oncekiBaslangic, baslangic), bugun) as FertilityOverview.Enabled

    val dongu = FertilityUiState(loaded = true, today = bugun, ttcGoal = true, cycles = genelBakis.cycles, selected = genelBakis.cycles.lastIndex)

    val hap = FertilityUiState(loaded = true, today = bugun, ttcGoal = true, disabled = true)

    val bos = FertilityUiState(loaded = true, today = bugun, ttcGoal = false, cycles = emptyList(), selected = -1)

    val gunlukKayit = Ornek.gunlukKayit.copy(
        fertility = FertilityInput(bbtText = "36,55", bbtTime = LocalTime.of(6, 40), bbtDisturbed = false, lh = LhResult.NEGATIVE, mucus = CervicalMucus.CREAMY),
    )
}
