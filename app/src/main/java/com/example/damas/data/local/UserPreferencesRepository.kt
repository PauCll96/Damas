package com.example.damas.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.damas.data.models.GameSettings
import com.example.damas.data.models.PlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository private constructor(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    companion object {
        private val PLAYER1_NAME  = stringPreferencesKey("player1_name")
        private val PLAYER1_COLOR = longPreferencesKey("player1_color")
        private val PLAYER2_NAME  = stringPreferencesKey("player2_name")
        private val PLAYER2_COLOR = longPreferencesKey("player2_color")
        private val MAX_TIME      = intPreferencesKey("max_time_minutes")

        @Volatile private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    /** Flujo continuo de preferencias guardadas — emite cada vez que cambian */
    val settingsFlow: Flow<GameSettings> = dataStore.data.map { prefs ->
        GameSettings(
            player1 = PlayerSettings(
                name     = prefs[PLAYER1_NAME]  ?: "Jugador 1",
                colorHex = prefs[PLAYER1_COLOR] ?: 0xFFFF0000L
            ),
            player2 = PlayerSettings(
                name     = prefs[PLAYER2_NAME]  ?: "Jugador 2",
                colorHex = prefs[PLAYER2_COLOR] ?: 0xFF000000L
            ),
            maxTimeMinutes = prefs[MAX_TIME] ?: 10
        )
    }

    suspend fun saveSettings(settings: GameSettings) {
        dataStore.edit { prefs ->
            prefs[PLAYER1_NAME]  = settings.player1.name
            prefs[PLAYER1_COLOR] = settings.player1.colorHex
            prefs[PLAYER2_NAME]  = settings.player2.name
            prefs[PLAYER2_COLOR] = settings.player2.colorHex
            prefs[MAX_TIME]      = settings.maxTimeMinutes
        }
    }
}
