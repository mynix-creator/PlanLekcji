package com.mynix.planlekcji.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val NOTIFY_MINUTES_KEY = intPreferencesKey("notify_minutes_before")
        val NOTIFY_ENABLED_KEY = booleanPreferencesKey("notify_enabled")
        val SHOW_LOCK_SCREEN_NOTIF_KEY = booleanPreferencesKey("show_lock_screen_notif")
        val LAST_VIEW_MODE_KEY = intPreferencesKey("last_view_mode")
    }

    val notifyMinutes: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_MINUTES_KEY] ?: 10 }

    val notifyEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFY_ENABLED_KEY] ?: true }
        
    val showLockScreenNotif: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_LOCK_SCREEN_NOTIF_KEY] ?: false }

    val lastViewMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LAST_VIEW_MODE_KEY] ?: 0 }

    suspend fun setNotifyMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_MINUTES_KEY] = minutes
        }
    }

    suspend fun setNotifyEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_ENABLED_KEY] = enabled
        }
    }

    suspend fun setShowLockScreenNotif(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_LOCK_SCREEN_NOTIF_KEY] = enabled
        }
    }

    suspend fun setLastViewMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_VIEW_MODE_KEY] = mode
        }
    }
}
