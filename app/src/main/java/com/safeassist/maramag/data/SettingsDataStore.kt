package com.safeassist.maramag.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.safeassist.maramag.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safeassist_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_SETUP = booleanPreferencesKey("has_completed_setup")
        val HAS_REQUESTED_INITIAL_PERMISSIONS = booleanPreferencesKey("has_requested_initial_permissions")
        val SOS_COUNTDOWN_SECONDS = intPreferencesKey("sos_countdown_seconds")
        val SIREN_ENABLED = booleanPreferencesKey("siren_enabled")
        val SIREN_USER_OVERRIDDEN = booleanPreferencesKey("siren_user_overridden")
        val ASK_GPS_BEFORE_SENDING = booleanPreferencesKey("ask_gps_before_sending")
        val CAREGIVER_MODE_ENABLED = booleanPreferencesKey("caregiver_mode_enabled")
        val MDRRMO_HOTLINE = stringPreferencesKey("mdrrmo_hotline")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            hasCompletedSetup = prefs[Keys.HAS_COMPLETED_SETUP] ?: false,
            hasRequestedInitialPermissions = prefs[Keys.HAS_REQUESTED_INITIAL_PERMISSIONS] ?: false,
            sosCountdownSeconds = prefs[Keys.SOS_COUNTDOWN_SECONDS]?.takeIf { it in listOf(0, 3, 5) } ?: 5,
            sirenEnabled = prefs[Keys.SIREN_ENABLED] ?: false,
            sirenUserOverridden = prefs[Keys.SIREN_USER_OVERRIDDEN] ?: false,
            askGpsBeforeSending = prefs[Keys.ASK_GPS_BEFORE_SENDING] ?: true,
            caregiverModeEnabled = prefs[Keys.CAREGIVER_MODE_ENABLED] ?: false,
            mdrrmoHotline = prefs[Keys.MDRRMO_HOTLINE]?.takeIf { it.isNotBlank() }
                ?: AppSettings.DEFAULT_MDRRMO_HOTLINE
        )
    }

    suspend fun setHasCompletedSetup(completed: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_SETUP] = completed }
    }

    suspend fun setHasRequestedInitialPermissions(requested: Boolean) {
        context.dataStore.edit { it[Keys.HAS_REQUESTED_INITIAL_PERMISSIONS] = requested }
    }

    suspend fun setSosCountdownSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.SOS_COUNTDOWN_SECONDS] = seconds.coerceIn(0, 5) }
    }

    suspend fun setSirenEnabled(enabled: Boolean, userOverridden: Boolean = true) {
        context.dataStore.edit {
            it[Keys.SIREN_ENABLED] = enabled
            if (userOverridden) {
                it[Keys.SIREN_USER_OVERRIDDEN] = true
            }
        }
    }

    suspend fun setAskGpsBeforeSending(ask: Boolean) {
        context.dataStore.edit { it[Keys.ASK_GPS_BEFORE_SENDING] = ask }
    }

    suspend fun setCaregiverModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CAREGIVER_MODE_ENABLED] = enabled }
    }

    suspend fun setMdrrmoHotline(hotline: String) {
        context.dataStore.edit {
            it[Keys.MDRRMO_HOTLINE] = hotline.trim().ifBlank { AppSettings.DEFAULT_MDRRMO_HOTLINE }
        }
    }
}
