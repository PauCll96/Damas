package com.example.damas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class CheckersViewModel : ViewModel() {

    var boardState by mutableStateOf(List(8) { row ->
        List(8) { col -> Square(row, col) }
    })
        private set

    var currentPlayer by mutableStateOf(PlayerColor.RED)
        private set

    var winner by mutableStateOf<PlayerColor?>(null)
        private set

    var timeElapsed by mutableStateOf(0L)
        private set

    private var timerJob: Job? = null

    init {
        resetGame()
    }

    fun resetGame() {
        stopTimer()
        timeElapsed = 0L
        boardState = List(8) { row ->
            List(8) { col ->
                val piece = when {
                    (row + col) % 2 != 0 && row < 3 -> Piece(PlayerColor.BLACK)
                    (row + col) % 2 != 0 && row > 4 -> Piece(PlayerColor.RED)
                    else -> null
                }
                Square(row, col, piece)
            }
        }
        currentPlayer = PlayerColor.RED
        winner = null
        startTimer()
    }

    fun surrender() {
        winner = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
        stopTimer()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (winner == null) {
                delay(1000)
                timeElapsed++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    fun onSquareClicked(row: Int, col: Int) {
        if (winner != null) return
        
        val selectedSquare = findSelectedSquare()
        val clickedSquare = boardState[row][col]

        if (selectedSquare != null) {
            if (clickedSquare.piece == null) {
                val isNormal = isValidNormalMove(selectedSquare, row, col)
                val capturedSquare = getCapturedSquare(selectedSquare, row, col)

                if (isNormal) {
                    movePiece(selectedSquare, row, col)
                    checkWinner()
                    switchTurn()
                } else if (capturedSquare != null) {
                    capturePiece(selectedSquare, row, col, capturedSquare)
                    checkWinner()
                    switchTurn()
                }
            } else if (clickedSquare.piece.color == currentPlayer) {
                selectSquare(row, col)
            }
        } else {
            if (clickedSquare.piece?.color == currentPlayer) {
                selectSquare(row, col)
            }
        }
    }

    private fun checkWinner() {
        val pieces = boardState.flatten().mapNotNull { it.piece }
        val redCount = pieces.count { it.color == PlayerColor.RED }
        val blackCount = pieces.count { it.color == PlayerColor.BLACK }

        if (redCount == 0) {
            winner = PlayerColor.BLACK
            stopTimer()
        }
        if (blackCount == 0) {
            winner = PlayerColor.RED
            stopTimer()
        }
    }

    private fun isValidNormalMove(from: Square, toRow: Int, toCol: Int): Boolean {
        val piece = from.piece ?: return false
        val rowDiff = toRow - from.row
        val colDiff = abs(toCol - from.col)
        if (colDiff != 1) return false
        if (piece.type == PieceType.QUEEN) return abs(rowDiff) == 1
        return if (piece.color == PlayerColor.RED) rowDiff == -1 else rowDiff == 1
    }

    private fun getCapturedSquare(from: Square, toRow: Int, toCol: Int): Square? {
        val piece = from.piece ?: return null
        val rowDiff = toRow - from.row
        val colDiff = abs(toCol - from.col)
        if (abs(rowDiff) != 2 || colDiff != 2) return null
        if (piece.type == PieceType.NORMAL) {
            if (piece.color == PlayerColor.RED && rowDiff != -2) return null
            if (piece.color == PlayerColor.BLACK && rowDiff != 2) return null
        }
        val midRow = (from.row + toRow) / 2
        val midCol = (from.col + toCol) / 2
        val midSquare = boardState[midRow][midCol]
        return if (midSquare.piece != null && midSquare.piece.color != piece.color) midSquare else null
    }

    private fun movePiece(from: Square, toRow: Int, toCol: Int) {
        boardState = boardState.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, square ->
                when {
                    r == from.row && c == from.col -> square.copy(piece = null, isSelected = false)
                    r == toRow && c == toCol -> {
                        val isPromoted = (toRow == 0 && from.piece?.color == PlayerColor.RED) || 
                                         (toRow == 7 && from.piece?.color == PlayerColor.BLACK)
                        val finalPiece = if (isPromoted) from.piece?.copy(type = PieceType.QUEEN) else from.piece
                        square.copy(piece = finalPiece, isSelected = false)
                    }
                    else -> square
                }
            }
        }
    }

    private fun capturePiece(from: Square, toRow: Int, toCol: Int, captured: Square) {
        boardState = boardState.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, square ->
                when {
                    r == from.row && c == from.col -> square.copy(piece = null, isSelected = false)
                    r == toRow && c == toCol -> {
                        val isPromoted = (toRow == 0 && from.piece?.color == PlayerColor.RED) || 
                                         (toRow == 7 && from.piece?.color == PlayerColor.BLACK)
                        val finalPiece = if (isPromoted) from.piece?.copy(type = PieceType.QUEEN) else from.piece
                        square.copy(piece = finalPiece, isSelected = false)
                    }
                    r == captured.row && c == captured.col -> square.copy(piece = null)
                    else -> square
                }
            }
        }
    }

    private fun findSelectedSquare(): Square? = boardState.flatten().find { it.isSelected }

    private fun selectSquare(row: Int, col: Int) {
        boardState = boardState.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, square ->
                square.copy(isSelected = (r == row && c == col))
            }
        }
    }

    private fun switchTurn() {
        currentPlayer = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
    }
}