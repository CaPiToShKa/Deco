package com.example.decosocio.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.CouponStatus
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.LoyaltyRepository
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

enum class RewardsTab { REWARDS, CAMPAIGNS, MY_COUPONS }

data class RewardsUiState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val tab: RewardsTab = RewardsTab.REWARDS,
    val account: LoyaltyAccount? = null,
    val rewards: List<Reward> = emptyList(),
    val campaigns: List<CouponCampaign> = emptyList(),
    val coupons: List<Coupon> = emptyList(),
    val today: LocalDate? = null,
    val busyId: String? = null,
    val confirmReward: Reward? = null,
    val message: UiText? = null,
    /** Set after a successful redeem/claim so the UI opens the new coupon. */
    val openCouponId: String? = null,
)

class RewardsViewModel(
    private val loyalty: LoyaltyRepository,
    private val dates: DateProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(RewardsUiState())
    val state: StateFlow<RewardsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = it.account == null, error = null) }
        viewModelScope.launch {
            resultOf {
                coroutineScope {
                    val account = async { loyalty.account() }
                    val rewards = async { loyalty.rewards() }
                    val campaigns = async { loyalty.campaigns() }
                    val coupons = async { loyalty.coupons() }
                    RewardsUiState(
                        loading = false,
                        account = account.await(),
                        rewards = rewards.await(),
                        campaigns = campaigns.await(),
                        coupons = coupons.await(),
                    )
                }
            }.onSuccess { loaded ->
                _state.update {
                    it.copy(
                        loading = false,
                        account = loaded.account,
                        rewards = loaded.rewards,
                        campaigns = loaded.campaigns,
                        coupons = loaded.coupons,
                        today = dates.today(),
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.toUiText()) }
            }
        }
    }

    fun selectTab(tab: RewardsTab) = _state.update { it.copy(tab = tab) }

    fun askRedeem(reward: Reward) = _state.update { it.copy(confirmReward = reward) }
    fun dismissRedeem() = _state.update { it.copy(confirmReward = null) }

    fun confirmRedeem() {
        val reward = _state.value.confirmReward ?: return
        _state.update { it.copy(confirmReward = null) }
        perform(reward.id) { loyalty.redeem(reward.id) }
    }

    fun claim(campaign: CouponCampaign) = perform(campaign.id) { loyalty.claim(campaign.id) }

    private fun perform(busyId: String, action: suspend () -> Coupon) {
        if (_state.value.busyId != null) return
        _state.update { it.copy(busyId = busyId) }
        viewModelScope.launch {
            resultOf { action() }
                .onSuccess { coupon -> _state.update { it.copy(busyId = null, openCouponId = coupon.id) } }
                .onFailure { e -> _state.update { it.copy(busyId = null, message = e.toUiText()) } }
            load()
        }
    }

    fun couponOpened() = _state.update { it.copy(openCouponId = null) }
    fun messageShown() = _state.update { it.copy(message = null) }

    fun couponStatus(coupon: Coupon): CouponStatus = coupon.status(_state.value.today ?: dates.today())
}

data class CouponUiState(
    val loading: Boolean = true,
    val coupon: Coupon? = null,
    val status: CouponStatus? = null,
    val error: UiText? = null,
)

class CouponViewModel(
    private val couponId: String,
    private val loyalty: LoyaltyRepository,
    private val dates: DateProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(CouponUiState())
    val state: StateFlow<CouponUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            resultOf { loyalty.coupons().firstOrNull { it.id == couponId } }
                .onSuccess { coupon ->
                    _state.value = CouponUiState(loading = false, coupon = coupon, status = coupon?.status(dates.today()))
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.toUiText()) } }
        }
    }
}
