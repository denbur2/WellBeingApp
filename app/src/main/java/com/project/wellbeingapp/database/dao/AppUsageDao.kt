package com.project.wellbeingapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.wellbeingapp.database.TrackingEntry
import com.project.wellbeingapp.database.entity.AppUsageEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AppUsageEntry)

    @Query("SELECT * FROM app_usage_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getByTimeRange(start: Long, end: Long): Flow<List<AppUsageEntry>>

    @Query("""
        SELECT l.timestamp, l.latitude, l.longitude, l.accuracy,
               a.packageName, a.appName, a.duration
        FROM location_entries l
        JOIN app_usage_entries a ON l.timestamp = a.timestamp
        WHERE l.timestamp BETWEEN :start AND :end
        ORDER BY l.timestamp ASC
    """)
    fun getTrackingEntries(start: Long, end: Long): Flow<List<TrackingEntry>>
}
