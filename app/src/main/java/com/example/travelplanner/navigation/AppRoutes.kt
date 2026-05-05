package com.example.travelplanner.navigation

import android.net.Uri

sealed class AppRoutes(val route: String) {

    object TripList   : AppRoutes("trip_list")
    object Catalog : AppRoutes("catalog?filter={filter}") {
        fun createRoute(filter: String?) = if (filter.isNullOrBlank()) {
            "catalog"
        } else {
            "catalog?filter=${Uri.encode(filter)}"
        }
    }
    object Profile    : AppRoutes("profile")
    object EditProfile : AppRoutes("edit_profile")
    object Login : AppRoutes("login")
    object Register : AppRoutes("register")
    object RealtimeStatus : AppRoutes("realtime_status")
    object RealtimeEvents : AppRoutes("realtime_events")
    object SecuritySettings : AppRoutes("security_settings")
    object Invite : AppRoutes("invite/{token}") {
        fun createRoute(token: String) = "invite/${Uri.encode(token)}"
    }
    object Public : AppRoutes("public")
    object DeferredOnboarding : AppRoutes("deferred_onboarding")
    object DebugDeepLink : AppRoutes("debug_deep_link")

    object CriticalAction : AppRoutes("critical_action/{tripId}") {
        fun createRoute(tripId: String) = "critical_action/${Uri.encode(tripId)}"
    }

    object TripDetail : AppRoutes("trip_detail/{tripId}") {
        fun createRoute(tripId: String) = "trip_detail/${Uri.encode(tripId)}"
    }

    object AddTrip : AppRoutes("add_trip")

    object AddRoutePoint : AppRoutes("add_route_point/{tripId}") {
        fun createRoute(tripId: String) = "add_route_point/${Uri.encode(tripId)}"
    }
}
