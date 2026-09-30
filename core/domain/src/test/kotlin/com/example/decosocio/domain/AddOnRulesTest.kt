package com.example.decosocio.domain

import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.AddOnKind
import com.example.decosocio.domain.model.AddOnRules
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.BillingPeriod
import com.example.decosocio.domain.model.SubscriptionStatus
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddOnRulesTest {
    private val day0 = LocalDate(2026, 9, 1)
    private val available = AddOn(
        id = "legal",
        name = "Apoio jurídico",
        description = "",
        priceCents = 499,
        period = BillingPeriod.MONTHLY,
        kind = AddOnKind.ADVICE,
        state = AddOnState.Available,
    )

    @Test
    fun activationOpensAFourteenDayWithdrawalWindow() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val state = assertIs<AddOnState.Active>(active.state)
        assertEquals(LocalDate(2026, 9, 15), state.withdrawalDeadline)
        assertTrue(AddOnRules.canWithdraw(active, LocalDate(2026, 9, 15)))
        assertFalse(AddOnRules.canWithdraw(active, LocalDate(2026, 9, 16)))
    }

    @Test
    fun expiredMembersCannotActivate() {
        val error = assertFailsWith<DomainError.NotEligible> {
            AddOnRules.activate(available, day0, SubscriptionStatus.EXPIRED)
        }
        assertEquals(Reason.SUBSCRIPTION_EXPIRED, error.reason)
    }

    @Test
    fun activatingTwiceIsRejected() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val error = assertFailsWith<DomainError.NotEligible> {
            AddOnRules.activate(active, day0, SubscriptionStatus.ACTIVE)
        }
        assertEquals(Reason.ALREADY_ACTIVE, error.reason)
    }

    @Test
    fun withdrawalRefundsInFullAndMakesItAvailableAgain() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val (after, receipt) = AddOnRules.withdraw(active, LocalDate(2026, 9, 10), "W-1")
        assertEquals(AddOnState.Available, after.state)
        assertEquals(499, receipt.refundCents)
        assertEquals("W-1", receipt.confirmationId)
    }

    @Test
    fun withdrawalAfterTheWindowIsRejected() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val error = assertFailsWith<DomainError.NotEligible> {
            AddOnRules.withdraw(active, LocalDate(2026, 9, 16), "W-2")
        }
        assertEquals(Reason.WITHDRAWAL_PERIOD_OVER, error.reason)
    }

    @Test
    fun cancellationRunsToTheEndOfTheCurrentPaidPeriod() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val cancelled = AddOnRules.requestCancellation(active, LocalDate(2026, 10, 20))
        val state = assertIs<AddOnState.CancellationRequested>(cancelled.state)
        // Cycles: 1 Sep-30 Sep, 1 Oct-31 Oct -> ends 31 Oct.
        assertEquals(LocalDate(2026, 10, 31), state.endsOn)
    }

    @Test
    fun cancellationCanBeUndoneBeforeItTakesEffect() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val cancelled = AddOnRules.requestCancellation(active, LocalDate(2026, 10, 20))
        val restored = AddOnRules.undoCancellation(cancelled, LocalDate(2026, 10, 25))
        assertIs<AddOnState.Active>(restored.state)
    }

    @Test
    fun settleTurnsAnEndedCancellationBackIntoAvailable() {
        val active = AddOnRules.activate(available, day0, SubscriptionStatus.ACTIVE)
        val cancelled = AddOnRules.requestCancellation(active, LocalDate(2026, 10, 20))
        assertIs<AddOnState.CancellationRequested>(AddOnRules.settle(cancelled, LocalDate(2026, 10, 31)).state)
        assertEquals(AddOnState.Available, AddOnRules.settle(cancelled, LocalDate(2026, 11, 1)).state)
    }

    @Test
    fun yearlyPeriodEndIsTheDayBeforeTheAnniversary() {
        val end = AddOnRules.currentPeriodEnd(LocalDate(2026, 2, 15), BillingPeriod.YEARLY, LocalDate(2026, 9, 30))
        assertEquals(LocalDate(2027, 2, 14), end)
    }
}
