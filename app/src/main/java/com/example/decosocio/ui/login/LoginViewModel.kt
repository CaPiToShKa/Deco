package com.example.decosocio.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.data.demo.DemoSeed
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resultOf
import com.example.decosocio.ui.common.toUiText
import com.example.decosocio.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AfterLogin { ONBOARDING, HOME }

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: UiText? = null,
    val next: AfterLogin? = null,
)

class LoginViewModel(
    private val auth: AuthRepository,
    private val member: MemberRepository,
    private val push: PushRegistrar,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun fillDemoAccount() = _state.update {
        it.copy(email = DemoSeed.DEMO_EMAIL, password = DemoSeed.DEMO_PASSWORD, error = null)
    }

    fun login() {
        val current = _state.value
        if (current.loading) return
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = UiText.Res(R.string.login_error_empty)) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            resultOf {
                val session = auth.login(current.email, current.password)
                push.onLogin(session.contactKey)
                // First login on this account: collect consents before anything else.
                !member.consents().hasBeenAsked
            }.onSuccess { needsOnboarding ->
                _state.update { it.copy(loading = false, next = if (needsOnboarding) AfterLogin.ONBOARDING else AfterLogin.HOME) }
            }.onFailure { e ->
                val message = if (e is DomainError.Unauthorized) UiText.Res(R.string.login_error_credentials) else e.toUiText()
                _state.update { it.copy(loading = false, error = message) }
            }
        }
    }

    fun onNavigated() = _state.update { it.copy(next = null, password = "") }
}
