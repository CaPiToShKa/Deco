package com.example.decosocio.domain.model

import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.Reason
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * What the add-on is, legally and for Google Play Payments policy:
 * DIGITAL content sold in-app must go through Play Billing; physical/real-world
 * SERVICE, INSURANCE and one-to-one ADVICE are exempt. Classify every real add-on.
 */
enum class AddOnKind { DIGITAL, SERVICE, INSURANCE, ADVICE }

sealed interface AddOnState {
    data object Available : AddOnState

    data class Active(
        val activatedOn: LocalDate,
        val withdrawalDeadline: LocalDate,
    ) : AddOnState

    data class CancellationRequested(
        val activatedOn: LocalDate,
        val endsOn: LocalDate,
    ) : AddOnState
}

data class AddOn(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val period: BillingPeriod,
    val kind: AddOnKind,
    val state: AddOnState,
)

data class WithdrawalReceipt(
    val addOnId: String,
    val addOnName: String,
    val withdrawnOn: LocalDate,
    val refundCents: Long,
    val confirmationId: String,
)

/**
 * Add-on lifecycle:
 * Available -> Active (14-day withdrawal window) -> CancellationRequested -> Available.
 * Withdrawal (EU Directive 2011/83 art. 9, "withdrawal button" from Directive 2023/2673)
 * is different from cancellation: it undoes the contract with a full refund and is only
 * possible inside the window; cancellation stops renewal at the end of the paid period.
 */
object AddOnRules {
    const val WITHDRAWAL_DAYS = 14

    fun activate(addOn: AddOn, today: LocalDate, subscription: SubscriptionStatus): AddOn {
        if (subscription == SubscriptionStatus.EXPIRED) throw DomainError.NotEligible(Reason.SUBSCRIPTION_EXPIRED)
        if (addOn.state !is AddOnState.Available) throw DomainError.NotEligible(Reason.ALREADY_ACTIVE)
        return addOn.copy(
            state = AddOnState.Active(
                activatedOn = today,
                withdrawalDeadline = today.plus(DatePeriod(days = WITHDRAWAL_DAYS)),
            ),
        )
    }

    fun canWithdraw(addOn: AddOn, today: LocalDate): Boolean {
        val state = addOn.state
        return state is AddOnState.Active && today <= state.withdrawalDeadline
    }

    fun withdraw(addOn: AddOn, today: LocalDate, confirmationId: String): Pair<AddOn, WithdrawalReceipt> {
        val state = addOn.state
        if (state !is AddOnState.Active) throw DomainError.NotEligible(Reason.NOT_ACTIVE)
        if (today > state.withdrawalDeadline) throw DomainError.NotEligible(Reason.WITHDRAWAL_PERIOD_OVER)
        val receipt = WithdrawalReceipt(
            addOnId = addOn.id,
            addOnName = addOn.name,
            withdrawnOn = today,
            refundCents = addOn.priceCents,
            confirmationId = confirmationId,
        )
        return addOn.copy(state = AddOnState.Available) to receipt
    }

    fun requestCancellation(addOn: AddOn, today: LocalDate): AddOn {
        val state = addOn.state
        if (state !is AddOnState.Active) throw DomainError.NotEligible(Reason.NOT_ACTIVE)
        return addOn.copy(
            state = AddOnState.CancellationRequested(
                activatedOn = state.activatedOn,
                endsOn = currentPeriodEnd(state.activatedOn, addOn.period, today),
            ),
        )
    }

    fun undoCancellation(addOn: AddOn, today: LocalDate): AddOn {
        val state = addOn.state
        if (state !is AddOnState.CancellationRequested) throw DomainError.NotEligible(Reason.CANCELLATION_NOT_PENDING)
        if (today > state.endsOn) throw DomainError.NotEligible(Reason.NOT_ACTIVE)
        return addOn.copy(
            state = AddOnState.Active(
                activatedOn = state.activatedOn,
                withdrawalDeadline = state.activatedOn.plus(DatePeriod(days = WITHDRAWAL_DAYS)),
            ),
        )
    }

    /** Applies a pending cancellation once its end date has passed. */
    fun settle(addOn: AddOn, today: LocalDate): AddOn {
        val state = addOn.state
        return if (state is AddOnState.CancellationRequested && today > state.endsOn) {
            addOn.copy(state = AddOnState.Available)
        } else {
            addOn
        }
    }

    /** Last day of the billing cycle that contains [today], counting cycles from [activatedOn]. */
    fun currentPeriodEnd(activatedOn: LocalDate, period: BillingPeriod, today: LocalDate): LocalDate {
        var cycles = 1
        var nextStart = activatedOn.plus(DatePeriod(months = period.months))
        while (nextStart <= today) {
            cycles++
            nextStart = activatedOn.plus(DatePeriod(months = period.months * cycles))
        }
        return nextStart.minus(DatePeriod(days = 1))
    }
}
