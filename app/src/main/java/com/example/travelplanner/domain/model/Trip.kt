package com.example.travelplanner.domain.model

import java.util.Date
import java.util.UUID

enum class SyncStatus {
    PENDING, SYNCED, ERROR
}

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String? = "unknown",
    val title: String,
    val destination: String,
    val startDate: Date,
    val endDate: Date,
    val totalBudget: Double,
    val currencyCode: String = "USD",
    @Transient val syncStatus: SyncStatus? = SyncStatus.PENDING,
    val coverImageUrl: String? = null,
    val notes: String = ""
) {
    val isSynced: Boolean
        get() = syncStatus == SyncStatus.SYNCED
}
