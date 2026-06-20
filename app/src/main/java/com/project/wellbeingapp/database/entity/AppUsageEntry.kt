package com.project.wellbeingapp.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "app_usage_entries",
    primaryKeys = ["timestamp", "packageName"],
    foreignKeys = [ForeignKey(
        entity = LocationEntry::class,
        parentColumns = ["timestamp"],
        childColumns = ["timestamp"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("timestamp")]
)
data class AppUsageEntry(
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val duration: Long
)
