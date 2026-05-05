package com.example.travelplanner.domain.repository

import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.result.SyncWriteResult
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeTrips(): Flow<List<Trip>>
    fun observeTripById(id: String): Flow<Trip?>
    fun observeRoutePoints(tripId: String): Flow<List<RoutePoint>>

    suspend fun saveTrip(trip: Trip): SyncWriteResult
    suspend fun updateTrip(trip: Trip): SyncWriteResult
    suspend fun deleteTrip(trip: Trip)

    suspend fun saveRoutePoint(point: RoutePoint): SyncWriteResult
    suspend fun updateRoutePoint(point: RoutePoint): SyncWriteResult
    suspend fun getRoutePointById(id: String): RoutePoint?
    suspend fun deleteRoutePoint(point: RoutePoint)

    suspend fun syncPendingTrips()
    suspend fun refreshTripsFromApi()

    suspend fun createInviteToken(tripId: String): String
    suspend fun getInvitePreview(token: String): com.example.travelplanner.domain.model.InviteTripPreview
    suspend fun acceptInvite(token: String): Trip
}
