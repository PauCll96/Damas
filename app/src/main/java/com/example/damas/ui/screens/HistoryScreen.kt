package com.example.damas.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.data.local.GameRecord
import com.example.damas.viewmodels.HistoryViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla de historial con bi-panel adaptativo oficial.
 *
 * En tablet (Medium/Expanded): muestra lista + detalle a la vez.
 * En móvil (Compact): muestra lista → detalle con animación, y el botón atrás
 * vuelve a la lista (gestionado por el propio scaffold).
 *
 * Antes hacíamos un Row manual con weight(0.4f)/weight(0.6f) y, en móvil,
 * lanzábamos GameDetailActivity con 9 putExtra. Ahora el scaffold gestiona
 * todo el layout responsive y pasamos sólo el id del registro seleccionado.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm: HistoryViewModel = viewModel()
    val games by vm.games.collectAsState()
    val scope = rememberCoroutineScope()

    // Navigator del scaffold: clave de contenido = id del GameRecord
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>()

    // Botón atrás: si el detalle está abierto en móvil, vuelve a la lista
    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    val selectedId = navigator.currentDestination?.contentKey
    val selectedGame = games.firstOrNull { it.id == selectedId }

    // Diálogo de confirmación antes de borrar todo el historial — evita
    // perder partidas por pulsación accidental.
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_all_dialog_title)) },
            text  = { Text(stringResource(R.string.delete_all_dialog_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        vm.deleteAll()
                    }
                ) {
                    Text(
                        text  = stringResource(R.string.btn_delete_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.history_title)) },
                            navigationIcon = {
                                TextButton(onClick = onBack) {
                                    Text(stringResource(R.string.btn_back))
                                }
                            },
                            actions = {
                                TextButton(onClick = { showDeleteDialog = true }) {
                                    Text(
                                        text  = stringResource(R.string.btn_delete_all),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    HistoryList(
                        games        = games,
                        selectedId   = selectedId,
                        modifier     = Modifier.padding(innerPadding),
                        onGameClick  = { game ->
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, game.id)
                            }
                        }
                    )
                }
            }
        },
        detailPane = {
            AnimatedPane {
                if (selectedGame != null) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(R.string.detail_title)) },
                                navigationIcon = {
                                    if (navigator.canNavigateBack()) {
                                        TextButton(onClick = { scope.launch { navigator.navigateBack() } }) {
                                            Text("← " + stringResource(R.string.btn_back))
                                        }
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        GameDetailContent(
                            game     = selectedGame,
                            modifier = Modifier.padding(innerPadding).padding(24.dp)
                        )
                    }
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
    )
}

@Composable
private fun HistoryList(
    games:       List<GameRecord>,
    selectedId:  Int?,
    modifier:    Modifier = Modifier,
    onGameClick: (GameRecord) -> Unit
) {
    if (games.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text  = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier            = modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(games, key = { it.id }) { game ->
                HistoryItem(
                    game       = game,
                    isSelected = game.id == selectedId,
                    onClick    = { onGameClick(game) }
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    game:       GameRecord,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = containerColor)
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

@Composable
fun GameDetailContent(game: GameRecord, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow(label = stringResource(R.string.label_date),         value = game.date)
        DetailRow(label = stringResource(R.string.winner_label_plain), value = game.winnerName)
        DetailRow(label = stringResource(R.string.label_players),      value = "${game.player1Name} vs ${game.player2Name}")
        DetailRow(label = stringResource(R.string.label_time_left),    value = game.timeLeft)
        DetailRow(label = stringResource(R.string.label_game_mode),    value = game.gameMode)
        DetailRow(
            label = stringResource(R.string.label_pieces_final),
            value = "${game.player1Name}: ${game.redPieces}  |  ${game.player2Name}: ${game.blackPieces}"
        )
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
