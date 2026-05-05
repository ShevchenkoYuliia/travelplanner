package com.example.travelplanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUserById(id: String): Flow<UserEntity?>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Upsert
    suspend fun upsertUserTrip(link: UserTripEntity)

    @Query("DELETE FROM user_trips WHERE userId = :userId AND tripId = :tripId")
    suspend fun removeUserTrip(userId: String, tripId: String)

    @Query("SELECT COUNT(*) FROM user_trips WHERE tripId = :tripId")
    suspend fun countUsersForTrip(tripId: String): Int

    @Query(
        """
        SELECT t.* FROM trips t
        INNER JOIN user_trips ut ON ut.tripId = t.id
        WHERE ut.userId = :userId
        ORDER BY t.startDate DESC
        """
    )
    fun getTripsForUser(userId: String): Flow<List<TripEntity>>
}
