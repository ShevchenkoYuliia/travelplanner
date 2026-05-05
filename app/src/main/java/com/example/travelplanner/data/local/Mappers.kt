package com.example.travelplanner.data.local

import com.example.travelplanner.domain.model.PointCategory
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.SyncStatus
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.models.User
import com.example.travelplanner.models.UserTrip
import java.util.Date

fun Trip.toEntity(syncStatus: String = "pending") = TripEntity(
    id = id,
    ownerId = ownerId ?: "unknown",
    title = title,
    destination = destination,
    startDate = startDate.time,
    endDate = endDate.time,
    totalBudget = totalBudget,
    currencyCode = currencyCode,
    coverImageUrl = coverImageUrl,
    notes = notes,
    syncStatus = syncStatus
)

fun TripEntity.toDomain() = Trip(
    id = id,
    ownerId = ownerId,
    title = title,
    destination = destination,
    startDate = Date(startDate),
    endDate = Date(endDate),
    totalBudget = totalBudget,
    currencyCode = currencyCode,
    syncStatus = syncStatus.toSyncStatus(),
    coverImageUrl = coverImageUrl,
    notes = notes
)


fun RoutePoint.toEntity(syncStatus: String = "pending") = RoutePointEntity(
    id = id,
    tripId = tripId,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    arrivalDate = arrivalDate.time,
    durationDays = durationDays,
    estimatedCost = estimatedCost,
    currencyCode = currencyCode,
    isVisited = isVisited,
    category = category.name,
    notes = notes,
    syncStatus = syncStatus
)

fun RoutePointEntity.toDomain() = RoutePoint(
    id = id,
    tripId = tripId,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    arrivalDate = Date(arrivalDate),
    durationDays = durationDays,
    estimatedCost = estimatedCost,
    currencyCode = currencyCode,
    isVisited = isVisited,
    category = runCatching { PointCategory.valueOf(category) }.getOrDefault(PointCategory.OTHER),
    notes = notes,
    syncStatus = syncStatus.toSyncStatus()
)

fun User.toEntity() = UserEntity(
    id = id,
    displayName = displayName,
    email = email,
    homeCity = homeCity,
    preferredCurrency = preferredCurrency,
    registeredAt = registeredAt.time
)

fun UserEntity.toDomain() = User(
    id = id,
    displayName = displayName,
    email = email,
    homeCity = homeCity,
    preferredCurrency = preferredCurrency,
    registeredAt = Date(registeredAt)
)

fun UserTrip.toEntity() = UserTripEntity(
    userId = userId,
    tripId = tripId
)

fun UserTripEntity.toDomain() = UserTrip(
    userId = userId,
    tripId = tripId
)

private fun String.toSyncStatus(): SyncStatus = when (lowercase()) {
    "synced" -> SyncStatus.SYNCED
    "error" -> SyncStatus.ERROR
    else -> SyncStatus.PENDING
}
