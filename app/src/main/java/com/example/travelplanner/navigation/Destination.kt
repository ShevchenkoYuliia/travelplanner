package com.example.travelplanner.navigation

sealed class Destination {
    data object Home : Destination()
    data class Detail(val id: String) : Destination()
    data class Catalog(val filter: String? = null) : Destination()
    data class Invite(val token: String) : Destination()
    data object Notifications : Destination()
    data object Debug : Destination()
    data object Public : Destination()
    data object DeferredOnboarding : Destination()
    data object Login : Destination()
}
