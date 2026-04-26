package com.example.damas

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuración de Partida", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Jugador 1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextField(
                    value = settings.player1.name,
                    onValueChange = { vm.settings = settings.copy(player1 = settings.player1.copy(name = it)) },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Color de fichas:", style = MaterialTheme.typography.bodySmall)
                ColorPicker(
                    selectedColor = settings.player1.colorHex,
                    disabledColors = listOf(settings.player2.colorHex)
                ) { 
                    vm.settings = settings.copy(player1 = settings.player1.copy(colorHex = it))
                }
            }
        }

        if (settings.mode == GameMode.PLAYER_VS_PLAYER) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Jugador 2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    TextField(
                        value = settings.player2.name,
                        onValueChange = { vm.settings = settings.copy(player2 = settings.player2.copy(name = it)) },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Color de fichas:", style = MaterialTheme.typography.bodySmall)
                    ColorPicker(
                        selectedColor = settings.player2.colorHex,
                        disabledColors = listOf(settings.player1.colorHex)
                    ) { 
                        vm.settings = settings.copy(player2 = settings.player2.copy(colorHex = it))
                    }
                }
            }
        } else {
            Text("IA lista para jugar (Color: Negro)", color = MaterialTheme.colorScheme.secondary)
            LaunchedEffect(settings.mode) { 
                // Forzamos a la IA a ser Negro
                val newP2 = settings.player2.copy(name = "IA", colorHex = 0xFF000000L)
                // Si el jugador 1 ya era Negro, lo cambiamos a Rojo
                val newP1 = if (settings.player1.colorHex == 0xFF000000L) {
                    settings.player1.copy(colorHex = 0xFFFF0000L)
                } else {
                    settings.player1
                }
                vm.settings = settings.copy(player1 = newP1, player2 = newP2)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tiempo de partida (minutos)", fontWeight = FontWeight.Bold)
                TextField(
                    value = if (settings.maxTimeMinutes == 0) "" else settings.maxTimeMinutes.toString(),
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        vm.settings = settings.copy(maxTimeMinutes = if (filtered.isEmpty()) 0 else filtered.toInt())
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: 10") }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { vm.startGame() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = settings.player1.name.isNotBlank() && 
                      (settings.mode == GameMode.PLAYER_VS_AI || settings.player2.name.isNotBlank()) &&
                      settings.maxTimeMinutes > 0
        ) {
            Text("▶  ¡EMPEZAR PARTIDA!")
        }
        TextButton(onClick = onBack) { Text("Cancelar") }
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
fun MainGameScreen(vm: CheckersViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val winner = vm.winner

    // Tema 3: Intent Explícit quan hi ha un guanyador
    LaunchedEffect(winner) {
        if (winner != null) {
            val winName = if (winner == PlayerColor.RED) vm.settings.player1.name else vm.settings.player2.name
            val intent = Intent(context, ResultsActivity::class.java).apply {
                putExtra("WINNER", winName)
            }
            context.startActivity(intent)
        }
    }

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
                Text("Tiempo restante: ${formatTime(vm.timeLeftSeconds)}")
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
