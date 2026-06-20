package com.project.wellbeingapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.project.wellbeingapp.database.dao.AppUsageDao
import com.project.wellbeingapp.database.dao.LocationDao
import com.project.wellbeingapp.database.dao.ScoreHistoryDao
import com.project.wellbeingapp.database.entity.AppUsageEntry
import com.project.wellbeingapp.database.entity.LocationEntry
import com.project.wellbeingapp.database.entity.ScoreHistoryEntry

@Database(
    entities = [LocationEntry::class, AppUsageEntry::class, ScoreHistoryEntry::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun scoreHistoryDao(): ScoreHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wellbeing_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
