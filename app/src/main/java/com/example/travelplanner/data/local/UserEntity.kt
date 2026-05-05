package com.example.travelplanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val homeCity: String,
    val preferredCurrency: String = "USD",
    val registeredAt: Long
)
