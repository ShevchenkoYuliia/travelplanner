package com.example.travelplanner.domain.usecase.invite

import com.example.travelplanner.domain.model.InviteTripPreview
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.repository.TripRepository

class CreateInviteTokenUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String): String {
        return repository.createInviteToken(tripId)
    }
}

class GetInvitePreviewUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(token: String): InviteTripPreview {
        return repository.getInvitePreview(token)
    }
}

class AcceptInviteUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(token: String): Trip {
        return repository.acceptInvite(token)
    }
}
