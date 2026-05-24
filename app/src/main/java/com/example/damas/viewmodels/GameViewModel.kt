package com.example.damas.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.damas.data.constants.GameMode
import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams
import com.example.damas.data.local.*
import com.example.damas.data.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// (2.13) AndroidViewModel para acceder al contexto de aplicación (DataStore)
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository.getInstance(application)
    private val recordRepository = GameRecordRepository.getInstance(application)

    /** Id del GameRecord persistido en Room al terminar la partida — observado por la pantalla
     *  para navegar a results/{id}. null mientras la partida sigue en curso. */
    var savedGameId by mutableStateOf<Int?>(null)
        private set

    // --- CONFIGURACIÓN ---
    var settings by mutableStateOf(GameSettings())
        private set

    var isGameStarted by mutableStateOf(false)
        private set

    /** true cuando las preferencias se han cargado de DataStore */
    var isSettingsLoaded by mutableStateOf(false)
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

    // Piezas restantes al finalizar (enviadas a ResultsActivity para guardar en Room)
    var finalRedPieces by mutableStateOf(0)
        private set
    var finalBlackPieces by mutableStateOf(0)
        private set

    private var activeMultiCapturePiece by mutableStateOf<Cell?>(null)

    val aiColor = Teams.BLACK

    /**
     * Job del temporizador — se crea en [startGame] y se cancela en
     * [stopTimer] cuando alguien gana o se agota el tiempo. Antes había un
     * `while(true)` infinito que vivía hasta que el ViewModel se destruía;
     * ahora se cancela en cuanto deja de tener sentido contar.
     */
    private var timerJob: Job? = null

    init {
        // Snapshot inicial: leemos las preferencias EN EL MOMENTO de crear el
        // ViewModel. Durante una partida no queremos que los cambios en
        // DataStore alteren los colores ni el tiempo máximo — por eso `first()`
        // es la primitiva correcta aquí (y no `collect`/`stateIn`).
        viewModelScope.launch {
            val saved = repository.settingsFlow.first()
            settings = saved
            isSettingsLoaded = true
        }
    }

    // ── CONFIGURACIÓN ─────────────────────────────────────────────────────────

    /** Establece el modo antes de iniciar — solo modifica el campo mode */
    fun initMode(mode: GameMode) {
        if (!isGameStarted) settings = settings.copy(mode = mode)
    }

    // ── ACCIONES DE PARTIDA ────────────────────────────────────────────────────

    fun startGame() {
        // En modo IA el jugador 2 siempre es la IA con color negro
        val effectiveSettings = if (settings.mode == GameMode.PLAYER_VS_AI) {
            val p1 = if (settings.player1.colorHex == 0xFF000000L)
                settings.player1.copy(colorHex = 0xFFFF0000L) else settings.player1
            settings.copy(player1 = p1, player2 = PlayerSettings("IA", 0xFF000000L))
        } else settings

        settings             = effectiveSettings
        board                = Board()
        currentPlayer        = Teams.RED
        winner               = null
        timeLeftSeconds      = settings.maxTimeMinutes * 60L
        isAiThinking         = false
        activeMultiCapturePiece = null
        isGameStarted        = true
        moveLog              = listOf("▶ Partida iniciada — Torn de: ${settings.player1.name}")
        startTimer()
    }

    /**
     * Arranca un Job que decrementa [timeLeftSeconds] cada segundo y se cancela
     * solo cuando llega a 0 o cuando otra ruta de fin de partida llame a
     * [stopTimer]. Sustituye al antiguo `while(true)` que se quedaba vivo
     * incluso después de terminar la partida.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeLeftSeconds > 0 && winner == null) {
                delay(1000)
                if (winner == null) timeLeftSeconds--
            }
            if (winner == null && timeLeftSeconds <= 0L && isGameStarted) onTimeUp()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun resetToMenu() {
        isGameStarted = false
    }

    fun surrender() {
        if (winner != null) return
        val loserName = playerName(currentPlayer)
        winner = if (currentPlayer == Teams.RED) Teams.BLACK else Teams.RED
        // Contabilizar piezas restantes al rendirse
        val pieces     = board.flatten().mapNotNull { it.piece }
        finalRedPieces   = pieces.count { it.team == Teams.RED }
        finalBlackPieces = pieces.count { it.team == Teams.BLACK }
        logEvent("🏳 $loserName es va rendir. Guanyador: ${playerName(winner!!)}")
        stopTimer()
        persistResultOnce()
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
        piece: Piece,
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
    private fun isPromotion(piece: Piece, toRow: Int): Boolean =
        (toRow == 0 && piece.team == Teams.RED) || (toRow == 7 && piece.team == Teams.BLACK)

    private fun checkGameEnd() {
        val w = GameRules.checkWinner(board.cells)
        winner = w
        if (w != null) {
            val pieces     = board.flatten().mapNotNull { it.piece }
            val redCount   = pieces.count { it.team == Teams.RED }
            val blackCount = pieces.count { it.team == Teams.BLACK }
            finalRedPieces   = redCount
            finalBlackPieces = blackCount
            logEvent("🏆 Guanyador: ${playerName(w)} — ${settings.player1.name}: $redCount peces, ${settings.player2.name}: $blackCount peces")
            stopTimer()
            persistResultOnce()
        }
    }

    private fun onTimeUp() {
        if (winner != null) return
        logEvent("⏰ Temps esgotat")
        val pieces     = board.flatten().mapNotNull { it.piece }
        val redCount   = pieces.count { it.team == Teams.RED }
        val blackCount = pieces.count { it.team == Teams.BLACK }
        finalRedPieces   = redCount
        finalBlackPieces = blackCount
        winner = when {
            redCount  > blackCount -> Teams.RED
            blackCount > redCount  -> Teams.BLACK
            else -> null
        }
        if (winner != null) {
            logEvent("🏆 Guanyador per peces: ${playerName(winner!!)}")
            stopTimer()
            persistResultOnce()
        } else {
            isGameStarted = false
            stopTimer()
            logEvent("Empat — mateixes peces restants")
        }
    }

    // ── PERSISTENCIA EN ROOM ─────────────────────────────────────────────────
    /**
     * Inserta el GameRecord final en Room exactamente una vez por partida.
     * Antes esto vivía en ResultsActivity con un CoroutineScope(Dispatchers.IO).launch
     * suelto — ahora es una función del ViewModel ligada a viewModelScope y se ejecuta
     * en el mismo punto en el que se descubre al ganador.
     */
    private fun persistResultOnce() {
        if (savedGameId != null) return
        val w = winner ?: return
        val winnerName = playerName(w)
        val date       = SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault()).format(Date())
        val record = GameRecord(
            date        = date,
            player1Name = settings.player1.name,
            player2Name = settings.player2.name,
            winnerName  = winnerName,
            gameMode    = settings.mode.name,
            timeLeft    = formatTimeSeconds(timeLeftSeconds),
            redPieces   = finalRedPieces,
            blackPieces = finalBlackPieces,
            moveLog     = moveLog.joinToString("\n")
        )
        viewModelScope.launch {
            val id = recordRepository.insert(record).toInt()
            savedGameId = id
        }
    }

    private fun formatTimeSeconds(secs: Long): String {
        val m = secs / 60; val s = secs % 60
        return "%02d:%02d".format(m, s)
    }

    // ── IA ─────────────────────────────────────────────────────────────────────

    private fun runAi() {
        isAiThinking = true
        viewModelScope.launch {
            delay(if (activeMultiCapturePiece != null) 400 else 800)
            val sel = GameLogic.calculateAiMove(board.cells, activeMultiCapturePiece)
            when {
                sel != null -> {
                    val move = GameRules.getMoveType(board.cells, sel.first, sel.second, sel.third)
                    executeMove(sel.first, sel.second, sel.third, move)
                }
                activeMultiCapturePiece != null -> {
                    // La peça ja no pot seguir capturant: fi de cadena multi-captura
                    activeMultiCapturePiece = null
                    clearSelection()
                    checkGameEnd()
                    if (winner == null) {
                        currentPlayer = Teams.RED
                        logEvent("► Torn de: ${playerName(Teams.RED)}")
                    }
                }
                else -> {
                    val pieces = board.flatten().mapNotNull { it.piece }
                    finalRedPieces   = pieces.count { it.team == Teams.RED }
                    finalBlackPieces = pieces.count { it.team == Teams.BLACK }
                    winner = Teams.RED
                    logEvent("🏆 IA sense moviments. Guanyador: ${playerName(Teams.RED)}")
                    stopTimer()
                    persistResultOnce()
                }
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
        piece: Piece,
        promoted: Boolean
    ) {
        val name        = playerName(piece.team)
        val captureNote = if (move is MoveType.Capture) " ✕${pos(move.victimRow, move.victimCol)}" else ""
        logEvent("$name: ${pos(from.row, from.col)}→${pos(toR, toC)}$captureNote")
        if (promoted) logEvent("♛ $name corona peça a ${pos(toR, toC)}")
    }

    /** Convierte coordenadas a notación tipo ajedrez: (0,0) → a8 */
    private fun pos(row: Int, col: Int) = "${'a' + col}${8 - row}"

    private fun playerName(team: Teams) =
        if (team == Teams.RED) settings.player1.name else settings.player2.name
}
