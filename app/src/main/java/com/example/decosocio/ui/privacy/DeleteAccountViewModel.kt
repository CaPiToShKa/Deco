package com.example.decosocio.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.DeletionReceipt
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

data class DeleteAccountUiState(
    val understood: Boolean = false,
    val loading: Boolean = false,
    val receipt: DeletionReceipt? = null,
    val error: UiText? = null,
)

/** In-app account deletion required by Google Play (plus a web link in the Play listing). */
class DeleteAccountViewModel(
    private val member: MemberRepository,
    private val auth: AuthRepository,
    private val push: PushRegistrar,
) : ViewModel() {
    private val _state = MutableStateFlow(DeleteAccountUiState())
    val state: StateFlow<DeleteAccountUiState> = _state.asStateFlow()

    fun setUnderstood(value: Boolean) = _state.update { it.copy(understood = value) }

    fun requestDeletion() {
        if (!_state.value.understood || _state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            resultOf { member.requestAccountDeletion() }
                .onSuccess { receipt -> _state.update { it.copy(loading = false, receipt = receipt) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUiText()) } }
        }
    }

    /** Ends the session after the member has seen the receipt. */
    fun finish() {
        viewModelScope.launch {
            push.onLogout()
            auth.logout()
        }
    }
}
