package com.project.wellbeingapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.wellbeingapp.database.entity.ScoreHistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScoreHistoryEntry)

    @Query("SELECT * FROM score_history ORDER BY date DESC")
    fun getAll(): Flow<List<ScoreHistoryEntry>>

    @Query("SELECT * FROM score_history WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getByDateRange(start: Long, end: Long): Flow<List<ScoreHistoryEntry>>

    @Query("SELECT * FROM score_history WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: Long): ScoreHistoryEntry?
}
