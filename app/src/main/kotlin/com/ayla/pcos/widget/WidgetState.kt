package com.ayla.pcos.widget

import com.ayla.cycle.CycleOverview
import com.ayla.cycle.PillRegimen
import com.ayla.cycle.statusOn
import com.ayla.pcos.data.db.DoseStatus
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.prefs.UserPreferences
import com.ayla.pcos.data.repo.DoseItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Ana ekran widget'ının gösterdiği durum (#14). Widget uygulama kilidinin ve ekran görüntüsü engelinin
 * dışında, başlatıcının içinde çizilir; bu yüzden gizlilik ayarı açıkken yalnızca nötr içerik gösterilir.
 */
sealed interface WidgetState {
    /** Karşılama bitmemiş, uygulama kilidi ya da gizli bildirimler açık: yalnızca "Ayla" ve "dokun". */
    data object Neutral : WidgetState

    data class Ready(
        val headline: WidgetHeadline,
        /** Bugünün henüz işaretlenmemiş en erken dozu (saati geçmiş olabilir). */
        val nextDose: NextDose?,
        /** Bugün planlı doz var mı (yoksa "Bugün doz yok", varsa ve hepsi işaretliyse "Dozlar tamam"). */
        val hasDosesToday: Boolean,
        val waterCups: Int,
        val waterGoal: Int,
    ) : WidgetState
}

/** Başlık: hap kullanıcısında paket günü (döngü tahmini yapılmaz), değilse döngü günü. */
sealed interface WidgetHeadline {
    data class CycleDay(val day: Int) : WidgetHeadline

    data class PillDay(val pill: Int, val placebo: Boolean) : WidgetHeadline

    /** Adet kaydı yok. */
    data object NoRecord : WidgetHeadline
}

data class NextDose(
    val medicationId: Long,
    val name: String,
    val date: LocalDate,
    val time: LocalTime,
    /** Saati geçti ve hâlâ işaretlenmedi. */
    val overdue: Boolean,
)

/**
 * Durumu kurallardan üretir (saf; testli). Widget'ta tahmin, 60/90. gün uyarıları ve kilo/bel hiç
 * gösterilmez; yalnızca bugünün günü, sıradaki doz ve su.
 */
fun buildWidgetState(
    prefs: UserPreferences,
    overview: CycleOverview?,
    activeMedications: List<MedicationEntity>,
    todayDoses: List<DoseItem>,
    waterCups: Int,
    now: LocalDateTime,
): WidgetState {
    if (!prefs.onboardingDone || prefs.healthDataConsentAt == null) return WidgetState.Neutral
    if (prefs.appLockEnabled || prefs.hideNotificationContent) return WidgetState.Neutral

    val today = now.toLocalDate()
    val pill = activeMedications.firstOrNull { med ->
        val start = med.packStartDate
        med.kind == MedicationKind.CONTRACEPTIVE && start != null && med.pillActiveDays != null && !today.isBefore(start)
    }
    val headline = if (pill != null) {
        val status = PillRegimen(pill.pillActiveDays!!, pill.pillPlaceboDays ?: 0).statusOn(pill.packStartDate!!, today)
        WidgetHeadline.PillDay(status.pillNumber, status.isPlacebo)
    } else {
        overview?.currentCycleDay?.let { WidgetHeadline.CycleDay(it) } ?: WidgetHeadline.NoRecord
    }
    val next = todayDoses
        .filter { it.status == DoseStatus.PENDING }
        .minByOrNull { it.time }
        ?.let { dose ->
            NextDose(
                medicationId = dose.medication.id,
                name = dose.medication.name,
                date = dose.date,
                time = dose.time,
                overdue = dose.time.isBefore(now.toLocalTime()),
            )
        }
    return WidgetState.Ready(
        headline = headline,
        nextDose = next,
        hasDosesToday = todayDoses.isNotEmpty(),
        waterCups = waterCups,
        waterGoal = prefs.waterGoalCups,
    )
}
