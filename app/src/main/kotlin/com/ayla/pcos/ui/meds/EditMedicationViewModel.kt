package com.ayla.pcos.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.cycle.PillRegimen
import com.ayla.pcos.data.db.MedicationEntity
import com.ayla.pcos.data.db.MedicationForm
import com.ayla.pcos.data.db.MedicationKind
import com.ayla.pcos.data.repo.DateTicker
import com.ayla.pcos.data.repo.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Yemekle alınma tercihi; null "fark etmez" anlamına gelen üç durumlu seçim. */
enum class YemekTercihi(val value: Boolean?) { YEMEKLE(true), AC_KARNA(false), FARK_ETMEZ(null) }

fun Boolean?.toYemekTercihi(): YemekTercihi = when (this) {
    true -> YemekTercihi.YEMEKLE
    false -> YemekTercihi.AC_KARNA
    null -> YemekTercihi.FARK_ETMEZ
}

/**
 * İlaç formu durumu. [id] 0 ise yeni kayıt. Doğum kontrol hapında [regimen] ve [packStart] zorunludur.
 */
data class EditMedicationState(
    val loaded: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val doseText: String = "",
    val kind: MedicationKind = MedicationKind.SUPPLEMENT,
    val times: List<LocalTime> = listOf(LocalTime.of(9, 0)),
    val food: YemekTercihi = YemekTercihi.FARK_ETMEZ,
    val stock: String = "",
    val stockWarnDays: Int = 5,
    val regimen: PillRegimen = PillRegimen.R24_4,
    val packStart: LocalDate? = null,
    val today: LocalDate = LocalDate.now(),
) {
    val isNew: Boolean get() = id == 0L
    val isPill: Boolean get() = kind == MedicationKind.CONTRACEPTIVE

    /** Kaydet düğmesi: ad ve en az bir saat; hapta paket başlangıcı da gerekir. */
    val canSave: Boolean get() = name.isNotBlank() && times.isNotEmpty() && (!isPill || packStart != null)

    /**
     * Formdan varlık üretir. [base] düzenlenen kayıttır (kimlik, oluşturma zamanı, etkinlik korunur).
     * Geçersizse null.
     */
    fun toEntity(base: MedicationEntity?): MedicationEntity? {
        if (!canSave) return null
        val trimmedName = name.trim().take(MAX_NAME)
        val dose = doseText.trim().take(MAX_DOSE).ifBlank { DEFAULT_DOSE }
        val stockCount = stock.filter(Char::isDigit).take(4).toIntOrNull()
        val form = when {
            isPill -> MedicationForm.TABLET
            base != null && base.kind == kind -> base.form
            kind == MedicationKind.HERBAL -> MedicationForm.TEA
            else -> MedicationForm.CAPSULE
        }
        val entity = (base ?: MedicationEntity(name = trimmedName, doseText = dose, kind = kind, times = times)).copy(
            name = trimmedName,
            doseText = dose,
            kind = kind,
            form = form,
            times = times.distinct().sorted(),
            withFood = if (isPill) null else food.value,
            stockCount = stockCount,
            stockWarnDays = stockWarnDays.coerceIn(STOCK_WARN_RANGE),
            pillActiveDays = if (isPill) regimen.activeDays else null,
            pillPlaceboDays = if (isPill) regimen.placeboDays else null,
            packStartDate = if (isPill) packStart else null,
        )
        return entity
    }

    companion object {
        const val MAX_NAME = 60
        const val MAX_DOSE = 40
        const val DEFAULT_DOSE = "1 doz"
        val STOCK_WARN_RANGE = 1..14

        fun from(entity: MedicationEntity, today: LocalDate): EditMedicationState = EditMedicationState(
            loaded = true,
            id = entity.id,
            name = entity.name,
            doseText = entity.doseText,
            kind = entity.kind,
            times = entity.times,
            food = entity.withFood.toYemekTercihi(),
            stock = entity.stockCount?.toString() ?: "",
            stockWarnDays = entity.stockWarnDays,
            regimen = if (entity.pillActiveDays != null) PillRegimen(entity.pillActiveDays, entity.pillPlaceboDays ?: 0) else PillRegimen.R24_4,
            packStart = entity.packStartDate,
            today = today,
        )
    }
}

@HiltViewModel
class EditMedicationViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val ticker: DateTicker,
) : ViewModel() {

    private val _state = MutableStateFlow(EditMedicationState())
    val state: StateFlow<EditMedicationState> = _state

    private var base: MedicationEntity? = null

    /** [id] 0 ise boş form; aksi hâlde kaydı yükler. Aynı kimlik için ikinci kez yüklemez. */
    fun load(id: Long) {
        if (_state.value.loaded && _state.value.id == id) return
        viewModelScope.launch {
            val today = ticker.now()
            val entity = if (id == 0L) null else repository.get(id)
            base = entity
            _state.value = entity?.let { EditMedicationState.from(it, today) }
                ?: EditMedicationState(loaded = true, today = today)
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value.take(EditMedicationState.MAX_NAME)) }
    fun setDose(value: String) = _state.update { it.copy(doseText = value.take(EditMedicationState.MAX_DOSE)) }
    fun setKind(value: MedicationKind) = _state.update {
        it.copy(kind = value, packStart = if (value == MedicationKind.CONTRACEPTIVE) it.packStart ?: it.today else it.packStart)
    }
    fun addTime(value: LocalTime) = _state.update { if (value in it.times) it else it.copy(times = (it.times + value).sorted()) }
    fun removeTime(value: LocalTime) = _state.update { if (it.times.size <= 1) it else it.copy(times = it.times - value) }
    fun setFood(value: YemekTercihi) = _state.update { it.copy(food = value) }
    fun setStock(value: String) = _state.update { it.copy(stock = value.filter(Char::isDigit).take(4)) }
    fun setStockWarnDays(value: Int) = _state.update { it.copy(stockWarnDays = value.coerceIn(EditMedicationState.STOCK_WARN_RANGE)) }
    fun setRegimen(value: PillRegimen) = _state.update { it.copy(regimen = value) }
    fun setPackStart(value: LocalDate) = _state.update { if (value.isAfter(it.today)) it else it.copy(packStart = value) }

    fun save(onDone: () -> Unit) {
        val entity = _state.value.toEntity(base) ?: return
        viewModelScope.launch {
            if (entity.id == 0L) repository.add(entity) else repository.update(entity)
            onDone()
        }
    }

    fun remove(onDone: () -> Unit) {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.deactivate(id)
            onDone()
        }
    }
}
