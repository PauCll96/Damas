package com.example.damas.data.local

import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams

data class Piece(
    val team: Teams,
    val type: PieceType = PieceType.NORMAL
)
