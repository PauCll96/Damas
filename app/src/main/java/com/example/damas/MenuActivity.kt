package com.example.damas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.damas.ui.theme.DamasTheme

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DamasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MenuScreen(
                        onPlayClick = {
                            val intent = Intent(this, GameActivity::class.java)
                            startActivity(intent)
                        },
                        onHelpClick = {
                            val intent = Intent(this, HelpActivity::class.java)
                            startActivity(intent)
                        },
                        onExitClick = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuScreen(
    onPlayClick: () -> Unit,
    onHelpClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_standard)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.menu_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        MenuButton(text = stringResource(R.string.btn_play), onClick = onPlayClick)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = stringResource(R.string.btn_help), onClick = onHelpClick)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = stringResource(R.string.btn_exit), onClick = onExitClick)
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
