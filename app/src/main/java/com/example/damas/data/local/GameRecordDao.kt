package com.example.damas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GameRecord): Long

    @Query("SELECT * FROM game_records ORDER BY id DESC")
    fun getAllOrderedByDate(): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): GameRecord?

    @Query("DELETE FROM game_records WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM game_records")
    suspend fun deleteAll()
}
