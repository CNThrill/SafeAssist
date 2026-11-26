package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeassist.maramag.audio.SirenManager
import com.safeassist.maramag.location.LocationProvider
import com.safeassist.maramag.model.AppSettings
import com.safeassist.maramag.model.EmergencyContact
import com.safeassist.maramag.model.LocationResult
import com.safeassist.maramag.model.UserProfile
import com.safeassist.maramag.repository.SafeAssistRepository
import com.safeassist.maramag.utils.EmergencyMessageFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SosViewModel(
    private val repository: SafeAssistRepository,
    private val locationProvider: LocationProvider,
    private val sirenManager: SirenManager
) : ViewModel() {

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

    val isSirenPlaying: StateFlow<Boolean> = sirenManager.isPlaying

    private val _locationResult = MutableStateFlow<LocationResult>(LocationResult.Loading)
    val locationResult: StateFlow<LocationResult> = _locationResult.asStateFlow()

    private val _includeGpsInMessage = MutableStateFlow(false)
    val includeGpsInMessage: StateFlow<Boolean> = _includeGpsInMessage.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(COUNTDOWN_SECONDS)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _countdownFinished = MutableStateFlow(false)
    val countdownFinished: StateFlow<Boolean> = _countdownFinished.asStateFlow()

    private var countdownJob: Job? = null
    private var locationJob: Job? = null
    private var hasPromptedLocationThisSos = false
    private var hasPromptedEnableLocationThisSos = false

    fun startSosCountdown() {
        countdownJob?.cancel()
        locationJob?.cancel()
        sirenManager.stopSiren()
        _countdownFinished.value = false
        _locationResult.value = LocationResult.Loading
        _includeGpsInMessage.value = false

        countdownJob = viewModelScope.launch {
            val settings = repository.appSettings.first()
            if (settings.askGpsBeforeSending && locationProvider.hasLocationPermission()) {
                fetchLocationIfPermitted()
            }
            for (sec in COUNTDOWN_SECONDS downTo 1) {
                _secondsRemaining.value = sec
                delay(1000L)
            }
            _secondsRemaining.value = 0
            if (settings.sirenEnabled) sirenManager.startSiren()
            _countdownFinished.value = true
        }
    }

    fun consumeCountdownFinished() {
        _countdownFinished.value = false
    }

    fun cancelSos() {
        countdownJob?.cancel()
        locationJob?.cancel()
        _countdownFinished.value = false
        sirenManager.stopSiren()
    }

    fun startSosSession() {
        locationJob?.cancel()
        sirenManager.stopSiren()
        hasPromptedLocationThisSos = false
        hasPromptedEnableLocationThisSos = false
        _locationResult.value = LocationResult.Loading
        _includeGpsInMessage.value = false
        viewModelScope.launch {
            val settings = repository.appSettings.first()
            if (settings.sirenEnabled && !settings.caregiverModeEnabled) sirenManager.startSiren()
            if (settings.askGpsBeforeSending && locationProvider.hasLocationPermission()) {
                fetchLocationIfPermitted()
            }
        }
    }

    fun toggleSiren() {
        if (sirenManager.isPlaying.value) {
            sirenManager.stopSiren()
        } else {
            sirenManager.startSiren()
        }
    }

    fun stopSiren() {
        sirenManager.stopSiren()
    }

    fun shouldPromptLocationPermissionThisSos(): Boolean = !hasPromptedLocationThisSos

    fun markLocationPermissionPromptedThisSos() {
        hasPromptedLocationThisSos = true
    }

    fun shouldPromptEnableLocationThisSos(): Boolean = !hasPromptedEnableLocationThisSos

    fun markEnableLocationPromptedThisSos() {
        hasPromptedEnableLocationThisSos = true
    }

    fun markGpsDisabledInSettings() {
        locationJob?.cancel()
        _includeGpsInMessage.value = false
        _locationResult.value = LocationResult.Unavailable("GPS is turned off in Settings.")
    }

    fun markLocationPermissionNeeded() {
        _includeGpsInMessage.value = false
        _locationResult.value = LocationResult.Unavailable("Location permission needed.")
    }

    fun markDeviceLocationOff() {
        locationJob?.cancel()
        _includeGpsInMessage.value = false
        _locationResult.value = LocationResult.Unavailable("Turn on device Location to detect GPS.")
    }

    fun ensureLocationFetchedIfPermitted() {
        if (_locationResult.value is LocationResult.Available) return
        fetchLocationIfPermitted()
    }

    fun fetchLocationIfPermitted() {
        if (locationJob?.isActive == true && _locationResult.value is LocationResult.Loading) {
            return
        }
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            _locationResult.value = LocationResult.Loading
            val result = locationProvider.getCurrentOrLastKnownLocation()
            _locationResult.value = result
            _includeGpsInMessage.value = result is LocationResult.Available
        }
    }

    fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

    fun isDeviceLocationEnabled(): Boolean = locationProvider.isLocationEnabled()

    fun toggleIncludeGps() {
        if (_locationResult.value is LocationResult.Available) {
            _includeGpsInMessage.value = !_includeGpsInMessage.value
        }
    }

    fun buildSosMessage(): String {
        return EmergencyMessageFormatter.formatSosMessage(
            profile = userProfile.value,
            caregiverMode = appSettings.value.caregiverModeEnabled,
            locationResult = _locationResult.value,
            includeGps = _includeGpsInMessage.value
        )
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        locationJob?.cancel()
        sirenManager.stopSiren()
    }

    companion object {
        private const val COUNTDOWN_SECONDS = 5
    }
}
