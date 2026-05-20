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

    // --- CONFIGURACIÓN ---
    var settings by mutableStateOf(GameSettings())
        private set

    var isGameStarted by mutableStateOf(false)
        private set

    // --- ESTADO DEL TABLERO ---
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

    // --- LOG DE MOVIMIENTOS (1.4) ---
    var moveLog by mutableStateOf<List<String>>(emptyList())
        private set

    private var activeMultiCapturePiece by mutableStateOf<Cell?>(null)

    val aiColor = Teams.BLACK

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (winner == null && isGameStarted) {
                    if (timeLeftSeconds > 0) timeLeftSeconds-- else onTimeUp()
                }
            }
        }
    }

    // ── CONFIGURACIÓN ─────────────────────────────────────────────────────────

    /** Actualización de ajustes desde la UI — único punto de escritura externo (2.13) */
    fun updateSettings(newSettings: GameSettings) {
        settings = newSettings
    }

    /** Establece el modo de juego antes de iniciar (llamado desde NavHost) */
    fun initMode(mode: GameMode) {
        if (!isGameStarted) settings = settings.copy(mode = mode)
    }

    // ── ACCIONES DE PARTIDA ────────────────────────────────────────────────────

    fun startGame() {
        board = Board()
        currentPlayer = Teams.RED
        winner = null
        timeLeftSeconds = settings.maxTimeMinutes * 60L
        isAiThinking = false
        activeMultiCapturePiece = null
        isGameStarted = true
        moveLog = listOf("▶ Partida iniciada — Torn de: ${settings.player1.name}")
    }

    fun resetToMenu() {
        isGameStarted = false
    }

    fun surrender() {
        val loserName  = playerName(currentPlayer)
        winner = if (currentPlayer == Teams.RED) Teams.BLACK else Teams.RED
        logEvent("🏳 $loserName es va rendir. Guanyador: ${playerName(winner!!)}")
    }

    // ── INTERACCIÓ ────────────────────────────────────────────────────────────

    fun onCellClicked(row: Int, col: Int) {
        if (winner != null || isAiThinking || !isGameStarted) return
        if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Teams.BLACK) return

        val clicked  = board.getCell(row, col)
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
                    clearSelection(); return
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

    // ── EJECUCIÓ DE MOVIMENTS ──────────────────────────────────────────────────

    private fun executeMove(from: Cell, toR: Int, toC: Int, move: MoveType) {
        val piece    = from.piece ?: return
        val promoted = isPromotion(piece, toR)

        board = Board(buildNewCells(from, toR, toC, move, piece, promoted))
        logMove(from, toR, toC, move, piece, promoted)

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
            logEvent("► Torn de: ${playerName(currentPlayer)}")
            if (settings.mode == GameMode.PLAYER_VS_AI && currentPlayer == Teams.BLACK) runAi()
        }
    }

    // ── HELPERS DE TABLERO (2.4) ───────────────────────────────────────────────

    /** Construye la nueva matriz de celdas tras un movimiento — función pura */
    private fun buildNewCells(
        from: Cell, toR: Int, toC: Int,
        move: MoveType,
        piece: com.example.damas.data.local.Piece,
        promoted: Boolean
    ): Array<Array<Cell>> = Array(8) { r ->
        Array(8) { c ->
            val cell = board.cells[r][c]
            when {
                r == from.row && c == from.col ->
                    cell.copy(piece = null, isSelected = false)
                r == toR && c == toC ->
                    cell.copy(piece = piece.copy(type = if (promoted) PieceType.QUEEN else piece.type), isSelected = false)
                move is MoveType.Capture && r == move.victimRow && c == move.victimCol ->
                    cell.copy(piece = null)
                else ->
                    cell.copy(isSelected = false)
            }
        }
    }

    /** Comprueba si la pieza debe coronarse al llegar a [toRow] */
    private fun isPromotion(piece: com.example.damas.data.local.Piece, toRow: Int): Boolean =
        (toRow == 0 && piece.team == Teams.RED) || (toRow == 7 && piece.team == Teams.BLACK)

    private fun checkGameEnd() {
        val w = GameRules.checkWinner(board.cells)
        winner = w
        if (w != null) logEvent("🏆 Guanyador: ${playerName(w)}")
    }

    private fun onTimeUp() {
        if (winner != null) return
        logEvent("⏰ Temps esgotat")
        val pieces     = board.flatten().mapNotNull { it.piece }
        val redCount   = pieces.count { it.team == Teams.RED }
        val blackCount = pieces.count { it.team == Teams.BLACK }
        winner = when {
            redCount  > blackCount -> Teams.RED
            blackCount > redCount  -> Teams.BLACK
            else -> null
        }
        if (winner != null) {
            logEvent("🏆 Guanyador per peces: ${playerName(winner!!)}")
        } else {
            isGameStarted = false
            logEvent("Empat — mateixes peces restants")
        }
    }

    // ── IA ─────────────────────────────────────────────────────────────────────

    private fun runAi() {
        isAiThinking = true
        viewModelScope.launch {
            delay(if (activeMultiCapturePiece != null) 400 else 800)
            val sel = GameLogic.calculateAiMove(board.cells, activeMultiCapturePiece)
            if (sel != null) {
                val move = GameRules.getMoveType(board.cells, sel.first, sel.second, sel.third)
                executeMove(sel.first, sel.second, sel.third, move)
            } else if (activeMultiCapturePiece == null) {
                winner = Teams.RED
                logEvent("🏆 IA sense moviments. Guanyador: ${playerName(Teams.RED)}")
            }
            isAiThinking = false
        }
    }

    // ── SELECCIÓ ───────────────────────────────────────────────────────────────

    private fun findSelectedCell() = board.flatten().find { it.isSelected }

    private fun selectCell(r: Int, c: Int) {
        board = Board(Array(8) { ri -> Array(8) { ci ->
            board.cells[ri][ci].copy(isSelected = ri == r && ci == c)
        }})
    }

    private fun clearSelection() {
        board = Board(Array(8) { r -> Array(8) { c ->
            board.cells[r][c].copy(isSelected = false)
        }})
    }

    // ── LOG (1.4) ──────────────────────────────────────────────────────────────

    private fun logEvent(msg: String) { moveLog = moveLog + msg }

    private fun logMove(
        from: Cell, toR: Int, toC: Int,
        move: MoveType,
        piece: com.example.damas.data.local.Piece,
        promoted: Boolean
    ) {
        val name        = playerName(piece.team)
        val captureNote = if (move is MoveType.Capture) " ✕${pos(move.victimRow, move.victimCol)}" else ""
        val promoNote   = if (promoted) " ♛" else ""
        logEvent("$name: ${pos(from.row, from.col)}→${pos(toR, toC)}$captureNote$promoNote")
    }

    /** Convierte coordenadas a notación tipo ajedrez: (0,0) → a8 */
    private fun pos(row: Int, col: Int) = "${'a' + col}${8 - row}"

    private fun playerName(team: Teams) =
        if (team == Teams.RED) settings.player1.name else settings.player2.name
}
