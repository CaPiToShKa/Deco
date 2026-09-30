package com.example.decosocio.bff

enum class SfmcMode {
    /** Calls are recorded in memory and visible at GET /debug/sfmc-calls. No Salesforce account needed. */
    MOCK,

    /** Real calls to the SFMC REST API with a server-to-server Installed Package. */
    LIVE,
}

/** Server-to-server Installed Package credentials. Never shipped in the mobile app. */
data class SfmcCredentials(
    /** Tenant-specific subdomain, e.g. mc563885gzs27c5t9-63k636ttgm (from the package's Auth Base URI). */
    val subdomain: String,
    val clientId: String,
    val clientSecret: String,
    /** MID of the business unit; optional when the package belongs to that BU. */
    val accountId: String?,
) {
    val authBaseUrl: String get() = "https://$subdomain.auth.marketingcloudapis.com"
}

/** External keys of the Data Extensions and Journey Builder entry events used by the app. */
data class SfmcKeys(
    val profileDataExtension: String,
    val consentsDataExtension: String,
    val addOnsDataExtension: String,
    val couponsDataExtension: String,
    /** Blank = do not fire the event. */
    val consentChangedEvent: String,
    val addOnChangedEvent: String,
    val deletionRequestedEvent: String,
)

data class BffConfig(
    val port: Int,
    val sfmcMode: SfmcMode,
    val credentials: SfmcCredentials?,
    val keys: SfmcKeys,
    val newsFeedUrl: String,
    val tokenTtlSeconds: Long,
) {
    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): BffConfig {
            fun value(key: String, default: String = "") = env[key]?.trim()?.takeIf { it.isNotEmpty() } ?: default
            val mode = if (value("SFMC_MODE", "mock").equals("live", ignoreCase = true)) SfmcMode.LIVE else SfmcMode.MOCK
            val credentials = if (mode == SfmcMode.LIVE) {
                SfmcCredentials(
                    subdomain = value("SFMC_SUBDOMAIN").ifEmpty { error("SFMC_SUBDOMAIN is required when SFMC_MODE=live") },
                    clientId = value("SFMC_CLIENT_ID").ifEmpty { error("SFMC_CLIENT_ID is required when SFMC_MODE=live") },
                    clientSecret = value("SFMC_CLIENT_SECRET").ifEmpty { error("SFMC_CLIENT_SECRET is required when SFMC_MODE=live") },
                    accountId = value("SFMC_ACCOUNT_ID").ifEmpty { null },
                )
            } else {
                null
            }
            return BffConfig(
                port = value("PORT", "8080").toInt(),
                sfmcMode = mode,
                credentials = credentials,
                keys = SfmcKeys(
                    profileDataExtension = value("SFMC_DE_PROFILE", "DECO_App_Profile"),
                    consentsDataExtension = value("SFMC_DE_CONSENTS", "DECO_App_Consents"),
                    addOnsDataExtension = value("SFMC_DE_ADDONS", "DECO_App_AddOns"),
                    couponsDataExtension = value("SFMC_DE_COUPONS", "DECO_App_Coupons"),
                    consentChangedEvent = value("SFMC_EVENT_CONSENT_CHANGED"),
                    addOnChangedEvent = value("SFMC_EVENT_ADDON_CHANGED"),
                    deletionRequestedEvent = value("SFMC_EVENT_DELETION_REQUESTED"),
                ),
                newsFeedUrl = value("NEWS_FEED_URL"),
                tokenTtlSeconds = value("APP_TOKEN_TTL_SECONDS", "3600").toLong(),
            )
        }
    }
}
