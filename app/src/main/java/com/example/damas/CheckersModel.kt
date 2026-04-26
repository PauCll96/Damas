package com.example.damas

enum class PlayerColor { RED, BLACK }
enum class PieceType { NORMAL, QUEEN }
enum class GameMode { PLAYER_VS_PLAYER, PLAYER_VS_AI }

/** Configuración de cada jugador */
data class PlayerSettings(
    val name: String,
    val colorHex: Long
)

/** Configuración global de la partida */
data class GameSettings(
    val mode: GameMode = GameMode.PLAYER_VS_PLAYER,
    val player1: PlayerSettings = PlayerSettings("Jugador 1", 0xFFFF0000), // Rojo
    val player2: PlayerSettings = PlayerSettings("Jugador 2", 0xFF000000), // Negro
    val maxTimeMinutes: Int = 10
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