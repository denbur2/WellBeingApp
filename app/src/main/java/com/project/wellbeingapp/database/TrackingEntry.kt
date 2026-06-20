package com.project.wellbeingapp.database

data class TrackingEntry(
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val packageName: String,
    val appName: String,
    val duration: Long
)
