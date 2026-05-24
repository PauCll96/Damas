package com.example.damas.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

class GameRecordRepository private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).gameRecordDao()

    val allGames: Flow<List<GameRecord>> = dao.getAllOrderedByDate()

    suspend fun insert(record: GameRecord): Long = dao.insert(record)

    suspend fun getById(id: Int): GameRecord? = dao.getById(id)

    suspend fun deleteById(id: Int) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        @Volatile
        private var INSTANCE: GameRecordRepository? = null

        fun getInstance(context: Context): GameRecordRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameRecordRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
