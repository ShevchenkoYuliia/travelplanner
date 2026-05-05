package com.example.travelplanner.data.realtime

import com.google.gson.Gson
import com.google.gson.JsonParseException

class SocketEventParser(
    private val gson: Gson = Gson()
) {
    fun parse(rawJson: String): SocketEvent {
        val payload = gson.fromJson(rawJson, SocketEventPayload::class.java)
            ?: throw JsonParseException("Invalid payload")
        return SocketEvent(
            type = payload.type.orEmpty().ifBlank { "unknown" },
            action = payload.action.orEmpty().ifBlank { "unknown" },
            tripId = payload.tripId,
            userId = payload.userId,
            message = payload.message.orEmpty(),
            timestamp = payload.timestamp ?: System.currentTimeMillis()
        )
    }
}

private data class SocketEventPayload(
    val type: String?,
    val action: String?,
    val tripId: String?,
    val userId: String?,
    val message: String?,
    val timestamp: Long?
)
