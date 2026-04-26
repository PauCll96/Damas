package com.example.damas.data.local

import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams
import kotlin.math.abs

sealed class MoveType {
    object Invalid : MoveType()
    object Simple : MoveType()
    data class Capture(val victimRow: Int, val victimCol: Int) : MoveType()
}

object GameRules {
    fun getMoveType(board: Array<Array<Cell>>, from: Cell, toR: Int, toC: Int): MoveType {
        val piece = from.piece ?: return MoveType.Invalid
        val rowDiff = toR - from.row
        val colDiff = toC - from.col
        val absRD = abs(rowDiff)
        val absCD = abs(colDiff)

        if (absRD != absCD || absRD == 0 || board[toR][toC].piece != null) return MoveType.Invalid
        val dr = rowDiff / absRD
        val dc = colDiff / absCD

        if (piece.type == PieceType.NORMAL) {
            if (absRD == 1) {
                val forward = if (piece.team == Teams.RED) -1 else 1
                return if (dr == forward) MoveType.Simple else MoveType.Invalid
            }
            if (absRD == 2) {
                val midR = from.row + dr
                val midC = from.col + dc
                val midP = board[midR][midC].piece
                return if (midP != null && midP.team != piece.team) MoveType.Capture(midR, midC) else MoveType.Invalid
            }
        } else {
            var piecesInBetween = 0
            var victimPos: Pair<Int, Int>? = null
            for (i in 1 until absRD) {
                val r = from.row + i * dr
                val c = from.col + i * dc
                val p = board[r][c].piece
                if (p != null) {
                    piecesInBetween++
                    if (p.team == piece.team) return MoveType.Invalid
                    victimPos = r to c
                }
            }
            return when (piecesInBetween) {
                0 -> MoveType.Simple
                1 -> MoveType.Capture(victimPos!!.first, victimPos.second)
                else -> MoveType.Invalid
            }
        }
        return MoveType.Invalid
    }

    fun hasAnyCapture(board: Array<Array<Cell>>, team: Teams): Boolean {
        return board.flatten().filter { it.piece?.team == team }.any { canPieceCapture(board, it) }
    }

    fun canPieceCapture(board: Array<Array<Cell>>, cell: Cell): Boolean {
        val piece = cell.piece ?: return false
        val range = if (piece.type == PieceType.QUEEN) 1..7 else listOf(2)
        for (dr in listOf(-1, 1)) {
            for (dc in listOf(-1, 1)) {
                for (dist in range) {
                    val tr = cell.row + dr * dist
                    val tc = cell.col + dc * dist
                    if (tr in 0..7 && tc in 0..7 && getMoveType(board, cell, tr, tc) is MoveType.Capture) return true
                }
            }
        }
        return false
    }
    
    fun checkWinner(board: Array<Array<Cell>>): Teams? {
        val pieces = board.flatten().mapNotNull { it.piece }
        if (pieces.none { it.team == Teams.BLACK }) return Teams.RED
        if (pieces.none { it.team == Teams.RED }) return Teams.BLACK
        return null
    }
}
