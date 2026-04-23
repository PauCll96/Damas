package com.example.damas

enum class PlayerColor { RED, BLACK }

enum class PieceType { NORMAL, QUEEN }

/** Modos de juego disponibles */
enum class GameMode { PLAYER_VS_PLAYER, PLAYER_VS_AI }

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

/** Representa un movimiento completo de la IA (puede incluir multicaptura) */
data class AiMove(
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val captures: List<Pair<Int, Int>> = emptyList()
)