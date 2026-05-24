package com.example.damas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.damas.R

@Composable
fun MenuScreen(
    onPvPClick:      () -> Unit,
    onPvAIClick:     () -> Unit,
    onHistoryClick:  () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick:     () -> Unit,
    onExitClick:     () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(R.dimen.padding_standard)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text  = stringResource(R.string.menu_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))
            MenuButton(text = stringResource(R.string.btn_play),     onClick = onPvPClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = stringResource(R.string.btn_play_ai),  onClick = onPvAIClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = stringResource(R.string.btn_history),  onClick = onHistoryClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = stringResource(R.string.btn_settings), onClick = onSettingsClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = stringResource(R.string.btn_help),     onClick = onHelpClick)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton(text = stringResource(R.string.btn_exit),     onClick = onExitClick)
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
        shape    = MaterialTheme.shapes.medium
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
