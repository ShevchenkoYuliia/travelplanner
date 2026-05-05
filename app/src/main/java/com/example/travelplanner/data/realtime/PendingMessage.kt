package com.example.travelplanner.data.realtime

import java.util.UUID

enum class PendingSyncStatus {
    PendingSync,
    Sent,
    Failed
}

data class PendingMessage(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val status: PendingSyncStatus = PendingSyncStatus.PendingSync,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val errorMessage: String? = null
)
