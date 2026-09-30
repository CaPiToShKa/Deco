package com.example.decosocio.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.R
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentRecord
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resultOf
import com.example.decosocio.ui.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsentsUiState(
    val loading: Boolean = true,
    val loadError: UiText? = null,
    /** What the member sees on screen; saved only when they press Save. */
    val draft: Map<ConsentPurpose, Boolean> = ConsentPurpose.entries.associateWith { false },
    val history: List<ConsentRecord> = emptyList(),
    val saving: Boolean = false,
    val message: UiText? = null,
    val saved: Boolean = false,
)

class ConsentsViewModel(
    private val member: MemberRepository,
    private val push: PushRegistrar,
) : ViewModel() {

    private val _state = MutableStateFlow(ConsentsUiState())
    val state: StateFlow<ConsentsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            resultOf { member.consents() }
                .onSuccess { consents -> _state.update { it.fromConsents(consents).copy(loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, loadError = e.toUiText()) } }
        }
    }

    fun toggle(purpose: ConsentPurpose, granted: Boolean) =
        _state.update { it.copy(draft = it.draft + (purpose to granted), saved = false) }

    fun save(source: ConsentSource) {
        if (_state.value.saving) return
        val draft = _state.value.draft
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            resultOf { member.updateConsents(draft, source) }
                .onSuccess { consents ->
                    push.setMarketingConsent(consents.isGranted(ConsentPurpose.MARKETING_PUSH))
                    _state.update {
                        it.fromConsents(consents).copy(saving = false, saved = true, message = UiText.Res(R.string.consents_saved))
                    }
                }
                .onFailure { e -> _state.update { it.copy(saving = false, message = e.toUiText()) } }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun ConsentsUiState.fromConsents(consents: Consents) = copy(
        draft = ConsentPurpose.entries.associateWith { consents.isGranted(it) },
        history = consents.history,
    )
}
