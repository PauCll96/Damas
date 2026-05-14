package com.example.damas.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.damas.R
import com.example.damas.data.models.GameResult
import com.example.damas.ui.theme.DamasTheme

/**
 * Activity de detalle solo para smartphones.
 * En tablet, el detalle se muestra en el panel derecho de HistoryActivity.
 */
class GameDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_WINNER    = "detail_winner"
        const val EXTRA_DATE      = "detail_date"
        const val EXTRA_TIME_LEFT = "detail_time_left"
        const val EXTRA_PLAYER1   = "detail_player1"
        const val EXTRA_PLAYER2   = "detail_player2"
        const val EXTRA_GAME_ID   = "detail_game_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Recibimos el GameResult desmontado en extras (Intent explícito)
        val game = GameResult(
            id          = intent.getIntExtra(EXTRA_GAME_ID, 0),
            winnerName  = intent.getStringExtra(EXTRA_WINNER)    ?: "",
            date        = intent.getStringExtra(EXTRA_DATE)       ?: "",
            timeLeft    = intent.getStringExtra(EXTRA_TIME_LEFT)  ?: "",
            player1Name = intent.getStringExtra(EXTRA_PLAYER1)    ?: "",
            player2Name = intent.getStringExtra(EXTRA_PLAYER2)    ?: ""
        )

        setContent {
            DamasTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GameDetailScreen(game = game, onBack = { finish() })
                }
            }
        }
    }
}

// ── STATELESS ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(game: GameResult, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        GameDetailContent(
            game     = game,
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize()
        )
    }
}
