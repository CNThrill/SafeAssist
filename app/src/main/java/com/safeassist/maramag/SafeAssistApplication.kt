package com.safeassist.maramag

import android.app.Application
import com.safeassist.maramag.audio.SirenManager
import com.safeassist.maramag.data.SettingsDataStore
import com.safeassist.maramag.database.SafeAssistDatabase
import com.safeassist.maramag.location.LocationProvider
import com.safeassist.maramag.repository.SafeAssistRepository

class SafeAssistApplication : Application() {

    val database: SafeAssistDatabase by lazy { SafeAssistDatabase.getInstance(this) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val repository: SafeAssistRepository by lazy {
        SafeAssistRepository(database, settingsDataStore)
    }
    val locationProvider: LocationProvider by lazy { LocationProvider(this) }
    val sirenManager: SirenManager by lazy { SirenManager(this) }
}
