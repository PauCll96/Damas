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

    // --- ESTADO (Basado en Tema 3 / MiniActv-5) ---
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

    var gameMode by mutableStateOf(GameMode.PLAYER_VS_PLAYER)
        private set

    val aiColor = PlayerColor.BLACK // La IA siempre juega con Negras en este modo simple

    init {
        startTimer()
    }

    // --- CONFIGURACIÓN ---
    fun changeGameMode(mode: GameMode) {
        gameMode = mode
        resetGame()
    }

    fun resetGame() {
        boardState = createInitialBoard()
        currentPlayer = PlayerColor.RED
        winner = null
        timeElapsed = 0L
        isAiThinking = false
    }

    fun surrender() {
        winner = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
    }

    // --- INTERACCIÓN ---
    fun onSquareClicked(row: Int, col: Int) {
        if (winner != null || isAiThinking) return
        if (gameMode == GameMode.PLAYER_VS_AI && currentPlayer == PlayerColor.BLACK) return

        val selected = findSelectedSquare()
        val clicked = boardState[row][col]

        if (selected != null && clicked.piece == null) {
            // Intentar mover a casilla vacía
            if (checkMove(selected, row, col)) {
                executeMove(selected, row, col)
            } else {
                clearSelection()
            }
        } else if (clicked.piece?.color == currentPlayer) {
            // Seleccionar pieza propia
            selectSquare(row, col)
        } else {
            clearSelection()
        }
    }

    // --- LÓGICA DE MOVIMIENTO (Simplificada) ---
    private fun checkMove(from: Square, toR: Int, toC: Int): Boolean {
        val piece = from.piece ?: return false
        val rowDiff = toR - from.row
        val colDiff = abs(toC - from.col)

        // Movimiento básico (diagonal 1 paso)
        val isStep = colDiff == 1 && (
                piece.type == PieceType.QUEEN || 
                (piece.color == PlayerColor.RED && rowDiff == -1) || 
                (piece.color == PlayerColor.BLACK && rowDiff == 1)
        )

        // Captura (diagonal 2 pasos saltando enemigo)
        val isJump = colDiff == 2 && abs(rowDiff) == 2 && let {
            val midR = (from.row + toR) / 2
            val midC = (from.col + toC) / 2
            val midPiece = boardState[midR][midC].piece
            midPiece != null && midPiece.color != piece.color
        }

        return isStep || isJump
    }

    private fun executeMove(from: Square, toR: Int, toC: Int) {
        val isJump = abs(from.row - toR) == 2
        
        // Actualizar tablero usando .map para seguir el patrón de Compose
        boardState = boardState.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, sq ->
                when {
                    r == from.row && c == from.col -> sq.copy(piece = null, isSelected = false)
                    r == toR && c == toC -> {
                        val promoted = (toR == 0 && from.piece?.color == PlayerColor.RED) || (toR == 7 && from.piece?.color == PlayerColor.BLACK)
                        val newType = if (promoted) PieceType.QUEEN else from.piece?.type ?: PieceType.NORMAL
                        sq.copy(piece = from.piece?.copy(type = newType), isSelected = false)
                    }
                    isJump && r == (from.row + toR) / 2 && c == (from.col + toC) / 2 -> sq.copy(piece = null)
                    else -> sq.copy(isSelected = false)
                }
            }
        }

        checkGameEnd()
        if (winner == null) {
            currentPlayer = if (currentPlayer == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
            if (gameMode == GameMode.PLAYER_VS_AI && currentPlayer == PlayerColor.BLACK) {
                runAi()
            }
        }
    }

    // --- IA SIMPLE (Integrada aquí) ---
    private fun runAi() {
        isAiThinking = true
        viewModelScope.launch {
            delay(600) // Pausa para que no sea instantáneo
            
            val allMoves = mutableListOf<Triple<Square, Int, Int>>()
            val allCaptures = mutableListOf<Triple<Square, Int, Int>>()

            // Buscar todos los movimientos posibles de la IA (BLACK)
            boardState.flatten().filter { it.piece?.color == PlayerColor.BLACK }.forEach { sq ->
                for (dr in listOf(-2, -1, 1, 2)) {
                    for (dc in listOf(-2, -1, 1, 2)) {
                        val tr = sq.row + dr
                        val tc = sq.col + dc
                        if (tr in 0..7 && tc in 0..7 && boardState[tr][tc].piece == null) {
                            if (checkMove(sq, tr, tc)) {
                                if (abs(dr) == 2) allCaptures.add(Triple(sq, tr, tc))
                                else allMoves.add(Triple(sq, tr, tc))
                            }
                        }
                    }
                }
            }

            // Prioridad: 1. Capturar si puede | 2. Mover normal | 3. Si no hay nada, rinde
            val bestMove = if (allCaptures.isNotEmpty()) allCaptures.random() 
                           else if (allMoves.isNotEmpty()) allMoves.random() 
                           else null

            if (bestMove != null) {
                executeMove(bestMove.first, bestMove.second, bestMove.third)
            } else {
                winner = PlayerColor.RED // IA se bloquea
            }
            isAiThinking = false
        }
    }

    // --- HELPERS ---
    private fun createInitialBoard() = List(8) { r ->
        List(8) { c ->
            val p = when {
                (r + c) % 2 != 0 && r < 3 -> Piece(PlayerColor.BLACK)
                (r + c) % 2 != 0 && r > 4 -> Piece(PlayerColor.RED)
                else -> null
            }
            Square(r, c, p)
        }
    }

    private fun findSelectedSquare() = boardState.flatten().find { it.isSelected }

    private fun selectSquare(r: Int, c: Int) {
        boardState = boardState.mapIndexed { rowIdx, rowList ->
            rowList.mapIndexed { colIdx, sq -> sq.copy(isSelected = (rowIdx == r && colIdx == c)) }
        }
    }

    private fun clearSelection() {
        boardState = boardState.mapIndexed { _, rowList -> rowList.map { it.copy(isSelected = false) } }
    }

    private fun checkGameEnd() {
        val pieces = boardState.flatten().mapNotNull { it.piece }
        if (pieces.none { it.color == PlayerColor.BLACK }) winner = PlayerColor.RED
        if (pieces.none { it.color == PlayerColor.RED }) winner = PlayerColor.BLACK
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (winner == null) timeElapsed++
            }
        }
    }
}