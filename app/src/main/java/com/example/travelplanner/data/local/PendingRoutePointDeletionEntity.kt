package com.example.travelplanner.data.local

import androidx.room.Entity

@Entity(
    tableName = "pending_route_point_deletions",
    primaryKeys = ["tripId", "pointId"]
)
data class PendingRoutePointDeletionEntity(
    val tripId: String,
    val pointId: String,
    val createdAt: Long = System.currentTimeMillis()
)
