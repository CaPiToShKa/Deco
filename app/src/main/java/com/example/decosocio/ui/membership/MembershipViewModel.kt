package com.example.decosocio.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.R
import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.SubscriptionRules
import com.example.decosocio.domain.model.SubscriptionStatus
import com.example.decosocio.domain.model.WithdrawalReceipt
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.MembershipRepository
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.common.resultOf
import com.example.decosocio.ui.common.toUiText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface AddOnAction {
    val addOn: AddOn

    data class Activate(override val addOn: AddOn) : AddOnAction
    data class Withdraw(override val addOn: AddOn) : AddOnAction
    data class Cancel(override val addOn: AddOn) : AddOnAction
    data class UndoCancel(override val addOn: AddOn) : AddOnAction
}

data class MembershipUiState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val subscription: Subscription? = null,
    val status: SubscriptionStatus? = null,
    val daysLeft: Int = 0,
    val addOns: List<AddOn> = emptyList(),
    val today: LocalDate? = null,
    val busyAddOnId: String? = null,
    val confirm: AddOnAction? = null,
    val receipt: WithdrawalReceipt? = null,
    val message: UiText? = null,
)

class MembershipViewModel(
    private val membership: MembershipRepository,
    private val dates: DateProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(MembershipUiState())
    val state: StateFlow<MembershipUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = it.subscription == null, error = null) }
        viewModelScope.launch {
            resultOf {
                // coroutineScope: a failing request surfaces here instead of cancelling the ViewModel scope.
                coroutineScope {
                    val subscription = async { membership.subscription() }
                    val addOns = async { membership.addOns() }
                    subscription.await() to addOns.await()
                }
            }.onSuccess { (subscription, addOns) ->
                val today = dates.today()
                _state.update {
                    it.copy(
                        loading = false,
                        subscription = subscription,
                        status = SubscriptionRules.status(subscription, today),
                        daysLeft = SubscriptionRules.daysUntilRenewal(subscription, today),
                        addOns = addOns,
                        today = today,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.toUiText()) }
            }
        }
    }

    fun ask(action: AddOnAction) = _state.update { it.copy(confirm = action) }
    fun dismissConfirm() = _state.update { it.copy(confirm = null) }
    fun dismissReceipt() = _state.update { it.copy(receipt = null) }
    fun messageShown() = _state.update { it.copy(message = null) }

    fun confirm() {
        val action = _state.value.confirm ?: return
        _state.update { it.copy(confirm = null, busyAddOnId = action.addOn.id) }
        viewModelScope.launch {
            resultOf {
                when (action) {
                    is AddOnAction.Activate -> {
                        membership.activateAddOn(action.addOn.id)
                        UiText.Res(R.string.addon_activated, listOf(action.addOn.name)) to null
                    }
                    is AddOnAction.Withdraw -> {
                        val receipt = membership.withdrawAddOn(action.addOn.id)
                        null to receipt
                    }
                    is AddOnAction.Cancel -> {
                        membership.cancelAddOn(action.addOn.id)
                        UiText.Res(R.string.addon_cancelled, listOf(action.addOn.name)) to null
                    }
                    is AddOnAction.UndoCancel -> {
                        membership.undoCancellation(action.addOn.id)
                        UiText.Res(R.string.addon_kept, listOf(action.addOn.name)) to null
                    }
                }
            }.onSuccess { (message, receipt) ->
                _state.update { it.copy(busyAddOnId = null, message = message, receipt = receipt) }
                load()
            }.onFailure { e ->
                _state.update { it.copy(busyAddOnId = null, message = e.toUiText()) }
                load()
            }
        }
    }
}
