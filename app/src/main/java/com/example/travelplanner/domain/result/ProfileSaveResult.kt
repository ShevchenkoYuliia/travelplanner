package com.example.travelplanner.domain.result

sealed class ProfileSaveResult {
    data object SyncedToServer : ProfileSaveResult()
    data class SavedLocallyOnly(val reason: String?) : ProfileSaveResult()
}
