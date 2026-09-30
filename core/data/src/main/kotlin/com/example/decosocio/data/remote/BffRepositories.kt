package com.example.decosocio.data.remote

import com.example.decosocio.api.AddOnDto
import com.example.decosocio.api.ApiRoutes
import com.example.decosocio.api.CampaignDto
import com.example.decosocio.api.ConsentsDto
import com.example.decosocio.api.CouponDto
import com.example.decosocio.api.DeletionReceiptDto
import com.example.decosocio.api.DemoLoginRequest
import com.example.decosocio.api.LoginResponse
import com.example.decosocio.api.LoyaltyDto
import com.example.decosocio.api.MemberExportDto
import com.example.decosocio.api.ProfileDto
import com.example.decosocio.api.RewardDto
import com.example.decosocio.api.SubscriptionDto
import com.example.decosocio.api.WithdrawalReceiptDto
import com.example.decosocio.api.toChangesDto
import com.example.decosocio.api.toDomain
import com.example.decosocio.api.toDto
import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.model.ProfileUpdate
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.WithdrawalReceipt
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.DeletionReceipt
import com.example.decosocio.domain.repository.LoyaltyRepository
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.domain.repository.MembershipRepository
import com.example.decosocio.domain.repository.Session
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** All repositories backed by the BFF over HTTPS. One instance per app process. */
class BffBackend(baseUrl: String, engine: HttpClientEngine? = null) :
    AuthRepository, MemberRepository, MembershipRepository, LoyaltyRepository {

    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    private val http = BffHttp(
        baseUrl = baseUrl,
        tokenProvider = { _session.value?.accessToken },
        onUnauthorized = { _session.value = null },
        engine = engine,
    )

    override suspend fun login(email: String, password: String): Session {
        val response: LoginResponse = http.send(HttpMethod.Post, ApiRoutes.LOGIN, DemoLoginRequest(email.trim(), password))
        return Session(response.contactKey, response.accessToken, response.displayName).also { _session.value = it }
    }

    override suspend fun logout() {
        _session.value = null
    }

    override suspend fun profile(): MemberProfile = http.get<ProfileDto>(ApiRoutes.PROFILE).toDomain()

    override suspend fun updateProfile(update: ProfileUpdate): MemberProfile =
        http.send<com.example.decosocio.api.ProfileUpdateDto, ProfileDto>(HttpMethod.Patch, ApiRoutes.PROFILE, update.toDto()).toDomain()

    override suspend fun consents(): Consents = http.get<ConsentsDto>(ApiRoutes.CONSENTS).toDomain()

    override suspend fun updateConsents(changes: Map<ConsentPurpose, Boolean>, source: ConsentSource): Consents =
        http.send<com.example.decosocio.api.ConsentsUpdateDto, ConsentsDto>(HttpMethod.Put, ApiRoutes.CONSENTS, changes.toChangesDto(source)).toDomain()

    override suspend fun exportData(): String {
        val dto = http.get<MemberExportDto>(ApiRoutes.EXPORT)
        return PrettyJson.encodeToString(dto)
    }

    override suspend fun requestAccountDeletion(): DeletionReceipt =
        http.delete<DeletionReceiptDto>(ApiRoutes.ME).toDomain()

    override suspend fun subscription(): Subscription = http.get<SubscriptionDto>(ApiRoutes.SUBSCRIPTION).toDomain()

    override suspend fun addOns(): List<AddOn> = http.get<List<AddOnDto>>(ApiRoutes.ADDONS).map { it.toDomain() }

    override suspend fun activateAddOn(id: String): AddOn = http.post<AddOnDto>(ApiRoutes.addOnActivation(id)).toDomain()

    override suspend fun withdrawAddOn(id: String): WithdrawalReceipt =
        http.post<WithdrawalReceiptDto>(ApiRoutes.addOnWithdrawal(id)).toDomain()

    override suspend fun cancelAddOn(id: String): AddOn = http.post<AddOnDto>(ApiRoutes.addOnCancellation(id)).toDomain()

    override suspend fun undoCancellation(id: String): AddOn =
        http.delete<AddOnDto>(ApiRoutes.addOnCancellation(id)).toDomain()

    override suspend fun account(): LoyaltyAccount = http.get<LoyaltyDto>(ApiRoutes.LOYALTY).toDomain()

    override suspend fun rewards(): List<Reward> = http.get<List<RewardDto>>(ApiRoutes.REWARDS).map { it.toDomain() }

    override suspend fun redeem(rewardId: String): Coupon = http.post<CouponDto>(ApiRoutes.rewardRedemption(rewardId)).toDomain()

    override suspend fun campaigns(): List<CouponCampaign> = http.get<List<CampaignDto>>(ApiRoutes.CAMPAIGNS).map { it.toDomain() }

    override suspend fun coupons(): List<Coupon> = http.get<List<CouponDto>>(ApiRoutes.COUPONS).map { it.toDomain() }

    override suspend fun claim(campaignId: String): Coupon = http.post<CouponDto>(ApiRoutes.campaignClaim(campaignId)).toDomain()

    private companion object {
        val PrettyJson = Json(from = com.example.decosocio.api.ApiJson) { prettyPrint = true }
    }
}
