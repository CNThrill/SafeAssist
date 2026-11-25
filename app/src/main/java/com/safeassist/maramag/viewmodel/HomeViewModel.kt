package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeassist.maramag.model.AppSettings
import com.safeassist.maramag.model.EmergencyContact
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.repository.SafeAssistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: SafeAssistRepository) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserProfile()
    )

    val appSettings: StateFlow<AppSettings> = repository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun toggleCaregiverMode() {
        viewModelScope.launch {
            val current = appSettings.value.caregiverModeEnabled
            repository.setCaregiverModeEnabled(!current)
        }
    }
}
