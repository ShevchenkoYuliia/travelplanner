package com.example.travelplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_trip_deletions")
data class PendingTripDeletionEntity(
    @PrimaryKey val tripId: String,
    val createdAt: Long = System.currentTimeMillis()
)
