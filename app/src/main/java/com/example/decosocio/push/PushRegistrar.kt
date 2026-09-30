package com.example.decosocio.push

import kotlinx.coroutines.flow.StateFlow

data class PushState(
    /** MobilePush keys and google-services.json are present in this build. */
    val sfmcConfigured: Boolean,
    val sdkReady: Boolean = false,
    val sdkError: String? = null,
    /** What the app asked the SDK: push on for a logged-in member, off otherwise. */
    val pushRequested: Boolean = false,
    val contactKey: String? = null,
)

/** What screens need from push; implemented by [PushManager], faked in unit tests. */
interface PushRegistrar {
    val state: StateFlow<PushState>
    fun onLogin(contactKey: String)
    fun onLogout()
    fun setMarketingConsent(granted: Boolean)
    fun syncPushWithPermission()
    fun notificationsPermitted(): Boolean
    fun showLocalTestNotification(title: String, body: String): Boolean
}
