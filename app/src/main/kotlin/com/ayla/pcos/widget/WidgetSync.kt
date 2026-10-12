package com.ayla.pcos.widget

import android.content.Context
import com.ayla.pcos.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulama içindeki yazmaları (doz, adet, su, ayar, tam silme, yedekten geri yükleme) widget'a taşır: durum
 * değişince kısa bir birleştirmeden sonra tazeleme ister. Yalnızca widget varken başlatılır.
 */
@Singleton
class WidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val source: WidgetStateSource,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            source.observe()
                .distinctUntilChanged()
                .drop(1)
                .debounce(DEBOUNCE_MS)
                .catch { started.set(false) }
                .collect { WidgetUpdater.requestUpdate(context) }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
