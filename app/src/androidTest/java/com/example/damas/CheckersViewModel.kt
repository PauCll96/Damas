package com.example.damas

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CheckersViewModel : ViewModel() {

    // 1. La "Backing Property" siguiendo tu imagen:
    // Privada y mutable internamente
    private var _boardState = mutableStateOf(createInitialBoard())

    // 2. La versión pública de solo lectura:
    // La UI solo puede "mirar" esta lista
    val boardState: List<Square>
        get() = _boardState.value

    private fun createInitialBoard(): List<Square> {
        return List(64) { index ->
            val row = index / 8
            val col = index % 8
            val piece = when {
                (row + col) % 2 != 0 && row < 3 -> Piece(PlayerColor.BLACK)
                (row + col) % 2 != 0 && row > 4 -> Piece(PlayerColor.RED)
                else -> null
            }
            Square(row, col, piece)
        }
    }

    // El "Puente" con la UI
    fun onSquareClicked(row: Int, col: Int) {
        // Ejemplo de lógica: Mover una pieza (Inmutable)
        val newList = _boardState.value.toMutableList()

        // Aquí vendría tu lógica de juego...
        // Por ejemplo, para cambiar algo, creas una COPIA del Square:
        // val index = row * 8 + col
        // newList[index] = newList[index].copy(piece = null)

        // 3. Actualizamos el estado completo
        _boardState.value = newList
    }
}