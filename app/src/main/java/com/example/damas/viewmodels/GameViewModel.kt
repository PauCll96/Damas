package com.example.damas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.damas.data.constants.GameMode
import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams
import com.example.damas.data.local.*
import com.example.damas.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    // --- CONFIGURACIÓN Y LOBBY ---
    var settings by mutableStateOf(GameSettings())
    var isGameStarted by mutableStateOf(false)
        private set

    // --- ESTADO ---
    var board by mutableStateOf(Board())
        private set

    var currentPlayer by mutableStateOf(Teams.RED)
        private set

    var winner by mutableStateOf<Teams?>(null)
        private set

    var timeLeftSeconds by mutableStateOf(0L)
        private set

    var isAiThinking by mutableStateOf(false)
        private set

    // Pieza que debe continuar capturando (encadenamiento)
    private var activeMultiCapturePiece by mutableStateOf<Cell?>(null)

    val aiColor = Teams.BLACK

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (winner == null && isGameStarted) {
                    if (timeLeftSeconds > 0) {
                        timeLeftSeconds--
                    } else {
                        onTimeUp()
                    }
                }
            }
        }
    }

    // --- ACCIONES DE PARTIDA ---
    fun startGame() {
        board = Board()
        currentPlayer = Teams.RED
        winner = null
        timeLeftSeconds = settings.maxTimeMinutes * 60L
        isAiThinking = false
        activeMultiCapturePiece = null
        isGameStarted = true
    }

    fun resetToMenu() {
        isGameStarted = false
    }

    fun surrender() {
        winner = if (currentPlayer == Teams.RED) Teams.BLACK else Teams.RED
    }

    // --- INTERACCIÓN ---
    fun onCellClicked(row: Int, col: Int) {
        if (winner != null || isAiThinking || !isGameStarted) return
        if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Teams.BLACK) return

        val clicked = board.getCell(row, col)
        val selected = findSelectedCell()

        if (activeMultiCapturePiece != null) {
            if (selected != null && clicked.piece == null) {
                val move = GameRules.getMoveType(board.cells, selected, row, col)
                if (move is MoveType.Capture) executeMove(selected, row, col, move)
            } else if (clicked.row == activeMultiCapturePiece?.row && clicked.col == activeMultiCapturePiece?.col) {
                selectCell(row, col)
            }
            return
        }

        if (selected != null && clicked.piece == null) {
            val move = GameRules.getMoveType(board.cells, selected, row, col)
            if (move != MoveType.Invalid) {
                if (GameRules.hasAnyCapture(board.cells, currentPlayer) && move is MoveType.Simple) {
                    clearSelection()
                    return
                }
                executeMove(selected, row, col, move)
            } else {
                clearSelection()
            }
        } else if (clicked.piece?.team == currentPlayer) {
            selectCell(row, col)
        } else {
            clearSelection()
        }
    }

    private fun executeMove(from: Cell, toR: Int, toC: Int, move: MoveType) {
        val piece = from.piece ?: return
        val newCells = Array(8) { r ->
            Array(8) { c ->
                val cell = board.cells[r][c]
                when {
                    r == from.row && c == from.col -> cell.copy(piece = null, isSelected = false)
                    r == toR && c == toC -> {
                        val promoted = (toR == 0 && piece.team == Teams.RED) || (toR == 7 && piece.team == Teams.BLACK)
                        cell.copy(piece = piece.copy(type = if (promoted) PieceType.QUEEN else piece.type), isSelected = false)
                    }
                    move is MoveType.Capture && r == move.victimRow && c == move.victimCol -> cell.copy(piece = null)
                    else -> cell.copy(isSelected = false)
                }
            }
        }
        board = board.copy(cells = newCells)

        if (move is MoveType.Capture && GameRules.canPieceCapture(board.cells, board.getCell(toR, toC))) {
            activeMultiCapturePiece = board.getCell(toR, toC)
            selectCell(toR, toC)
            if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Teams.BLACK) runAi()
            return
        }

        activeMultiCapturePiece = null
        checkGameEnd()
        if (winner == null) {
            currentPlayer = if (currentPlayer == Teams.RED) Teams.BLACK else Teams.RED
            if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Teams.BLACK) runAi()
        }
    }

    private fun runAi() {
        isAiThinking = true
        viewModelScope.launch {
            delay(if (activeMultiCapturePiece != null) 400 else 800)
            val pieces = if (activeMultiCapturePiece != null) listOf(activeMultiCapturePiece!!) else board.flatten().filter { it.piece?.team == Teams.BLACK }
            val caps = mutableListOf<Triple<Cell, Int, Int>>()
            val moves = mutableListOf<Triple<Cell, Int, Int>>()
            for (sq in pieces) {
                val range = if (sq.piece?.type == PieceType.QUEEN) 1..7 else 1..2
                for (dr in listOf(-1, 1)) for (dc in listOf(-1, 1)) for (dist in range) {
                    val tr = sq.row + dr * dist
                    val tc = sq.col + dc * dist
                    if (tr in 0..7 && tc in 0..7) {
                        val m = GameRules.getMoveType(board.cells, sq, tr, tc)
                        if (m is MoveType.Capture) caps.add(Triple(sq, tr, tc)) else if (m is MoveType.Simple) moves.add(Triple(sq, tr, tc))
                    }
                }
            }
            val sel = if (caps.isNotEmpty()) caps.random() else if (moves.isNotEmpty() && !GameRules.hasAnyCapture(board.cells, Teams.BLACK)) moves.random() else null
            if (sel != null) executeMove(sel.first, sel.second, sel.third, GameRules.getMoveType(board.cells, sel.first, sel.second, sel.third))
            else if (activeMultiCapturePiece == null) winner = Teams.RED
            isAiThinking = false
        }
    }

    private fun findSelectedCell() = board.flatten().find { it.isSelected }

    private fun selectCell(r: Int, c: Int) {
        val newCells = Array(8) { ri ->
            Array(8) { ci ->
                board.cells[ri][ci].copy(isSelected = ri == r && ci == c)
            }
        }
        board = board.copy(cells = newCells)
    }

    private fun clearSelection() {
        val newCells = Array(8) { r ->
            Array(8) { c ->
                board.cells[r][c].copy(isSelected = false)
            }
        }
        board = board.copy(cells = newCells)
    }

    private fun checkGameEnd() {
        winner = GameRules.checkWinner(board.cells)
    }

    private fun onTimeUp() {
        if (winner != null) return
        val pieces = board.flatten().mapNotNull { it.piece }
        val redCount = pieces.count { it.team == Teams.RED }
        val blackCount = pieces.count { it.team == Teams.BLACK }

        winner = when {
            redCount > blackCount -> Teams.RED
            blackCount > redCount -> Teams.BLACK
            else -> null
        }
        if (winner == null && redCount == blackCount) {
             isGameStarted = false
        }
    }
}
