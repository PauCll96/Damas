package com.example.damas.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

// Material3: Compact < 600dp, Medium/Expanded >= 600dp
private const val MEDIUM_WIDTH_DP = 600

/**
 * Devuelve true si el ancho disponible corresponde a tablet (Medium o Expanded).
 * Se recalcula automáticamente al rotar la pantalla.
 */
@Composable
fun rememberIsTwoPanel(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= MEDIUM_WIDTH_DP
}
