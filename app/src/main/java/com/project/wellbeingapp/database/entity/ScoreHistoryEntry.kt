package com.project.wellbeingapp.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score_history")
data class ScoreHistoryEntry(
    @PrimaryKey val date: Long,
    val score: Int,
    val level: Int,
    val screenTimeMinutes: Int
)
