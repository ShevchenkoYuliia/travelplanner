package com.example.travelplanner.data

import com.example.travelplanner.data.local.UserDao
import com.example.travelplanner.data.local.toDomain
import com.example.travelplanner.data.local.toEntity
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.models.User
import com.example.travelplanner.models.UserTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserTripRepository(
    private val userDao: UserDao
) {
    fun getUsers(): Flow<List<User>> =
        userDao.getAllUsers().map { users -> users.map { it.toDomain() } }

    fun getTripsForUser(userId: String): Flow<List<Trip>> =
        userDao.getTripsForUser(userId).map { trips -> trips.map { it.toDomain() } }

    suspend fun saveUser(user: User) {
        userDao.upsertUser(user.toEntity())
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user.toEntity())
    }

    suspend fun linkUserTrip(userTrip: UserTrip) {
        userDao.upsertUserTrip(userTrip.toEntity())
    }

    suspend fun unlinkUserTrip(userId: String, tripId: String) {
        userDao.removeUserTrip(userId, tripId)
    }
}
