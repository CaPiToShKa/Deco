package com.example.decosocio.ui

import com.example.decosocio.R
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.data.demo.DemoSeed
import com.example.decosocio.data.demo.SubscriptionScenario
import com.example.decosocio.domain.ProfileField
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.SubscriptionStatus
import com.example.decosocio.ui.common.UiText
import com.example.decosocio.ui.login.AfterLogin
import com.example.decosocio.ui.login.LoginViewModel
import com.example.decosocio.ui.membership.AddOnAction
import com.example.decosocio.ui.membership.MembershipViewModel
import com.example.decosocio.ui.privacy.ConsentsViewModel
import com.example.decosocio.ui.privacy.DeleteAccountViewModel
import com.example.decosocio.ui.profile.EditProfileViewModel
import com.example.decosocio.ui.rewards.RewardsViewModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {
    @Test
    fun wrongPasswordShowsCredentialsError() = vmTest {
        val vm = LoginViewModel(demoBackend(signedIn = false), demoBackend(), FakePush())
        vm.onEmailChange(DemoSeed.DEMO_EMAIL)
        vm.onPasswordChange("wrong")
        vm.login()
        assertEquals(UiText.Res(R.string.login_error_credentials), vm.state.value.error)
        assertNull(vm.state.value.next)
    }

    @Test
    fun firstLoginGoesToOnboardingAndLinksPush() = vmTest {
        val backend = demoBackend(signedIn = false)
        val push = FakePush()
        val vm = LoginViewModel(backend, backend, push)
        vm.fillDemoAccount()
        vm.login()
        assertEquals(AfterLogin.ONBOARDING, vm.state.value.next)
        assertEquals(DemoSeed.CONTACT_KEY, push.loggedInAs)
    }

    @Test
    fun loginAfterConsentsGoesHome() = vmTest {
        val backend = demoBackend()
        backend.updateConsents(mapOf(ConsentPurpose.NEWSLETTER to false), ConsentSource.ONBOARDING)
        val vm = LoginViewModel(backend, backend, FakePush())
        vm.fillDemoAccount()
        vm.login()
        assertEquals(AfterLogin.HOME, vm.state.value.next)
    }
}

class ConsentsViewModelTest {
    @Test
    fun savingMirrorsMarketingPushConsentToTheSdk() = vmTest {
        val push = FakePush()
        val vm = ConsentsViewModel(demoBackend(), push)
        assertFalse(vm.state.value.loading)
        vm.toggle(ConsentPurpose.MARKETING_PUSH, true)
        vm.save(ConsentSource.ONBOARDING)
        assertTrue(vm.state.value.saved)
        assertEquals(true, push.marketingConsent)
        assertEquals(ConsentPurpose.entries.size, vm.state.value.history.size)
    }
}

class MembershipViewModelTest {
    @Test
    fun loadsStatusAndAddOns() = vmTest {
        val vm = MembershipViewModel(demoBackend(), FixedDates)
        val state = vm.state.value
        assertEquals(SubscriptionStatus.ACTIVE, state.status)
        assertEquals(4, state.addOns.size)
    }

    @Test
    fun withdrawalProducesAReceipt() = vmTest {
        val vm = MembershipViewModel(demoBackend(), FixedDates)
        val legal = vm.state.value.addOns.first { it.id == "apoio-juridico" }
        vm.ask(AddOnAction.Withdraw(legal))
        vm.confirm()
        val receipt = assertNotNull(vm.state.value.receipt)
        assertEquals(499, receipt.refundCents)
        assertEquals(AddOnState.Available, vm.state.value.addOns.first { it.id == "apoio-juridico" }.state)
    }

    @Test
    fun expiredMemberGetsAClearMessage() = vmTest {
        val control = DemoControl().apply { setSubscription(SubscriptionScenario.EXPIRED) }
        val vm = MembershipViewModel(demoBackend(control), FixedDates)
        assertEquals(SubscriptionStatus.EXPIRED, vm.state.value.status)
        vm.ask(AddOnAction.Activate(vm.state.value.addOns.first { it.id == "analise-energia" }))
        vm.confirm()
        assertEquals(UiText.Res(R.string.error_subscription_expired), vm.state.value.message)
    }

    @Test
    fun networkFailureShowsAnError() = vmTest {
        val control = DemoControl().apply { setNetworkError(true) }
        val vm = MembershipViewModel(demoBackend(control, signedIn = false), FixedDates)
        assertEquals(UiText.Res(R.string.error_network), vm.state.value.error)
        assertNull(vm.state.value.subscription)
    }
}

class RewardsViewModelTest {
    @Test
    fun claimingOpensTheNewCoupon() = vmTest {
        val vm = RewardsViewModel(demoBackend(), FixedDates)
        val campaign = vm.state.value.campaigns.first { it.id == "c-eletro" }
        vm.claim(campaign)
        val couponId = assertNotNull(vm.state.value.openCouponId)
        assertTrue(vm.state.value.coupons.any { it.id == couponId })
    }

    @Test
    fun redeemingDeductsPoints() = vmTest {
        val vm = RewardsViewModel(demoBackend(), FixedDates)
        vm.askRedeem(vm.state.value.rewards.first { it.id == "r-cinema" })
        vm.confirmRedeem()
        assertEquals(520, vm.state.value.account?.points)
    }
}

class EditProfileViewModelTest {
    @Test
    fun invalidPostalCodeIsFlaggedAndNotSaved() = vmTest {
        val vm = EditProfileViewModel(demoBackend())
        vm.edit(ProfileField.POSTAL_CODE, "1000")
        vm.save()
        assertTrue(ProfileField.POSTAL_CODE in vm.state.value.invalid)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun validChangesAreSaved() = vmTest {
        val backend = demoBackend()
        val vm = EditProfileViewModel(backend)
        vm.edit(ProfileField.CITY, "Porto")
        vm.edit(ProfileField.POSTAL_CODE, "4000-001")
        vm.save()
        assertTrue(vm.state.value.saved)
        assertEquals("Porto", backend.profile().address?.city)
    }
}

class DeleteAccountViewModelTest {
    @Test
    fun deletionNeedsConfirmationThenEndsTheSession() = vmTest {
        val backend = demoBackend()
        val push = FakePush()
        val vm = DeleteAccountViewModel(backend, backend, push)
        vm.requestDeletion()
        assertNull(vm.state.value.receipt)
        vm.setUnderstood(true)
        vm.requestDeletion()
        assertIs<com.example.decosocio.domain.repository.DeletionReceipt>(vm.state.value.receipt)
        vm.finish()
        assertTrue(push.loggedOut)
        assertNull(backend.session.value)
    }
}
