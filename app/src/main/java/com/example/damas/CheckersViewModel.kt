package com.example.damas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class CheckersViewModel : ViewModel() {

    // --- CONFIGURACIÓN Y LOBBY ---
    var settings by mutableStateOf(GameSettings())
    var isGameStarted by mutableStateOf(false)
        private set

    // --- ESTADO ---
    var boardState by mutableStateOf(createInitialBoard())
        private set

    var currentPlayer by mutableStateOf(PlayerColor.RED)
        private set

    var winner by mutableStateOf<PlayerColor?>(null)
        private set

    var timeElapsed by mutableStateOf(0L)
        private set

    var isAiThinking by mutableStateOf(false)
        private set

    // Pieza que debe continuar capturando (encadenamiento)
    private var activeMultiCapturePiece by mutableStateOf<Square?>(null)

    val aiColor = PlayerColor.BLACK

    init {
        startTimer()
    }

    // --- ACCIONES DE PARTIDA ---
    fun startGame() {
        boardState = createInitialBoard()
        currentPlayer = PlayerColor.RED
        winner = null
        timeElapsed = 0L
        isAiThinking = false
        activeMultiCapturePiece = null
        isGameStarted = true
    }

    fun resetToMenu() {
        isGameStarted = false
    }

    fun surrender() {
        winner = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
    }

    // --- INTERACCIÓN ---
    fun onSquareClicked(row: Int, col: Int) {
        if (winner != null || isAiThinking || !isGameStarted) return
        if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == PlayerColor.BLACK) return

        val clicked = boardState[row][col]
        val selected = findSelectedSquare()

        if (activeMultiCapturePiece != null) {
            if (selected != null && clicked.piece == null) {
                val move = getMoveType(selected, row, col)
                if (move is MoveType.Capture) executeMove(selected, row, col, move)
            } else if (clicked.row == activeMultiCapturePiece?.row && clicked.col == activeMultiCapturePiece?.col) {
                selectSquare(row, col)
            }
            return
        }

        if (selected != null && clicked.piece == null) {
            val move = getMoveType(selected, row, col)
            if (move != MoveType.Invalid) {
                if (hasAnyCapture(currentPlayer) && move is MoveType.Simple) {
                    clearSelection()
                    return
                }
                executeMove(selected, row, col, move)
            } else {
                clearSelection()
            }
        } else if (clicked.piece?.color == currentPlayer) {
            selectSquare(row, col)
        } else {
            clearSelection()
        }
    }

    sealed class MoveType {
        object Invalid : MoveType(); object Simple : MoveType()
        data class Capture(val victimRow: Int, val victimCol: Int) : MoveType()
    }

    private fun getMoveType(from: Square, toR: Int, toC: Int): MoveType {
        val piece = from.piece ?: return MoveType.Invalid
        val rowDiff = toR - from.row
        val colDiff = toC - from.col
        val absRD = abs(rowDiff); val absCD = abs(colDiff)

        if (absRD != absCD || absRD == 0 || boardState[toR][toC].piece != null) return MoveType.Invalid
        val dr = rowDiff / absRD; val dc = colDiff / absCD

        if (piece.type == PieceType.NORMAL) {
            if (absRD == 1) {
                val forward = if (piece.color == PlayerColor.RED) -1 else 1
                return if (dr == forward) MoveType.Simple else MoveType.Invalid
            }
            if (absRD == 2) {
                val midR = from.row + dr; val midC = from.col + dc
                val midP = boardState[midR][midC].piece
                return if (midP != null && midP.color != piece.color) MoveType.Capture(midR, midC) else MoveType.Invalid
            }
        } else {
            var piecesInBetween = 0; var victimPos: Pair<Int, Int>? = null
            for (i in 1 until absRD) {
                val r = from.row + i * dr; val c = from.col + i * dc
                val p = boardState[r][c].piece
                if (p != null) {
                    piecesInBetween++; if (p.color == piece.color) return MoveType.Invalid
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

    private fun hasAnyCapture(player: PlayerColor) = boardState.flatten().filter { it.piece?.color == player }.any { canPieceCapture(it) }

    private fun canPieceCapture(sq: Square): Boolean {
        val piece = sq.piece ?: return false
        val range = if (piece.type == PieceType.QUEEN) 1..7 else listOf(2)
        for (dr in listOf(-1, 1)) for (dc in listOf(-1, 1)) for (dist in range) {
            val tr = sq.row + dr * dist; val tc = sq.col + dc * dist
            if (tr in 0..7 && tc in 0..7 && getMoveType(sq, tr, tc) is MoveType.Capture) return true
        }
        return false
    }

    private fun executeMove(from: Square, toR: Int, toC: Int, move: MoveType) {
        val piece = from.piece ?: return
        boardState = boardState.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, sq ->
                when {
                    r == from.row && c == from.col -> sq.copy(piece = null, isSelected = false)
                    r == toR && c == toC -> {
                        val promoted = (toR == 0 && piece.color == PlayerColor.RED) || (toR == 7 && piece.color == PlayerColor.BLACK)
                        sq.copy(piece = piece.copy(type = if (promoted) PieceType.QUEEN else piece.type), isSelected = false)
                    }
                    move is MoveType.Capture && r == move.victimRow && c == move.victimCol -> sq.copy(piece = null)
                    else -> sq.copy(isSelected = false)
                }
            }
        }

        if (move is MoveType.Capture && canPieceCapture(boardState[toR][toC])) {
            activeMultiCapturePiece = boardState[toR][toC]
            selectSquare(toR, toC)
            if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == PlayerColor.BLACK) runAi()
            return
        }

        activeMultiCapturePiece = null; checkGameEnd()
        if (winner == null) {
            currentPlayer = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
            if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == PlayerColor.BLACK) runAi()
        }
    }

    private fun runAi() {
        isAiThinking = true
        viewModelScope.launch {
            delay(if (activeMultiCapturePiece != null) 400 else 800)
            val pieces = if (activeMultiCapturePiece != null) listOf(activeMultiCapturePiece!!) else boardState.flatten().filter { it.piece?.color == PlayerColor.BLACK }
            val caps = mutableListOf<Triple<Square, Int, Int>>(); val moves = mutableListOf<Triple<Square, Int, Int>>()
            for (sq in pieces) {
                val range = if (sq.piece?.type == PieceType.QUEEN) 1..7 else 1..2
                for (dr in listOf(-1, 1)) for (dc in listOf(-1, 1)) for (dist in range) {
                    val tr = sq.row + dr * dist; val tc = sq.col + dc * dist
                    if (tr in 0..7 && tc in 0..7) {
                        val m = getMoveType(sq, tr, tc)
                        if (m is MoveType.Capture) caps.add(Triple(sq, tr, tc)) else if (m is MoveType.Simple) moves.add(Triple(sq, tr, tc))
                    }
                }
            }
            val sel = if (caps.isNotEmpty()) caps.random() else if (moves.isNotEmpty() && !hasAnyCapture(PlayerColor.BLACK)) moves.random() else null
            if (sel != null) executeMove(sel.first, sel.second, sel.third, getMoveType(sel.first, sel.second, sel.third))
            else if (activeMultiCapturePiece == null) winner = PlayerColor.RED
            isAiThinking = false
        }
    }

    private fun createInitialBoard() = List(8) { r -> List(8) { c -> Square(r, c, if ((r + c) % 2 != 0) (if (r < 3) Piece(PlayerColor.BLACK) else if (r > 4) Piece(PlayerColor.RED) else null) else null) } }
    private fun findSelectedSquare() = boardState.flatten().find { it.isSelected }
    private fun selectSquare(r: Int, c: Int) { boardState = boardState.mapIndexed { ri, row -> row.mapIndexed { ci, sq -> sq.copy(isSelected = ri == r && ci == c) } } }
    private fun clearSelection() { boardState = boardState.map { row -> row.map { it.copy(isSelected = false) } } }
    private fun checkGameEnd() {
        val p = boardState.flatten().mapNotNull { it.piece }
        if (p.none { it.color == PlayerColor.BLACK }) winner = PlayerColor.RED
        if (p.none { it.color == PlayerColor.RED }) winner = PlayerColor.BLACK
    }
    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (winner == null && isGameStarted) {
                    timeElapsed++
                    if (timeElapsed >= settings.maxTimeMinutes * 60L) {
                        // Se acabó el tiempo
                        winner = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
                    }
                }
            }
        }
    }
}