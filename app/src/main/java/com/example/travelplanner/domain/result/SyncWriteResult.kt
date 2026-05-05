package com.example.travelplanner.domain.result

sealed interface SyncWriteResult {
    data object Synced : SyncWriteResult
    data class Queued(val reason: String? = null) : SyncWriteResult
}
