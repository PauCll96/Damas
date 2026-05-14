package com.example.damas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.damas.data.local.GameHistoryRepository
import com.example.damas.data.models.GameResult

class HistoryViewModel : ViewModel() {

    var gameHistory by mutableStateOf<List<GameResult>>(emptyList())
        private set

    // null = ninguna seleccionada; se usa en bi-panel tablet
    var selectedGame by mutableStateOf<GameResult?>(null)
        private set

    init {
        // Carga el historial acumulado en el repositorio singleton
        gameHistory = GameHistoryRepository.history
    }

    fun refreshHistory() {
        gameHistory = GameHistoryRepository.history
    }

    fun onGameSelected(game: GameResult) {
        selectedGame = game
    }

    fun clearSelection() {
        selectedGame = null
    }
}
