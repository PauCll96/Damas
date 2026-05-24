package com.example.damas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.data.constants.GameMode
import com.example.damas.data.constants.PieceType
import com.example.damas.data.constants.Teams
import com.example.damas.data.local.Board
import com.example.damas.data.models.GameSettings
import com.example.damas.ui.theme.BoardDark
import com.example.damas.ui.theme.BoardLight
import com.example.damas.ui.theme.SelectedSquare
import com.example.damas.ui.utils.LocalWindowSizeClass
import com.example.damas.ui.utils.isTwoPanel
import com.example.damas.viewmodels.GameViewModel

@Composable
fun GameScreen(
    mode: GameMode,
    onGameFinished: (gameId: Int) -> Unit
) {
    val vm: GameViewModel = viewModel()

    LaunchedEffect(vm.isSettingsLoaded) {
        if (vm.isSettingsLoaded && !vm.isGameStarted) {
            vm.initMode(mode)
            vm.startGame()
        }
    }

    LaunchedEffect(vm.savedGameId) {
        vm.savedGameId?.let { id -> onGameFinished(id) }
    }

    // Confirmación al pulsar atrás: rendirse o cancelar. Sustituye al
    // antiguo BackHandler { } vacío que simplemente bloqueaba el botón.
    var showSurrenderDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = vm.winner == null) { showSurrenderDialog = true }

    if (showSurrenderDialog) {
        SurrenderConfirmDialog(
            currentPlayerName = if (vm.currentPlayer == Teams.RED) vm.settings.player1.name else vm.settings.player2.name,
            onConfirm = {
                showSurrenderDialog = false
                vm.surrender()
            },
            onDismiss = { showSurrenderDialog = false }
        )
    }

    if (!vm.isSettingsLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold { innerPadding ->
        val isTwoPanel = LocalWindowSizeClass.current.isTwoPanel()
        MainGameContent(
            settings        = vm.settings,
            board           = vm.board,
            currentPlayer   = vm.currentPlayer,
            timeLeftSeconds = vm.timeLeftSeconds,
            isAiThinking    = vm.isAiThinking,
            moveLog         = vm.moveLog,
            isTwoPanel      = isTwoPanel,
            modifier        = Modifier.padding(innerPadding),
            onCellClicked   = vm::onCellClicked,
            onSurrender     = { showSurrenderDialog = true }
        )
    }
}

@Composable
private fun SurrenderConfirmDialog(
    currentPlayerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(stringResource(R.string.surrender_dialog_title)) },
        text    = { Text(stringResource(R.string.surrender_dialog_msg, currentPlayerName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.surrender_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.surrender_dialog_cancel))
            }
        }
    )
}

@Composable
private fun MainGameContent(
    settings:        GameSettings,
    board:           Board,
    currentPlayer:   Teams,
    timeLeftSeconds: Long,
    isAiThinking:    Boolean,
    moveLog:         List<String>,
    isTwoPanel:      Boolean,
    modifier:        Modifier = Modifier,
    onCellClicked:   (Int, Int) -> Unit,
    onSurrender:     () -> Unit
) {
    // Conteos memoizados: sólo se recalculan cuando cambia el tablero (B10).
    // Antes board.flatten().count {...} corría en cada recomposición — en bi-panel
    // el panel y el tablero recomponían cada segundo (por el cronómetro), aunque
    // las piezas no hubiesen cambiado.
    val redCount   = remember(board) { board.flatten().count { it.piece?.team == Teams.RED } }
    val blackCount = remember(board) { board.flatten().count { it.piece?.team == Teams.BLACK } }

    if (isTwoPanel) {
        Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                CheckersBoard(board, settings.player1.colorHex, settings.player2.colorHex, onCellClicked)
            }
            Spacer(modifier = Modifier.width(16.dp))
            GameStatsPanel(
                modifier        = Modifier.weight(1f).fillMaxHeight(),
                settings        = settings,
                redCount        = redCount,
                blackCount      = blackCount,
                currentPlayer   = currentPlayer,
                timeLeftSeconds = timeLeftSeconds,
                isAiThinking    = isAiThinking,
                moveLog         = moveLog,
                onSurrender     = onSurrender
            )
        }
    } else {
        Column(
            modifier            = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.game_screen_title), style = MaterialTheme.typography.headlineMedium)

            val turnName     = if (currentPlayer == Teams.RED) settings.player1.name else settings.player2.name
            val turnColorHex = if (currentPlayer == Teams.RED) settings.player1.colorHex else settings.player2.colorHex

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

@Composable
private fun GameStatsPanel(
    modifier:        Modifier = Modifier,
    settings:        GameSettings,
    redCount:        Int,
    blackCount:      Int,
    currentPlayer:   Teams,
    timeLeftSeconds: Long,
    isAiThinking:    Boolean,
    moveLog:         List<String>,
    onSurrender:     () -> Unit
) {
    val turnName     = if (currentPlayer == Teams.RED) settings.player1.name else settings.player2.name
    val turnColorHex = if (currentPlayer == Teams.RED) settings.player1.colorHex else settings.player2.colorHex

    val listState = rememberLazyListState()
    LaunchedEffect(moveLog.size) {
        if (moveLog.isNotEmpty()) listState.animateScrollToItem(moveLog.lastIndex)
    }

    Card(modifier = modifier) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.stats_panel_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            Text(
                text       = stringResource(R.string.turn_label, turnName),
                color      = Color(turnColorHex),
                fontSize   = 18.sp,
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

            HorizontalDivider()
            Text(
                text  = stringResource(R.string.log_panel_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            LazyColumn(
                state               = listState,
                modifier            = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(moveLog) { entry ->
                    Text(
                        text  = entry,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick  = onSurrender,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) { Text(stringResource(R.string.btn_surrender)) }
        }
    }
}

@Composable
private fun CheckersBoard(
    board:         Board,
    player1Color:  Long,
    player2Color:  Long,
    onCellClicked: (Int, Int) -> Unit
) {
    val boardDesc = stringResource(R.string.cd_board)
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border(2.dp, Color.Gray)
            .semantics { contentDescription = boardDesc }
    ) {
        board.cells.forEachIndexed { r, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { c, cell ->
                    val isLight = (r + c) % 2 == 0
                    val pieceDesc = cell.piece?.let { piece ->
                        val team = if (piece.team == Teams.RED) "vermella" else "negra"
                        val kind = if (piece.type == PieceType.QUEEN) "reina" else "peça"
                        "$kind $team a ${'a' + c}${8 - r}"
                    } ?: "casella ${'a' + c}${8 - r}"

                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .background(if (isLight) BoardLight else BoardDark)
                            .run { if (cell.isSelected) border(3.dp, SelectedSquare) else this }
                            .clickable { onCellClicked(r, c) }
                            .semantics { contentDescription = pieceDesc },
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

fun formatTime(seconds: Long): String {
    val mins = seconds / 60; val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
