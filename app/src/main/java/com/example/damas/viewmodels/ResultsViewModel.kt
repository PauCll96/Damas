package com.example.damas.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.damas.data.local.GameRecord
import com.example.damas.data.local.GameRecordRepository
import kotlinx.coroutines.launch

/**
 * Lee el id del GameRecord desde la ruta de Navigation Compose (results/{id})
 * y carga el detalle desde Room. Antes la pantalla recibía 8 putExtra desde
 * el Intent; ahora pasa un único id y consulta la BD.
 */
class ResultsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = GameRecordRepository.getInstance(application)

    /** id obtenido de la ruta results/{id} */
    private val gameId: Int = savedStateHandle.get<Int>("id") ?: -1

    var record by mutableStateOf<GameRecord?>(null)
        private set

    init {
        if (gameId > 0) {
            viewModelScope.launch {
                record = repository.getById(gameId)
            }
        }
    }
}
