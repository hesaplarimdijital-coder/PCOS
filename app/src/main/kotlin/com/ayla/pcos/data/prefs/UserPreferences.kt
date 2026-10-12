package com.ayla.pcos.data.prefs

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Kullanıcının karşılamada seçtiği öncelikler. */
enum class Goal { CYCLE, SYMPTOMS, TTC, PILL }

/** Tek kaynaklı kullanıcı ayarları; DataStore'da saklanır. */
data class UserPreferences(
    val onboardingDone: Boolean = false,
    val displayName: String = "",
    val goals: Set<Goal> = emptySet(),
    /** Kayıt tutmaya başlanan gün; seyrek adet işareti için gerekir. */
    val trackingSince: LocalDate? = null,

    // Rıza (KVKK). Sağlık verisinin cihazda işlenmesine açık rıza karşılamada ayrı bir onayla alınır ve
    // zamanı saklanır. Kişiselleştirme isteğe bağlıdır ve varsayılan kapalıdır (önceden işaretli rıza yok).
    // Analitik ya da araştırma işlemesi olmadığı için onlara ait rıza anahtarı da yoktur.
    val healthDataConsentAt: Instant? = null,
    val consentPersonalization: Boolean = false,

    // Su
    val waterGoalCups: Int = 8,
    val cupMl: Int = 250,
    val waterRemindersEnabled: Boolean = true,
    val waterIntervalMinutes: Int = 120,
    val activeHoursStart: LocalTime = LocalTime.of(9, 0),
    val activeHoursEnd: LocalTime = LocalTime.of(21, 0),

    // Bildirim davranışı
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 30),
    val quietHoursEnd: LocalTime = LocalTime.of(8, 0),
    val hideNotificationContent: Boolean = false,
    val cycleNotificationsEnabled: Boolean = true,
    val progressNotificationsEnabled: Boolean = true,
    val eveningRoutineEnabled: Boolean = false,
    val eveningRoutineTime: LocalTime = LocalTime.of(22, 30),
    val dailyCheckTime: LocalTime = LocalTime.of(9, 0),

    // Gizlilik
    val appLockEnabled: Boolean = false,
    val blockScreenshots: Boolean = true,

    // Görünüm
    val hidePrediction: Boolean = false,
    val showBodyMeasurements: Boolean = false,

    // Uyarı tekrarını önlemek için
    val lastLongCycleAlertKey: String = "",
    /** "Bildirimler kapalı" bandı kapatıldı; bildirimler yeniden açılınca sıfırlanır. */
    val notificationsBandDismissed: Boolean = false,
)
