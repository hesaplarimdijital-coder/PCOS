package com.ayla.pcos

import android.app.Application
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

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    /** Widget varsa uygulama içi değişiklikleri ona taşır; veritabanı yalnızca gerekince çözülür. */
    @Inject lateinit var widgetSync: dagger.Lazy<WidgetSync>

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        // Uygulama her başladığında alarmlar tazelenir; ucuz ve yeniden başlatma sonrası güvenli.
        appScope.launch { runCatching { scheduler.rescheduleAll() } }
        appScope.launch { if (WidgetUpdater.hasWidgets(this@AylaApp)) widgetSync.get().start() }
    }
}
