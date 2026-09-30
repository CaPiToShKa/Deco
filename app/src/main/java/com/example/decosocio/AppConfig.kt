package com.example.decosocio

import com.example.decosocio.data.BackendMode

/** MobilePush app credentials from SFMC Setup > MobilePush > Administration (not API secrets). */
data class SfmcConfig(
    val appId: String,
    val accessToken: String,
    val serverUrl: String,
    val mid: String,
    val senderId: String,
)

data class AppConfig(
    val backendMode: BackendMode,
    val bffBaseUrl: String,
    val newsFeedUrl: String,
    val renewUrl: String,
    val privacyPolicyUrl: String,
    /** Null when Firebase or the MobilePush keys are missing: push stays off, the app still works. */
    val sfmc: SfmcConfig?,
    val versionName: String,
) {
    val isDemo: Boolean get() = backendMode == BackendMode.DEMO

    companion object {
        fun fromBuildConfig(): AppConfig {
            val sfmc = SfmcConfig(
                appId = BuildConfig.SFMC_APP_ID,
                accessToken = BuildConfig.SFMC_ACCESS_TOKEN,
                serverUrl = BuildConfig.SFMC_SERVER_URL,
                mid = BuildConfig.SFMC_MID,
                senderId = BuildConfig.SFMC_SENDER_ID,
            )
            val sfmcReady = BuildConfig.FIREBASE_CONFIGURED &&
                sfmc.appId.isNotBlank() && sfmc.accessToken.isNotBlank() && sfmc.serverUrl.isNotBlank()
            return AppConfig(
                backendMode = if (BuildConfig.BACKEND_MODE.equals("bff", ignoreCase = true)) BackendMode.BFF else BackendMode.DEMO,
                bffBaseUrl = BuildConfig.BFF_BASE_URL,
                newsFeedUrl = BuildConfig.NEWS_FEED_URL,
                renewUrl = BuildConfig.RENEW_URL,
                privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
                sfmc = sfmc.takeIf { sfmcReady },
                versionName = BuildConfig.VERSION_NAME,
            )
        }
    }
}
