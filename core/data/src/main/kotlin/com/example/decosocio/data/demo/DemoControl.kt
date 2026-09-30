package com.example.decosocio.data.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SubscriptionScenario { ACTIVE, EXPIRING, EXPIRED }

data class DemoSettings(
    val subscription: SubscriptionScenario = SubscriptionScenario.ACTIVE,
    val networkError: Boolean = false,
    val emptyCoupons: Boolean = false,
    /** Incremented to ask the demo backend to reseed its data. */
    val resetGeneration: Int = 0,
)

/** Switches used by the hidden demo menu to show every state of the app during a presentation. */
class DemoControl {
    private val _settings = MutableStateFlow(DemoSettings())
    val settings: StateFlow<DemoSettings> = _settings.asStateFlow()

    fun setSubscription(scenario: SubscriptionScenario) = _settings.update { it.copy(subscription = scenario) }
    fun setNetworkError(enabled: Boolean) = _settings.update { it.copy(networkError = enabled) }
    fun setEmptyCoupons(enabled: Boolean) = _settings.update { it.copy(emptyCoupons = enabled) }
    fun resetData() = _settings.update { DemoSettings(resetGeneration = it.resetGeneration + 1) }
}
