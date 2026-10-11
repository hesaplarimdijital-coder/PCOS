package com.ayla.cycle

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Doğum kontrol hapı düzeni. Hap kullanırken doğal döngü tahmini yapılmaz; plasebo günlerindeki
 * kanama çekilme kanamasıdır.
 */
data class PillRegimen(val activeDays: Int, val placeboDays: Int) {
    init {
        require(activeDays > 0) { "activeDays > 0 olmalı" }
        require(placeboDays >= 0) { "placeboDays >= 0 olmalı" }
    }

    val packLength: Int get() = activeDays + placeboDays

    companion object {
        val R21_7 = PillRegimen(21, 7)
        val R24_4 = PillRegimen(24, 4)
        val CONTINUOUS_28 = PillRegimen(28, 0)
    }
}

/** Belirli bir gün için paketteki konum. */
data class PillStatus(
    /** 1 tabanlı; paket uzunluğuna kadar. */
    val pillNumber: Int,
    val isPlacebo: Boolean,
    /** 1 tabanlı paket sayısı (ilk paket 1). */
    val packNumber: Int,
    /** Bir sonraki paketin başlangıcına kalan gün (bugün dahil değil). */
    val daysUntilNextPack: Int,
)

/** [date] gününde paketteki konum; [packStart] ilk paketin ilk hapının günüdür. */
fun PillRegimen.statusOn(packStart: LocalDate, date: LocalDate): PillStatus {
    require(!date.isBefore(packStart)) { "Tarih paket başlangıcından önce olamaz" }
    val daysSince = ChronoUnit.DAYS.between(packStart, date).toInt()
    val index = daysSince % packLength
    return PillStatus(
        pillNumber = index + 1,
        isPlacebo = index >= activeDays,
        packNumber = daysSince / packLength + 1,
        daysUntilNextPack = packLength - index,
    )
}
