package com.example.travelplanner.models

import java.util.Date

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val homeCity: String = "",
    val totalTrips: Int = 0,
    val totalKmTraveled: Double = 0.0,
    val isPremium: Boolean = false,
    val registeredAt: Date = Date(),
    val preferredCurrency: String = "USD"
)
