package com.example.travelplanner.data.realtime

data class SocketEvent(
    val type: String,
    val action: String,
    val tripId: String?,
    val userId: String?,
    val message: String,
    val timestamp: Long
)
