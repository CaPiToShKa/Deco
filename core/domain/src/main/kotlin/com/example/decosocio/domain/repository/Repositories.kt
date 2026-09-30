package com.example.decosocio.domain.repository

import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.model.NewsFeed
import com.example.decosocio.domain.model.ProfileUpdate
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.WithdrawalReceipt
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

data class Session(
    val contactKey: String,
    val accessToken: String,
    val displayName: String,
)

data class DeletionReceipt(
    val requestId: String,
    val requestedOn: LocalDate,
    val completesBy: LocalDate,
)

/*
 * All repositories throw [com.example.decosocio.domain.DomainError] on failure.
 * Implementations: an in-memory demo backend (default) and a BFF-backed HTTP client.
 */

interface AuthRepository {
    val session: StateFlow<Session?>
    suspend fun login(email: String, password: String): Session
    suspend fun logout()
}

interface MemberRepository {
    suspend fun profile(): MemberProfile
    suspend fun updateProfile(update: ProfileUpdate): MemberProfile
    suspend fun consents(): Consents
    suspend fun updateConsents(changes: Map<ConsentPurpose, Boolean>, source: ConsentSource): Consents

    /** Machine-readable copy of everything held about the member (GDPR art. 15 and 20). */
    suspend fun exportData(): String

    /** Starts account deletion (Google Play account-deletion policy, GDPR art. 17). */
    suspend fun requestAccountDeletion(): DeletionReceipt
}

interface MembershipRepository {
    suspend fun subscription(): Subscription
    suspend fun addOns(): List<AddOn>
    suspend fun activateAddOn(id: String): AddOn
    suspend fun withdrawAddOn(id: String): WithdrawalReceipt
    suspend fun cancelAddOn(id: String): AddOn
    suspend fun undoCancellation(id: String): AddOn
}

interface LoyaltyRepository {
    suspend fun account(): LoyaltyAccount
    suspend fun rewards(): List<Reward>
    suspend fun redeem(rewardId: String): Coupon
    suspend fun campaigns(): List<CouponCampaign>
    suspend fun coupons(): List<Coupon>
    suspend fun claim(campaignId: String): Coupon
}

interface NewsRepository {
    suspend fun latest(forceRefresh: Boolean = false): NewsFeed
    suspend fun article(id: String): com.example.decosocio.domain.model.Article?
}

interface DateProvider {
    fun today(): LocalDate
    fun nowEpochMillis(): Long
}
