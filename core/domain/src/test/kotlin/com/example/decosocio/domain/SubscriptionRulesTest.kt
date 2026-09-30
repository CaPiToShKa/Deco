package com.example.decosocio.domain

import com.example.decosocio.domain.model.BillingPeriod
import com.example.decosocio.domain.model.Plan
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.SubscriptionRules
import com.example.decosocio.domain.model.SubscriptionStatus
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionRulesTest {
    private val today = LocalDate(2026, 9, 30)
    private val plan = Plan("TOTAL", "Plano Total", 1290, BillingPeriod.MONTHLY)

    private fun subscription(renewal: LocalDate, autoRenew: Boolean) =
        Subscription(plan, LocalDate(2025, 1, 1), renewal, autoRenew, "SEPA")

    @Test
    fun autoRenewingSubscriptionIsActiveEvenCloseToRenewal() {
        val sub = subscription(LocalDate(2026, 10, 5), autoRenew = true)
        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionRules.status(sub, today))
    }

    @Test
    fun withoutAutoRenewItIsExpiringInsideThirtyDays() {
        val sub = subscription(LocalDate(2026, 10, 30), autoRenew = false)
        assertEquals(30, SubscriptionRules.daysUntilRenewal(sub, today))
        assertEquals(SubscriptionStatus.EXPIRING, SubscriptionRules.status(sub, today))
    }

    @Test
    fun withoutAutoRenewItIsActiveOutsideTheWindow() {
        val sub = subscription(LocalDate(2026, 10, 31), autoRenew = false)
        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionRules.status(sub, today))
    }

    @Test
    fun renewalDayItselfIsStillExpiringNotExpired() {
        val sub = subscription(today, autoRenew = false)
        assertEquals(0, SubscriptionRules.daysUntilRenewal(sub, today))
        assertEquals(SubscriptionStatus.EXPIRING, SubscriptionRules.status(sub, today))
    }

    @Test
    fun pastRenewalDateIsExpiredAndLosesBenefits() {
        val sub = subscription(LocalDate(2026, 9, 29), autoRenew = true)
        val status = SubscriptionRules.status(sub, today)
        assertEquals(SubscriptionStatus.EXPIRED, status)
        assertFalse(SubscriptionRules.hasMemberBenefits(status))
        assertTrue(SubscriptionRules.hasMemberBenefits(SubscriptionStatus.EXPIRING))
    }
}
