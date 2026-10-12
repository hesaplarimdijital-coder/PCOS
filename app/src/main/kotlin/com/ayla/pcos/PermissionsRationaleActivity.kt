package com.ayla.pcos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ayla.pcos.ui.me.GizlilikPolitikasiIcerik
import com.ayla.pcos.ui.theme.AylaTheme

/**
 * Health Connect izin ekranındaki "Gizlilik politikası" bağlantısı buraya gelir: Android 13 ve altında
 * `ACTION_SHOW_PERMISSIONS_RATIONALE`, Android 14 ve üstünde `VIEW_PERMISSION_USAGE` takma adıyla (#11).
 * Play Console'daki politikanın aynısını gösterir. Yalnızca herkese açık metindir: veritabanına, Keystore'a
 * ve Hilt'e dokunmaz, gelen niyetten veri okumaz; uygulama kilidi ve ekran görüntüsü engeli gerekmez.
 * MainActivity kullanılmaz, çünkü `singleTask` ve kilit ekranı politikanın önüne geçerdi.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AylaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.safeDrawingPadding()) {
                        GizlilikPolitikasiIcerik(onClose = { finish() }, closeIsBack = false)
                    }
                }
            }
        }
    }
}
