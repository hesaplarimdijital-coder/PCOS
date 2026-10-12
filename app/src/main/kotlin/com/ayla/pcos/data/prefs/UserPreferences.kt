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
    /**
     * Health Connect'ten uyku okumaya verilen açık rıza (gerekçe ekranında "Onaylıyorum"); bağlantı kesilince
     * silinir. Rıza olmadan, Health Connect izni verilmiş olsa bile hiçbir şey okunmaz (#11).
     */
    val healthConnectConsentAt: Instant? = null,

    /** Cilt/saç fotoğrafları için ayrı, geri alınabilir açık rıza (ADR-0010); geri alma = fotoğrafların silinmesi. */
    val photoConsentAt: Instant? = null,
    /** Health Connect izin penceresinin art arda kapatılma sayısı; 2'de pencere bir daha açılmaz. */
    val healthConnectPermissionCancels: Int = 0,
    /**
     * Ayla'dan silinen Health Connect gecelerinin kayıt kimlikleri (yalnızca kimlik, saat yok): sonraki almada
     * yeniden eklenmesinler diye tutulur; kayıt alma penceresinden çıkınca listeden düşer.
     */
    val healthConnectHiddenIds: Set<String> = emptySet(),

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
    /** İlaçlar ekranındaki pil tasarrufu bandı kapatıldı; Ayarlar'daki satır her zaman görünür. */
    val batteryBandDismissed: Boolean = false,
)
