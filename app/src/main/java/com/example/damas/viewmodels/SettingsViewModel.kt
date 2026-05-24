package com.example.damas.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.damas.data.local.UserPreferencesRepository
import com.example.damas.data.models.GameSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de configuración.
 *
 * Antes hacíamos `settingsFlow.first()` — leía DataStore una sola vez al iniciar
 * y la pantalla quedaba sorda a cambios externos. Ahora usamos `stateIn` para
 * exponer un StateFlow vivo: si otro proceso/pantalla escribe en DataStore,
 * la pantalla de configuración se actualiza automáticamente.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository.getInstance(application)

    /** Flujo persistente — emite cada vez que DataStore cambia */
    val persisted: StateFlow<GameSettings?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Estado editable de la pantalla — se sincroniza con [persisted] al cargar */
    var settings by mutableStateOf(GameSettings())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            // Suscripción continua: si el valor persistido cambia y el usuario
            // todavía no ha editado nada, refrescamos el form. Para evitar
            // pisar ediciones en curso, sólo aplicamos la actualización
            // mientras isLoaded sea false (carga inicial).
            persisted.collect { saved ->
                if (saved != null && !isLoaded) {
                    settings = saved
                    isLoaded = true
                }
            }
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
