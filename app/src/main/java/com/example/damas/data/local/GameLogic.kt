package com.example.damas.data.local

import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams

object GameLogic {

    /** Rango de distancias a explorar según tipo de pieza */
    private fun moveRange(cell: Cell): IntRange =
        if (cell.piece?.type == PieceType.QUEEN) 1..7 else 1..1

    private fun captureRange(cell: Cell): IntRange =
        if (cell.piece?.type == PieceType.QUEEN) 1..7 else 2..2

    /** Todas las capturas disponibles para una pieza concreta */
    fun getCaptureMoves(board: Array<Array<Cell>>, cell: Cell): List<Triple<Cell, Int, Int>> {
        val result = mutableListOf<Triple<Cell, Int, Int>>()
        for (dr in listOf(-1, 1)) for (dc in listOf(-1, 1)) for (dist in captureRange(cell)) {
            val tr = cell.row + dr * dist
            val tc = cell.col + dc * dist
            if (tr in 0..7 && tc in 0..7 &&
                GameRules.getMoveType(board, cell, tr, tc) is MoveType.Capture) {
                result.add(Triple(cell, tr, tc))
            }
        }
        return result
    }

    /** Todos los movimientos simples disponibles para una pieza concreta */
    fun getSimpleMoves(board: Array<Array<Cell>>, cell: Cell): List<Triple<Cell, Int, Int>> {
        val result = mutableListOf<Triple<Cell, Int, Int>>()
        for (dr in listOf(-1, 1)) for (dc in listOf(-1, 1)) for (dist in moveRange(cell)) {
            val tr = cell.row + dr * dist
            val tc = cell.col + dc * dist
            if (tr in 0..7 && tc in 0..7 &&
                GameRules.getMoveType(board, cell, tr, tc) is MoveType.Simple) {
                result.add(Triple(cell, tr, tc))
            }
        }
        return result
    }

    /**
     * Calcula el mejor movimiento de la IA (BLACK).
     * Prioriza capturas sobre movimientos simples.
     * Si se pasa [activePiece], restringe la búsqueda a esa pieza (multi-captura).
     */
    fun calculateAiMove(board: Array<Array<Cell>>, activePiece: Cell?): Triple<Cell, Int, Int>? {
        val pieces = if (activePiece != null)
            listOf(activePiece)
        else
            board.flatten().filter { it.piece?.team == Teams.BLACK }

        val captures = pieces.flatMap { getCaptureMoves(board, it) }
        if (captures.isNotEmpty()) return captures.random()

        // Solo usa movimientos simples si no hay capturas disponibles en todo el equipo
        if (GameRules.hasAnyCapture(board, Teams.BLACK)) return null
        val simpleMoves = pieces.flatMap { getSimpleMoves(board, it) }
        return if (simpleMoves.isNotEmpty()) simpleMoves.random() else null
    }
}
