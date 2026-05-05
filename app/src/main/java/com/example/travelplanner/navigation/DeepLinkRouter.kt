package com.example.travelplanner.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

interface DeepLinkNavigator {
    fun navigate(destination: Destination, resetBackStack: Boolean = true)
}

class DeepLinkRouter(
    private val navigator: DeepLinkNavigator,
    private val isAuthenticated: () -> Boolean = { true },
    private val logger: (String) -> Unit = { message -> Logger.getLogger(TAG).warning(message) }
) {
    private val _currentDestination = MutableStateFlow<Destination>(Destination.Home)
    val currentDestination: StateFlow<Destination> = _currentDestination

    private val _pendingDestination = MutableStateFlow<Destination?>(null)
    val pendingDestination: StateFlow<Destination?> = _pendingDestination

    fun parseURL(url: String): Destination? {
        if (url.isBlank()) {
            logger("Deep link is empty")
            return null
        }

        val uri = runCatching { URI(url.trim()) }.getOrElse {
            logger("Malformed deep link ignored: $url")
            return null
        }

        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        val segments = when {
            scheme == APP_SCHEME -> listOfNotNull(host) + uri.pathSegments()
            scheme == HTTPS_SCHEME && host == APP_HOST -> uri.pathSegments()
            else -> {
                logger("Unsupported deep link ignored: $url")
                return null
            }
        }

        return when {
            segments == listOf("home") || segments.isEmpty() -> Destination.Home
            segments.firstOrNull() == "items" && segments.size == 2 ->
                Destination.Detail(segments[1])
            segments.firstOrNull() == "catalog" && segments.size == 1 ->
                Destination.Catalog(uri.queryParameters()["filter"])
            segments.firstOrNull() == "invite" && segments.size == 2 ->
                Destination.Invite(segments[1])
            segments == listOf("notifications") -> Destination.Notifications
            segments == listOf("debug") -> Destination.Debug
            segments == listOf("public") -> Destination.Public
            else -> {
                logger("Unknown deep link route ignored: $url")
                null
            }
        }
    }

    fun handle(url: String) {
        val destination = parseURL(url) ?: return
        navigate(destination)
    }

    fun navigate(to: Destination) {
        val protectedDestination = to.requiresAuthentication()
        if (protectedDestination && !isAuthenticated()) {
            _pendingDestination.value = to
            _currentDestination.value = Destination.Login
            navigator.navigate(Destination.Login, resetBackStack = true)
            return
        }

        _pendingDestination.value = null
        _currentDestination.value = to
        navigator.navigate(to, resetBackStack = true)
    }

    fun consumePendingAfterLogin(): Destination? {
        val pending = _pendingDestination.value ?: return null
        _pendingDestination.value = null
        return pending
    }

    private fun Destination.requiresAuthentication(): Boolean =
        this !is Destination.Catalog && this != Destination.Debug &&
        this != Destination.Public && this != Destination.DeferredOnboarding

    private fun URI.pathSegments(): List<String> =
        rawPath.orEmpty()
            .split("/")
            .filter { it.isNotBlank() }
            .map { it.decode() }

    private fun URI.queryParameters(): Map<String, String> =
        rawQuery.orEmpty()
            .split("&")
            .filter { it.isNotBlank() }
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                val key = parts.getOrNull(0)?.decode().orEmpty()
                if (key.isBlank()) null else key to parts.getOrNull(1).orEmpty().decode()
            }
            .toMap()

    private fun String.decode(): String =
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())

    companion object {
        const val APP_SCHEME = "myapp"
        const val APP_HOST = "myapp.com"
        private const val HTTPS_SCHEME = "https"
        private const val TAG = "DeepLinkRouter"
    }
}

object DeepLinkUrlBuilder {
    fun tripDetailUrl(id: String): String = "${DeepLinkRouter.APP_SCHEME}://items/$id"
    fun publicUrl(): String = "https://${DeepLinkRouter.APP_HOST}/public"
}

class MockDeepLinkRouter(private val router: DeepLinkRouter) {
    fun open(url: String) {
        router.handle(url)
    }
}
