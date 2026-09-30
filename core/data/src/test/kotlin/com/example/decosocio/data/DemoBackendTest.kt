package com.example.decosocio.data

import com.example.decosocio.data.demo.DemoBackend
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.data.demo.DemoSeed
import com.example.decosocio.data.demo.SubscriptionScenario
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.Reason
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.Session
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DemoBackendTest {
    private val dates = object : DateProvider {
        override fun today() = LocalDate(2026, 9, 30)
        override fun nowEpochMillis() = 1_790_000_000_000L
    }
    private val control = DemoControl()
    private val backend = DemoBackend(control, dates, latencyMs = 0)

    private fun loggedIn(): Session = runBlocking { backend.login(DemoSeed.DEMO_EMAIL, DemoSeed.DEMO_PASSWORD) }

    @Test
    fun wrongPasswordIsRejectedAndDataNeedsASession(): Unit = runBlocking {
        assertFailsWith<DomainError.Unauthorized> { backend.login(DemoSeed.DEMO_EMAIL, "nope") }
        assertFailsWith<DomainError.Unauthorized> { backend.profile() }
        Unit
    }

    @Test
    fun loginExposesTheContactKeyUsedBySfmc() {
        val session = loggedIn()
        assertEquals(DemoSeed.CONTACT_KEY, session.contactKey)
        assertEquals(session, backend.session.value)
    }

    @Test
    fun activateThenWithdrawWithinFourteenDays(): Unit = runBlocking {
        loggedIn()
        val active = backend.activateAddOn("revista-digital")
        assertIs<AddOnState.Active>(active.state)
        val receipt = backend.withdrawAddOn("revista-digital")
        assertTrue(receipt.confirmationId.startsWith("RET-"))
        assertEquals(AddOnState.Available, backend.addOns().first { it.id == "revista-digital" }.state)
    }

    @Test
    fun oldAddOnCanOnlyBeCancelled(): Unit = runBlocking {
        loggedIn()
        val error = assertFailsWith<DomainError.NotEligible> { backend.withdrawAddOn("seguro-compras") }
        assertEquals(Reason.WITHDRAWAL_PERIOD_OVER, error.reason)
        assertIs<AddOnState.CancellationRequested>(backend.cancelAddOn("seguro-compras").state)
    }

    @Test
    fun expiredScenarioBlocksActivationAndClaims(): Unit = runBlocking {
        loggedIn()
        control.setSubscription(SubscriptionScenario.EXPIRED)
        val activation = assertFailsWith<DomainError.NotEligible> { backend.activateAddOn("analise-energia") }
        assertEquals(Reason.SUBSCRIPTION_EXPIRED, activation.reason)
        val claim = assertFailsWith<DomainError.NotEligible> { backend.claim("c-eletro") }
        assertEquals(Reason.SUBSCRIPTION_EXPIRED, claim.reason)
    }

    @Test
    fun lastSingleUseCodeGoesOnceAndClaimIsIdempotent(): Unit = runBlocking {
        loggedIn()
        val first = backend.claim("c-saude")
        val again = backend.claim("c-saude")
        assertEquals(first.code, again.code)
        assertEquals(0, backend.campaigns().first { it.id == "c-saude" }.remainingCodes)
        assertEquals(1, backend.coupons().count { it.campaignId == "c-saude" })
    }

    @Test
    fun networkErrorScenarioFailsEveryCall(): Unit = runBlocking {
        loggedIn()
        control.setNetworkError(true)
        assertFailsWith<DomainError.Network> { backend.subscription() }
        control.setNetworkError(false)
        assertNotNull(backend.subscription())
        Unit
    }

    @Test
    fun consentsAndDeletionAreRecorded(): Unit = runBlocking {
        loggedIn()
        val consents = backend.updateConsents(
            ConsentPurpose.entries.associateWith { it == ConsentPurpose.NEWSLETTER },
            ConsentSource.ONBOARDING,
        )
        assertTrue(consents.hasBeenAsked)
        val receipt = backend.requestAccountDeletion()
        assertEquals(LocalDate(2026, 10, 30), receipt.completesBy)
        val again = assertFailsWith<DomainError.NotEligible> { backend.requestAccountDeletion() }
        assertEquals(Reason.DELETION_ALREADY_REQUESTED, again.reason)
    }

    @Test
    fun resetRestoresTheSeed(): Unit = runBlocking {
        loggedIn()
        backend.redeem("r-cinema")
        control.resetData()
        assertEquals(820, backend.account().points)
    }
}
