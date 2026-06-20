package com.project.wellbeingapp.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_entries")
data class LocationEntry(
    @PrimaryKey val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?
)
