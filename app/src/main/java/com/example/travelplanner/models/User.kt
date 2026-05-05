package com.example.travelplanner.models

import java.util.Date

data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val homeCity: String = "",
    val preferredCurrency: String = "USD",
    val age: Int = 0,
    val isActive: Boolean = true,
    val registeredAt: Date = Date()
)
