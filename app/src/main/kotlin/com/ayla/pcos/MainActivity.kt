package com.ayla.pcos

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ayla.pcos.data.prefs.UserPreferencesRepository
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.ui.AylaRoot
import com.ayla.pcos.ui.theme.AylaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tek etkinlik; tüm ekranlar Compose. FragmentActivity, uygulama kilidinin BiometricPrompt'u için gerekir.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var ticker: DateTicker
    @Inject lateinit var prefs: UserPreferencesRepository

    /** Bildirimden ya da widget'tan gelen hedef ([OPEN_TODAY], [OPEN_MEDS], "plan"). */
    private var openTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Döndürme, tema değişimi ya da süreç ölümünden dönüşte hedef yeniden uygulanmaz (yığın korunur).
        if (savedInstanceState == null) openTarget = intent?.getStringExtra(EXTRA_OPEN)

        // "Ekran görüntüsünü engelle" ayarı: son uygulamalar ve ekran görüntüsünde içerik gizlenir.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                prefs.preferences.map { it.blockScreenshots }.distinctUntilChanged().collect { block ->
                    if (block) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            AylaTheme {
                AylaRoot(
                    openTarget = openTarget,
                    onOpenTargetConsumed = {
                        openTarget = null
                        intent?.removeExtra(EXTRA_OPEN)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTarget = intent.getStringExtra(EXTRA_OPEN)
    }

    override fun onResume() {
        super.onResume()
        ticker.refresh()
    }

    companion object {
        const val EXTRA_OPEN = "com.ayla.pcos.extra.OPEN"

        /** Bildirim ve widget'ın açabileceği hedefler; kök yalnızca bunları tanır (beyaz liste). */
        const val OPEN_TODAY = "today"
        const val OPEN_MEDS = "meds"
    }
}
