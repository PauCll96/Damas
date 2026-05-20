package com.example.damas.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.damas.data.local.GameRecord
import com.example.damas.data.local.GameRecordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// (2.13) AndroidViewModel para acceder al contexto de aplicación (Room)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRecordRepository.getInstance(application)

    // Flow del DAO convertido a StateFlow — la UI reacciona automáticamente a cambios
    val games: StateFlow<List<GameRecord>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // null = ninguna seleccionada; se usa en bi-panel tablet
    var selectedGame by mutableStateOf<GameRecord?>(null)
        private set

    fun onGameSelected(game: GameRecord) {
        selectedGame = game
    }

    fun clearSelection() {
        selectedGame = null
    }

    fun deleteGame(id: Int) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}
