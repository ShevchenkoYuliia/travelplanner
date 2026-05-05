package com.example.travelplanner.domain.usecase.trip

import com.example.travelplanner.domain.model.InviteTripPreview
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.repository.TripRepository
import com.example.travelplanner.domain.result.SyncWriteResult
import kotlinx.coroutines.flow.Flow

class ObserveTripsUseCase(private val repository: TripRepository) {
    operator fun invoke(): Flow<List<Trip>> = repository.observeTrips()
}

class ObserveTripByIdUseCase(private val repository: TripRepository) {
    operator fun invoke(id: String): Flow<Trip?> = repository.observeTripById(id)
}

class ObserveRoutePointsUseCase(private val repository: TripRepository) {
    operator fun invoke(tripId: String): Flow<List<RoutePoint>> = repository.observeRoutePoints(tripId)
}

class SaveTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip): SyncWriteResult = repository.saveTrip(trip)
}

class DeleteTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip) = repository.deleteTrip(trip)
}

class SaveRoutePointUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(point: RoutePoint): SyncWriteResult = repository.saveRoutePoint(point)
}

class UpdateRoutePointUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(point: RoutePoint): SyncWriteResult = repository.updateRoutePoint(point)
}

class DeleteRoutePointUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(point: RoutePoint) = repository.deleteRoutePoint(point)
}

class SyncPendingTripsUseCase(private val repository: TripRepository) {
    suspend operator fun invoke() = repository.syncPendingTrips()
}

class RefreshTripsUseCase(private val repository: TripRepository) {
    suspend operator fun invoke() = repository.refreshTripsFromApi()
}

class CreateInviteTokenUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String): String = repository.createInviteToken(tripId)
}

class GetInvitePreviewUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(token: String): InviteTripPreview = repository.getInvitePreview(token)
}

class AcceptInviteUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(token: String): Trip = repository.acceptInvite(token)
}

data class TripUseCases(
    val observeTrips: ObserveTripsUseCase,
    val observeTripById: ObserveTripByIdUseCase,
    val observeRoutePoints: ObserveRoutePointsUseCase,
    val saveTrip: SaveTripUseCase,
    val deleteTrip: DeleteTripUseCase,
    val saveRoutePoint: SaveRoutePointUseCase,
    val updateRoutePoint: UpdateRoutePointUseCase,
    val deleteRoutePoint: DeleteRoutePointUseCase,
    val syncPendingTrips: SyncPendingTripsUseCase,
    val refreshTrips: RefreshTripsUseCase,
    val createInviteToken: CreateInviteTokenUseCase,
    val getInvitePreview: GetInvitePreviewUseCase,
    val acceptInvite: AcceptInviteUseCase
)
