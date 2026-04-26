package com.example.damas.data.local

import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams

object GameLogic {
    fun calculateAiMove(board: Array<Array<Cell>>, activePiece: Cell?): Triple<Cell, Int, Int>? {
        val pieces = if (activePiece != null) listOf(activePiece) else board.flatten().filter { it.piece?.team == Teams.BLACK }
        val caps = mutableListOf<Triple<Cell, Int, Int>>()
        val moves = mutableListOf<Triple<Cell, Int, Int>>()
        
        for (sq in pieces) {
            val range = if (sq.piece?.type == PieceType.QUEEN) 1..7 else 1..2
            for (dr in listOf(-1, 1)) {
                for (dc in listOf(-1, 1)) {
                    for (dist in range) {
                        val tr = sq.row + dr * dist
                        val tc = sq.col + dc * dist
                        if (tr in 0..7 && tc in 0..7) {
                            val m = GameRules.getMoveType(board, sq, tr, tc)
                            if (m is MoveType.Capture) caps.add(Triple(sq, tr, tc)) 
                            else if (m is MoveType.Simple) moves.add(Triple(sq, tr, tc))
                        }
                    }
                }
            }
        }
        
        return if (caps.isNotEmpty()) caps.random() 
               else if (moves.isNotEmpty() && !GameRules.hasAnyCapture(board, Teams.BLACK)) moves.random() 
               else null
    }
}
