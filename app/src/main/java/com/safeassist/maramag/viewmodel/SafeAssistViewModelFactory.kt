package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.safeassist.maramag.SafeAssistApplication

class SafeAssistViewModelFactory(
    private val app: SafeAssistApplication
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(app.repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(app.repository) as T
            modelClass.isAssignableFrom(ContactsViewModel::class.java) ->
                ContactsViewModel(app.repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app.repository, app.sirenManager) as T
            modelClass.isAssignableFrom(PrepareEmergencyViewModel::class.java) ->
                PrepareEmergencyViewModel(app.repository) as T
            modelClass.isAssignableFrom(SosViewModel::class.java) ->
                SosViewModel(app.repository, app.locationProvider, app.sirenManager) as T
            modelClass.isAssignableFrom(SetupViewModel::class.java) ->
                SetupViewModel(app.repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
