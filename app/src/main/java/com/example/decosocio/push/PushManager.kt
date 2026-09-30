package com.example.decosocio.push

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.decosocio.R
import com.example.decosocio.SfmcConfig
import com.salesforce.marketingcloud.MarketingCloudConfig
import com.salesforce.marketingcloud.pushfeature.PushFeature
import com.salesforce.marketingcloud.pushfeature.config.PushFeatureConfig
import com.salesforce.marketingcloud.pushfeature.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.sfmcsdk.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Wraps the Salesforce Marketing Cloud MobilePush SDK 11 (unified SFMCSdk API).
 *
 * - The member's SFMC Contact Key is set as the SDK profile ID at login, so a push sent from
 *   Journey Builder to that contact reaches this device.
 * - Push is enabled only for a logged-in member who granted the Android 13+ permission, and is
 *   disabled at logout so a shared device never receives someone else's messages.
 * - The marketing consent is mirrored as the SDK attribute `MarketingPushConsent`; journeys must
 *   filter on it (and on the consent Data Extension kept by the BFF) before sending promotions.
 *
 * Without configuration every call is a no-op and the app runs normally.
 */
class PushManager(
    private val context: Context,
    private val config: SfmcConfig?,
) : PushRegistrar {
    private val _state = MutableStateFlow(PushState(sfmcConfigured = config != null))
    override val state: StateFlow<PushState> = _state.asStateFlow()

    fun initialize() {
        createChannels()
        val cfg = config ?: return
        try {
            val moduleConfig = SFMCSdkModuleConfig.build {
                engagementModuleConfig = MarketingCloudConfig.builder().apply {
                    setApplicationId(cfg.appId)
                    setAccessToken(cfg.accessToken)
                    setMarketingCloudServerUrl(cfg.serverUrl)
                    if (cfg.mid.isNotBlank()) setMid(cfg.mid)
                    // Open/engagement analytics are off until the analytics consent flow is agreed
                    // with the DPO (ePrivacy: tracking needs consent).
                    setAnalyticsEnabled(false)
                    setInboxEnabled(false)
                }.build(context)
                pushFeatureModuleConfig = PushFeatureConfig.builder().apply {
                    if (cfg.senderId.isNotBlank()) setSenderId(cfg.senderId)
                    setNotificationCustomizationOptions(
                        NotificationCustomizationOptions.create(R.drawable.ic_stat_notification),
                    )
                }.build()
            }
            SFMCSdk.configure(context.applicationContext as Application, moduleConfig) { initStatus ->
                val ok = initStatus.status == InitializationStatus.SUCCESS
                _state.update { it.copy(sdkReady = ok, sdkError = if (ok) null else "SFMC SDK initialization failed") }
                if (!ok) Log.e(TAG, "SFMC SDK initialization failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SFMC SDK configuration error", e)
            _state.update { it.copy(sdkError = e.message ?: e.javaClass.simpleName) }
        }
    }

    /** Call after login: links this device to the member's SFMC contact. */
    override fun onLogin(contactKey: String) {
        _state.update { it.copy(contactKey = contactKey) }
        if (config == null) return
        SFMCSdk.requestSdk { sdk ->
            sdk.identity.edit { profileId = contactKey }
        }
        syncPushWithPermission()
    }

    /** Call at logout: stop pushes to this device until the next login. */
    override fun onLogout() {
        _state.update { it.copy(contactKey = null) }
        setPushEnabled(false)
    }

    /** Mirrors the marketing-push consent to the SDK so journeys can filter on it. */
    override fun setMarketingConsent(granted: Boolean) {
        if (config == null) return
        SFMCSdk.requestSdk { sdk ->
            sdk.identity.edit { attributes.put(ATTR_MARKETING_CONSENT, granted.toString()) }
        }
    }

    /** Re-evaluates push after the notification permission changed (or at login). */
    override fun syncPushWithPermission() {
        setPushEnabled(_state.value.contactKey != null && notificationsPermitted())
    }

    override fun notificationsPermitted(): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun setPushEnabled(enabled: Boolean) {
        _state.update { it.copy(pushRequested = enabled) }
        if (config == null) return
        PushFeature.requestSdk { feature ->
            val manager = feature.getPushMessageManager()
            if (enabled && !manager.isPushEnabled()) manager.enablePush()
            if (!enabled && manager.isPushEnabled()) manager.disablePush()
        }
    }

    /** Local notification used by the demo menu to show the notification path without SFMC. */
    override fun showLocalTestNotification(title: String, body: String): Boolean {
        if (!notificationsPermitted()) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()
        try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            return false
        }
        return true
    }

    private fun launchIntent(): android.app.PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, context.getString(R.string.channel_service_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_service_description)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MARKETING, context.getString(R.string.channel_marketing_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_marketing_description)
            },
        )
    }

    companion object {
        private const val TAG = "PushManager"
        const val CHANNEL_SERVICE = "service"
        const val CHANNEL_MARKETING = "marketing"
        const val ATTR_MARKETING_CONSENT = "MarketingPushConsent"
        private const val TEST_NOTIFICATION_ID = 1001
    }
}
