package com.example.travelplanner.data

import android.content.Context
import com.example.travelplanner.data.local.UserDao
import com.example.travelplanner.data.local.UserEntity
import com.example.travelplanner.data.remote.UserApiService
import com.example.travelplanner.data.remote.UserPayload
import com.example.travelplanner.domain.model.Trip
import com.example.travelplanner.domain.repository.TripRepository
import com.example.travelplanner.domain.result.ProfileSaveResult
import com.example.travelplanner.models.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

class UserRepository(
    context: Context,
    private val tripRepository: TripRepository,
    private val userDao: UserDao,
    private val userApiService: UserApiService
) {

    private data class ProfileData(
        val displayName: String = "",
        val email: String = "",
        val homeCity: String = "Київ",
        val preferredCurrency: String = "USD"
    )

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val profileData = MutableStateFlow(loadProfileDataForCurrentUser())
    private val isLoggedInState = MutableStateFlow(
        prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    )
    private val currentUserEmailState = MutableStateFlow(getCurrentUserEmail())
    private val registeredAt = Calendar.getInstance().apply { set(2024, 0, 20) }.time
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val adminEmails: List<String> by lazy {
        loadAdminEmails(context)
    }

    suspend fun updateProfile(
        displayName: String,
        email: String,
        homeCity: String,
        preferredCurrency: String
    ): ProfileSaveResult {
        val currentEmail = getCurrentUserEmail()
            ?: return ProfileSaveResult.SavedLocallyOnly("Увійдіть у профіль")
        val updated = ProfileData(
            displayName = displayName,
            email = currentEmail,
            homeCity = homeCity,
            preferredCurrency = preferredCurrency
        )
        saveProfileDataForCurrentUser(updated)
        profileData.value = updated

        val normalizedEmail = currentEmail.trim().lowercase()
        markProfilePendingSync(normalizedEmail, true)
        val currentUserId = getCurrentUserId()
            ?: return ProfileSaveResult.SavedLocallyOnly("ID користувача не знайдений")
        
        val payload = UserPayload(
            id = currentUserId,
            displayName = updated.displayName,
            email = updated.email.ifBlank { normalizedEmail },
            password = null,
            homeCity = updated.homeCity,
            preferredCurrency = updated.preferredCurrency,
            registeredAt = getRegisteredAtMillis(normalizedEmail)
        )
        userDao.upsertUser(
            UserEntity(
                id = payload.id,
                displayName = payload.displayName,
                email = payload.email,
                homeCity = payload.homeCity,
                preferredCurrency = payload.preferredCurrency,
                registeredAt = payload.registeredAt
            )
        )
        val remoteOk = runCatching { syncUserProfileToServer(payload) }.isSuccess
        if (remoteOk) {
            markProfilePendingSync(normalizedEmail, false)
        }
        return if (remoteOk) ProfileSaveResult.SyncedToServer
        else ProfileSaveResult.SavedLocallyOnly("Немає зв'язку з сервером")
    }

    fun getUserProfile(): Flow<UserProfile> {
        return combine(tripRepository.observeTrips(), profileData) { trips, profile ->
            val totalTrips = trips.size
            val totalKm = calculateTotalKm(trips)

            profile.toUserProfile(
                totalTrips = totalTrips,
                totalKmTraveled = totalKm
            )
        }
    }

    fun getCachedUserProfile(): UserProfile =
        profileData.value.toUserProfile(totalTrips = 0, totalKmTraveled = 0.0)

    fun isLoggedIn(): StateFlow<Boolean> = isLoggedInState
    fun currentUserEmail(): StateFlow<String?> = currentUserEmailState
    fun hasRegisteredAccounts(): Boolean =
        prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty().isNotEmpty()

    fun register(email: String, password: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.length < 6) {
            return Result.failure(IllegalArgumentException("Некоректні дані реєстрації"))
        }
        val updatedEmails = prefs.getStringSet(KEY_REGISTERED_EMAILS, emptySet()).orEmpty().toMutableSet()
        if (updatedEmails.contains(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Цей email вже зареєстрований"))
        }
        updatedEmails += normalizedEmail
        val defaultProfile = defaultProfileData(normalizedEmail)
        
        var userId: String? = null
        val syncResult = runCatching {
            runBlocking {
                val payload = registerUserOnServer(
                    email = normalizedEmail,
                    data = defaultProfile,
                    password = password
                )
                userId = payload.id
            }
        }
        if (syncResult.isFailure) {
            return Result.failure(IllegalStateException("Не вдалося зареєструвати користувача на сервері"))
        }
        prefs.edit()
            .putStringSet(KEY_REGISTERED_EMAILS, updatedEmails)
            .putString(accountPasswordKey(normalizedEmail), password)
            .putString(KEY_CURRENT_USER_EMAIL, normalizedEmail)
            .putString(KEY_LAST_AUTHENTICATED_EMAIL, normalizedEmail)
            .putString("last_authenticated_user_id", userId ?: normalizedEmail)
            .putString(KEY_CURRENT_USER_ID, userId ?: normalizedEmail)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .commit()
        saveProfileDataForEmail(normalizedEmail, defaultProfile)
        profileData.value = defaultProfile
        isLoggedInState.value = true
        currentUserEmailState.value = normalizedEmail
        return Result.success(Unit)
    }

    fun login(email: String, password: String): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        var userId: String? = null
        val isServerAuthValid = runCatching {
            runBlocking {
                val users = userApiService.getUsers()
                val user = users.find { user ->
                    user.email.trim().lowercase() == normalizedEmail &&
                        !user.password.isNullOrBlank() &&
                        user.password == password
                }
                userId = user?.id
                user != null
            }
        }.getOrDefault(false)
        return if (isServerAuthValid) {
            prefs.edit()
                .putString(KEY_CURRENT_USER_EMAIL, normalizedEmail)
                .putString(KEY_LAST_AUTHENTICATED_EMAIL, normalizedEmail)
                .putString("last_authenticated_user_id", userId ?: normalizedEmail)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(accountPasswordKey(normalizedEmail), password)
                .putString(KEY_CURRENT_USER_ID, userId ?: normalizedEmail)
                .commit()
            profileData.value = loadProfileDataForEmail(normalizedEmail)
            isLoggedInState.value = true
            currentUserEmailState.value = normalizedEmail
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Невірний email або пароль"))
        }
    }

    fun unlockWithBiometrics(): Result<Unit> {
        val lastEmail = prefs.getString(KEY_LAST_AUTHENTICATED_EMAIL, null)
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Немає збереженого профілю для біометричного входу"))

        val lastUserId = prefs.getString("last_authenticated_user_id", null)

        prefs.edit()
            .putString(KEY_CURRENT_USER_EMAIL, lastEmail)
            .putString(KEY_CURRENT_USER_ID, lastUserId ?: lastEmail)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .commit()
        profileData.value = loadProfileDataForEmail(lastEmail)
        isLoggedInState.value = true
        currentUserEmailState.value = lastEmail
        return Result.success(Unit)
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_USER_EMAIL)
            .remove(KEY_CURRENT_USER_ID)
            .commit()
        profileData.value = defaultProfileData("")
        isLoggedInState.value = false
        currentUserEmailState.value = null
    }
    private suspend fun calculateTotalKm(
        trips: List<Trip>
    ): Double {
        var totalKm = 0.0

        for (trip in trips) {
            val points = tripRepository.observeRoutePoints(trip.id).first()
            points.zipWithNext().forEach { (current, next) ->
                totalKm += current.distanceTo(next)
            }
        }

        return (totalKm * 10).roundToInt() / 10.0
    }

    private fun loadProfileDataForCurrentUser(): ProfileData {
        val currentEmail = getCurrentUserEmail()
        return if (currentEmail.isNullOrBlank()) defaultProfileData("") else loadProfileDataForEmail(currentEmail)
    }

    private fun loadProfileDataForEmail(email: String): ProfileData {
        val default = defaultProfileData(email)
        return ProfileData(
            displayName = prefs.getString(profileKey(email, KEY_DISPLAY_NAME), default.displayName)
                ?.takeIf { it.isNotBlank() } ?: default.displayName,
            email = prefs.getString(profileKey(email, KEY_EMAIL), default.email)
                ?.takeIf { it.isNotBlank() } ?: default.email,
            homeCity = prefs.getString(profileKey(email, KEY_HOME_CITY), default.homeCity)
                ?.takeIf { it.isNotBlank() } ?: default.homeCity,
            preferredCurrency = prefs.getString(profileKey(email, KEY_CURRENCY), default.preferredCurrency)
                ?.takeIf { it.isNotBlank() } ?: default.preferredCurrency
        )
    }

    private fun defaultProfileData(email: String): ProfileData {
        val normalizedEmail = email.trim().lowercase()
        val safeName = normalizedEmail.substringBefore("@").ifBlank { "Новий мандрівник" }
        return ProfileData(
            displayName = safeName,
            email = normalizedEmail,
            homeCity = "Київ",
            preferredCurrency = "USD"
        )
    }

    private fun saveProfileDataForCurrentUser(data: ProfileData) {
        getCurrentUserEmail()?.let { saveProfileDataForEmail(it, data) }
    }

    private fun saveProfileDataForEmail(email: String, data: ProfileData) {
        prefs.edit()
            .putString(profileKey(email, KEY_DISPLAY_NAME), data.displayName)
            .putString(profileKey(email, KEY_EMAIL), data.email)
            .putString(profileKey(email, KEY_HOME_CITY), data.homeCity)
            .putString(profileKey(email, KEY_CURRENCY), data.preferredCurrency)
            .putLong(profileKey(email, KEY_REGISTERED_AT), getRegisteredAtMillis(email))
            .commit()
    }

    private fun markProfilePendingSync(email: String, pending: Boolean) {
        prefs.edit()
            .putBoolean(profileKey(email, KEY_PROFILE_PENDING_SYNC), pending)
            .commit()
    }

    /**
     * Register a new user on the server - only called during registration
     */
    private suspend fun registerUserOnServer(
        email: String,
        data: ProfileData,
        password: String
    ): UserPayload {
        val normalizedEmail = email.trim().lowercase()
        val payload = UserPayload(
            id = generateNumericId(),
            displayName = data.displayName,
            email = data.email.ifBlank { normalizedEmail },
            password = password,
            homeCity = data.homeCity,
            preferredCurrency = data.preferredCurrency,
            registeredAt = getRegisteredAtMillis(normalizedEmail)
        )
        userDao.upsertUser(
            UserEntity(
                id = payload.id,
                displayName = payload.displayName,
                email = payload.email,
                homeCity = payload.homeCity,
                preferredCurrency = payload.preferredCurrency,
                registeredAt = payload.registeredAt
            )
        )
        userApiService.createUser(payload)
        return payload
    }

    /**
     * Update existing user profile on server - does not create new users on login
     */
    private suspend fun syncUserProfileToServer(payload: UserPayload) {
        runCatching { userApiService.updateUser(payload) }
            .getOrThrow()
    }

    private fun generateNumericId(): String {
        return System.currentTimeMillis().toString() + (Math.random() * 100000).toInt()
    }

    private suspend fun validateCredentialsOnServer(email: String, password: String): Boolean {
        val users = userApiService.getUsers()
        return users.any { user ->
            user.email.trim().lowercase() == email &&
                !user.password.isNullOrBlank() &&
                user.password == password
        }
    }

    private fun getRegisteredAtMillis(email: String): Long {
        val key = profileKey(email, KEY_REGISTERED_AT)
        val stored = prefs.getLong(key, -1L)
        if (stored > 0L) return stored
        val now = Date().time
        prefs.edit().putLong(key, now).commit()
        return now
    }

    private fun getCurrentUserEmail(): String? =
        prefs.getString(KEY_CURRENT_USER_EMAIL, null)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

    private fun getCurrentUserId(): String? =
        prefs.getString(KEY_CURRENT_USER_ID, null)?.takeIf { it.isNotBlank() }

    private fun accountPasswordKey(email: String): String = "auth_password_${sanitizeKey(email)}"

    private fun profileKey(email: String, key: String): String = "${sanitizeKey(email)}_$key"

    private fun sanitizeKey(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9_]"), "_")

    fun isAdmin(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        return adminEmails.contains(normalizedEmail)
    }

    private fun loadAdminEmails(context: Context): List<String> {
        return try {
            val inputStream = context.assets.open("data.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, type)
            val adminEmails = data["adminEmails"] as? List<String> ?: emptyList()
            adminEmails.map { it.trim().lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_HOME_CITY = "home_city"
        private const val KEY_CURRENCY = "preferred_currency"
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_LAST_AUTHENTICATED_EMAIL = "last_authenticated_email"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_REGISTERED_EMAILS = "registered_emails"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REGISTERED_AT = "registered_at"
        private const val KEY_PROFILE_PENDING_SYNC = "profile_pending_sync"
    }

    private fun ProfileData.toUserProfile(
        totalTrips: Int,
        totalKmTraveled: Double
    ) = UserProfile(
        id = email.ifBlank { "guest" },
        displayName = displayName,
        email = email,
        homeCity = homeCity,
        totalTrips = totalTrips,
        totalKmTraveled = totalKmTraveled,
        isPremium = false,
        registeredAt = Date(getRegisteredAtMillis(email)),
        preferredCurrency = preferredCurrency
    )
}
