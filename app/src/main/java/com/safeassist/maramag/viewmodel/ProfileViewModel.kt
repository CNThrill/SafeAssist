package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.repository.SafeAssistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: SafeAssistRepository) : ViewModel() {

    // Null until Room emits the real row so ProfileFragment doesn't treat the empty default as loaded
    val userProfile: StateFlow<UserProfile?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun saveProfile(
        fullName: String,
        contactNumber: String,
        barangay: String,
        purok: String,
        streetOrHousehold: String,
        landmark: String,
        mobilityProfile: MobilityProfile,
        assistanceNotes: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val profile = UserProfile(
                id = 1,
                fullName = fullName.trim(),
                contactNumber = contactNumber.trim(),
                barangay = barangay.trim(),
                purok = purok.trim(),
                streetOrHousehold = streetOrHousehold.trim(),
                landmark = landmark.trim(),
                mobilityProfile = mobilityProfile,
                assistanceNotes = assistanceNotes.trim()
            )
            repository.saveUserProfile(profile)
            onSaved()
        }
    }
}
