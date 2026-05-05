package com.example.travelplanner.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class UserPayload(
    val id: String,
    val displayName: String,
    val email: String,
    val password: String? = null,
    val homeCity: String,
    val preferredCurrency: String,
    val registeredAt: Long
)

interface UserApiService {
    suspend fun getUsers(): List<UserPayload>
    suspend fun createUser(user: UserPayload): UserPayload
    suspend fun updateUser(user: UserPayload): UserPayload
    suspend fun deleteUser(id: String)
}

private interface RetrofitUserApi {
    @GET("users")
    suspend fun getUsers(): List<UserPayload>

    @POST("users")
    suspend fun createUser(@Body user: UserPayload): UserPayload

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: UserPayload): UserPayload

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String)

}

class HttpUserApiService(
    baseUrl: String
) : UserApiService {
    private val api: RetrofitUserApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RetrofitUserApi::class.java)

    override suspend fun getUsers(): List<UserPayload> = api.getUsers()
    override suspend fun createUser(user: UserPayload): UserPayload = api.createUser(user)
    override suspend fun updateUser(user: UserPayload): UserPayload = api.updateUser(user.id, user)
    override suspend fun deleteUser(id: String) = api.deleteUser(id)
}

class MockUserApiService : UserApiService {
    override suspend fun getUsers(): List<UserPayload> = emptyList()
    override suspend fun createUser(user: UserPayload): UserPayload = user
    override suspend fun updateUser(user: UserPayload): UserPayload = user
    override suspend fun deleteUser(id: String) {}
}
