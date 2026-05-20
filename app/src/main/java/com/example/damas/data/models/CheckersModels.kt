package com.example.damas.data.models

import com.example.damas.data.constants.GameMode

/** Configuración de cada jugador */
data class PlayerSettings(
    var name: String,
    var colorHex: Long
)

/** Configuración global de la partida */
data class GameSettings(
    var mode: GameMode = GameMode.PLAYER_VS_PLAYER,
    var player1: PlayerSettings = PlayerSettings("Jugador 1", 0xFFFF0000), // Rojo
    var player2: PlayerSettings = PlayerSettings("Jugador 2", 0xFF000000), // Negro
    var maxTimeMinutes: Int = 10
)

data class AiMove(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val captures: List<Pair<Int, Int>> = emptyList()
)

/** Resultado de una partida completada, usado en el historial */
data class GameResult(
    val id: Int = 0,
    val date: String,
    val winnerName: String,
    val timeLeft: String,
    val player1Name: String,
    val player2Name: String,
    val moveLog: String = ""  // log completo de movimientos (1.4)
)
