package com.example.decosocio.data.demo

import com.example.decosocio.api.ApiJson
import com.example.decosocio.api.MemberExportDto
import com.example.decosocio.api.toDto
import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.Reason
import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.AddOnRules
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentRules
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.CouponRules
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.LoyaltyRules
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.model.ProfileUpdate
import com.example.decosocio.domain.model.ProfileValidator
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.SubscriptionRules
import com.example.decosocio.domain.model.SubscriptionStatus
import com.example.decosocio.domain.model.WithdrawalReceipt
import com.example.decosocio.domain.model.apply
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.DeletionReceipt
import com.example.decosocio.domain.repository.LoyaltyRepository
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.domain.repository.MembershipRepository
import com.example.decosocio.domain.repository.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import java.time.Instant
import java.util.UUID

/**
 * In-memory stand-in for the BFF. Applies exactly the same domain rules the real backend uses,
 * so the demo behaves like production: withdrawal windows, single-use codes, consent history.
 * The BFF module also uses it as its demo data store.
 */
class DemoBackend(
    private val control: DemoControl,
    private val dates: DateProvider,
    private val latencyMs: Long = 350,
    private val newId: () -> String = { UUID.randomUUID().toString().take(8).uppercase() },
) : AuthRepository, MemberRepository, MembershipRepository, LoyaltyRepository {

    private val mutex = Mutex()
    private var state: DemoState = DemoSeed.initial(dates.today())
    private var seenResetGeneration = control.settings.value.resetGeneration

    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    /** Runs [block] like a network call: latency, optional simulated failure, serialized access. */
    private suspend fun <T> call(requireSession: Boolean = true, block: () -> T): T {
        if (latencyMs > 0) delay(latencyMs)
        val settings = control.settings.value
        if (settings.networkError) throw DomainError.Network()
        return mutex.withLock {
            if (settings.resetGeneration != seenResetGeneration) {
                seenResetGeneration = settings.resetGeneration
                state = DemoSeed.initial(dates.today())
            }
            if (requireSession && _session.value == null) throw DomainError.Unauthorized()
            block()
        }
    }

    private fun currentSubscription(): Subscription =
        DemoSeed.subscription(control.settings.value.subscription, dates.today())

    private fun subscriptionStatus(): SubscriptionStatus =
        SubscriptionRules.status(currentSubscription(), dates.today())

    /* ---------------- Auth ---------------- */

    override suspend fun login(email: String, password: String): Session = call(requireSession = false) {
        val ok = email.trim().equals(DemoSeed.DEMO_EMAIL, ignoreCase = true) && password == DemoSeed.DEMO_PASSWORD
        if (!ok) throw DomainError.Unauthorized()
        Session(
            contactKey = state.profile.contactKey,
            accessToken = "demo-${newId()}",
            displayName = state.profile.fullName,
        ).also { _session.value = it }
    }

    override suspend fun logout() {
        _session.value = null
    }

    /** Used by the BFF, which authenticates callers itself and then acts on their behalf. */
    fun actAs(session: Session) {
        _session.value = session
    }

    /* ---------------- Member ---------------- */

    override suspend fun profile(): MemberProfile = call { state.profile }

    override suspend fun updateProfile(update: ProfileUpdate): MemberProfile = call {
        val errors = ProfileValidator.validate(update)
        if (errors.isNotEmpty()) throw DomainError.Validation(errors)
        state = state.copy(profile = state.profile.apply(update))
        state.profile
    }

    override suspend fun consents(): Consents = call { state.consents }

    override suspend fun updateConsents(changes: Map<ConsentPurpose, Boolean>, source: ConsentSource): Consents = call {
        state = state.copy(consents = ConsentRules.apply(state.consents, changes, dates.nowEpochMillis(), source))
        state.consents
    }

    override suspend fun exportData(): String = call {
        val today = dates.today()
        val dto = MemberExportDto(
            exportedAt = Instant.ofEpochMilli(dates.nowEpochMillis()).toString(),
            profile = state.profile.toDto(),
            subscription = currentSubscription().toDto(subscriptionStatus()),
            addOns = state.addOns.map { AddOnRules.settle(it, today).toDto() },
            consents = state.consents.toDto(),
            loyalty = state.loyalty.toDto(),
            coupons = visibleCoupons().map { it.toDto() },
        )
        PrettyJson.encodeToString(dto)
    }

    override suspend fun requestAccountDeletion(): DeletionReceipt = call {
        state.deletion?.let { throw DomainError.NotEligible(Reason.DELETION_ALREADY_REQUESTED) }
        val today = dates.today()
        val receipt = DeletionReceipt(
            requestId = "DEL-${newId()}",
            requestedOn = today,
            completesBy = today.plus(DatePeriod(days = 30)),
        )
        state = state.copy(deletion = receipt)
        receipt
    }

    /* ---------------- Membership ---------------- */

    override suspend fun subscription(): Subscription = call { currentSubscription() }

    override suspend fun addOns(): List<AddOn> = call {
        val today = dates.today()
        state = state.copy(addOns = state.addOns.map { AddOnRules.settle(it, today) })
        state.addOns
    }

    private fun updateAddOn(id: String, transform: (AddOn) -> AddOn): AddOn {
        val current = state.addOns.firstOrNull { it.id == id } ?: throw DomainError.NotFound("add-on $id")
        val updated = transform(AddOnRules.settle(current, dates.today()))
        state = state.copy(addOns = state.addOns.map { if (it.id == id) updated else it })
        return updated
    }

    override suspend fun activateAddOn(id: String): AddOn = call {
        updateAddOn(id) { AddOnRules.activate(it, dates.today(), subscriptionStatus()) }
    }

    override suspend fun withdrawAddOn(id: String): WithdrawalReceipt = call {
        var receipt: WithdrawalReceipt? = null
        updateAddOn(id) { addOn ->
            val (after, r) = AddOnRules.withdraw(addOn, dates.today(), confirmationId = "RET-${newId()}")
            receipt = r
            after
        }
        requireNotNull(receipt)
    }

    override suspend fun cancelAddOn(id: String): AddOn = call {
        updateAddOn(id) { AddOnRules.requestCancellation(it, dates.today()) }
    }

    override suspend fun undoCancellation(id: String): AddOn = call {
        updateAddOn(id) { AddOnRules.undoCancellation(it, dates.today()) }
    }

    /* ---------------- Loyalty & coupons ---------------- */

    override suspend fun account(): LoyaltyAccount = call { state.loyalty }

    override suspend fun rewards(): List<Reward> = call { state.rewards }

    override suspend fun redeem(rewardId: String): Coupon = call {
        val reward = state.rewards.firstOrNull { it.id == rewardId } ?: throw DomainError.NotFound("reward $rewardId")
        val prefix = rewardId.removePrefix("r-").take(5).uppercase()
        val (account, coupon) = LoyaltyRules.redeem(
            account = state.loyalty,
            reward = reward,
            subscription = subscriptionStatus(),
            today = dates.today(),
            couponId = "cp-${newId()}",
            code = DemoSeed.generateCode(prefix),
        )
        state = state.copy(loyalty = account, coupons = listOf(coupon) + state.coupons)
        coupon
    }

    override suspend fun campaigns(): List<CouponCampaign> = call {
        if (control.settings.value.emptyCoupons) emptyList() else state.campaigns
    }

    private fun visibleCoupons(): List<Coupon> =
        if (control.settings.value.emptyCoupons) emptyList() else state.coupons

    override suspend fun coupons(): List<Coupon> = call { visibleCoupons() }

    override suspend fun claim(campaignId: String): Coupon = call {
        val campaign = state.campaigns.firstOrNull { it.id == campaignId } ?: throw DomainError.NotFound("campaign $campaignId")
        var pool = state.codePools[campaignId].orEmpty()
        val coupon = CouponRules.claim(
            campaign = campaign,
            existing = state.coupons,
            subscription = subscriptionStatus(),
            today = dates.today(),
            couponId = "cp-${newId()}",
        ) {
            // Atomic because we hold the mutex: take the first code and remove it from the pool.
            pool.firstOrNull()?.also { pool = pool.drop(1) }
        }
        if (state.coupons.none { it.id == coupon.id }) {
            state = state.copy(
                coupons = listOf(coupon) + state.coupons,
                codePools = state.codePools + (campaignId to pool),
                campaigns = state.campaigns.map {
                    if (it.id == campaignId && it.remainingCodes != null) it.copy(remainingCodes = pool.size) else it
                },
            )
        }
        coupon
    }

    private companion object {
        val PrettyJson = kotlinx.serialization.json.Json(from = ApiJson) { prettyPrint = true }
    }
}
