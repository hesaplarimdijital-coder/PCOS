package com.ayla.pcos.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayla.pcos.data.db.DoctorQuestionEntity
import com.ayla.pcos.data.repo.DoctorQuestionRepository
import com.ayla.pcos.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionsUiState(
    val loaded: Boolean = false,
    val open: List<DoctorQuestionEntity> = emptyList(),
    val asked: List<DoctorQuestionEntity> = emptyList(),
    val draft: String = "",
) {
    val canAdd: Boolean get() = draft.isNotBlank()
}

/** Doktora sorular (#26, US-103). Yazmalar uygulama kapsamında: ekran kapansa da yarıda kalmaz (#27). */
@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val repository: DoctorQuestionRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val draft = MutableStateFlow("")

    val state: StateFlow<QuestionsUiState> = combine(repository.observeAll(), draft) { all, text ->
        QuestionsUiState(
            loaded = true,
            open = all.filter { it.askedAt == null },
            asked = all.filter { it.askedAt != null }.sortedByDescending { it.askedAt },
            draft = text,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuestionsUiState())

    fun setDraft(text: String) = draft.update { text.take(DoctorQuestionRepository.MAX_LENGTH) }

    /** Öneri çipi taslağı doldurur; kullanıcı düzenleyip ekler. */
    fun useSuggestion(text: String) = setDraft(text)

    fun add() {
        val text = draft.value
        if (text.isBlank()) return
        draft.value = ""
        appScope.launch { repository.add(text) }
    }

    fun setAsked(id: Long, asked: Boolean) {
        appScope.launch { repository.markAsked(id, asked) }
    }

    fun delete(id: Long) {
        appScope.launch { repository.delete(id) }
    }
}
