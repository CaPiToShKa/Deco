package com.example.decosocio.domain

import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.CouponRules
import com.example.decosocio.domain.model.CouponStatus
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.LoyaltyRules
import com.example.decosocio.domain.model.LoyaltyTier
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.model.SubscriptionStatus
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class LoyaltyAndCouponRulesTest {
    private val today = LocalDate(2026, 9, 30)

    @Test
    fun tiersFollowLifetimePoints() {
        assertEquals(LoyaltyTier.BRONZE, LoyaltyRules.tierFor(0))
        assertEquals(LoyaltyTier.SILVER, LoyaltyRules.tierFor(500))
        assertEquals(LoyaltyTier.GOLD, LoyaltyRules.tierFor(4000))
        assertEquals(LoyaltyTier.GOLD to 300, LoyaltyRules.nextTier(1200))
        assertNull(LoyaltyRules.nextTier(1500))
    }

    private val reward = Reward("r1", "Cinema 2x1", "", "Parceiro", 300, "2x1", validDays = 60)

    @Test
    fun redeemingDeductsPointsAndIssuesACoupon() {
        val account = LoyaltyAccount(points = 350, lifetimePoints = 900, history = emptyList())
        val (after, coupon) = LoyaltyRules.redeem(account, reward, SubscriptionStatus.ACTIVE, today, "c1", "CODE1")
        assertEquals(50, after.points)
        assertEquals(900, after.lifetimePoints)
        assertEquals(-300, after.history.first().delta)
        assertEquals(LocalDate(2026, 11, 29), coupon.validUntil)
        assertEquals(CouponStatus.ACTIVE, coupon.status(today))
    }

    @Test
    fun redeemingWithoutEnoughPointsFails() {
        val account = LoyaltyAccount(points = 100, lifetimePoints = 100, history = emptyList())
        val error = assertFailsWith<DomainError.NotEligible> {
            LoyaltyRules.redeem(account, reward, SubscriptionStatus.ACTIVE, today, "c1", "CODE1")
        }
        assertEquals(Reason.INSUFFICIENT_POINTS, error.reason)
    }

    private val campaign = CouponCampaign(
        id = "camp",
        title = "10% em eletrodomésticos",
        description = "",
        partner = "Loja",
        discountLabel = "-10%",
        validUntil = LocalDate(2026, 12, 31),
        remainingCodes = 5,
    )

    @Test
    fun claimingTwiceReturnsTheSameCouponWithoutUsingAnotherCode() {
        var taken = 0
        val next = { taken++; "CODE-$taken" }
        val first = CouponRules.claim(campaign, emptyList(), SubscriptionStatus.ACTIVE, today, "id1", next)
        val second = CouponRules.claim(campaign, listOf(first), SubscriptionStatus.ACTIVE, today, "id2", next)
        assertSame(first, second)
        assertEquals(1, taken)
        assertEquals("CODE-1", first.code)
    }

    @Test
    fun emptyPoolAndExpiredCampaignsAreRejected() {
        val empty = assertFailsWith<DomainError.NotEligible> {
            CouponRules.claim(campaign.copy(remainingCodes = 0), emptyList(), SubscriptionStatus.ACTIVE, today, "x") { "C" }
        }
        assertEquals(Reason.COUPON_POOL_EMPTY, empty.reason)

        val noCode = assertFailsWith<DomainError.NotEligible> {
            CouponRules.claim(campaign, emptyList(), SubscriptionStatus.ACTIVE, today, "x") { null }
        }
        assertEquals(Reason.COUPON_POOL_EMPTY, noCode.reason)

        val expired = assertFailsWith<DomainError.NotEligible> {
            CouponRules.claim(campaign, emptyList(), SubscriptionStatus.ACTIVE, LocalDate(2027, 1, 1), "x") { "C" }
        }
        assertEquals(Reason.COUPON_EXPIRED, expired.reason)
    }

    @Test
    fun expiredMembersCannotClaim() {
        val error = assertFailsWith<DomainError.NotEligible> {
            CouponRules.claim(campaign, emptyList(), SubscriptionStatus.EXPIRED, today, "x") { "C" }
        }
        assertEquals(Reason.SUBSCRIPTION_EXPIRED, error.reason)
    }

    @Test
    fun couponStatusReflectsUseAndExpiry() {
        val coupon = Coupon("c", "camp", null, "t", "p", "-10%", "CODE", today, LocalDate(2026, 10, 1), usedOn = null)
        assertEquals(CouponStatus.ACTIVE, coupon.status(LocalDate(2026, 10, 1)))
        assertEquals(CouponStatus.EXPIRED, coupon.status(LocalDate(2026, 10, 2)))
        assertEquals(CouponStatus.USED, coupon.copy(usedOn = today).status(today))
    }
}
