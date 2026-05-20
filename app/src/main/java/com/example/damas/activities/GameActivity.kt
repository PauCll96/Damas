package com.example.damas.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.data.constants.GameMode
import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams
import com.example.damas.data.local.Board
import com.example.damas.ui.theme.*
import com.example.damas.ui.utils.rememberIsTwoPanel
import com.example.damas.viewmodels.GameViewModel

class GameActivity : ComponentActivity() {
    companion object { const val EXTRA_MODE = "extra_game_mode" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialMode = GameMode.valueOf(
            intent.getStringExtra(EXTRA_MODE) ?: GameMode.PLAYER_VS_PLAYER.name
        )

        // (3.4) setContent lo más simple posible: solo delega al composable raíz
        setContent {
            DamasTheme {
                val vm: GameViewModel = viewModel()

                // Arranca la partida en cuanto DataStore carga las preferencias guardadas
                LaunchedEffect(vm.isSettingsLoaded) {
                    if (vm.isSettingsLoaded && !vm.isGameStarted) {
                        vm.initMode(initialMode)
                        vm.startGame()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BackHandler { /* bloquea el retroceso accidental durante la partida */ }

                    if (!vm.isSettingsLoaded) {
                        // Breve estado de carga mientras DataStore inicializa
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        MainGameScreen(
                            settings            = vm.settings,
                            board               = vm.board,
                            currentPlayer       = vm.currentPlayer,
                            timeLeftSeconds     = vm.timeLeftSeconds,
                            isAiThinking        = vm.isAiThinking,
                            winner              = vm.winner,
                            isTwoPanel          = rememberIsTwoPanel(),
                            onCellClicked       = { r, c -> vm.onCellClicked(r, c) },
                            onSurrender         = { vm.surrender() },
                            onNavigateToResults = { winName, time ->
                                startActivity(Intent(this, ResultsActivity::class.java).apply {
                                    putExtra(ResultsActivity.EXTRA_WINNER,      winName)
                                    putExtra(ResultsActivity.EXTRA_TIME_LEFT,   time)
                                    putExtra(ResultsActivity.EXTRA_PLAYER1,     vm.settings.player1.name)
                                    putExtra(ResultsActivity.EXTRA_PLAYER2,     vm.settings.player2.name)
                                    putExtra(ResultsActivity.EXTRA_LOG,         vm.moveLog.joinToString("\n"))
                                    putExtra(ResultsActivity.EXTRA_MODE,        vm.settings.mode.name)
                                    putExtra(ResultsActivity.EXTRA_RED_PIECES,  vm.finalRedPieces)
                                    putExtra(ResultsActivity.EXTRA_BLACK_PIECES, vm.finalBlackPieces)
                                })
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── PANTALLA DE JOC ────────────────────────────────────────────────────────────

@Composable
fun MainGameScreen(
    settings:            com.example.damas.data.models.GameSettings,
    board:               Board,
    currentPlayer:       Teams,
    timeLeftSeconds:     Long,
    isAiThinking:        Boolean,
    winner:              Teams?,
    isTwoPanel:          Boolean,
    onCellClicked:       (Int, Int) -> Unit,
    onSurrender:         () -> Unit,
    onNavigateToResults: (String, String) -> Unit
) {
    LaunchedEffect(winner) {
        if (winner != null) {
            val winName = if (winner == Teams.RED) settings.player1.name else settings.player2.name
            onNavigateToResults(winName, formatTime(timeLeftSeconds))
        }
    }

    if (isTwoPanel) {
        // TABLET: tablero | panel estadísticas
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                CheckersBoard(board, settings.player1.colorHex, settings.player2.colorHex, onCellClicked)
            }
            Spacer(modifier = Modifier.width(16.dp))
            GameStatsPanel(
                modifier        = Modifier.weight(1f).fillMaxHeight(),
                settings        = settings,
                board           = board,
                currentPlayer   = currentPlayer,
                timeLeftSeconds = timeLeftSeconds,
                isAiThinking    = isAiThinking,
                onSurrender     = onSurrender
            )
        }
    } else {
        // PHONE: mono-panel vertical (1.11) con conteo de piezas visible
        Column(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.game_screen_title), style = MaterialTheme.typography.headlineMedium)

            val turnName     = if (currentPlayer == Teams.RED) settings.player1.name else settings.player2.name
            val turnColorHex = if (currentPlayer == Teams.RED) settings.player1.colorHex else settings.player2.colorHex
            val redCount     = board.flatten().count { it.piece?.team == Teams.RED }
            val blackCount   = board.flatten().count { it.piece?.team == Teams.BLACK }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text       = stringResource(R.string.turn_label, turnName),
                    color      = Color(turnColorHex),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text       = stringResource(R.string.time_remaining_label, formatTime(timeLeftSeconds)),
                    color      = if (timeLeftSeconds < 30) Color.Red else Color.Unspecified,
                    fontWeight = if (timeLeftSeconds < 30) FontWeight.Bold else FontWeight.Normal
                )
                // (1.11) Piezas restantes visibles en phone
                Text(
                    text  = stringResource(R.string.pieces_score_label,
                        settings.player1.name, redCount,
                        settings.player2.name, blackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAiThinking) LinearProgressIndicator(Modifier.width(100.dp))
            }

            CheckersBoard(board, settings.player1.colorHex, settings.player2.colorHex, onCellClicked)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(
                    onClick = onSurrender,
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) { Text(stringResource(R.string.btn_surrender)) }
            }
        }
    }
}

// ── PANEL LATERAL TABLET ───────────────────────────────────────────────────────

@Composable
fun GameStatsPanel(
    modifier:        Modifier = Modifier,
    settings:        com.example.damas.data.models.GameSettings,
    board:           Board,
    currentPlayer:   Teams,
    timeLeftSeconds: Long,
    isAiThinking:    Boolean,
    onSurrender:     () -> Unit
) {
    val turnName     = if (currentPlayer == Teams.RED) settings.player1.name else settings.player2.name
    val turnColorHex = if (currentPlayer == Teams.RED) settings.player1.colorHex else settings.player2.colorHex
    val redCount     = board.flatten().count { it.piece?.team == Teams.RED }
    val blackCount   = board.flatten().count { it.piece?.team == Teams.BLACK }

    Card(modifier = modifier) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.stats_panel_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Text(
                text       = stringResource(R.string.turn_label, turnName),
                color      = Color(turnColorHex),
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text       = stringResource(R.string.time_remaining_label, formatTime(timeLeftSeconds)),
                color      = if (timeLeftSeconds < 30) Color.Red else Color.Unspecified,
                fontWeight = if (timeLeftSeconds < 30) FontWeight.Bold else FontWeight.Normal,
                style      = MaterialTheme.typography.titleMedium
            )
            if (isAiThinking) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.ai_thinking_label), style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
            Text(stringResource(R.string.pieces_label, settings.player1.name, redCount),  color = Color(settings.player1.colorHex), fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.pieces_label, settings.player2.name, blackCount), color = Color(settings.player2.colorHex), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick  = onSurrender,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) { Text(stringResource(R.string.btn_surrender)) }
        }
    }
}

// ── TABLERO ────────────────────────────────────────────────────────────────────

@Composable
fun CheckersBoard(board: Board, player1Color: Long, player2Color: Long, onCellClicked: (Int, Int) -> Unit) {
    Column(modifier = Modifier.aspectRatio(1f).border(2.dp, Color.Gray)) {
        board.cells.forEachIndexed { r, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { c, cell ->
                    val isLight = (r + c) % 2 == 0
                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .background(if (isLight) BoardLight else BoardDark)
                            .run { if (cell.isSelected) border(3.dp, SelectedSquare) else this }
                            .clickable { onCellClicked(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        cell.piece?.let { piece ->
                            val pColor = Color(if (piece.team == Teams.RED) player1Color else player2Color)
                            Canvas(modifier = Modifier.fillMaxSize(0.8f)) {
                                drawCircle(color = pColor)
                                if (piece.type == PieceType.QUEEN) drawCircle(color = Color.White, radius = size.minDimension / 4)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── UTILITAT ───────────────────────────────────────────────────────────────────

fun formatTime(seconds: Long): String {
    val mins = seconds / 60; val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
