package com.example.travelplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TripEntity::class,
        RoutePointEntity::class,
        UserEntity::class,
        UserTripEntity::class,
        PendingTripDeletionEntity::class,
        PendingRoutePointDeletionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun routePointDao(): RoutePointDao
    abstract fun userDao(): UserDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'USD'"
                )
                database.execSQL(
                    "ALTER TABLE route_points ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'USD'"
                )
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        email TEXT NOT NULL,
                        homeCity TEXT NOT NULL,
                        preferredCurrency TEXT NOT NULL DEFAULT 'USD',
                        registeredAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_trips (
                        userId TEXT NOT NULL,
                        tripId TEXT NOT NULL,
                        PRIMARY KEY(userId, tripId),
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_trips_userId ON user_trips(userId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_trips_tripId ON user_trips(tripId)"
                )
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_trip_deletions (
                        tripId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(tripId)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_route_point_deletions (
                        tripId TEXT NOT NULL,
                        pointId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(tripId, pointId)
                    )
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN ownerId TEXT NOT NULL DEFAULT 'unknown'"
                )
            }
        }

        @Volatile
        private var INSTANCE: TravelDatabase? = null

        fun getInstance(context: Context): TravelDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_planner.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
