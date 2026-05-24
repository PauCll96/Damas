package com.example.damas.data.models

import com.example.damas.data.constants.GameMode

/** Configuración de cada jugador — inmutable, se sustituye con .copy() */
data class PlayerSettings(
    val name: String,
    val colorHex: Long
)

/** Configuración global de la partida — inmutable, se sustituye con .copy() */
data class GameSettings(
    val mode: GameMode = GameMode.PLAYER_VS_PLAYER,
    val player1: PlayerSettings = PlayerSettings("Jugador 1", 0xFFFF0000),
    val player2: PlayerSettings = PlayerSettings("Jugador 2", 0xFF000000),
    val maxTimeMinutes: Int = 10
)

