package com.example.decosocio.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

/** Device-only preferences. Nothing personal is stored here. */
class AppPreferences(private val context: Context) {

    /** Service notifications about the member's own contract (renewal, add-ons). Default on. */
    val serviceNotifications: Flow<Boolean> =
        context.dataStore.data.map { it[SERVICE_NOTIFICATIONS] ?: true }

    suspend fun setServiceNotifications(enabled: Boolean) {
        context.dataStore.edit { it[SERVICE_NOTIFICATIONS] = enabled }
    }

    private companion object {
        val SERVICE_NOTIFICATIONS = booleanPreferencesKey("service_notifications")
    }
}
