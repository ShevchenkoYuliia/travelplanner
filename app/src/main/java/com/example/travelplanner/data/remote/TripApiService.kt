package com.example.travelplanner.data.remote

import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.model.RoutePoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * REST contract (mocked implementation for now).
 *
 * 1) GET /trips
 *    - Params: none
 *    - Returns: List<Trip>
 *      fields: id, title, destination, startDate, endDate, totalBudget, currencyCode, coverImageUrl, notes
 *
 * 2) GET /trips/{id}
 *    - Params: path id: String
 *    - Returns: Trip? with the same fields as above
 *
 * 3) POST /trips
 *    - Body: Trip
 *      fields: title, destination, startDate, endDate, totalBudget, currencyCode, coverImageUrl, notes
 *    - Returns: created Trip (with id)
 *
 * 4) PUT /trips/{id}
 *    - Params: path id: String
 *    - Body: Trip
 *    - Returns: updated Trip
 *
 * 5) DELETE /trips/{id}
 *    - Params: path id: String
 *    - Returns: 204 No Content
 *
 * 6) GET /trips/{tripId}/route-points
 *    - Params: path tripId: String
 *    - Returns: List<RoutePoint>
 *
 * 7) POST /trips/{tripId}/route-points
 *    - Params: path tripId: String
 *    - Body: RoutePoint
 *      fields: id, tripId, name, address, latitude, longitude, arrivalDate, durationDays, estimatedCost, currencyCode, isVisited, category, notes
 *    - Returns: created RoutePoint
 *
 * Communication strategy: offline-first. The app writes to local Room first and
 * then syncs with API. Sync state is tracked in local entities with syncStatus.
 */
interface TripApiService {
    suspend fun getUserTrips(userId: String): List<Trip>
    suspend fun getTripById(id: String): Trip?
    suspend fun createTrip(trip: Trip): Trip
    suspend fun updateTrip(trip: Trip): Trip
    suspend fun deleteTrip(id: String)
    suspend fun getRoutePoints(tripId: String): List<RoutePoint>
    suspend fun getRoutePointById(tripId: String, pointId: String): RoutePoint?
    suspend fun createRoutePoint(point: RoutePoint): RoutePoint
    suspend fun updateRoutePoint(point: RoutePoint): RoutePoint
    suspend fun deleteRoutePoint(tripId: String, pointId: String)
    
    suspend fun createInviteToken(tripId: String): String
    suspend fun getInvitePreview(token: String): com.example.travelplanner.domain.model.InviteTripPreview
    suspend fun acceptInvite(token: String, userId: String): Trip
}

private interface RetrofitTripApi {
    @GET("users/{userId}/trips")
    suspend fun getUserTrips(@Path("userId") userId: String): List<Trip>

    @GET("trips/{id}")
    suspend fun getTripById(@Path("id") id: String): Trip?

    @POST("trips")
    suspend fun createTrip(@Body trip: Trip): Trip

    @PUT("trips/{id}")
    suspend fun updateTrip(@Path("id") id: String, @Body trip: Trip): Trip

    @DELETE("trips/{id}")
    suspend fun deleteTrip(@Path("id") id: String)

    @GET("trips/{tripId}/route-points")
    suspend fun getRoutePoints(@Path("tripId") tripId: String): List<RoutePoint>

    @GET("trips/{tripId}/route-points/{pointId}")
    suspend fun getRoutePointById(
        @Path("tripId") tripId: String,
        @Path("pointId") pointId: String
    ): RoutePoint?

    @POST("trips/{tripId}/route-points")
    suspend fun createRoutePoint(
        @Path("tripId") tripId: String,
        @Body point: RoutePoint
    ): RoutePoint

    @PUT("trips/{tripId}/route-points/{pointId}")
    suspend fun updateRoutePoint(
        @Path("tripId") tripId: String,
        @Path("pointId") pointId: String,
        @Body point: RoutePoint
    ): RoutePoint

    @DELETE("trips/{tripId}/route-points/{pointId}")
    suspend fun deleteRoutePoint(
        @Path("tripId") tripId: String,
        @Path("pointId") pointId: String
    )

    @POST("trips/{tripId}/invite")
    suspend fun createInviteToken(@Path("tripId") tripId: String): String

    @GET("invites/{token}/preview")
    suspend fun getInvitePreview(@Path("token") token: String): com.example.travelplanner.domain.model.InviteTripPreview

    @POST("invites/{token}/accept")
    suspend fun acceptInvite(@Path("token") token: String, @Body body: Map<String, String>): Trip
}

class HttpTripApiService(
    baseUrl: String
) : TripApiService {
    private val api: RetrofitTripApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RetrofitTripApi::class.java)

    override suspend fun getUserTrips(userId: String): List<Trip> = api.getUserTrips(userId)
    override suspend fun getTripById(id: String): Trip? = api.getTripById(id)
    override suspend fun createTrip(trip: Trip): Trip = api.createTrip(trip)
    override suspend fun updateTrip(trip: Trip): Trip = api.updateTrip(trip.id, trip)
    override suspend fun deleteTrip(id: String) = api.deleteTrip(id)
    override suspend fun getRoutePoints(tripId: String): List<RoutePoint> = api.getRoutePoints(tripId)
    override suspend fun getRoutePointById(tripId: String, pointId: String): RoutePoint? =
        api.getRoutePointById(tripId, pointId)
    override suspend fun createRoutePoint(point: RoutePoint): RoutePoint =
        api.createRoutePoint(point.tripId, point)
    override suspend fun updateRoutePoint(point: RoutePoint): RoutePoint =
        api.updateRoutePoint(point.tripId, point.id, point)
    override suspend fun deleteRoutePoint(tripId: String, pointId: String) =
        api.deleteRoutePoint(tripId, pointId)

    override suspend fun createInviteToken(tripId: String): String = api.createInviteToken(tripId)
    override suspend fun getInvitePreview(token: String) = api.getInvitePreview(token)
    override suspend fun acceptInvite(token: String, userId: String): Trip = api.acceptInvite(token, mapOf("userId" to userId))
}

object ApiConfig {

    const val BASE_URL: String = "http://10.0.2.2:8080/"
    const val WS_URL: String = "ws://10.0.2.2:8080/ws"
    const val USE_FAKE_WS: Boolean = false
}

class MockTripApiService : TripApiService {
    override suspend fun getUserTrips(userId: String): List<Trip> = emptyList()
    override suspend fun getTripById(id: String): Trip? = null
    override suspend fun createTrip(trip: Trip): Trip = trip
    override suspend fun updateTrip(trip: Trip): Trip = trip
    override suspend fun deleteTrip(id: String) {}
    override suspend fun getRoutePoints(tripId: String): List<RoutePoint> = emptyList()
    override suspend fun getRoutePointById(tripId: String, pointId: String): RoutePoint? = null
    override suspend fun createRoutePoint(point: RoutePoint): RoutePoint = point
    override suspend fun updateRoutePoint(point: RoutePoint): RoutePoint = point
    override suspend fun deleteRoutePoint(tripId: String, pointId: String) {}

    override suspend fun createInviteToken(tripId: String): String {
        return java.util.UUID.randomUUID().toString()
    }

    override suspend fun getInvitePreview(token: String): com.example.travelplanner.domain.model.InviteTripPreview {
        return com.example.travelplanner.domain.model.InviteTripPreview(
            token = token,
            title = "Спільна поїздка",
            destination = "Невідомо",
            description = "Вас запросили приєднатися до маршруту, бюджету та нотаток цієї поїздки.",
            invitedBy = "Організатор"
        )
    }

    override suspend fun acceptInvite(token: String, userId: String): Trip {
        val start = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 14) }
        val end = (start.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, 5) }
        return Trip(
            id = java.util.UUID.randomUUID().toString(),
            title = "Спільна поїздка",
            destination = "Невідомо",
            startDate = start.time,
            endDate = end.time,
            totalBudget = 0.0,
            currencyCode = "USD"
        )
    }
}

