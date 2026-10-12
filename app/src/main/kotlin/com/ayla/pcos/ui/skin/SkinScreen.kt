package com.ayla.pcos.ui.skin

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ayla.pcos.R
import com.ayla.pcos.data.db.AcneSelfReport
import com.ayla.pcos.data.db.RoutineSlot
import com.ayla.pcos.data.db.SkinProfileEntity
import com.ayla.pcos.data.db.SkinType
import com.ayla.pcos.data.photos.PhotoKind
import com.ayla.pcos.data.photos.PhotoRecord
import com.ayla.pcos.data.photos.PhotoStore
import com.ayla.pcos.data.skin.RoutineNote
import com.ayla.pcos.data.skin.RoutineStep
import com.ayla.pcos.report.DoctorReportBuilder
import com.ayla.pcos.ui.components.AylaCard
import com.ayla.pcos.ui.components.BUYUK_YAZI_ESIGI
import com.ayla.pcos.ui.components.BantTuru
import com.ayla.pcos.ui.components.CipEtiketi
import com.ayla.pcos.ui.components.EkranBasligi
import com.ayla.pcos.ui.components.UyariBandi
import com.ayla.pcos.ui.plan.BaslikVeKanit
import com.ayla.pcos.ui.plan.BilgiKarti
import com.ayla.pcos.ui.plan.BolumBasligi
import com.ayla.pcos.ui.plan.IcerikKarti
import com.ayla.pcos.ui.plan.KaynakSatiri
import com.ayla.pcos.ui.theme.AylaExpressiveShapes
import com.ayla.pcos.ui.theme.AylaMotion
import com.ayla.pcos.ui.theme.LocalAylaColors
import com.ayla.pcos.ui.theme.LocalReducedMotion
import com.ayla.pcos.ui.theme.hareket
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ekranın geri çağırmaları; ekran görüntüleri varsayılan boş değerlerle çizer. */
data class SkinActions(
    val onBack: () -> Unit = {},
    val onProfile: ((SkinProfileEntity) -> SkinProfileEntity) -> Unit = {},
    val onToggleStep: (RoutineSlot, String) -> Unit = { _, _ -> },
    val onGiveConsent: () -> Unit = {},
    val onRevokeConsent: () -> Unit = {},
    val onOpenPrivacyPolicy: () -> Unit = {},
    val onTakePhoto: (PhotoKind, String?) -> Unit = { _, _ -> },
    val onPickPhoto: (PhotoKind, String?) -> Unit = { _, _ -> },
    val onDeletePhoto: (String) -> Unit = {},
    val onExportPhoto: (PhotoRecord) -> Unit = {},
    val onExportAll: () -> Unit = {},
    val onResetPhotos: () -> Unit = {},
)

/** Cilt ve saç (#25): rutin, profil, "ne zaman doktora" ve şifreli fotoğraflar. Tanı ya da tedavi değildir. */
@Composable
fun SkinScreen(onBack: () -> Unit, onOpenPrivacyPolicy: () -> Unit, viewModel: SkinViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Kamera hedefi ve seçimler süreç ölümüne dayanır: kamera öndeyken uygulama kapanabilir (ADR-0010).
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingKind by rememberSaveable { mutableStateOf(PhotoKind.SKIN.name) }
    var pendingArea by rememberSaveable { mutableStateOf<String?>(null) }
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capture = pendingId?.let { viewModel.capture(it) }
        if (capture != null) viewModel.saveCapture(capture, success, PhotoKind.valueOf(pendingKind), pendingArea)
        pendingId = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.savePicked(uri, PhotoKind.valueOf(pendingKind), pendingArea)
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri: Uri? ->
        val id = exportId
        if (uri != null && id != null) viewModel.exportPhoto(uri, id)
        exportId = null
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) viewModel.exportAll(uri)
    }

    if (!state.loaded) return
    SkinContent(
        state = state,
        actions = SkinActions(
            onBack = onBack,
            onProfile = viewModel::updateProfile,
            onToggleStep = viewModel::toggleStep,
            onGiveConsent = viewModel::giveConsent,
            onRevokeConsent = viewModel::revokeConsent,
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onTakePhoto = { kind, area ->
                val target = viewModel.cameraTarget()
                pendingId = target.id
                pendingKind = kind.name
                pendingArea = area
                try {
                    camera.launch(target.uri)
                } catch (e: ActivityNotFoundException) {
                    pendingId = null
                    viewModel.cameraUnavailable(target)
                }
            },
            onPickPhoto = { kind, area ->
                pendingKind = kind.name
                pendingArea = area
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDeletePhoto = viewModel::deletePhoto,
            onExportPhoto = { record ->
                exportId = record.id
                exporter.launch(PhotoStore.suggestedFileName(record))
            },
            onExportAll = { runCatching { folderPicker.launch(null) } },
            onResetPhotos = viewModel::resetPhotos,
        ),
        loadThumbnail = { record, px -> viewModel.thumbnail(record, px)?.asImageBitmap() },
    )
}

private val tarih: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr"))

/** Durumsuz içerik; ekran görüntüsü testleri doğrudan bunu çizer. */
@Composable
internal fun SkinContent(
    state: SkinUiState,
    actions: SkinActions,
    loadThumbnail: suspend (PhotoRecord, Int) -> ImageBitmap?,
    initialTab: Int = 0,
    initialCompare: Set<String> = emptySet(),
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val reduced = LocalReducedMotion.current
    val buyukYazi = LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI

    Column(modifier = Modifier.fillMaxSize()) {
        EkranBasligi(stringResource(R.string.skin_title), actions.onBack)
        val tabs: @Composable () -> Unit = {
            listOf(R.string.skin_tab_routine, R.string.skin_tab_profile, R.string.skin_tab_doctor, R.string.skin_tab_photos).forEachIndexed { index, res ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(stringResource(res), maxLines = 1, softWrap = false) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (buyukYazi) {
            PrimaryScrollableTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface, edgePadding = 20.dp, tabs = tabs)
        } else {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface, tabs = tabs)
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (reduced) EnterTransition.None togetherWith ExitTransition.None else fadeIn(tween(220)) togetherWith fadeOut(tween(140))
            },
            modifier = Modifier.weight(1f),
            label = "skinTab",
        ) { current ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BilgiNotu(stringResource(R.string.skin_review_pending))
                when (current) {
                    0 -> RutinSekmesi(state, actions, onOpenProfile = { tab = 1 }, onOpenDoctor = { tab = 2 })
                    1 -> ProfilSekmesi(state, actions)
                    2 -> DoktorSekmesi(state)
                    else -> FotografSekmesi(state, actions, loadThumbnail, initialCompare)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BilgiNotu(text: String) {
    val colors = LocalAylaColors.current
    Surface(shape = MaterialTheme.shapes.medium, color = colors.lavantaAcik, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.lavanta)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

// --- Rutin ---

@Composable
private fun ColumnScope.RutinSekmesi(state: SkinUiState, actions: SkinActions, onOpenProfile: () -> Unit, onOpenDoctor: () -> Unit) {
    val plan = state.plan
    if (state.profile == null) {
        UyariBandi(
            tur = BantTuru.BILGI,
            title = stringResource(R.string.skin_profile_start_title),
            text = stringResource(R.string.skin_routine_empty),
            actionLabel = stringResource(R.string.skin_profile_start_action),
            onAction = onOpenProfile,
        )
    }
    if (plan.activesSuppressed) {
        UyariBandi(
            tur = BantTuru.DIKKAT,
            title = stringResource(R.string.skin_doctor_acne_severe_title),
            text = stringResource(R.string.skin_actives_suppressed),
            actionLabel = stringResource(R.string.skin_doctor_title),
            onAction = onOpenDoctor,
        )
    }
    if (plan.avoid.isNotEmpty()) KacinilacaklarKarti(plan.avoid.map { SkinIcerigi.avoid(it) })

    Text(
        stringResource(R.string.skin_routine_week_count, state.weekMorning, state.weekEvening),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    BolumBasligi(stringResource(R.string.skin_routine_morning))
    plan.morning.forEach { step ->
        AdimKarti(step, done = state.isDone(RoutineSlot.MORNING, step.key), ttcNote = step == RoutineStep.NIACINAMIDE && RoutineNote.TTC_NIACINAMIDE in plan.notes) {
            actions.onToggleStep(RoutineSlot.MORNING, step.key)
        }
    }

    BolumBasligi(stringResource(R.string.skin_routine_evening))
    if (RoutineNote.ONE_ACTIVE_PER_NIGHT in plan.notes) NotSatiri(stringResource(R.string.skin_one_active_note))
    if (RoutineNote.PATCH_TEST_FIRST in plan.notes) NotSatiri(stringResource(R.string.skin_patch_first_note))
    plan.evening.forEach { step ->
        AdimKarti(step, done = state.isDone(RoutineSlot.EVENING, step.key), ttcNote = step == RoutineStep.NIACINAMIDE && RoutineNote.TTC_NIACINAMIDE in plan.notes) {
            actions.onToggleStep(RoutineSlot.EVENING, step.key)
        }
    }

    AylaCard {
        Text(stringResource(R.string.skin_routine_rules_title), style = MaterialTheme.typography.titleMedium)
        SkinIcerigi.routineRules.forEach { res ->
            Text("• " + stringResource(res), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        KaynakSatiri(R.string.skin_routine_rules_source)
    }
    BolumBasligi(stringResource(R.string.skin_tips_title))
    plan.tips.forEach { IcerikKarti(SkinIcerigi.tip(it)) }
    Text(stringResource(R.string.skin_not_treatment), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun NotSatiri(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun KacinilacaklarKarti(items: List<BilgiKarti>) {
    val colors = LocalAylaColors.current
    AylaCard(containerColor = colors.uyariAcik, bordered = false) {
        Text(stringResource(R.string.skin_avoid_title), style = MaterialTheme.typography.titleMedium, color = colors.uyari)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.skin_avoid_text), style = MaterialTheme.typography.bodyMedium)
        items.forEach { item ->
            Spacer(Modifier.height(10.dp))
            Text(stringResource(item.title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(item.text), style = MaterialTheme.typography.bodyMedium)
            KaynakSatiri(item.source)
        }
    }
}

/**
 * Rutin adımı: solda "yapıldı" düğmesi (daire → yonca biçim morfu, onay simgesi; renk tek başına değil), başlık,
 * kanıt çipi, metin ve kaynak. Düğme 48 dp; ekran okuyucu durumu duyurur.
 */
@Composable
private fun AdimKarti(step: RoutineStep, done: Boolean, ttcNote: Boolean, onToggle: () -> Unit) {
    val card = SkinIcerigi.step(step)
    val title = stringResource(card.title)
    val scheme = MaterialTheme.colorScheme
    val colors = LocalAylaColors.current
    val morph by animateFloatAsState(if (done) 1f else 0f, hareket(AylaMotion.spatial()), label = "stepMorph")
    val shape = remember(morph) { AylaExpressiveShapes.polar(lobes = 4, amplitude = 0.20f * morph.coerceIn(0f, 1f)) }
    val a11y = if (done) stringResource(R.string.skin_step_done_a11y, title) else stringResource(R.string.skin_step_toggle_a11y, title)
    AylaCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
                    .semantics { contentDescription = a11y },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shape)
                        .background(if (done) colors.basari else scheme.surface)
                        .border(BorderStroke(2.dp, if (done) colors.basari else scheme.outline), shape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.surface, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                BaslikVeKanit(title, card.evidence)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(card.text), style = MaterialTheme.typography.bodyMedium)
                if (ttcNote) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.skin_ttc_niacinamide_note), style = MaterialTheme.typography.bodySmall, color = colors.lavanta)
                }
                Spacer(Modifier.height(6.dp))
                KaynakSatiri(card.source)
            }
        }
    }
}

// --- Profil ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.ProfilSekmesi(state: SkinUiState, actions: SkinActions) {
    val profile = state.profile ?: SkinProfileEntity()
    val scheme = MaterialTheme.colorScheme
    Text(stringResource(R.string.skin_profile_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.skin_profile_text), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

    AylaCard {
        Text(stringResource(R.string.skin_profile_skin_type), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SkinType.entries.forEach { type ->
                val selected = profile.skinType == type
                FilterChip(
                    selected = selected,
                    onClick = { actions.onProfile { it.copy(skinType = if (selected) null else type) } },
                    label = { CipEtiketi(stringResource(DoctorReportBuilder.skinTypeLabel(type))) },
                    leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                )
            }
        }
        Text(stringResource(R.string.skin_type_help), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }

    AylaCard {
        Text(stringResource(R.string.skin_profile_acne), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AcneSelfReport.entries.forEach { acne ->
                val selected = profile.acne == acne
                FilterChip(
                    selected = selected,
                    onClick = { actions.onProfile { it.copy(acne = if (selected) null else acne) } },
                    label = { CipEtiketi(stringResource(DoctorReportBuilder.acneLabel(acne))) },
                    leadingIcon = if (selected) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                )
            }
        }
        Text(stringResource(R.string.skin_acne_help), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }

    AylaCard {
        ProfilAnahtari(stringResource(R.string.skin_profile_dark_spots), null, profile.darkSpots) { v -> actions.onProfile { it.copy(darkSpots = v) } }
        ProfilAnahtari(stringResource(R.string.skin_profile_acanthosis), stringResource(R.string.skin_acanthosis_help), profile.acanthosis) { v ->
            actions.onProfile { it.copy(acanthosis = v) }
        }
        ProfilAnahtari(stringResource(R.string.skin_profile_hair_growth), stringResource(R.string.skin_hair_growth_help), profile.unwantedHair) { v ->
            actions.onProfile { it.copy(unwantedHair = v) }
        }
        ProfilAnahtari(stringResource(R.string.skin_profile_hair_thinning), null, profile.hairThinning) { v -> actions.onProfile { it.copy(hairThinning = v) } }
        ProfilAnahtari(stringResource(R.string.skin_profile_pregnant_now), stringResource(R.string.skin_pregnant_help), profile.pregnantOrBreastfeeding) { v ->
            actions.onProfile { it.copy(pregnantOrBreastfeeding = v) }
        }
    }

    AylaCard {
        Text(stringResource(R.string.skin_profile_pregnancy), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(if (state.pregnancyPlan) R.string.skin_pregnancy_on else R.string.skin_pregnancy_off),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.primary,
        )
        if (state.pregnancyPlan) {
            Text(stringResource(R.string.skin_pregnancy_from_goals), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }

    BolumBasligi(stringResource(R.string.skin_hair_title))
    SkinIcerigi.hairCards.forEach { IcerikKarti(it) }
    BolumBasligi(stringResource(R.string.skin_hair_removal_title))
    Text(stringResource(R.string.skin_hair_removal_text), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    SkinIcerigi.hairRemovalCards.forEach { IcerikKarti(it) }
    SkinIcerigi.allergenCards.forEach { IcerikKarti(it) }
    Text(stringResource(R.string.skin_not_diagnosis), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
}

@Composable
private fun ProfilAnahtari(label: String, help: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (help != null) Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

// --- Doktor ---

@Composable
private fun ColumnScope.DoktorSekmesi(state: SkinUiState) {
    val highlighted = state.plan.doctorCards.map { it.key }
    Text(stringResource(R.string.skin_doctor_title), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.skin_doctor_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val (forYou, rest) = SkinIcerigi.doctorCards.partition { it.key in highlighted }
    if (forYou.isNotEmpty()) {
        BolumBasligi(stringResource(R.string.skin_doctor_for_you))
        forYou.sortedBy { highlighted.indexOf(it.key) }.forEach { DoktorKartiGorunumu(it, emphasized = true) }
        BolumBasligi(stringResource(R.string.skin_doctor_all))
    }
    rest.forEach { DoktorKartiGorunumu(it, emphasized = false) }
}

@Composable
private fun DoktorKartiGorunumu(item: SkinIcerigi.DoktorKarti, emphasized: Boolean) {
    val colors = LocalAylaColors.current
    AylaCard(containerColor = if (emphasized) colors.uyariAcik else colors.kart, bordered = !emphasized) {
        item.urgency?.let { urgency ->
            Surface(shape = CircleShape, color = if (urgency == SkinIcerigi.Aciliyet.NOW) colors.uyari else colors.lavantaAcik) {
                Text(
                    stringResource(urgency.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (urgency == SkinIcerigi.Aciliyet.NOW) MaterialTheme.colorScheme.surface else colors.lavanta,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        BaslikVeKanit(stringResource(item.card.title), item.card.evidence)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(item.card.text), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        KaynakSatiri(item.card.source)
    }
}

// --- Fotoğraflar ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.FotografSekmesi(
    state: SkinUiState,
    actions: SkinActions,
    loadThumbnail: suspend (PhotoRecord, Int) -> ImageBitmap?,
    initialCompare: Set<String>,
) {
    val scheme = MaterialTheme.colorScheme
    var kind by rememberSaveable { mutableStateOf(PhotoKind.SKIN) }
    var area by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(initialCompare.toList()) }
    var confirmDelete by remember { mutableStateOf<PhotoRecord?>(null) }
    var confirmRevoke by remember { mutableStateOf(false) }

    Text(stringResource(R.string.skin_photo_title), style = MaterialTheme.typography.titleLarge)
    if (!state.photoConsent) {
        AylaCard {
            Text(stringResource(R.string.skin_photo_consent_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.skin_photo_consent_text), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = actions.onGiveConsent, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.skin_photo_consent_accept))
            }
            TextButton(onClick = actions.onOpenPrivacyPolicy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.skin_photo_consent_policy))
            }
        }
        Text(stringResource(R.string.skin_photo_not_diagnosis), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        return
    }
    if (state.photo.status == PhotoStatus.UNAVAILABLE) {
        UyariBandi(
            tur = BantTuru.HATA,
            title = stringResource(R.string.skin_photo_unavailable_title),
            text = stringResource(R.string.skin_photo_unavailable_text),
            actionLabel = stringResource(R.string.skin_photo_unavailable_reset),
            onAction = actions.onResetPhotos,
        )
        return
    }
    Text(stringResource(R.string.skin_photo_intro), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)

    AylaCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PhotoKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k },
                    label = { CipEtiketi(stringResource(kindLabel(k))) },
                    leadingIcon = if (kind == k) ({ Icon(Icons.Rounded.Check, contentDescription = null) }) else null,
                )
            }
        }
        OutlinedTextField(
            value = area,
            onValueChange = { area = it.take(40) },
            label = { Text(stringResource(R.string.skin_photo_area_label)) },
            placeholder = { Text(stringResource(R.string.skin_photo_area_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        Spacer(Modifier.height(10.dp))
        val areaOrNull = area.trim().ifEmpty { null }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.onTakePhoto(kind, areaOrNull) }, enabled = !state.photo.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                Text(" " + stringResource(R.string.skin_photo_take))
            }
            OutlinedButton(onClick = { actions.onPickPhoto(kind, areaOrNull) }, enabled = !state.photo.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Text(" " + stringResource(R.string.skin_photo_pick))
            }
        }
        if (state.photo.busy) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    "  " + stringResource(if (state.photo.exporting) R.string.skin_photo_exporting else R.string.skin_photo_saving),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.photo.error) {
            Text(stringResource(R.string.skin_photo_error), style = MaterialTheme.typography.bodySmall, color = scheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        if (state.photo.cameraMissing) {
            Text(stringResource(R.string.skin_photo_camera_missing), style = MaterialTheme.typography.bodySmall, color = scheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        state.photo.exportResult?.let { result ->
            val text = when {
                result.failed == 0 -> stringResource(R.string.skin_photo_export_all_done, result.exported)
                else -> stringResource(R.string.skin_photo_export_all_partial, result.exported, result.failed)
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = if (result.failed == 0) scheme.onSurfaceVariant else scheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }

    AylaCard {
        Text(stringResource(R.string.skin_photo_tips_title), style = MaterialTheme.typography.titleMedium)
        SkinIcerigi.photoTips.forEach { Text("• " + stringResource(it), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(8.dp))
        KaynakSatiri(R.string.skin_photo_tips_source)
    }

    val compare = selected.mapNotNull { id -> state.photos.firstOrNull { it.id == id } }
    if (compare.size == 2) {
        AylaCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.skin_photo_compare_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { selected = emptyList() }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.skin_photo_compare_close))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                compare.sortedBy { it.date }.forEach { record ->
                    Column(modifier = Modifier.weight(1f)) {
                        FotoGorseli(record, 900, loadThumbnail, Modifier.fillMaxWidth().aspectRatio(0.75f))
                        Text(record.date.format(tarih), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }

    if (state.photos.isEmpty()) {
        Text(stringResource(R.string.skin_photo_empty), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    } else {
        Text(stringResource(R.string.skin_photo_compare_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        state.photos.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { record ->
                    FotoKutusu(
                        record = record,
                        selected = record.id in selected,
                        loadThumbnail = loadThumbnail,
                        onSelect = {
                            selected = when {
                                record.id in selected -> selected - record.id
                                selected.size >= 2 -> selected.drop(1) + record.id
                                else -> selected + record.id
                            }
                        },
                        onExport = { actions.onExportPhoto(record) },
                        onDelete = { confirmDelete = record },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    Text(stringResource(R.string.skin_photo_not_diagnosis), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    if (state.photos.isNotEmpty()) {
        // Fotoğraflar yedeğe girmez; telefon değiştirirken klasöre topluca kaydedilir.
        OutlinedButton(onClick = actions.onExportAll, enabled = !state.photo.busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null)
            Text(" " + stringResource(R.string.skin_photo_export_all))
        }
        Text(stringResource(R.string.skin_photo_export_all_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
    TextButton(onClick = { confirmRevoke = true }, enabled = !state.photo.busy, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.skin_photo_revoke), color = scheme.error)
    }

    confirmDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.skin_photo_delete_confirm_title)) },
            text = { Text(stringResource(R.string.skin_photo_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDeletePhoto(record.id)
                    selected = selected - record.id
                    confirmDelete = null
                }) { Text(stringResource(R.string.action_delete), color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmRevoke) {
        AlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = { Text(stringResource(R.string.skin_photo_revoke_title)) },
            text = { Text(stringResource(R.string.skin_photo_revoke_text)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onRevokeConsent()
                    confirmRevoke = false
                }) { Text(stringResource(R.string.skin_photo_revoke_confirm), color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRevoke = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun kindLabel(kind: PhotoKind): Int = when (kind) {
    PhotoKind.SKIN -> R.string.skin_photo_kind_skin
    PhotoKind.HAIR -> R.string.skin_photo_kind_hair
}

@Composable
private fun FotoKutusu(
    record: PhotoRecord,
    selected: Boolean,
    loadThumbnail: suspend (PhotoRecord, Int) -> ImageBitmap?,
    onSelect: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val date = record.date.format(tarih)
    val a11y = if (selected) stringResource(R.string.skin_photo_selected_a11y, date) else stringResource(R.string.skin_photo_select_a11y, date)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(MaterialTheme.shapes.small)
                .border(BorderStroke(if (selected) 3.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant), MaterialTheme.shapes.small)
                .clickable(role = Role.Checkbox, onClick = onSelect)
                .semantics { contentDescription = a11y },
        ) {
            FotoGorseli(record, 360, loadThumbnail, Modifier.fillMaxSize())
            if (selected) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).background(scheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp)) }
            }
        }
        Text(date, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        Row {
            IconButton(onClick = onExport) { Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.skin_photo_export)) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.skin_photo_delete)) }
        }
    }
}

/** Fotoğrafı şifre çözerek arka planda yükler; yüklenene dek düz zemin. */
@Composable
private fun FotoGorseli(record: PhotoRecord, maxPx: Int, load: suspend (PhotoRecord, Int) -> ImageBitmap?, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, record.id, maxPx) { value = load(record, maxPx) }
    Box(modifier = modifier.background(LocalAylaColors.current.cukur)) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }
}
