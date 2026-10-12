package com.ayla.pcos

import android.app.Application
import com.ayla.pcos.data.photos.PhotoStore
import com.ayla.pcos.di.ApplicationScope
import com.ayla.pcos.reminders.NotificationChannels
import com.ayla.pcos.reminders.ReminderScheduler
import com.ayla.pcos.widget.WidgetSync
import com.ayla.pcos.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AylaApp : Application() {

    /** Zamanlayıcı veritabanını gerektirir (SQLCipher + Keystore); arka planda çözülür. */
    @Inject lateinit var scheduler: dagger.Lazy<ReminderScheduler>
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    /** Widget varsa uygulama içi değişiklikleri ona taşır; veritabanı yalnızca gerekince çözülür. */
    @Inject lateinit var widgetSync: dagger.Lazy<WidgetSync>

    /** Kamera için açılmış geçici düz dosyalar (ADR-0010); anahtara dokunmadan temizlenir. */
    @Inject lateinit var photoStore: dagger.Lazy<PhotoStore>

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        // Uygulama her başladığında alarmlar tazelenir; ucuz ve yeniden başlatma sonrası güvenli.
        appScope.launch { runCatching { scheduler.get().rescheduleAll() } }
        appScope.launch { if (WidgetUpdater.hasWidgets(this@AylaApp)) widgetSync.get().start(refreshNow = true) }
        appScope.launch { runCatching { photoStore.get().cleanupTemp() } }
    }
}
