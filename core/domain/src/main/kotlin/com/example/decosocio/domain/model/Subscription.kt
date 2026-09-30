package com.example.decosocio.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

enum class SubscriptionStatus { ACTIVE, EXPIRING, EXPIRED }

enum class BillingPeriod(val months: Int) { MONTHLY(1), QUARTERLY(3), YEARLY(12) }

data class Plan(
    val code: String,
    val name: String,
    val priceCents: Long,
    val period: BillingPeriod,
)

data class Subscription(
    val plan: Plan,
    val startDate: LocalDate,
    val renewalDate: LocalDate,
    val autoRenew: Boolean,
    val paymentMethodLabel: String,
)

object SubscriptionRules {
    /** Without auto-renewal, the member is warned this many days before the renewal date. */
    const val EXPIRING_WINDOW_DAYS = 30

    fun status(subscription: Subscription, today: LocalDate): SubscriptionStatus {
        val daysLeft = today.daysUntil(subscription.renewalDate)
        return when {
            daysLeft < 0 -> SubscriptionStatus.EXPIRED
            !subscription.autoRenew && daysLeft <= EXPIRING_WINDOW_DAYS -> SubscriptionStatus.EXPIRING
            else -> SubscriptionStatus.ACTIVE
        }
    }

    /** Negative when the renewal date has already passed. */
    fun daysUntilRenewal(subscription: Subscription, today: LocalDate): Int =
        today.daysUntil(subscription.renewalDate)

    fun hasMemberBenefits(status: SubscriptionStatus): Boolean = status != SubscriptionStatus.EXPIRED
}
