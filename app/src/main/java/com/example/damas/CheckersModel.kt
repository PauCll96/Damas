package com.example.damas

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