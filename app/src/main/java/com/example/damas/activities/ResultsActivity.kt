package com.example.damas.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.damas.R
import com.example.damas.data.local.GameHistoryRepository
import com.example.damas.data.models.GameResult
import com.example.damas.ui.theme.DamasTheme
import java.text.SimpleDateFormat
import java.util.*

class ResultsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_WINNER    = "WINNER"
        const val EXTRA_TIME_LEFT = "TIME_LEFT"
        const val EXTRA_PLAYER1   = "PLAYER1"
        const val EXTRA_PLAYER2   = "PLAYER2"
        const val EXTRA_LOG       = "MOVE_LOG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val winner   = intent.getStringExtra(EXTRA_WINNER)    ?: "Desconegut"
        val timeLeft = intent.getStringExtra(EXTRA_TIME_LEFT) ?: "00:00"
        val player1  = intent.getStringExtra(EXTRA_PLAYER1)   ?: ""
        val player2  = intent.getStringExtra(EXTRA_PLAYER2)   ?: ""
        val moveLog  = intent.getStringExtra(EXTRA_LOG)       ?: ""
        val date     = SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault()).format(Date())

        // Guardar al repositorio solo la primera vez (evita duplicado en rotación)
        if (savedInstanceState == null) {
            GameHistoryRepository.addGame(
                GameResult(
                    date        = date,
                    winnerName  = winner,
                    timeLeft    = timeLeft,
                    player1Name = player1,
                    player2Name = player2,
                    moveLog     = moveLog
                )
            )
        }

        val logDisplay = moveLog.ifBlank {
            getString(R.string.log_template, winner) + "\n${getString(R.string.time_remaining_label, timeLeft)}"
        }

        setContent {
            DamasTheme {
                ResultsScreen(
                    date       = date,
                    log        = logDisplay,
                    onSendEmail = { email, body -> sendEmail(email, body) },
                    onNewGame   = {
                        startActivity(
                            Intent(this, MenuActivity::class.java)
                                .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }
                        )
                    },
                    onExit = { finishAffinity() }
                )
            }
        }
    }

    private fun sendEmail(email: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL,   arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Resultats Partida Dames")
            putExtra(Intent.EXTRA_TEXT,    body)
        }
        startActivity(intent)
    }
}

// ── STATELESS ─────────────────────────────────────────────────────────────────

@Composable
fun ResultsScreen(
    date:        String,
    log:         String,
    onSendEmail: (String, String) -> Unit,
    onNewGame:   () -> Unit,
    onExit:      () -> Unit
) {
    var email by remember { mutableStateOf("") }

    // (3.1) Column con scroll para que funcione en landscape y textos largos
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.results_title), style = MaterialTheme.typography.headlineMedium)

        Text(text = stringResource(R.string.label_date))
        OutlinedTextField(
            value         = date,
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.fillMaxWidth()
        )

        Text(text = stringResource(R.string.label_log))
        OutlinedTextField(
            value         = log,
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
            maxLines      = 12
        )

        Text(text = stringResource(R.string.label_email))
        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            modifier      = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onSendEmail(email, log) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_send_email))
        }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_new_game))
        }
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_exit))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
