package com.example.decosocio.api

import com.example.decosocio.domain.DomainError
import com.example.decosocio.domain.ProfileField
import com.example.decosocio.domain.Reason
import com.example.decosocio.domain.model.AddOn
import com.example.decosocio.domain.model.AddOnKind
import com.example.decosocio.domain.model.AddOnState
import com.example.decosocio.domain.model.Address
import com.example.decosocio.domain.model.Article
import com.example.decosocio.domain.model.BillingPeriod
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentRecord
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.Consents
import com.example.decosocio.domain.model.Coupon
import com.example.decosocio.domain.model.CouponCampaign
import com.example.decosocio.domain.model.LoyaltyAccount
import com.example.decosocio.domain.model.MemberProfile
import com.example.decosocio.domain.model.Plan
import com.example.decosocio.domain.model.PointsEntry
import com.example.decosocio.domain.model.ProfileUpdate
import com.example.decosocio.domain.model.Reward
import com.example.decosocio.domain.model.Subscription
import com.example.decosocio.domain.model.SubscriptionStatus
import com.example.decosocio.domain.model.WithdrawalReceipt
import com.example.decosocio.domain.repository.DeletionReceipt
import kotlinx.datetime.LocalDate

// Dates travel as ISO-8601 strings (yyyy-MM-dd). Enums travel by name.

private fun String.date(): LocalDate = LocalDate.parse(this)

fun Address.toDto() = AddressDto(street, postalCode, city)
fun AddressDto.toDomain() = Address(street, postalCode, city)

fun MemberProfile.toDto() = ProfileDto(
    contactKey = contactKey,
    memberNumber = memberNumber,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    address = address?.toDto(),
    nif = nif,
    preferredLanguage = preferredLanguage,
)

fun ProfileDto.toDomain() = MemberProfile(
    contactKey = contactKey,
    memberNumber = memberNumber,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    address = address?.toDomain(),
    nif = nif,
    preferredLanguage = preferredLanguage,
)

fun ProfileUpdate.toDto() = ProfileUpdateDto(firstName, lastName, email, phone, address?.toDto())
fun ProfileUpdateDto.toDomain() = ProfileUpdate(firstName, lastName, email, phone, address?.toDomain())

fun Consents.toDto() = ConsentsDto(
    current = current.mapKeys { it.key.name },
    history = history.map {
        ConsentRecordDto(it.purpose.name, it.granted, it.changedAtEpochMs, it.source.name, it.textVersion)
    },
)

fun ConsentsDto.toDomain() = Consents(
    current = current.mapNotNull { (key, value) -> purposeOrNull(key)?.let { it to value } }.toMap(),
    history = history.mapNotNull { dto ->
        val purpose = purposeOrNull(dto.purpose) ?: return@mapNotNull null
        ConsentRecord(
            purpose = purpose,
            granted = dto.granted,
            changedAtEpochMs = dto.changedAtEpochMs,
            source = ConsentSource.entries.firstOrNull { it.name == dto.source } ?: ConsentSource.SETTINGS,
            textVersion = dto.textVersion,
        )
    },
)

fun Map<ConsentPurpose, Boolean>.toChangesDto(source: ConsentSource) =
    ConsentsUpdateDto(changes = mapKeys { it.key.name }, source = source.name)

fun ConsentsUpdateDto.changesToDomain(): Map<ConsentPurpose, Boolean> =
    changes.mapNotNull { (key, value) -> purposeOrNull(key)?.let { it to value } }.toMap()

fun ConsentsUpdateDto.sourceToDomain(): ConsentSource =
    ConsentSource.entries.firstOrNull { it.name == source } ?: ConsentSource.SETTINGS

private fun purposeOrNull(name: String) = ConsentPurpose.entries.firstOrNull { it.name == name }

fun Plan.toDto() = PlanDto(code, name, priceCents, period.name)
fun PlanDto.toDomain() = Plan(code, name, priceCents, BillingPeriod.valueOf(period))

fun Subscription.toDto(status: SubscriptionStatus) = SubscriptionDto(
    plan = plan.toDto(),
    startDate = startDate.toString(),
    renewalDate = renewalDate.toString(),
    autoRenew = autoRenew,
    paymentMethodLabel = paymentMethodLabel,
    status = status.name,
)

fun SubscriptionDto.toDomain() = Subscription(
    plan = plan.toDomain(),
    startDate = startDate.date(),
    renewalDate = renewalDate.date(),
    autoRenew = autoRenew,
    paymentMethodLabel = paymentMethodLabel,
)

fun AddOn.toDto(): AddOnDto {
    val base = AddOnDto(
        id = id,
        name = name,
        description = description,
        priceCents = priceCents,
        period = period.name,
        kind = kind.name,
        state = "AVAILABLE",
    )
    return when (val s = state) {
        AddOnState.Available -> base
        is AddOnState.Active -> base.copy(
            state = "ACTIVE",
            activatedOn = s.activatedOn.toString(),
            withdrawalDeadline = s.withdrawalDeadline.toString(),
        )
        is AddOnState.CancellationRequested -> base.copy(
            state = "CANCELLATION_REQUESTED",
            activatedOn = s.activatedOn.toString(),
            endsOn = s.endsOn.toString(),
        )
    }
}

fun AddOnDto.toDomain() = AddOn(
    id = id,
    name = name,
    description = description,
    priceCents = priceCents,
    period = BillingPeriod.valueOf(period),
    kind = AddOnKind.valueOf(kind),
    state = when (state) {
        "ACTIVE" -> AddOnState.Active(
            activatedOn = requireNotNull(activatedOn).date(),
            withdrawalDeadline = requireNotNull(withdrawalDeadline).date(),
        )
        "CANCELLATION_REQUESTED" -> AddOnState.CancellationRequested(
            activatedOn = requireNotNull(activatedOn).date(),
            endsOn = requireNotNull(endsOn).date(),
        )
        else -> AddOnState.Available
    },
)

fun WithdrawalReceipt.toDto() =
    WithdrawalReceiptDto(addOnId, addOnName, withdrawnOn.toString(), refundCents, confirmationId)

fun WithdrawalReceiptDto.toDomain() =
    WithdrawalReceipt(addOnId, addOnName, withdrawnOn.date(), refundCents, confirmationId)

fun LoyaltyAccount.toDto() = LoyaltyDto(
    points = points,
    lifetimePoints = lifetimePoints,
    tier = tier.name,
    history = history.map { PointsEntryDto(it.date.toString(), it.delta, it.description) },
)

fun LoyaltyDto.toDomain() = LoyaltyAccount(
    points = points,
    lifetimePoints = lifetimePoints,
    history = history.map { PointsEntry(it.date.date(), it.delta, it.description) },
)

fun Reward.toDto() = RewardDto(id, title, description, partner, costPoints, discountLabel, validDays)
fun RewardDto.toDomain() = Reward(id, title, description, partner, costPoints, discountLabel, validDays)

fun CouponCampaign.toDto() =
    CampaignDto(id, title, description, partner, discountLabel, validUntil.toString(), remainingCodes)

fun CampaignDto.toDomain() =
    CouponCampaign(id, title, description, partner, discountLabel, validUntil.date(), remainingCodes)

fun Coupon.toDto() = CouponDto(
    id = id,
    campaignId = campaignId,
    rewardId = rewardId,
    title = title,
    partner = partner,
    discountLabel = discountLabel,
    code = code,
    issuedOn = issuedOn.toString(),
    validUntil = validUntil.toString(),
    usedOn = usedOn?.toString(),
)

fun CouponDto.toDomain() = Coupon(
    id = id,
    campaignId = campaignId,
    rewardId = rewardId,
    title = title,
    partner = partner,
    discountLabel = discountLabel,
    code = code,
    issuedOn = issuedOn.date(),
    validUntil = validUntil.date(),
    usedOn = usedOn?.date(),
)

fun ArticleDto.toDomain(isSample: Boolean = false) = Article(
    id = id,
    title = title,
    summary = summary,
    body = body,
    category = category,
    publishedOn = publishedOn.date(),
    imageUrl = imageUrl,
    url = url,
    membersOnly = membersOnly,
    isSample = isSample,
)

fun Article.toDto() = ArticleDto(id, title, summary, body, category, publishedOn.toString(), imageUrl, url, membersOnly)

fun DeletionReceipt.toDto() = DeletionReceiptDto(requestId, requestedOn.toString(), completesBy.toString())
fun DeletionReceiptDto.toDomain() = DeletionReceipt(requestId, requestedOn.date(), completesBy.date())

/* ---- Errors: one stable code per failure, so both sides map them the same way. ---- */

object ErrorCodes {
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val NOT_FOUND = "NOT_FOUND"
    const val VALIDATION_PREFIX = "VALIDATION:"
    const val NOT_ELIGIBLE_PREFIX = "NOT_ELIGIBLE:"
    const val INTERNAL = "INTERNAL"
}

fun DomainError.toErrorDto(): ErrorDto = when (this) {
    is DomainError.NotEligible -> ErrorDto(ErrorCodes.NOT_ELIGIBLE_PREFIX + reason.name, message.orEmpty())
    is DomainError.Validation -> ErrorDto(
        ErrorCodes.VALIDATION_PREFIX + fields.joinToString(",") { it.name },
        message.orEmpty(),
    )
    is DomainError.NotFound -> ErrorDto(ErrorCodes.NOT_FOUND, message.orEmpty())
    is DomainError.Unauthorized -> ErrorDto(ErrorCodes.UNAUTHORIZED, message.orEmpty())
    is DomainError.Network -> ErrorDto(ErrorCodes.INTERNAL, message.orEmpty())
    is DomainError.Server -> ErrorDto(code, message.orEmpty())
}

fun ErrorDto.toDomainError(): DomainError = when {
    code == ErrorCodes.UNAUTHORIZED -> DomainError.Unauthorized()
    code == ErrorCodes.NOT_FOUND -> DomainError.NotFound(message)
    code.startsWith(ErrorCodes.NOT_ELIGIBLE_PREFIX) -> {
        val reason = Reason.entries.firstOrNull { it.name == code.removePrefix(ErrorCodes.NOT_ELIGIBLE_PREFIX) }
        if (reason != null) DomainError.NotEligible(reason) else DomainError.Server(code)
    }
    code.startsWith(ErrorCodes.VALIDATION_PREFIX) -> DomainError.Validation(
        code.removePrefix(ErrorCodes.VALIDATION_PREFIX)
            .split(",")
            .mapNotNull { name -> ProfileField.entries.firstOrNull { it.name == name } }
            .toSet(),
    )
    else -> DomainError.Server(code)
}
