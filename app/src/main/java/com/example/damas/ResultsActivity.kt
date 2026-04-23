package com.example.damas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.damas.ui.theme.DamasTheme
import java.text.SimpleDateFormat
import java.util.*

class ResultsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Rebem qui ha guanyat per Intent (Tema 3)
        val winner = intent.getStringExtra("WINNER") ?: "Desconegut"
        val date = SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault()).format(Date())
        val logContent = getString(R.string.log_template, winner)

        setContent {
            DamasTheme {
                ResultsScreen(
                    date = date,
                    log = logContent,
                    onSendEmail = { email, body -> sendEmail(email, body) },
                    onNewGame = {
                        val i = Intent(this, MenuActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(i)
                    },
                    onExit = { finishAffinity() }
                )
            }
        }
    }

    private fun sendEmail(email: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Resultats Partida Dames")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(intent)
    }
}

@Composable
fun ResultsScreen(
    date: String,
    log: String,
    onSendEmail: (String, String) -> Unit,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.results_title), style = MaterialTheme.typography.headlineMedium)

        Text(text = stringResource(R.string.label_date))
        OutlinedTextField(value = date, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())

        Text(text = stringResource(R.string.label_log))
        OutlinedTextField(value = log, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth())

        Text(text = stringResource(R.string.label_email))
        OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onSendEmail(email, log) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_send_email))
        }

        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_new_game))
        }

        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_exit))
        }
    }
}