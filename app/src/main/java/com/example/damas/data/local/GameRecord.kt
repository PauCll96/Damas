package com.example.damas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val player1Name: String,
    val player2Name: String,
    val winnerName: String,
    val gameMode: String,    // GameMode.name: "PLAYER_VS_PLAYER" o "PLAYER_VS_AI"
    val timeLeft: String,    // tiempo restante en formato MM:SS
    val redPieces: Int,      // piezas rojas al finalizar
    val blackPieces: Int,    // piezas negras al finalizar
    val moveLog: String      // log completo de movimientos (1.4)
)
