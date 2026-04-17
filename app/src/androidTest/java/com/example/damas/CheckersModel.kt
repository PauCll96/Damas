package com.example.damas // Assegura't que el package coincideix amb el teu

enum class PlayerColor {
    RED, BLACK
}

enum class PieceType {
    NORMAL, QUEEN
}

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