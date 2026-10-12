package com.ayla.pcos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.ui.calendar.CalendarScreen
import com.ayla.pcos.ui.components.AltGezinme
import com.ayla.pcos.ui.log.DailyLogScreen
import com.ayla.pcos.ui.me.GizlilikPolitikasiDialog
import com.ayla.pcos.ui.me.GizlilikPolitikasiEkrani
import com.ayla.pcos.ui.me.MeScreen
import com.ayla.pcos.ui.me.SettingsScreen
import com.ayla.pcos.ui.meds.AddMedicationScreen
import com.ayla.pcos.ui.meds.EditMedicationScreen
import com.ayla.pcos.ui.meds.MedsScreen
import com.ayla.pcos.ui.navigation.AylaNavigator
import com.ayla.pcos.ui.navigation.Screen
import com.ayla.pcos.ui.navigation.rememberAylaNavigator
import com.ayla.pcos.ui.onboarding.OnboardingScreen
import com.ayla.pcos.ui.plan.PlanScreen
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.today.TodayScreen

/**
 * Kök: yükleniyor → karşılama → (kilit) → sekmeler. Bildirimden gelen [openTarget] ilgili sekmeyi açar.
 */
@Composable
fun AylaRoot(
    openTarget: String?,
    onOpenTargetConsumed: () -> Unit,
    viewModel: RootViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    // Anahtar kaybı diyaloğunda "Yedekten geri yükle" seçildi; ana kabuk açılınca Ayarlar'a gidilir.
    var restoreRequested by rememberSaveable { mutableStateOf(false) }

    // Arka plana gidip gelince kilidi yeniden değerlendir.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onBackgrounded(System.currentTimeMillis())
                Lifecycle.Event.ON_START -> viewModel.onForegrounded(System.currentTimeMillis())
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        state.loading -> Box(Modifier.fillMaxSize())
        !state.onboardingDone -> OnboardingScreen()
        state.locked -> AppLockScreen(onUnlocked = viewModel::onUnlocked)
        else -> MainShell(
            openTarget = openTarget,
            onOpenTargetConsumed = onOpenTargetConsumed,
            restoreRequested = restoreRequested,
            onRestoreOpened = { restoreRequested = false },
        )
    }

    // Rızası kaydedilmemiş kurulumlar (bu sürümden önce karşılamayı bitirenler) bir kez açık rıza verir.
    var showPolicy by rememberSaveable { mutableStateOf(false) }
    if (!state.loading && state.needsHealthConsent && !state.dataResetNotice) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.consent_dialog_title)) },
            text = { Text(stringResource(R.string.consent_dialog_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::giveHealthDataConsent) { Text(stringResource(R.string.consent_dialog_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showPolicy = true }) { Text(stringResource(R.string.privacy_policy_read)) }
            },
        )
    }
    if (showPolicy) GizlilikPolitikasiDialog(onDismiss = { showPolicy = false })

    if (!state.loading && state.dataResetNotice) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.data_reset_title)) },
            text = { Text(stringResource(R.string.data_reset_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::acknowledgeDataReset) { Text(stringResource(R.string.action_understood)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.acknowledgeDataReset()
                    restoreRequested = true
                }) { Text(stringResource(R.string.data_reset_restore)) }
            },
        )
    }
}

/**
 * [openTarget] dışarıdan (bildirim, başlatıcı) gelebilir; yalnızca sekme seçer. Yedek geri yükleme
 * ise yalnızca uygulama içinden ([restoreRequested]) açılır: başka bir uygulamanın gönderdiği bir intent
 * dosya seçiciyi açtıramaz.
 */
@Composable
private fun MainShell(openTarget: String?, onOpenTargetConsumed: () -> Unit, restoreRequested: Boolean, onRestoreOpened: () -> Unit) {
    val navigator = rememberAylaNavigator()

    LaunchedEffect(restoreRequested) {
        if (restoreRequested) {
            navigator.push(Screen.RestoreBackup)
            onRestoreOpened()
        }
    }

    LaunchedEffect(openTarget) {
        when (openTarget) {
            "today" -> navigator.selectTab(Screen.Today)
            "meds" -> navigator.selectTab(Screen.Meds)
            "plan" -> navigator.selectTab(Screen.Plan)
            "calendar" -> navigator.selectTab(Screen.Calendar)
            "me" -> navigator.selectTab(Screen.Me)
        }
        if (openTarget != null) onOpenTargetConsumed()
    }

    BackHandler(enabled = navigator.canGoBack) { navigator.pop() }

    Scaffold(
        bottomBar = {
            if (navigator.showsBottomBar) {
                AltGezinme(selected = navigator.tab, onSelect = navigator::selectTab)
            }
        },
    ) { padding ->
        val current = navigator.current
        AnimatedContent(
            targetState = current,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = {
                val pushing = targetState !is Screen.Tab && initialState is Screen.Tab
                val popping = targetState is Screen.Tab && initialState !is Screen.Tab
                when {
                    pushing -> slideInHorizontally(AylaMotion.enter()) { it / 6 } + fadeIn(tween(220)) togetherWith
                        fadeOut(tween(160))
                    popping -> fadeIn(tween(220)) togetherWith
                        slideOutHorizontally(tween(260)) { it / 6 } + fadeOut(tween(160))
                    else -> fadeIn(tween(240)) togetherWith fadeOut(tween(160))
                }
            },
            label = "screen",
        ) { screen ->
            // Android 17 büyük ekranlarda yönlendirme/boyut kısıtlarını yok sayar: içerik ortalanır,
            // 720 dp'den geniş olmaz; telefonlarda etkisizdir.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                    ScreenHost(screen, navigator)
                }
            }
        }
    }
}

@Composable
private fun ScreenHost(screen: Screen, navigator: AylaNavigator) {
    when (screen) {
        Screen.Today -> TodayScreen(
            onOpenLog = { date -> navigator.push(Screen.DailyLog(date)) },
            onOpenMeds = { navigator.selectTab(Screen.Meds) },
            onOpenSettings = { navigator.push(Screen.Settings) },
        )
        Screen.Calendar -> CalendarScreen(onOpenLog = { date -> navigator.push(Screen.DailyLog(date)) })
        Screen.Meds -> MedsScreen(
            onAdd = { navigator.push(Screen.AddMedication) },
            onEdit = { id -> navigator.push(Screen.EditMedication(id)) },
        )
        Screen.Plan -> PlanScreen()
        Screen.Me -> MeScreen(onOpenSettings = { navigator.push(Screen.Settings) })
        is Screen.DailyLog -> DailyLogScreen(date = screen.date, onClose = { navigator.pop() })
        Screen.AddMedication -> AddMedicationScreen(
            onDone = { navigator.pop() },
            onCustom = { navigator.replaceTop(Screen.EditMedication(0)) },
        )
        is Screen.EditMedication -> EditMedicationScreen(id = screen.id, onDone = { navigator.pop() })
        Screen.Settings -> SettingsScreen(onBack = { navigator.pop() }, onOpenPrivacyPolicy = { navigator.push(Screen.PrivacyPolicy) })
        Screen.RestoreBackup -> SettingsScreen(
            onBack = { navigator.pop() },
            autoStartImport = true,
            onOpenPrivacyPolicy = { navigator.push(Screen.PrivacyPolicy) },
        )
        Screen.PrivacyPolicy -> GizlilikPolitikasiEkrani(onBack = { navigator.pop() })
    }
}
