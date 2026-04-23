package com.example.damas

enum class PlayerColor { RED, BLACK }
enum class PieceType { NORMAL, QUEEN }
enum class GameMode { PLAYER_VS_PLAYER, PLAYER_VS_AI }

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

data class Piece(
    val color: PlayerColor,
    val type: PieceType = PieceType.NORMAL
)

data class Square(
    val row: Int,
    val col: Int,
    val piece: Piece? = null,
    val isSelected: Boolean = false
)

data class AiMove(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val captures: List<Pair<Int, Int>> = emptyList()
)