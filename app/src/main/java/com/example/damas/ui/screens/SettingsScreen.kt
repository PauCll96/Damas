package com.example.damas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.damas.R
import com.example.damas.data.models.GameSettings
import com.example.damas.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()

    fun saveAndClose() { vm.save(); onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = ::saveAndClose) {
                        Text(stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    TextButton(onClick = ::saveAndClose) {
                        Text(stringResource(R.string.btn_save), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!vm.isLoaded) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SettingsPlayer1Card(settings = vm.settings, onSettingsChange = vm::updateSettings)
                SettingsPlayer2Card(settings = vm.settings, onSettingsChange = vm::updateSettings)
                SettingsTimeCard(settings = vm.settings, onSettingsChange = vm::updateSettings)

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick  = ::saveAndClose,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsPlayer1Card(settings: GameSettings, onSettingsChange: (GameSettings) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text       = stringResource(R.string.player_1_label),
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value         = settings.player1.name,
                onValueChange = { onSettingsChange(settings.copy(player1 = settings.player1.copy(name = it))) },
                label         = { Text(stringResource(R.string.name_label)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            Text(stringResource(R.string.piece_color_label), style = MaterialTheme.typography.bodySmall)
            PreferencesColorPicker(
                selectedColor  = settings.player1.colorHex,
                disabledColors = listOf(settings.player2.colorHex)
            ) { onSettingsChange(settings.copy(player1 = settings.player1.copy(colorHex = it))) }
        }
    }
}

@Composable
private fun SettingsPlayer2Card(settings: GameSettings, onSettingsChange: (GameSettings) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text       = stringResource(R.string.player_2_label),
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.secondary
            )
            Text(
                text  = stringResource(R.string.player_2_pvp_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value         = settings.player2.name,
                onValueChange = { onSettingsChange(settings.copy(player2 = settings.player2.copy(name = it))) },
                label         = { Text(stringResource(R.string.name_label)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            Text(stringResource(R.string.piece_color_label), style = MaterialTheme.typography.bodySmall)
            PreferencesColorPicker(
                selectedColor  = settings.player2.colorHex,
                disabledColors = listOf(settings.player1.colorHex)
            ) { onSettingsChange(settings.copy(player2 = settings.player2.copy(colorHex = it))) }
        }
    }
}

@Composable
private fun SettingsTimeCard(settings: GameSettings, onSettingsChange: (GameSettings) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.game_time_label), fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value           = if (settings.maxTimeMinutes == 0) "" else settings.maxTimeMinutes.toString(),
                onValueChange   = { v ->
                    val digits = v.filter { it.isDigit() }
                    onSettingsChange(settings.copy(maxTimeMinutes = if (digits.isEmpty()) 0 else digits.toInt()))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.fillMaxWidth(),
                label           = { Text(stringResource(R.string.game_time_label)) },
                placeholder     = { Text(stringResource(R.string.time_placeholder)) }
            )
        }
    }
}

@Composable
private fun PreferencesColorPicker(
    selectedColor:   Long,
    disabledColors:  List<Long>,
    onColorSelected: (Long) -> Unit
) {
    val colors = listOf(0xFFFF0000L, 0xFF0000FFL, 0xFF00FF00L, 0xFF000000L, 0xFFFFA500L)
    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { hex ->
            val isSelected = selectedColor == hex
            val isDisabled = disabledColors.contains(hex)
            val colorName = when (hex) {
                0xFFFF0000L -> stringResource(R.string.color_red)
                0xFF0000FFL -> stringResource(R.string.color_blue)
                0xFF00FF00L -> stringResource(R.string.color_green)
                0xFF000000L -> stringResource(R.string.color_black)
                0xFFFFA500L -> stringResource(R.string.color_orange)
                else        -> ""
            }
            val desc = stringResource(R.string.cd_color_option, colorName)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDisabled) Color.Gray.copy(alpha = 0.2f) else Color(hex))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isDisabled -> Color.Transparent
                            else       -> Color.Gray
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = !isDisabled) { onColorSelected(hex) }
                    .semantics { contentDescription = desc },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Text(
                    text       = "✓",
                    color      = if (hex == 0xFF000000L || hex == 0xFF0000FFL) Color.White else Color.Black,
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
