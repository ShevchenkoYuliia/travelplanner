package com.example.travelplanner.data

import com.example.travelplanner.domain.model.PointCategory
import com.example.travelplanner.domain.model.RoutePoint
import com.example.travelplanner.domain.model.SyncStatus
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.models.UserProfile
import java.util.Calendar
import java.util.Date

object MockRepository {

    private fun date(year: Int, month: Int, day: Int): Date {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }


    val trips = mutableListOf<Trip>(
        Trip (
            id = "trip_1",
            title = "Японський місяць",
            destination = "Токіо, Японія",
            startDate = date(2025, 4, 10),
            endDate = date(2025, 5, 10),
            totalBudget = 3500.0,
            currencyCode = "USD",
            syncStatus = SyncStatus.SYNCED,
            notes = "Хочу потрапити на сезон цвітіння сакури"
        ),
        Trip(
            id = "trip_2",
            title = "Балканське кільце",
            destination = "Дубровник, Хорватія",
            startDate = date(2025, 7, 1),
            endDate = date(2025, 7, 21),
            totalBudget = 2200.0,
            currencyCode = "EUR",
            syncStatus = SyncStatus.PENDING,
            notes = "Хорватія → Чорногорія → Боснія"
        ),
        Trip(
            id = "trip_3",
            title = "Ісландія взимку",
            destination = "Рейк'явік, Ісландія",
            startDate = date(2026, 1, 15),
            endDate = date(2026, 1, 25),
            totalBudget = 2800.0,
            currencyCode = "USD",
            syncStatus = SyncStatus.SYNCED,
            notes = "Північне сяйво та льодовики"
        )
    )

    val routePoints: List<RoutePoint> = listOf(
        RoutePoint(
            id = "rp_1",
            tripId = "trip_1",
            name = "Токіо — готель Shinjuku",
            address = "1-2-3 Kabukicho, Shinjuku, Tokyo",
            latitude = 35.6938,
            longitude = 139.7034,
            arrivalDate = date(2025, 4, 10),
            durationDays = 7,
            estimatedCost = 700.0,
            currencyCode = "USD",
            isVisited = false,
            category = PointCategory.HOTEL,
            notes = "Забронювати заздалегідь — сезон пік"
        ),
        RoutePoint(
            id = "rp_2",
            tripId = "trip_1",
            name = "Храм Сенсо-дзі",
            address = "2-3-1 Asakusa, Taito City, Tokyo",
            latitude = 35.7147,
            longitude = 139.7966,
            arrivalDate = date(2025, 4, 12),
            durationDays = 1,
            estimatedCost = 0.0,
            currencyCode = "USD",
            isVisited = false,
            category = PointCategory.ATTRACTION,
            notes = "Найстаріший буддистський храм Токіо, вхід безкоштовний"
        ),
        RoutePoint(
            id = "rp_3",
            tripId = "trip_2",
            name = "Старе місто Дубровник",
            address = "Stari grad, Dubrovnik, Croatia",
            latitude = 42.6507,
            longitude = 18.0944,
            arrivalDate = date(2025, 7, 1),
            durationDays = 5,
            estimatedCost = 400.0,
            currencyCode = "EUR",
            isVisited = false,
            category = PointCategory.HOTEL
        )
    )

    val currentUser = UserProfile(
        id = "user_1",
        displayName = "Андрій Мандрівник",
        email = "andriy@example.com",
        homeCity = "Київ",
        totalTrips = 3,
        totalKmTraveled = 14250.0,
        isPremium = false,
        registeredAt = date(2024, 1, 20)
    )


    fun getTripById(id: String): Trip? = trips.find { it.id == id }

    fun getPointsForTrip(tripId: String): List<RoutePoint> =
        routePoints.filter { it.tripId == tripId }
    fun addTrip(trip: Trip) {
        trips.add(trip)
    }
}
