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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.data.models.GameResult
import com.example.damas.ui.theme.DamasTheme
import com.example.damas.ui.utils.rememberIsTwoPanel
import com.example.damas.viewmodels.HistoryViewModel

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DamasTheme {
                // ── STATEFUL ──────────────────────────────────────────────
                val vm: HistoryViewModel = viewModel()
                val isTwoPanel = rememberIsTwoPanel()

                // Refresca la lista cada vez que la Activity vuelve al frente
                vm.refreshHistory()

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HistoryScreenContent(
                        gameHistory  = vm.gameHistory,
                        selectedGame = vm.selectedGame,
                        isTwoPanel   = isTwoPanel,
                        onGameClick  = { game ->
                            if (isTwoPanel) {
                                // Tablet: mostrar detalle en el panel derecho
                                vm.onGameSelected(game)
                            } else {
                                // Phone: Intent explícito a GameDetailActivity
                                startActivity(
                                    Intent(this, GameDetailActivity::class.java).apply {
                                        putExtra(GameDetailActivity.EXTRA_WINNER,    game.winnerName)
                                        putExtra(GameDetailActivity.EXTRA_DATE,      game.date)
                                        putExtra(GameDetailActivity.EXTRA_TIME_LEFT, game.timeLeft)
                                        putExtra(GameDetailActivity.EXTRA_PLAYER1,   game.player1Name)
                                        putExtra(GameDetailActivity.EXTRA_PLAYER2,   game.player2Name)
                                        putExtra(GameDetailActivity.EXTRA_GAME_ID,   game.id)
                                    }
                                )
                            }
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

// ── STATELESS ─────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreenContent(
    gameHistory:  List<GameResult>,
    selectedGame: GameResult?,
    isTwoPanel:   Boolean,
    onGameClick:  (GameResult) -> Unit,
    onBack:       () -> Unit
) {
    if (isTwoPanel) {
        // TABLET: lista izquierda | detalle derecha
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                HistoryTopBar(onBack = onBack)
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
        // PHONE: solo la lista
        Column(modifier = Modifier.fillMaxSize()) {
            HistoryTopBar(onBack = onBack)
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
fun HistoryTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.history_title)) },
        navigationIcon = {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.btn_back))
            }
        }
    )
}

@Composable
fun GameHistoryList(
    gameHistory:    List<GameResult>,
    selectedGameId: Int?,
    onGameClick:    (GameResult) -> Unit
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
            items(gameHistory) { game ->
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
    game:       GameResult,
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
                text       = stringResource(R.string.game_number_label, game.id + 1),
                fontWeight = FontWeight.Bold,
                style      = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = stringResource(R.string.winner_label, game.winnerName),
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
fun GameDetailContent(game: GameResult, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text  = stringResource(R.string.detail_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider()
        DetailRow(label = stringResource(R.string.label_date),      value = game.date)
        DetailRow(label = stringResource(R.string.winner_label_plain), value = game.winnerName)
        DetailRow(label = stringResource(R.string.label_players),   value = "${game.player1Name} vs ${game.player2Name}")
        DetailRow(label = stringResource(R.string.label_time_left), value = game.timeLeft)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
