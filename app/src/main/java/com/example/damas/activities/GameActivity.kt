package com.example.damas.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.damas.R
import com.example.damas.data.constants.GameMode
import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams
import com.example.damas.data.local.Board
import com.example.damas.viewmodels.GameViewModel
import com.example.damas.ui.theme.*
import com.example.damas.ui.utils.rememberIsTwoPanel

class GameActivity : ComponentActivity() {
    companion object { const val EXTRA_MODE = "extra_game_mode" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val modeName = intent.getStringExtra(EXTRA_MODE) ?: GameMode.PLAYER_VS_PLAYER.name
        val initialMode = GameMode.valueOf(modeName)

        setContent {
            DamasTheme {
                val vm: GameViewModel = viewModel()
                
                // Inicialización única (Stateful)
                LaunchedEffect(Unit) {
                    if (!vm.isGameStarted) {
                        vm.settings = vm.settings.copy(mode = initialMode)
                    }
                }

                // Detección de pantalla — Stateful, recalcula al rotar
                val isTwoPanel = rememberIsTwoPanel()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!vm.isGameStarted) {
                        SetupScreen(
                            settings = vm.settings,
                            onSettingsChange = { vm.settings = it },
                            onStartGame = { vm.startGame() },
                            onBack = { finish() }
                        )
                    } else {
                        MainGameScreen(
                            settings = vm.settings,
                            board = vm.board,
                            currentPlayer = vm.currentPlayer,
                            timeLeftSeconds = vm.timeLeftSeconds,
                            isAiThinking = vm.isAiThinking,
                            winner = vm.winner,
                            isTwoPanel = isTwoPanel,
                            onCellClicked = { r, c -> vm.onCellClicked(r, c) },
                            onSurrender = { vm.surrender() },
                            onNavigateToResults = { winName, time ->
                                val intent = Intent(this, ResultsActivity::class.java).apply {
                                    putExtra(ResultsActivity.EXTRA_WINNER,    winName)
                                    putExtra(ResultsActivity.EXTRA_TIME_LEFT, time)
                                    putExtra(ResultsActivity.EXTRA_PLAYER1,   vm.settings.player1.name)
                                    putExtra(ResultsActivity.EXTRA_PLAYER2,   vm.settings.player2.name)
                                }
                                startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENTES STATELESS (Sin dependencia directa del ViewModel) ---

@Composable
fun SetupScreen(
    settings: com.example.damas.data.models.GameSettings,
    onSettingsChange: (com.example.damas.data.models.GameSettings) -> Unit,
    onStartGame: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.player_1_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextField(
                    value = settings.player1.name,
                    onValueChange = { onSettingsChange(settings.copy(player1 = settings.player1.copy(name = it))) },
                    label = { Text(stringResource(R.string.name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.piece_color_label), style = MaterialTheme.typography.bodySmall)
                ColorPicker(
                    selectedColor = settings.player1.colorHex,
                    disabledColors = listOf(settings.player2.colorHex)
                ) { 
                    onSettingsChange(settings.copy(player1 = settings.player1.copy(colorHex = it)))
                }
            }
        }

        if (settings.mode == GameMode.PLAYER_VS_PLAYER) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.player_2_label), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    TextField(
                        value = settings.player2.name,
                        onValueChange = { onSettingsChange(settings.copy(player2 = settings.player2.copy(name = it))) },
                        label = { Text(stringResource(R.string.name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.piece_color_label), style = MaterialTheme.typography.bodySmall)
                    ColorPicker(
                        selectedColor = settings.player2.colorHex,
                        disabledColors = listOf(settings.player1.colorHex)
                    ) { 
                        onSettingsChange(settings.copy(player2 = settings.player2.copy(colorHex = it)))
                    }
                }
            }
        } else {
            Text(stringResource(R.string.ia_ready_label), color = MaterialTheme.colorScheme.secondary)
            LaunchedEffect(settings.mode) { 
                val newP2 = settings.player2.copy(name = "IA", colorHex = 0xFF000000L)
                val newP1 = if (settings.player1.colorHex == 0xFF000000L) {
                    settings.player1.copy(colorHex = 0xFFFF0000L)
                } else {
                    settings.player1
                }
                onSettingsChange(settings.copy(player1 = newP1, player2 = newP2))
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.game_time_label), fontWeight = FontWeight.Bold)
                TextField(
                    value = if (settings.maxTimeMinutes == 0) "" else settings.maxTimeMinutes.toString(),
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        onSettingsChange(settings.copy(maxTimeMinutes = if (filtered.isEmpty()) 0 else filtered.toInt()))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.time_placeholder)) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = settings.player1.name.isNotBlank() && 
                      (settings.mode == GameMode.PLAYER_VS_AI || settings.player2.name.isNotBlank()) &&
                      settings.maxTimeMinutes > 0
        ) {
            Text(stringResource(R.string.btn_start_game))
        }
        TextButton(onClick = onBack) { Text(stringResource(R.string.btn_cancel)) }
    }
}

@Composable
fun ColorPicker(selectedColor: Long, disabledColors: List<Long>, onColorSelected: (Long) -> Unit) {
    val colors = listOf(0xFFFF0000, 0xFF0000FF, 0xFF00FF00, 0xFF000000, 0xFFFFA500)
    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { hex ->
            val isSelected = selectedColor == hex
            val isDisabled = disabledColors.contains(hex)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDisabled) Color.Gray.copy(alpha = 0.2f) else Color(hex))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDisabled) Color.Transparent else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable(enabled = !isDisabled) { onColorSelected(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = if (hex == 0xFF000000L || hex == 0xFF0000FFL) Color.White else Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MainGameScreen(
    settings: com.example.damas.data.models.GameSettings,
    board: Board,
    currentPlayer: Teams,
    timeLeftSeconds: Long,
    isAiThinking: Boolean,
    winner: Teams?,
    isTwoPanel: Boolean,
    onCellClicked: (Int, Int) -> Unit,
    onSurrender: () -> Unit,
    onNavigateToResults: (String, String) -> Unit
) {
    LaunchedEffect(winner) {
        if (winner != null) {
            val winName = if (winner == Teams.RED) settings.player1.name else settings.player2.name
            onNavigateToResults(winName, formatTime(timeLeftSeconds))
        }
    }

    if (isTwoPanel) {
        // TABLET: tablero izquierda | estadísticas derecha
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                CheckersBoard(
                    board = board,
                    player1Color = settings.player1.colorHex,
                    player2Color = settings.player2.colorHex,
                    onCellClicked = onCellClicked
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            GameStatsPanel(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                settings = settings,
                board = board,
                currentPlayer = currentPlayer,
                timeLeftSeconds = timeLeftSeconds,
                isAiThinking = isAiThinking,
                onSurrender = onSurrender
            )
        }
    } else {
        // PHONE: diseño mono-panel, solo el tablero con info mínima
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.game_screen_title), style = MaterialTheme.typography.headlineMedium)

            val turnName     = if (currentPlayer == Teams.RED) settings.player1.name else settings.player2.name
            val turnColorHex = if (currentPlayer == Teams.RED) settings.player1.colorHex else settings.player2.colorHex

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.turn_label, turnName),
                    color = Color(turnColorHex),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.time_remaining_label, formatTime(timeLeftSeconds)),
                    color = if (timeLeftSeconds < 30) Color.Red else Color.Unspecified,
                    fontWeight = if (timeLeftSeconds < 30) FontWeight.Bold else FontWeight.Normal
                )
                if (isAiThinking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.width(100.dp))
                }
            }

            CheckersBoard(
                board = board,
                player1Color = settings.player1.colorHex,
                player2Color = settings.player2.colorHex,
                onCellClicked = onCellClicked
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(
                    onClick = onSurrender,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.btn_surrender))
                }
            }
        }
    }
}

/** Panel lateral de estadísticas para el layout tablet (stateless) */
@Composable
fun GameStatsPanel(
    modifier: Modifier = Modifier,
    settings: com.example.damas.data.models.GameSettings,
    board: Board,
    currentPlayer: Teams,
    timeLeftSeconds: Long,
    isAiThinking: Boolean,
    onSurrender: () -> Unit
) {
    val turnName     = if (currentPlayer == Teams.RED) settings.player1.name else settings.player2.name
    val turnColorHex = if (currentPlayer == Teams.RED) settings.player1.colorHex else settings.player2.colorHex
    val redCount     = board.flatten().count { it.piece?.team == Teams.RED }
    val blackCount   = board.flatten().count { it.piece?.team == Teams.BLACK }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_panel_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.turn_label, turnName),
                color = Color(turnColorHex),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.time_remaining_label, formatTime(timeLeftSeconds)),
                color = if (timeLeftSeconds < 30) Color.Red else Color.Unspecified,
                fontWeight = if (timeLeftSeconds < 30) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.titleMedium
            )

            if (isAiThinking) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.ai_thinking_label), style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.pieces_label, settings.player1.name, redCount),
                color = Color(settings.player1.colorHex),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.pieces_label, settings.player2.name, blackCount),
                color = Color(settings.player2.colorHex),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onSurrender,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text(stringResource(R.string.btn_surrender))
            }
        }
    }
}

@Composable
fun CheckersBoard(
    board: Board,
    player1Color: Long,
    player2Color: Long,
    onCellClicked: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.aspectRatio(1f).border(2.dp, Color.Gray)) {
        board.cells.forEachIndexed { r, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { c, cell ->
                    val isLight = (r + c) % 2 == 0
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(if (isLight) BoardLight else BoardDark)
                            .run { if (cell.isSelected) border(3.dp, SelectedSquare) else this }
                            .clickable { onCellClicked(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        cell.piece?.let { piece ->
                            val pColor = Color(if (piece.team == Teams.RED) player1Color else player2Color)
                            Canvas(modifier = Modifier.fillMaxSize(0.8f)) {
                                drawCircle(color = pColor)
                                if (piece.type == PieceType.QUEEN) {
                                    drawCircle(color = Color.White, radius = size.minDimension / 4)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val mins = seconds / 60; val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
