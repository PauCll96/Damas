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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.ui.theme.DamasTheme

class GameActivity : ComponentActivity() {
    companion object { const val EXTRA_MODE = "extra_game_mode" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val modeName = intent.getStringExtra(EXTRA_MODE) ?: GameMode.PLAYER_VS_PLAYER.name
        val initialMode = GameMode.valueOf(modeName)

        setContent {
            DamasTheme {
                val vm: CheckersViewModel = viewModel()
                LaunchedEffect(Unit) { vm.settings.mode = initialMode }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!vm.isGameStarted) {
                        SetupScreen(vm) { finish() }
                    } else {
                        MainGameScreen(vm) { vm.resetToMenu() }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupScreen(vm: CheckersViewModel, onBack: () -> Unit) {
    val settings = vm.settings
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuración de Partida", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Jugador 1", fontWeight = FontWeight.Bold)
                TextField(value = settings.player1.name, onValueChange = { settings.player1 = settings.player1.copy(name = it) }, modifier = Modifier.fillMaxWidth())
                ColorPicker(selectedColor = settings.player1.colorHex) { settings.player1 = settings.player1.copy(colorHex = it) }
            }
        }

        if (settings.mode == GameMode.PLAYER_VS_PLAYER) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Jugador 2", fontWeight = FontWeight.Bold)
                    TextField(value = settings.player2.name, onValueChange = { settings.player2 = settings.player2.copy(name = it) }, modifier = Modifier.fillMaxWidth())
                    ColorPicker(selectedColor = settings.player2.colorHex) { settings.player2 = settings.player2.copy(colorHex = it) }
                }
            }
        } else {
            Text("IA lista para jugar", color = MaterialTheme.colorScheme.secondary)
            LaunchedEffect(Unit) { settings.player2.name = "IA" }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tiempo: ${settings.maxTimeMinutes} min", modifier = Modifier.weight(1f))
                Slider(value = settings.maxTimeMinutes.toFloat(), onValueChange = { settings.maxTimeMinutes = it.toInt() }, valueRange = 1f..30f, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { vm.startGame() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("▶  ¡EMPEZAR PARTIDA!")
        }
        TextButton(onClick = onBack) { Text("Cancelar") }
    }
}

@Composable
fun ColorPicker(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    val colors = listOf(0xFFFF0000, 0xFF0000FF, 0xFF00FF00, 0xFF000000, 0xFFFFA500)
    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(hex))
                    .border(2.dp, if (selectedColor == hex) Color.White else Color.Transparent, CircleShape)
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

@Composable
fun MainGameScreen(vm: CheckersViewModel, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Damas", style = MaterialTheme.typography.headlineMedium)

        if (vm.winner != null) {
            val winName = if (vm.winner == PlayerColor.RED) vm.settings.player1.name else vm.settings.player2.name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("¡Ganador: $winName!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { vm.startGame() }) { Text("Nueva Partida") }
            }
        } else {
            val turnName = if (vm.currentPlayer == PlayerColor.RED) vm.settings.player1.name else vm.settings.player2.name
            val turnColor = Color(if (vm.currentPlayer == PlayerColor.RED) vm.settings.player1.colorHex else vm.settings.player2.colorHex)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Turno de: $turnName", color = turnColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tiempo: ${formatTime(vm.timeElapsed)}")
                if (vm.isAiThinking) LinearProgressIndicator(Modifier.width(100.dp))
            }
        }

        CheckersBoard(vm)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onBack) { Text("Menú") }
            OutlinedButton(onClick = { vm.surrender() }) { Text("Rendirse") }
        }
    }
}

@Composable
fun CheckersBoard(vm: CheckersViewModel) {
    Column(modifier = Modifier.aspectRatio(1f).border(2.dp, Color.Gray)) {
        vm.boardState.forEachIndexed { r, row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEachIndexed { c, sq ->
                    val isLight = (r + c) % 2 == 0
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(if (isLight) Color(0xFFF0D9B5) else Color(0xFFB58863))
                            .run { if (sq.isSelected) border(3.dp, Color.Yellow) else this }
                            .clickable { vm.onSquareClicked(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        sq.piece?.let { piece ->
                            val pColor = Color(if (piece.color == PlayerColor.RED) vm.settings.player1.colorHex else vm.settings.player2.colorHex)
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
