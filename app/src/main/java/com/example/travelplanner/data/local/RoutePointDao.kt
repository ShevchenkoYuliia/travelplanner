package com.example.travelplanner.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutePointDao {

    @Query("SELECT * FROM route_points WHERE tripId = :tripId ORDER BY arrivalDate ASC")
    fun getPointsForTrip(tripId: String): Flow<List<RoutePointEntity>>

    @Query("SELECT * FROM route_points WHERE id = :id")
    suspend fun getPointById(id: String): RoutePointEntity?

    @Upsert
    suspend fun upsertPoint(point: RoutePointEntity)

    @Delete
    suspend fun deletePoint(point: RoutePointEntity)

    @Query("SELECT * FROM route_points WHERE syncStatus IN ('pending', 'error')")
    suspend fun getUnsyncedPoints(): List<RoutePointEntity>

    @Upsert
    suspend fun upsertPendingRoutePointDeletion(deletion: PendingRoutePointDeletionEntity)

    @Query("SELECT * FROM pending_route_point_deletions ORDER BY createdAt ASC")
    suspend fun getPendingRoutePointDeletions(): List<PendingRoutePointDeletionEntity>

    @Query("DELETE FROM pending_route_point_deletions WHERE tripId = :tripId AND pointId = :pointId")
    suspend fun removePendingRoutePointDeletion(tripId: String, pointId: String)
}