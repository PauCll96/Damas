package com.example.damas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CheckersViewModel : ViewModel() {

    // L'estat del tauler: Lectura pública, Escriptura privada (private set)
    // Això garanteix l'encapsulació professional
    var boardState by mutableStateOf(List(8) { row ->
        List(8) { col -> Square(row, col) }
    })
        private set

    init {
        resetBoard()
    }

    private fun resetBoard() {
        boardState = List(8) { row ->
            List(8) { col ->
                val piece = when {
                    // Col·loquem fitxes negres a les primeres 3 files (només caselles fosques)
                    (row + col) % 2 != 0 && row < 3 -> Piece(PlayerColor.BLACK)
                    // Col·loquem fitxes vermelles a les últimes 3 files
                    (row + col) % 2 != 0 && row > 4 -> Piece(PlayerColor.RED)
                    else -> null
                }
                Square(row, col, piece)
            }
        }
    }

    // Aquesta funció serà el "pont" amb la UI
    fun onSquareClicked(row: Int, col: Int) {
        // De moment només per comprovar que funciona
        println("Has clicat la casella: $row, $col")
    }
}