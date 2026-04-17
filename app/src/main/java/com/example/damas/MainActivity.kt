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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.ui.theme.DamasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DamasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Tema 3: El ViewModel es el dueño del estado
                    val checkersViewModel: CheckersViewModel = viewModel()
                    GameScreen(checkersViewModel)
                }
            }
        }
    }
}

/**
 * Composable STATEFUL: Se comunica con el ViewModel (MiniActv-5)
 */
@Composable
fun GameScreen(viewModel: CheckersViewModel) {
    val board = viewModel.boardState
    val currentPlayer = viewModel.currentPlayer
    val winner = viewModel.winner

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_standard)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.game_title),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_medium)))

        if (winner != null) {
            // Muestra quién ha ganado (Tema 2)
            val winnerText = if (winner == PlayerColor.RED) {
                stringResource(R.string.winner_red)
            } else {
                stringResource(R.string.winner_black)
            }
            
            Text(
                text = winnerText,
                style = MaterialTheme.typography.headlineSmall,
                color = colorResource(R.color.board_dark)
            )

            Button(
                onClick = { viewModel.resetGame() },
                modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_standard))
            ) {
                // Texto del botón desde resources
                Text(text = stringResource(R.string.reset_button))
            }
        } else {
            // Texto del turno
            val turnText = if (currentPlayer == PlayerColor.RED) {
                stringResource(R.string.turn_red)
            } else {
                stringResource(R.string.turn_black)
            }
            
            Text(
                text = turnText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (currentPlayer == PlayerColor.RED) colorResource(R.color.piece_red) else colorResource(R.color.piece_black)
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_medium)))

        CheckersBoard(
            board = board, 
            onSquareClick = { r, c -> viewModel.onSquareClicked(r, c) }
        )
    }
}

/**
 * Composable STATELESS: Solo dibuja, no conoce la lógica (Alta Cohesión)
 */
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
                            // Si la casilla está seleccionada, añadimos un borde de resaltado (Tema 2)
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
                                
                                // Si es REINA, dibujamos una marca (Tema 2)
                                if (piece.type == PieceType.QUEEN) {
                                    drawCircle(
                                        color = androidx.compose.ui.graphics.Color.White,
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