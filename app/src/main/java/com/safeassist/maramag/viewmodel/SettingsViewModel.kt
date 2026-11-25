package com.safeassist.maramag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeassist.maramag.audio.SirenManager
import com.safeassist.maramag.model.AppSettings
import com.safeassist.maramag.repository.SafeAssistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SafeAssistRepository,
    private val sirenManager: SirenManager
) : ViewModel() {

    val appSettings: StateFlow<AppSettings> = repository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val isSirenPlaying: StateFlow<Boolean> = sirenManager.isPlaying

    fun setSirenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSirenEnabled(enabled)
            if (!enabled) {
                sirenManager.stopSiren()
            }
        }
    }

    fun setAskGpsBeforeSending(ask: Boolean) {
        viewModelScope.launch {
            repository.setAskGpsBeforeSending(ask)
        }
    }

    fun toggleTestSiren(): Boolean {
        return if (sirenManager.isPlaying.value) {
            sirenManager.stopSiren()
            false
        } else {
            sirenManager.startSiren()
        }
    }

    fun stopSiren() {
        sirenManager.stopSiren()
    }

    override fun onCleared() {
        super.onCleared()
        sirenManager.stopSiren()
    }
}
