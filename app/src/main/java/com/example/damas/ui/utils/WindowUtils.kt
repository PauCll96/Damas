package com.example.damas.ui.utils

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * CompositionLocal con el WindowSizeClass actual.
 * Se inicializa en MainActivity con calculateWindowSizeClass(this) y se propaga
 * a todo el árbol de composición — cada pantalla decide su layout sin volver a
 * leer LocalConfiguration.
 */
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass no inicializado: envolver con CompositionLocalProvider en MainActivity")
}

/**
 * Helper para obtener el WindowSizeClass desde una Activity (Material3 API oficial).
 * Anotado con la opt-in requerida para evitar contaminar cada pantalla.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val activity = LocalContext.current as Activity
    return calculateWindowSizeClass(activity)
}

/**
 * Helper semántico: true si el ancho permite bi-panel (Medium o Expanded).
 * Sustituye al antiguo cálculo manual basado en screenWidthDp.
 */
fun WindowSizeClass.isTwoPanel(): Boolean =
    widthSizeClass != WindowWidthSizeClass.Compact
