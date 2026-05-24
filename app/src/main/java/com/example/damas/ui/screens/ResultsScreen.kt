package com.example.damas.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.viewmodels.ResultsViewModel

@Composable
fun ResultsScreen(
    onNewGame: () -> Unit,
    onExit:    () -> Unit
) {
    val vm: ResultsViewModel = viewModel()
    val context = LocalContext.current

    val record = vm.record
    if (record == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val logDisplay = record.moveLog.ifBlank {
        stringResource(R.string.log_template, record.winnerName) +
                "\n" + stringResource(R.string.time_remaining_label, record.timeLeft)
    }

    var email by remember { mutableStateOf("") }

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
            value         = record.date,
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.fillMaxWidth()
        )

        Text(text = stringResource(R.string.label_log))
        OutlinedTextField(
            value         = logDisplay,
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

        Button(
            onClick  = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL,   arrayOf(email))
                    putExtra(Intent.EXTRA_SUBJECT, "Resultats Partida Dames")
                    putExtra(Intent.EXTRA_TEXT,    logDisplay)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.btn_send_email))
        }
        Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.btn_new_game))
        }
        Button(
            onClick  = {
                (context as? Activity)?.finishAffinity()
                onExit()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.btn_exit))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
