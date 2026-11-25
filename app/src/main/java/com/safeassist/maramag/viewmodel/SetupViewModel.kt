package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.repository.SafeAssistRepository
import kotlinx.coroutines.launch

class SetupViewModel(private val repository: SafeAssistRepository) : ViewModel() {

    fun completeSetup(
        fullName: String,
        contactNumber: String,
        barangay: String,
        purok: String,
        landmark: String,
        mobilityProfile: MobilityProfile,
        assistanceNotes: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val profile = UserProfile(
                id = 1,
                fullName = fullName.trim(),
                contactNumber = contactNumber.trim(),
                barangay = barangay.trim(),
                purok = purok.trim(),
                landmark = landmark.trim(),
                mobilityProfile = mobilityProfile,
                assistanceNotes = assistanceNotes.trim()
            )
            repository.saveUserProfile(profile)
            repository.setHasCompletedSetup(true)
            onComplete()
        }
    }

    fun skipSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.setHasCompletedSetup(true)
            onComplete()
        }
    }
}
