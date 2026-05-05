package com.example.travelplanner.domain.model

import java.util.Date
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RoutePoint(
    val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val arrivalDate: Date,
    val durationDays: Int,
    val estimatedCost: Double,
    val currencyCode: String = "USD",
    val isVisited: Boolean = false,
    val category: PointCategory = PointCategory.OTHER,
    val notes: String = "",
    val syncStatus: SyncStatus? = SyncStatus.PENDING
) {
    val isSynced: Boolean
        get() = syncStatus == SyncStatus.SYNCED

    fun distanceTo(other: RoutePoint): Double = distanceTo(other.latitude, other.longitude)

    fun distanceTo(lat: Double, lon: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat - latitude)
        val dLon = Math.toRadians(lon - longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(lat)) *
            sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}

enum class PointCategory {
    HOTEL, ATTRACTION, RESTAURANT, TRANSPORT, OTHER
}
