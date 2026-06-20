package com.project.wellbeingapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.wellbeingapp.database.entity.LocationEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LocationEntry)

    @Query("SELECT * FROM location_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<LocationEntry>>

    @Query("SELECT * FROM location_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getByTimeRange(start: Long, end: Long): Flow<List<LocationEntry>>

    @Query("DELETE FROM location_entries WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
