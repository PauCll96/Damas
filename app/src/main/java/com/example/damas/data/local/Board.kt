package com.example.damas.data.local

import com.example.damas.data.constants.Teams

class Board(
    val cells: Array<Array<Cell>> = createInitialBoard()
) {
    companion object {
        fun createInitialBoard(): Array<Array<Cell>> {
            return Array(8) { r ->
                Array(8) { c ->
                    val piece = if ((r + c) % 2 != 0) {
                        when {
                            r < 3 -> Piece(Teams.BLACK)
                            r > 4 -> Piece(Teams.RED)
                            else -> null
                        }
                    } else null
                    Cell(r, c, piece)
                }
            }
        }
    }

    fun getCell(row: Int, col: Int): Cell = cells[row][col]

    fun copy(cells: Array<Array<Cell>> = this.cells): Board = Board(cells)
    
    fun flatten() = cells.flatten()
}
