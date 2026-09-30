package com.example.decosocio.domain.model

/**
 * Marketing purposes that need prior, specific, opt-in consent (GDPR art. 6(1)(a),
 * Lei 41/2004 art. 13.º-A, CNPD Diretriz 2022/1). All default to false.
 * Service notifications about the member's own contract are NOT here: they are a separate,
 * local preference based on the contract, and are never used for promotions.
 */
enum class ConsentPurpose { NEWSLETTER, MARKETING_PUSH, PARTNER_OFFERS, PERSONALISATION }

enum class ConsentSource { ONBOARDING, SETTINGS }

data class ConsentRecord(
    val purpose: ConsentPurpose,
    val granted: Boolean,
    val changedAtEpochMs: Long,
    val source: ConsentSource,
    val textVersion: String,
)

data class Consents(
    val current: Map<ConsentPurpose, Boolean>,
    val history: List<ConsentRecord>,
) {
    fun isGranted(purpose: ConsentPurpose): Boolean = current[purpose] == true

    /** True once the member has made at least one explicit choice (onboarding done). */
    val hasBeenAsked: Boolean get() = history.isNotEmpty()
}

object ConsentRules {
    /** Bump when the consent wording shown in the app changes; stored with every record. */
    const val TEXT_VERSION = "2026-09"

    fun defaults(): Consents = Consents(ConsentPurpose.entries.associateWith { false }, emptyList())

    /**
     * Applies the member's choices. Every purpose shown on screen is recorded on the first
     * (onboarding) save, even when left off, so there is proof of the choice; afterwards only
     * real changes are appended to the history.
     */
    fun apply(
        consents: Consents,
        changes: Map<ConsentPurpose, Boolean>,
        nowEpochMs: Long,
        source: ConsentSource,
    ): Consents {
        val recordAll = !consents.hasBeenAsked
        val newRecords = changes
            .filter { (purpose, granted) -> recordAll || consents.current[purpose] != granted }
            .map { (purpose, granted) -> ConsentRecord(purpose, granted, nowEpochMs, source, TEXT_VERSION) }
        if (newRecords.isEmpty()) return consents
        return Consents(
            current = consents.current + changes,
            history = newRecords + consents.history,
        )
    }

    /** Marketing push needs both the member's consent and the Android notification permission. */
    fun marketingPushAllowed(consents: Consents, notificationPermissionGranted: Boolean): Boolean =
        notificationPermissionGranted && consents.isGranted(ConsentPurpose.MARKETING_PUSH)
}
