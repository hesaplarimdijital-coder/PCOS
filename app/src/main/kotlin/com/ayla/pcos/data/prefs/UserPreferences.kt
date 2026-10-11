package com.ayla.pcos.data.prefs

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

    // Rıza (KVKK): temel işlev her zaman açık, diğerleri varsayılan kapalı
    val consentPersonalization: Boolean = true,
    val consentAnalytics: Boolean = false,
    val consentResearch: Boolean = false,

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
