package com.example.decosocio.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Routes of the BFF (Backend-for-Frontend). The app never calls SFMC directly. */
object ApiRoutes {
    const val LOGIN = "/v1/auth/demo-login"
    const val PROFILE = "/v1/me/profile"
    const val CONSENTS = "/v1/me/consents"
    const val SUBSCRIPTION = "/v1/me/subscription"
    const val ADDONS = "/v1/me/addons"
    fun addOnActivation(id: String) = "/v1/me/addons/$id/activation"
    fun addOnWithdrawal(id: String) = "/v1/me/addons/$id/withdrawal"
    fun addOnCancellation(id: String) = "/v1/me/addons/$id/cancellation"
    const val LOYALTY = "/v1/me/loyalty"
    const val REWARDS = "/v1/rewards"
    fun rewardRedemption(id: String) = "/v1/me/rewards/$id/redemption"
    const val CAMPAIGNS = "/v1/campaigns"
    const val COUPONS = "/v1/me/coupons"
    fun campaignClaim(id: String) = "/v1/me/campaigns/$id/claim"
    const val EXPORT = "/v1/me/export"
    const val ME = "/v1/me"
    const val NEWS = "/v1/news"
}

val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class DemoLoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val contactKey: String,
    val displayName: String,
)

@Serializable
data class AddressDto(val street: String, val postalCode: String, val city: String)

@Serializable
data class ProfileDto(
    val contactKey: String,
    val memberNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val address: AddressDto? = null,
    val nif: String? = null,
    val preferredLanguage: String = "pt-PT",
)

@Serializable
data class ProfileUpdateDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val address: AddressDto? = null,
)

@Serializable
data class ConsentRecordDto(
    val purpose: String,
    val granted: Boolean,
    val changedAtEpochMs: Long,
    val source: String,
    val textVersion: String,
)

@Serializable
data class ConsentsDto(
    val current: Map<String, Boolean>,
    val history: List<ConsentRecordDto>,
)

@Serializable
data class ConsentsUpdateDto(
    val changes: Map<String, Boolean>,
    val source: String,
)

@Serializable
data class PlanDto(val code: String, val name: String, val priceCents: Long, val period: String)

@Serializable
data class SubscriptionDto(
    val plan: PlanDto,
    val startDate: String,
    val renewalDate: String,
    val autoRenew: Boolean,
    val paymentMethodLabel: String,
    /** Server-computed status, informational; the app recomputes it with the same rules. */
    val status: String,
)

@Serializable
data class AddOnDto(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Long,
    val period: String,
    val kind: String,
    /** AVAILABLE | ACTIVE | CANCELLATION_REQUESTED */
    val state: String,
    val activatedOn: String? = null,
    val withdrawalDeadline: String? = null,
    val endsOn: String? = null,
)

@Serializable
data class WithdrawalReceiptDto(
    val addOnId: String,
    val addOnName: String,
    val withdrawnOn: String,
    val refundCents: Long,
    val confirmationId: String,
)

@Serializable
data class PointsEntryDto(val date: String, val delta: Int, val description: String)

@Serializable
data class LoyaltyDto(
    val points: Int,
    val lifetimePoints: Int,
    val tier: String,
    val history: List<PointsEntryDto>,
)

@Serializable
data class RewardDto(
    val id: String,
    val title: String,
    val description: String,
    val partner: String,
    val costPoints: Int,
    val discountLabel: String,
    val validDays: Int,
)

@Serializable
data class CampaignDto(
    val id: String,
    val title: String,
    val description: String,
    val partner: String,
    val discountLabel: String,
    val validUntil: String,
    val remainingCodes: Int? = null,
)

@Serializable
data class CouponDto(
    val id: String,
    val campaignId: String? = null,
    val rewardId: String? = null,
    val title: String,
    val partner: String,
    val discountLabel: String,
    val code: String,
    val issuedOn: String,
    val validUntil: String,
    val usedOn: String? = null,
)

@Serializable
data class ArticleDto(
    val id: String,
    val title: String,
    val summary: String,
    val body: String = "",
    val category: String = "",
    val publishedOn: String,
    val imageUrl: String? = null,
    val url: String? = null,
    val membersOnly: Boolean = false,
)

@Serializable
data class DeletionReceiptDto(val requestId: String, val requestedOn: String, val completesBy: String)

@Serializable
data class ErrorDto(val code: String, val message: String)

@Serializable
data class MemberExportDto(
    val exportedAt: String,
    val profile: ProfileDto,
    val subscription: SubscriptionDto,
    val addOns: List<AddOnDto>,
    val consents: ConsentsDto,
    val loyalty: LoyaltyDto,
    val coupons: List<CouponDto>,
)
