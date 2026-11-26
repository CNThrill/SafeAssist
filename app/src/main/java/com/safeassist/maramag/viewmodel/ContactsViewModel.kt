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

class ContactsViewModel(private val repository: SafeAssistRepository) : ViewModel() {

    val emergencyContacts: StateFlow<List<EmergencyContact>> = repository.emergencyContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val appSettings: StateFlow<AppSettings> = repository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val userProfile: StateFlow<UserProfile> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserProfile()
    )

    fun saveContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.addOrUpdateContact(contact)
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun pinContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.pinPrimaryContact(contact.id)
        }
    }

    fun unpinContacts() {
        viewModelScope.launch {
            repository.clearPrimaryContacts()
        }
    }

    fun updateMdrrmoHotline(hotline: String) {
        viewModelScope.launch {
            repository.setMdrrmoHotline(hotline)
        }
    }
}
