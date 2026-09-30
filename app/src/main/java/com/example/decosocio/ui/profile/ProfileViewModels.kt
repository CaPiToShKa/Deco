package com.example.decosocio.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.R
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.ProfileField
import com.example.decosocio.domain.model.Address
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.model.ProfileUpdate
import com.example.decosocio.domain.model.ProfileValidator
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.prefs.AppPreferences
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.push.PushState
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resultOf
import com.example.decosocio.ui.common.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: MemberProfile? = null,
    val error: UiText? = null,
)

class ProfileViewModel(
    private val member: MemberRepository,
    private val auth: AuthRepository,
    private val push: PushRegistrar,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    val pushState: StateFlow<PushState> = push.state
    val serviceNotifications: StateFlow<Boolean> =
        preferences.serviceNotifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = it.profile == null, error = null) }
        viewModelScope.launch {
            resultOf { member.profile() }
                .onSuccess { profile -> _state.update { it.copy(loading = false, profile = profile) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUiText()) } }
        }
    }

    fun notificationsPermitted(): Boolean = push.notificationsPermitted()

    fun onNotificationPermissionResult() = push.syncPushWithPermission()

    fun setServiceNotifications(enabled: Boolean) {
        viewModelScope.launch { preferences.setServiceNotifications(enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            push.onLogout()
            auth.logout()
        }
    }
}

data class EditProfileUiState(
    val loading: Boolean = true,
    val loadError: UiText? = null,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    val invalid: Set<ProfileField> = emptySet(),
    val saving: Boolean = false,
    val message: UiText? = null,
    val saved: Boolean = false,
) {
    fun toUpdate() = ProfileUpdate(
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone.ifBlank { null },
        address = if (street.isBlank() && postalCode.isBlank() && city.isBlank()) null else Address(street, postalCode, city),
    )
}

class EditProfileViewModel(private val member: MemberRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            resultOf { member.profile() }
                .onSuccess { p ->
                    _state.update {
                        it.copy(
                            loading = false,
                            firstName = p.firstName,
                            lastName = p.lastName,
                            email = p.email,
                            phone = p.phone.orEmpty(),
                            street = p.address?.street.orEmpty(),
                            postalCode = p.address?.postalCode.orEmpty(),
                            city = p.address?.city.orEmpty(),
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, loadError = e.toUiText()) } }
        }
    }

    fun edit(field: ProfileField, value: String) = _state.update {
        val updated = when (field) {
            ProfileField.FIRST_NAME -> it.copy(firstName = value)
            ProfileField.LAST_NAME -> it.copy(lastName = value)
            ProfileField.EMAIL -> it.copy(email = value)
            ProfileField.PHONE -> it.copy(phone = value)
            ProfileField.STREET -> it.copy(street = value)
            ProfileField.POSTAL_CODE -> it.copy(postalCode = value)
            ProfileField.CITY -> it.copy(city = value)
        }
        updated.copy(invalid = updated.invalid - field)
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val update = current.toUpdate()
        val invalid = ProfileValidator.validate(update)
        if (invalid.isNotEmpty()) {
            _state.update { it.copy(invalid = invalid, message = UiText.Res(R.string.error_validation)) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            resultOf { member.updateProfile(update) }
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    val fields = (e as? DomainError.Validation)?.fields.orEmpty()
                    _state.update { it.copy(saving = false, invalid = fields, message = e.toUiText()) }
                }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}
