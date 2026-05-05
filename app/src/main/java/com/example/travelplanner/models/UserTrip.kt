package com.example.travelplanner.models
import java.util.Date
data class UserTrip(
    val userId: String,
    val tripId: String,
    val role: String = "member",
    val joinedAt: Date = Date(),
    val isFavorite: Boolean = false,
    val contribution: Double = 0.0
)
