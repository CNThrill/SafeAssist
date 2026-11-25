package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeassist.maramag.model.AppSettings
import com.safeassist.maramag.model.AssistanceNeed
import com.safeassist.maramag.model.EmergencyReport
import com.safeassist.maramag.model.IncidentType
import com.safeassist.maramag.model.MobilityProfile
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.model.WhoNeedsHelp
import com.safeassist.maramag.repository.SafeAssistRepository
import com.safeassist.maramag.utils.EmergencyMessageFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrepareEmergencyViewModel(private val repository: SafeAssistRepository) : ViewModel() {

    private val _draft = MutableStateFlow(EmergencyReport())
    val draft: StateFlow<EmergencyReport> = _draft.asStateFlow()

    private val _isDraftReady = MutableStateFlow(false)
    val isDraftReady: StateFlow<Boolean> = _isDraftReady.asStateFlow()

    private var initializedForSession = false

    val userProfile: StateFlow<UserProfile> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserProfile()
    )

    val emergencyContacts: StateFlow<List<com.safeassist.maramag.model.EmergencyContact>> = repository.emergencyContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val appSettings: StateFlow<AppSettings> = repository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    fun startNewOrResume() {
        if (initializedForSession) return
        initializedForSession = true
        viewModelScope.launch {
            val profile = repository.userProfile.first()
            val settings = repository.appSettings.first()
            val initialWho = if (settings.caregiverModeEnabled) {
                WhoNeedsHelp.FAMILY_MEMBER
            } else {
                WhoNeedsHelp.ME
            }
            val initialNeeds = if (!settings.caregiverModeEnabled &&
                profile.mobilityProfile == MobilityProfile.MOBILITY
            ) {
                listOf(AssistanceNeed.MOBILITY_SUPPORT)
            } else {
                emptyList()
            }
            _draft.value = EmergencyReport(
                whoNeedsHelp = initialWho,
                barangay = if (settings.caregiverModeEnabled) "" else profile.barangay,
                purok = if (settings.caregiverModeEnabled) "" else profile.purok,
                street = if (settings.caregiverModeEnabled) "" else profile.streetOrHousehold,
                landmark = if (settings.caregiverModeEnabled) "" else profile.landmark,
                assistanceNeeds = initialNeeds,
                extraDetails = if (settings.caregiverModeEnabled) "" else profile.assistanceNotes.take(120),
                occurredAtMillis = System.currentTimeMillis()
            )
            _isDraftReady.value = true
        }
    }

    fun resetDraft() {
        initializedForSession = false
        _isDraftReady.value = false
        _draft.value = EmergencyReport()
    }

    fun updateWho(who: WhoNeedsHelp) {
        _draft.value = _draft.value.copy(whoNeedsHelp = who)
    }

    fun updatePersonName(name: String) {
        _draft.value = _draft.value.copy(personName = name)
    }

    fun updateLocation(
        barangay: String,
        purok: String,
        street: String,
        landmark: String,
        householdDescription: String
    ) {
        _draft.value = _draft.value.copy(
            barangay = barangay.trim(),
            purok = purok.trim(),
            street = street.trim(),
            landmark = landmark.trim(),
            householdDescription = householdDescription.trim()
        )
    }

    fun updateIncidentType(type: IncidentType) {
        _draft.value = _draft.value.copy(incidentType = type)
    }

    fun toggleAssistanceNeed(need: AssistanceNeed) {
        val current = _draft.value.assistanceNeeds.toMutableList()
        if (current.contains(need)) {
            current.remove(need)
        } else {
            current.add(need)
        }
        _draft.value = _draft.value.copy(assistanceNeeds = current)
    }

    fun updateExtraDetails(details: String) {
        _draft.value = _draft.value.copy(extraDetails = details.take(120))
    }

    fun updateOccurredAt(millis: Long) {
        _draft.value = _draft.value.copy(occurredAtMillis = millis)
    }

    fun buildFormattedSummary(): String {
        return EmergencyMessageFormatter.formatPreparedReport(
            report = _draft.value,
            profile = userProfile.value
        )
    }
}
