package com.example.travelplanner.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeTripById(id: String): Flow<TripEntity?>

    @Upsert
    suspend fun upsertTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: String)

    @Query("SELECT * FROM trips WHERE syncStatus IN ('pending', 'error')")
    suspend fun getUnsyncedTrips(): List<TripEntity>

    @Upsert
    suspend fun upsertPendingTripDeletion(deletion: PendingTripDeletionEntity)

    @Query("SELECT * FROM pending_trip_deletions ORDER BY createdAt ASC")
    suspend fun getPendingTripDeletions(): List<PendingTripDeletionEntity>

    @Query("DELETE FROM pending_trip_deletions WHERE tripId = :tripId")
    suspend fun removePendingTripDeletion(tripId: String)
}