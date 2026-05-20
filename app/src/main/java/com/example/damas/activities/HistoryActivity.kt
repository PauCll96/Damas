package com.example.damas.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.data.local.GameRecord
import com.example.damas.ui.theme.DamasTheme
import com.example.damas.ui.utils.rememberIsTwoPanel
import com.example.damas.viewmodels.HistoryViewModel

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DamasTheme {
                val vm: HistoryViewModel = viewModel()
                val isTwoPanel = rememberIsTwoPanel()
                // StateFlow → Compose state; la UI se actualiza automáticamente
                val gameHistory by vm.games.collectAsState()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HistoryScreenContent(
                        gameHistory  = gameHistory,
                        selectedGame = vm.selectedGame,
                        isTwoPanel   = isTwoPanel,
                        onGameClick  = { game ->
                            if (isTwoPanel) {
                                vm.onGameSelected(game)
                            } else {
                                startActivity(
                                    Intent(this, GameDetailActivity::class.java).apply {
                                        putExtra(GameDetailActivity.EXTRA_GAME_ID,   game.id)
                                        putExtra(GameDetailActivity.EXTRA_WINNER,    game.winnerName)
                                        putExtra(GameDetailActivity.EXTRA_DATE,      game.date)
                                        putExtra(GameDetailActivity.EXTRA_TIME_LEFT, game.timeLeft)
                                        putExtra(GameDetailActivity.EXTRA_PLAYER1,   game.player1Name)
                                        putExtra(GameDetailActivity.EXTRA_PLAYER2,   game.player2Name)
                                        putExtra(GameDetailActivity.EXTRA_MODE,      game.gameMode)
                                        putExtra(GameDetailActivity.EXTRA_RED_PIECES,   game.redPieces)
                                        putExtra(GameDetailActivity.EXTRA_BLACK_PIECES, game.blackPieces)
                                        putExtra(GameDetailActivity.EXTRA_LOG,       game.moveLog)
                                    }
                                )
                            }
                        },
                        onDeleteAll = { vm.deleteAll() },
                        onBack      = { finish() }
                    )
                }
            }
        }
    }
}

// ── STATELESS ─────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreenContent(
    gameHistory:  List<GameRecord>,
    selectedGame: GameRecord?,
    isTwoPanel:   Boolean,
    onGameClick:  (GameRecord) -> Unit,
    onDeleteAll:  () -> Unit,
    onBack:       () -> Unit
) {
    if (isTwoPanel) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                HistoryTopBar(onBack = onBack, onDeleteAll = onDeleteAll)
                GameHistoryList(
                    gameHistory    = gameHistory,
                    selectedGameId = selectedGame?.id,
                    onGameClick    = onGameClick
                )
            }
            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
            Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                if (selectedGame != null) {
                    GameDetailContent(
                        game     = selectedGame,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text  = stringResource(R.string.detail_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryTopBar(onBack = onBack, onDeleteAll = onDeleteAll)
            GameHistoryList(
                gameHistory    = gameHistory,
                selectedGameId = null,
                onGameClick    = onGameClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTopBar(onBack: () -> Unit, onDeleteAll: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.history_title)) },
        navigationIcon = {
            TextButton(onClick = onBack) { Text(stringResource(R.string.btn_back)) }
        },
        actions = {
            TextButton(onClick = onDeleteAll) {
                Text(stringResource(R.string.btn_delete_all), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
fun GameHistoryList(
    gameHistory:    List<GameRecord>,
    selectedGameId: Int?,
    onGameClick:    (GameRecord) -> Unit
) {
    if (gameHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text  = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gameHistory, key = { it.id }) { game ->
                GameHistoryItem(
                    game       = game,
                    isSelected = game.id == selectedGameId,
                    onClick    = { onGameClick(game) }
                )
            }
        }
    }
}

@Composable
fun GameHistoryItem(
    game:       GameRecord,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = stringResource(R.string.winner_label, game.winnerName),
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "${game.player1Name} vs ${game.player2Name}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text  = game.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Composable de detalle reutilizado en tablet (panel) y en GameDetailActivity (phone) */
@Composable
fun GameDetailContent(game: GameRecord, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = stringResource(R.string.detail_title),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()
        DetailRow(label = stringResource(R.string.label_date),           value = game.date)
        DetailRow(label = stringResource(R.string.winner_label_plain),   value = game.winnerName)
        DetailRow(label = stringResource(R.string.label_players),        value = "${game.player1Name} vs ${game.player2Name}")
        DetailRow(label = stringResource(R.string.label_time_left),      value = game.timeLeft)
        DetailRow(label = stringResource(R.string.label_game_mode),      value = game.gameMode)
        DetailRow(label = stringResource(R.string.label_pieces_final),
            value = "${game.player1Name}: ${game.redPieces}  |  ${game.player2Name}: ${game.blackPieces}")
        HorizontalDivider()
        Text(
            text  = stringResource(R.string.label_log),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text  = game.moveLog.ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
