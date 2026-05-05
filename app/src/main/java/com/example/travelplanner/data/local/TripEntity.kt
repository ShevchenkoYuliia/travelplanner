package com.example.travelplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for Trip.
 * syncStatus: "pending" | "synced" | "error"
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val ownerId: String = "unknown",
    val title: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val totalBudget: Double,
    val currencyCode: String = "USD",
    val coverImageUrl: String?,
    val notes: String,
    val syncStatus: String = "pending"
)
