package com.example.travelplanner.data.repository

import android.content.Context
import com.example.travelplanner.data.local.UserDao
import com.example.travelplanner.data.local.UserEntity
import com.example.travelplanner.data.local.UserTripEntity
import com.example.travelplanner.data.local.RoutePointDao
import com.example.travelplanner.data.local.TripDao
import com.example.travelplanner.data.local.PendingRoutePointDeletionEntity
import com.example.travelplanner.data.local.PendingTripDeletionEntity
import com.example.travelplanner.data.local.toDomain
import com.example.travelplanner.data.local.toEntity
import com.example.travelplanner.data.remote.TripApiService
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.repository.TripRepository
import com.example.travelplanner.domain.result.SyncWriteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import retrofit2.HttpException

class OfflineFirstTripRepository(
    context: Context,
    private val tripDao: TripDao,
    private val routePointDao: RoutePointDao,
    private val userDao: UserDao,
    private val api: TripApiService
) : TripRepository {
    private val syncMutex = Mutex()

    private val prefs = context.applicationContext
        .getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

    override fun observeTrips(): Flow<List<Trip>> =
        getCurrentUserId()?.let { userId ->
            userDao.getTripsForUser(userId).map { entities -> entities.map { it.toDomain() } }
        } ?: flowOf(emptyList())

    override fun observeRoutePoints(tripId: String): Flow<List<RoutePoint>> =
        routePointDao.getPointsForTrip(tripId).map { entities -> entities.map { it.toDomain() } }

    override fun observeTripById(id: String): Flow<Trip?> =
        tripDao.observeTripById(id).map { entity -> entity?.toDomain() }

    override suspend fun saveTrip(trip: Trip): SyncWriteResult {
        return syncMutex.withLock {
            val userId = getCurrentUserId()
                ?: return@withLock SyncWriteResult.Queued("Потрібно увійти в профіль перед створенням поїздки")
            ensureUserExists(userId)
            
            val tripWithOwner = if (trip.ownerId == "unknown") trip.copy(ownerId = userId) else trip

            tripDao.upsertTrip(tripWithOwner.toEntity(syncStatus = "pending"))
            userDao.upsertUserTrip(UserTripEntity(userId = userId, tripId = tripWithOwner.id))
            runCatching { api.createTrip(tripWithOwner) }
                .fold(
                    onSuccess = {
                        tripDao.upsertTrip(tripWithOwner.toEntity(syncStatus = "synced"))
                        SyncWriteResult.Synced
                    },
                    onFailure = {
                        tripDao.upsertTrip(tripWithOwner.toEntity(syncStatus = "error"))
                        SyncWriteResult.Queued(it.message)
                    }
                )
        }
    }

    override suspend fun updateTrip(trip: Trip): SyncWriteResult {
        tripDao.upsertTrip(trip.toEntity(syncStatus = "pending"))
        return runCatching { api.updateTrip(trip) }
            .fold(
                onSuccess = {
                    tripDao.upsertTrip(trip.toEntity(syncStatus = "synced"))
                    SyncWriteResult.Synced
                },
                onFailure = {
                    tripDao.upsertTrip(trip.toEntity(syncStatus = "error"))
                    SyncWriteResult.Queued(it.message)
                }
            )
    }

    override suspend fun deleteTrip(trip: Trip) {
        val currentUser = getCurrentUserId()
        if (currentUser != null) {
            userDao.removeUserTrip(currentUser, trip.id)
        }
        val linksLeft = userDao.countUsersForTrip(trip.id)
        if (linksLeft == 0) {
            tripDao.upsertPendingTripDeletion(PendingTripDeletionEntity(tripId = trip.id))
            tripDao.deleteTripById(trip.id)
            runCatching { api.deleteTrip(trip.id) }
                .onSuccess { tripDao.removePendingTripDeletion(trip.id) }
        }
    }

    override suspend fun saveRoutePoint(point: RoutePoint): SyncWriteResult {
        routePointDao.upsertPoint(point.toEntity(syncStatus = "pending"))
        return runCatching { api.createRoutePoint(point) }
            .fold(
                onSuccess = {
                    routePointDao.upsertPoint(point.toEntity(syncStatus = "synced"))
                    SyncWriteResult.Synced
                },
                onFailure = {
                    routePointDao.upsertPoint(point.toEntity(syncStatus = "error"))
                    SyncWriteResult.Queued(it.message)
                }
            )
    }

    override suspend fun updateRoutePoint(point: RoutePoint): SyncWriteResult {
        routePointDao.upsertPoint(point.toEntity(syncStatus = "pending"))
        return runCatching { api.updateRoutePoint(point) }
            .fold(
                onSuccess = {
                    routePointDao.upsertPoint(point.toEntity(syncStatus = "synced"))
                    SyncWriteResult.Synced
                },
                onFailure = {
                    routePointDao.upsertPoint(point.toEntity(syncStatus = "error"))
                    SyncWriteResult.Queued(it.message)
                }
            )
    }

    override suspend fun getRoutePointById(id: String): RoutePoint? =
        routePointDao.getPointById(id)?.toDomain()

    override suspend fun deleteRoutePoint(point: RoutePoint) {
        routePointDao.upsertPendingRoutePointDeletion(
            PendingRoutePointDeletionEntity(tripId = point.tripId, pointId = point.id)
        )
        routePointDao.deletePoint(point.toEntity())
        runCatching { api.deleteRoutePoint(point.tripId, point.id) }
            .onSuccess { routePointDao.removePendingRoutePointDeletion(point.tripId, point.id) }
    }

    override suspend fun syncPendingTrips() {
        syncMutex.withLock {
            val pendingTripDeletes = tripDao.getPendingTripDeletions()
            pendingTripDeletes.forEach { deletion ->
                runCatching { api.deleteTrip(deletion.tripId) }
                    .onSuccess { tripDao.removePendingTripDeletion(deletion.tripId) }
            }

            val pendingPointDeletes = routePointDao.getPendingRoutePointDeletions()
            pendingPointDeletes.forEach { deletion ->
                runCatching { api.deleteRoutePoint(deletion.tripId, deletion.pointId) }
                    .onSuccess {
                        routePointDao.removePendingRoutePointDeletion(
                            deletion.tripId,
                            deletion.pointId
                        )
                    }
            }

            val unsynced = tripDao.getUnsyncedTrips()
            unsynced.forEach { entity ->
                val trip = entity.toDomain()
                runCatching { api.updateTrip(trip) }
                    .recoverCatching { error ->
                        val isNotFound = (error as? HttpException)?.code() == 404
                        if (isNotFound) {
                            api.createTrip(trip)
                        } else {
                            throw error
                        }
                    }
                    .onSuccess { tripDao.upsertTrip(entity.copy(syncStatus = "synced")) }
                    .onFailure { tripDao.upsertTrip(entity.copy(syncStatus = "error")) }
            }

            val unsyncedPoints = routePointDao.getUnsyncedPoints()
            unsyncedPoints.forEach { entity ->
                val point = entity.toDomain()
                runCatching { api.updateRoutePoint(point) }
                    .recoverCatching { error ->
                        val isNotFound = (error as? HttpException)?.code() == 404
                        if (isNotFound) {
                            api.createRoutePoint(point)
                        } else {
                            throw error
                        }
                    }
                    .onSuccess { routePointDao.upsertPoint(entity.copy(syncStatus = "synced")) }
                    .onFailure { routePointDao.upsertPoint(entity.copy(syncStatus = "error")) }
            }
        }
    }

    override suspend fun refreshTripsFromApi() {
        val userId = getCurrentUserId() ?: return
        runCatching { api.getUserTrips(userId) }
            .onSuccess { remoteTrips ->
                remoteTrips.forEach { remoteTrip ->
                    tripDao.upsertTrip(remoteTrip.toEntity(syncStatus = "synced"))
                    userDao.upsertUserTrip(UserTripEntity(userId = userId, tripId = remoteTrip.id))
                }
            }
    }

    override suspend fun createInviteToken(tripId: String): String = api.createInviteToken(tripId)

    override suspend fun getInvitePreview(token: String): com.example.travelplanner.domain.model.InviteTripPreview {
        return try {
            api.getInvitePreview(token)
        } catch (e: Exception) {
            com.example.travelplanner.domain.model.InviteTripPreview(
                token = token,
                title = "Офлайн Поїздка",
                destination = "Невідомо",
                description = "Прев'ю недоступне, бо немає зв'язку із сервером. Але ви можете додати цю поїздку локально.",
                invitedBy = "Організатор"
            )
        }
    }

    override suspend fun acceptInvite(token: String): Trip {
        val userId = getCurrentUserId() ?: throw IllegalStateException("User not logged in")
        val trip = try {
            api.acceptInvite(token, userId)
        } catch (e: Exception) {
            val start = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 14) }
            val end = (start.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, 5) }
            Trip(
                id = java.util.UUID.randomUUID().toString(),
                ownerId = "unknown",
                title = "Офлайн поїздка",
                destination = "Невідомо",
                startDate = start.time,
                endDate = end.time,
                totalBudget = 0.0,
                currencyCode = "USD"
            )
        }
        syncMutex.withLock {
            ensureUserExists(userId)
            tripDao.upsertTrip(trip.toEntity(syncStatus = "synced"))
            userDao.upsertUserTrip(UserTripEntity(userId = userId, tripId = trip.id))
        }
        return trip
    }

    private fun getCurrentUserId(): String? {
        val id = prefs.getString("current_user_id", null)?.takeIf { it.isNotBlank() }
        if (id != null) return id
        return prefs.getString("current_user_email", null)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    }

    private suspend fun ensureUserExists(userId: String) {
        val normalizedId = userId.trim().lowercase()
        val name = prefs.getString("${sanitizeKey(normalizedId)}_display_name", normalizedId.substringBefore("@"))
            ?.takeIf { it.isNotBlank() } ?: normalizedId.substringBefore("@")
        val homeCity = prefs.getString("${sanitizeKey(normalizedId)}_home_city", "Київ")
            ?.takeIf { it.isNotBlank() } ?: "Київ"
        val currency = prefs.getString("${sanitizeKey(normalizedId)}_preferred_currency", "USD")
            ?.takeIf { it.isNotBlank() } ?: "USD"

        userDao.upsertUser(
            UserEntity(
                id = normalizedId,
                displayName = name,
                email = normalizedId,
                homeCity = homeCity,
                preferredCurrency = currency,
                registeredAt = Date().time
            )
        )
    }

    private fun sanitizeKey(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9_]"), "_")
}
