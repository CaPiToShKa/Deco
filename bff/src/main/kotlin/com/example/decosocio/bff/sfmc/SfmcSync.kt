package com.example.decosocio.bff.sfmc

import com.example.decosocio.bff.SfmcKeys
import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentRules
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.repository.DeletionReceipt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Pushes member changes to SFMC after the BFF has committed them.
 *
 * The member's request never waits for Marketing Cloud: SFMC is the marketing engine, not the
 * system of record, and its API limits are shared with campaign sends. Each sync runs in the
 * background with retries; in production replace this with a durable outbox (a DB table or a
 * queue) so nothing is lost if the process restarts.
 */
class SfmcSync(
    private val client: SfmcClient,
    private val keys: SfmcKeys,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val retryDelaysMs: List<Long> = listOf(1_000, 4_000, 9_000),
) {
    private val log = LoggerFactory.getLogger(SfmcSync::class.java)
    val failures = CopyOnWriteArrayList<String>()

    private fun now(): String = Instant.ofEpochMilli(clock()).toString()

    fun profileUpdated(profile: MemberProfile) = enqueue("profile ${profile.contactKey}") {
        client.upsertRows(
            keys.profileDataExtension,
            listOf(
                DeRow(
                    keys = mapOf("ContactKey" to profile.contactKey),
                    values = mapOf(
                        "MemberNumber" to profile.memberNumber,
                        "FirstName" to profile.firstName,
                        "LastName" to profile.lastName,
                        "Email" to profile.email,
                        "Phone" to profile.phone.orEmpty(),
                        "PostalCode" to profile.address?.postalCode.orEmpty(),
                        "City" to profile.address?.city.orEmpty(),
                        "PreferredLanguage" to profile.preferredLanguage,
                        "UpdatedAt" to now(),
                    ),
                ),
            ),
        )
    }

    fun consentsChanged(contactKey: String, consents: Consents) = enqueue("consents $contactKey") {
        val values = mapOf(
            "Newsletter" to consents.isGranted(ConsentPurpose.NEWSLETTER).toString(),
            "MarketingPush" to consents.isGranted(ConsentPurpose.MARKETING_PUSH).toString(),
            "PartnerOffers" to consents.isGranted(ConsentPurpose.PARTNER_OFFERS).toString(),
            "Personalisation" to consents.isGranted(ConsentPurpose.PERSONALISATION).toString(),
            "TextVersion" to ConsentRules.TEXT_VERSION,
            "UpdatedAt" to now(),
        )
        client.upsertRows(keys.consentsDataExtension, listOf(DeRow(mapOf("ContactKey" to contactKey), values)))
        if (keys.consentChangedEvent.isNotBlank()) client.fireEntryEvent(keys.consentChangedEvent, contactKey, values)
    }

    fun addOnChanged(contactKey: String, addOn: AddOn, action: String) = enqueue("add-on ${addOn.id} $contactKey") {
        val state = addOn.state
        val values = mapOf(
            "Name" to addOn.name,
            "Status" to when (state) {
                AddOnState.Available -> "INACTIVE"
                is AddOnState.Active -> "ACTIVE"
                is AddOnState.CancellationRequested -> "CANCELLATION_REQUESTED"
            },
            "ActivatedOn" to when (state) {
                is AddOnState.Active -> state.activatedOn.toString()
                is AddOnState.CancellationRequested -> state.activatedOn.toString()
                AddOnState.Available -> ""
            },
            "WithdrawalDeadline" to ((state as? AddOnState.Active)?.withdrawalDeadline?.toString() ?: ""),
            "EndsOn" to ((state as? AddOnState.CancellationRequested)?.endsOn?.toString() ?: ""),
            "LastAction" to action,
            "UpdatedAt" to now(),
        )
        client.upsertRows(
            keys.addOnsDataExtension,
            listOf(DeRow(mapOf("ContactKey" to contactKey, "AddOnId" to addOn.id), values)),
        )
        if (keys.addOnChangedEvent.isNotBlank()) {
            client.fireEntryEvent(keys.addOnChangedEvent, contactKey, values + ("AddOnId" to addOn.id))
        }
    }

    fun couponIssued(contactKey: String, coupon: Coupon) = enqueue("coupon ${coupon.id} $contactKey") {
        client.upsertRows(
            keys.couponsDataExtension,
            listOf(
                DeRow(
                    keys = mapOf("ContactKey" to contactKey, "CouponId" to coupon.id),
                    values = mapOf(
                        "CampaignId" to coupon.campaignId.orEmpty(),
                        "RewardId" to coupon.rewardId.orEmpty(),
                        "Code" to coupon.code,
                        "IssuedOn" to coupon.issuedOn.toString(),
                        "ValidUntil" to coupon.validUntil.toString(),
                    ),
                ),
            ),
        )
    }

    fun deletionRequested(contactKey: String, receipt: DeletionReceipt) = enqueue("deletion $contactKey") {
        if (keys.deletionRequestedEvent.isNotBlank()) {
            client.fireEntryEvent(
                keys.deletionRequestedEvent,
                contactKey,
                mapOf("RequestId" to receipt.requestId, "CompletesBy" to receipt.completesBy.toString()),
            )
        }
    }

    private fun enqueue(what: String, block: suspend () -> Unit) {
        scope.launch {
            for (attempt in 0..retryDelaysMs.size) {
                try {
                    block()
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (attempt == retryDelaysMs.size) {
                        log.error("SFMC sync failed for {} after {} attempts: {}", what, attempt + 1, e.message)
                        failures += "$what: ${e.message}"
                    } else {
                        log.warn("SFMC sync for {} failed (attempt {}), retrying: {}", what, attempt + 1, e.message)
                        delay(retryDelaysMs[attempt])
                    }
                }
            }
        }
    }
}
