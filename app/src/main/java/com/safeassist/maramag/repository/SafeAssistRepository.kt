package com.safeassist.maramag.repository

import com.safeassist.maramag.data.SettingsDataStore
import com.safeassist.maramag.database.SafeAssistDatabase
import com.safeassist.maramag.model.AppSettings
import com.safeassist.maramag.model.EmergencyContact
import com.safeassist.maramag.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SafeAssistRepository(
    private val database: SafeAssistDatabase,
    private val settingsDataStore: SettingsDataStore
) {
    private val profileDao = database.userProfileDao()
    private val contactDao = database.emergencyContactDao()

    val userProfile: Flow<UserProfile> = profileDao.observeProfile().map { it ?: UserProfile() }
    val emergencyContacts: Flow<List<EmergencyContact>> = contactDao.observeContacts()
    val appSettings: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun saveUserProfile(profile: UserProfile) {
        profileDao.upsertProfile(profile.copy(id = 1))
        val currentSettings = settingsDataStore.settingsFlow.first()
        if (!currentSettings.sirenUserOverridden) {
            settingsDataStore.setSirenEnabled(
                enabled = profile.mobilityProfile.defaultSirenEnabled,
                userOverridden = false
            )
        }
    }

    suspend fun addOrUpdateContact(contact: EmergencyContact) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: EmergencyContact) {
        contactDao.deleteContact(contact)
    }

    suspend fun setHasCompletedSetup(completed: Boolean) =
        settingsDataStore.setHasCompletedSetup(completed)

    suspend fun setHasRequestedInitialPermissions(requested: Boolean) =
        settingsDataStore.setHasRequestedInitialPermissions(requested)

    suspend fun setSosCountdownSeconds(seconds: Int) =
        settingsDataStore.setSosCountdownSeconds(seconds)

    suspend fun setSirenEnabled(enabled: Boolean) =
        settingsDataStore.setSirenEnabled(enabled, userOverridden = true)

    suspend fun setAskGpsBeforeSending(ask: Boolean) =
        settingsDataStore.setAskGpsBeforeSending(ask)

    suspend fun setCaregiverModeEnabled(enabled: Boolean) =
        settingsDataStore.setCaregiverModeEnabled(enabled)

    suspend fun setMdrrmoHotline(hotline: String) =
        settingsDataStore.setMdrrmoHotline(hotline)
}
