package com.example.decosocio.domain.model

import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.Reason
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class LoyaltyTier(val minLifetimePoints: Int) { BRONZE(0), SILVER(500), GOLD(1500) }

data class PointsEntry(
    val date: LocalDate,
    val delta: Int,
    val description: String,
)

data class LoyaltyAccount(
    val points: Int,
    val lifetimePoints: Int,
    val history: List<PointsEntry>,
) {
    val tier: LoyaltyTier get() = LoyaltyRules.tierFor(lifetimePoints)
}

data class Reward(
    val id: String,
    val title: String,
    val description: String,
    val partner: String,
    val costPoints: Int,
    val discountLabel: String,
    val validDays: Int,
)

data class CouponCampaign(
    val id: String,
    val title: String,
    val description: String,
    val partner: String,
    val discountLabel: String,
    val validUntil: LocalDate,
    /** Null = unlimited generic code; otherwise how many single-use codes are left. */
    val remainingCodes: Int?,
)

enum class CouponStatus { ACTIVE, USED, EXPIRED }

data class Coupon(
    val id: String,
    val campaignId: String?,
    val rewardId: String?,
    val title: String,
    val partner: String,
    val discountLabel: String,
    val code: String,
    val issuedOn: LocalDate,
    val validUntil: LocalDate,
    val usedOn: LocalDate?,
) {
    fun status(today: LocalDate): CouponStatus = when {
        usedOn != null -> CouponStatus.USED
        today > validUntil -> CouponStatus.EXPIRED
        else -> CouponStatus.ACTIVE
    }
}

object LoyaltyRules {
    fun tierFor(lifetimePoints: Int): LoyaltyTier =
        LoyaltyTier.entries.last { lifetimePoints >= it.minLifetimePoints }

    /** Next tier and the lifetime points still missing, or null at the top tier. */
    fun nextTier(lifetimePoints: Int): Pair<LoyaltyTier, Int>? {
        val next = LoyaltyTier.entries.firstOrNull { it.minLifetimePoints > lifetimePoints } ?: return null
        return next to (next.minLifetimePoints - lifetimePoints)
    }

    fun redeem(
        account: LoyaltyAccount,
        reward: Reward,
        subscription: SubscriptionStatus,
        today: LocalDate,
        couponId: String,
        code: String,
    ): Pair<LoyaltyAccount, Coupon> {
        if (!SubscriptionRules.hasMemberBenefits(subscription)) throw DomainError.NotEligible(Reason.SUBSCRIPTION_EXPIRED)
        if (account.points < reward.costPoints) throw DomainError.NotEligible(Reason.INSUFFICIENT_POINTS)
        val updated = account.copy(
            points = account.points - reward.costPoints,
            history = listOf(PointsEntry(today, -reward.costPoints, reward.title)) + account.history,
        )
        val coupon = Coupon(
            id = couponId,
            campaignId = null,
            rewardId = reward.id,
            title = reward.title,
            partner = reward.partner,
            discountLabel = reward.discountLabel,
            code = code,
            issuedOn = today,
            validUntil = today.plus(DatePeriod(days = reward.validDays)),
            usedOn = null,
        )
        return updated to coupon
    }
}

object CouponRules {
    /**
     * Claims a single-use code from a campaign. Idempotent: a member who already holds a coupon
     * for the campaign gets the same coupon back instead of burning another code.
     * [nextCode] must atomically take one code from the pool (null when the pool is empty).
     */
    fun claim(
        campaign: CouponCampaign,
        existing: List<Coupon>,
        subscription: SubscriptionStatus,
        today: LocalDate,
        couponId: String,
        nextCode: () -> String?,
    ): Coupon {
        existing.firstOrNull { it.campaignId == campaign.id }?.let { return it }
        if (!SubscriptionRules.hasMemberBenefits(subscription)) throw DomainError.NotEligible(Reason.SUBSCRIPTION_EXPIRED)
        if (today > campaign.validUntil) throw DomainError.NotEligible(Reason.COUPON_EXPIRED)
        if (campaign.remainingCodes == 0) throw DomainError.NotEligible(Reason.COUPON_POOL_EMPTY)
        val code = nextCode() ?: throw DomainError.NotEligible(Reason.COUPON_POOL_EMPTY)
        return Coupon(
            id = couponId,
            campaignId = campaign.id,
            rewardId = null,
            title = campaign.title,
            partner = campaign.partner,
            discountLabel = campaign.discountLabel,
            code = code,
            issuedOn = today,
            validUntil = campaign.validUntil,
            usedOn = null,
        )
    }
}
