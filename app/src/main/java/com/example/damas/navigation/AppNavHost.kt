package com.example.damas.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.damas.data.constants.GameMode
import com.example.damas.ui.screens.GameScreen
import com.example.damas.ui.screens.HelpScreen
import com.example.damas.ui.screens.HistoryScreen
import com.example.damas.ui.screens.MenuScreen
import com.example.damas.ui.screens.ResultsScreen
import com.example.damas.ui.screens.SettingsScreen

/** Rutas de navegación de la app — string templates para Navigation Compose */
object Routes {
    const val MENU     = "menu"
    const val GAME     = "game/{mode}"
    const val RESULTS  = "results/{id}"
    const val HISTORY  = "history"
    const val SETTINGS = "settings"
    const val HELP     = "help"

    fun game(mode: GameMode): String = "game/${mode.name}"
    fun results(id: Int):     String = "results/$id"
}

/**
 * Grafo de navegación principal — sustituye a los Intent + startActivity dispersos
 * por las 7 Activities anteriores. La única Activity (MainActivity) hospeda
 * este NavHost; cada Composable recibe lambdas de navegación.
 */
@Composable
fun AppNavHost(onExitApp: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MENU) {

        composable(Routes.MENU) {
            MenuScreen(
                onPvPClick      = { navController.navigate(Routes.game(GameMode.PLAYER_VS_PLAYER)) },
                onPvAIClick     = { navController.navigate(Routes.game(GameMode.PLAYER_VS_AI)) },
                onHistoryClick  = { navController.navigate(Routes.HISTORY) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onHelpClick     = { navController.navigate(Routes.HELP) },
                onExitClick     = onExitApp
            )
        }

        composable(
            route     = Routes.GAME,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { entry ->
            val modeStr = entry.arguments?.getString("mode") ?: GameMode.PLAYER_VS_PLAYER.name
            val mode    = GameMode.valueOf(modeStr)
            GameScreen(
                mode = mode,
                onGameFinished = { gameId ->
                    // Reemplaza la ruta GAME por RESULTS para que "atrás" no vuelva a la partida
                    navController.navigate(Routes.results(gameId)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Routes.RESULTS,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) {
            ResultsScreen(
                onNewGame = {
                    navController.navigate(Routes.MENU) {
                        popUpTo(Routes.MENU) { inclusive = true }
                    }
                },
                onExit = onExitApp
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
    }
}
