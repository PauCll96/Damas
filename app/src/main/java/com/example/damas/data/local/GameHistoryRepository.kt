package com.example.damas.data.local

import com.example.damas.data.models.GameResult

/**
 * Almacenamiento en memoria de partidas completadas.
 * Singleton de proceso: persiste mientras la app está viva.
 */
object GameHistoryRepository {

    private val _history = mutableListOf<GameResult>()

    val history: List<GameResult> get() = _history.toList()

    fun addGame(result: GameResult) {
        _history.add(result.copy(id = _history.size))
    }

    fun clear() {
        _history.clear()
    }
}
