package com.example.damas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.ui.theme.DamasTheme

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DamasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val checkersViewModel: CheckersViewModel = viewModel()
                    GameScreen(
                        viewModel = checkersViewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: CheckersViewModel, onBack: () -> Unit) {
    val board = viewModel.boardState
    val currentPlayer = viewModel.currentPlayer
    val winner = viewModel.winner

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_standard)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabecera con título y botón volver
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text(text = "←", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = stringResource(R.string.game_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(48.dp)) // Equilibrio visual
        }

        if (winner != null) {
            val winnerText = if (winner == PlayerColor.RED) {
                stringResource(R.string.winner_red)
            } else {
                stringResource(R.string.winner_black)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = winnerText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorResource(R.color.board_dark)
                )
                Text(
                    text = stringResource(R.string.timer_label, formatTime(viewModel.timeElapsed)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { viewModel.resetGame() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(text = stringResource(R.string.reset_button))
                }
            }
        } else {
            val turnText = if (currentPlayer == PlayerColor.RED) {
                stringResource(R.string.turn_red)
            } else {
                stringResource(R.string.turn_black)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = turnText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (currentPlayer == PlayerColor.RED) colorResource(R.color.piece_red) else colorResource(R.color.piece_black)
                )
                Text(
                    text = stringResource(R.string.timer_label, formatTime(viewModel.timeElapsed)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        CheckersBoard(
            board = board, 
            onSquareClick = { r, c -> viewModel.onSquareClicked(r, c) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.resetGame() }) {
                Text(text = stringResource(R.string.reset_button))
            }
            OutlinedButton(onClick = { 
                viewModel.surrender()
            }) {
                Text(text = stringResource(R.string.surrender_button))
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun CheckersBoard(board: List<List<Square>>, onSquareClick: (Int, Int) -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                dimensionResource(R.dimen.border_thin), 
                colorResource(R.color.border_color)
            )
    ) {
        board.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { colIndex, square ->
                    val backgroundColor = if ((rowIndex + colIndex) % 2 == 0) {
                        colorResource(R.color.board_light)
                    } else {
                        colorResource(R.color.board_dark)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(backgroundColor)
                            .run {
                                if (square.isSelected) {
                                    this.border(
                                        dimensionResource(R.dimen.border_thin),
                                        colorResource(R.color.selected_highlight)
                                    )
                                } else this
                            }
                            .clickable { onSquareClick(rowIndex, colIndex) },
                        contentAlignment = Alignment.Center
                    ) {
                        square.piece?.let { piece ->
                            val pieceColor = if (piece.color == PlayerColor.RED) {
                                colorResource(R.color.piece_red)
                            } else {
                                colorResource(R.color.piece_black)
                            }

                            Canvas(modifier = Modifier.size(dimensionResource(R.dimen.piece_size))) {
                                drawCircle(color = pieceColor)
                                if (piece.type == PieceType.QUEEN) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = size.minDimension / 4
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
