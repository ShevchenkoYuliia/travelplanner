package com.example.travelplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class RoutePointEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val arrivalDate: Long,
    val durationDays: Int,
    val estimatedCost: Double,
    val currencyCode: String = "USD",
    val isVisited: Boolean,
    val category: String,
    val notes: String,
    val syncStatus: String = "pending"
)
