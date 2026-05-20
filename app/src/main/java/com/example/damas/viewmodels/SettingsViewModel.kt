package com.example.damas.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.damas.data.local.UserPreferencesRepository
import com.example.damas.data.models.GameSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository.getInstance(application)

    var settings by mutableStateOf(GameSettings())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            settings = repository.settingsFlow.first()
            isLoaded = true
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        settings = newSettings
    }

    /** Persiste las preferencias actuales en DataStore */
    fun save() {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }
}
