package com.example.damas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.damas.navigation.AppNavHost
import com.example.damas.ui.theme.DamasTheme
import com.example.damas.ui.utils.LocalWindowSizeClass
import com.example.damas.ui.utils.rememberWindowSizeClass

/**
 * Activity única de la app — sustituye a las 7 Activities anteriores
 * (Menu, Game, History, GameDetail, Settings, Help, Results).
 *
 * Aquí calculamos UNA SOLA VEZ el WindowSizeClass oficial de Material3 y lo
 * propagamos por CompositionLocal para que cualquier pantalla pueda decidir
 * su layout (compact/medium/expanded) sin volver a leer LocalConfiguration.
 *
 * Toda la navegación entre pantallas se delega a AppNavHost.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DamasTheme {
                val windowSizeClass = rememberWindowSizeClass()
                CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color    = MaterialTheme.colorScheme.background
                    ) {
                        AppNavHost(onExitApp = { finish() })
                    }
                }
            }
        }
    }
}
