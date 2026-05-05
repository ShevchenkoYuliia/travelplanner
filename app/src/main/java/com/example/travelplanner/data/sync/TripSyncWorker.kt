package com.example.travelplanner.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.example.travelplanner.data.local.TravelDatabase
import com.example.travelplanner.data.remote.ApiConfig
import com.example.travelplanner.data.remote.HttpTripApiService
import com.example.travelplanner.data.remote.HttpUserApiService
import com.example.travelplanner.data.remote.UserPayload
import com.example.travelplanner.data.repository.OfflineFirstTripRepository
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class TripSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = TravelDatabase.getInstance(applicationContext)
        val repository = OfflineFirstTripRepository(
            context = applicationContext,
            tripDao = db.tripDao(),
            routePointDao = db.routePointDao(),
            userDao = db.userDao(),
            api = HttpTripApiService(ApiConfig.BASE_URL)
        )
        val userApi = HttpUserApiService(ApiConfig.BASE_URL)

        return runCatching {
            repository.syncPendingTrips()
            repository.refreshTripsFromApi()
            syncAllUsersWithServer(userApi, db.userDao())
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    private suspend fun syncAllUsersWithServer(
        userApi: HttpUserApiService,
        userDao: com.example.travelplanner.data.local.UserDao
    ) {
        // Получаем всех юзеров с сервера
        val serverUsers = runCatching { userApi.getUsers() }.getOrDefault(emptyList())
        
        // Сохраняем их в локальную базу
        serverUsers.forEach { payload ->
            userDao.upsertUser(
                com.example.travelplanner.data.local.UserEntity(
                    id = payload.id,
                    displayName = payload.displayName,
                    email = payload.email,
                    homeCity = payload.homeCity,
                    preferredCurrency = payload.preferredCurrency,
                    registeredAt = payload.registeredAt
                )
            )
        }
        
        // Синхронизируем текущего юзера на сервер (если он есть)
        val prefs = applicationContext.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
        val currentEmail = prefs.getString("current_user_email", null)?.trim()?.lowercase()
            ?.takeIf { it.isNotBlank() } ?: return
        val currentUserId = prefs.getString("current_user_id", null)
            ?.takeIf { it.isNotBlank() } ?: return
        
        val safe = sanitizeKey(currentEmail)
        val pendingProfileKey = "${safe}_profile_pending_sync"
        if (!prefs.getBoolean(pendingProfileKey, false)) {
            return
        }

        val payload = UserPayload(
            id = currentUserId,
            displayName = prefs.getString("${safe}_display_name", currentEmail.substringBefore("@"))
                ?.takeIf { it.isNotBlank() } ?: currentEmail.substringBefore("@"),
            email = prefs.getString("${safe}_email", currentEmail)
                ?.takeIf { it.isNotBlank() } ?: currentEmail,
            password = null,
            homeCity = prefs.getString("${safe}_home_city", "Київ")
                ?.takeIf { it.isNotBlank() } ?: "Київ",
            preferredCurrency = prefs.getString("${safe}_preferred_currency", "USD")
                ?.takeIf { it.isNotBlank() } ?: "USD",
            registeredAt = prefs.getLong("${safe}_registered_at", System.currentTimeMillis())
        )
        runCatching { userApi.updateUser(payload) }
            .recoverCatching { error ->
                if (error is HttpException && error.code() == 404) {
                    userApi.createUser(payload)
                } else {
                    throw error
                }
            }
            .getOrThrow()

        prefs.edit()
            .putBoolean(pendingProfileKey, false)
            .commit()
    }

    private fun sanitizeKey(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9_]"), "_")

    companion object {
        private const val PERIODIC_WORK_NAME = "trip_sync_periodic"
        private const val ONE_TIME_WORK_NAME = "trip_sync_one_time"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<TripSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<TripSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
