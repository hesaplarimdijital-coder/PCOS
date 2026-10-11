package com.ayla.pcos

import android.app.Application
import com.ayla.pcos.di.ApplicationScope
import com.ayla.pcos.reminders.NotificationChannels
import com.ayla.pcos.reminders.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AylaApp : Application() {

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        // Uygulama her başladığında alarmlar tazelenir; ucuz ve yeniden başlatma sonrası güvenli.
        appScope.launch { runCatching { scheduler.rescheduleAll() } }
    }
}
