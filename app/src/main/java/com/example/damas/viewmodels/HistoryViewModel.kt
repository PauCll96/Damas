package com.example.damas.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.damas.data.local.GameRecord
import com.example.damas.data.local.GameRecordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRecordRepository.getInstance(application)

    val games: StateFlow<List<GameRecord>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}
