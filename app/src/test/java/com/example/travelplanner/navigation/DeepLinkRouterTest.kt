package com.example.travelplanner.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkRouterTest {
    private val silentLogger: (String) -> Unit = {}

    @Test
    fun parseItemsCustomSchemeReturnsDetail() {
        val router = router()

        assertEquals(Destination.Detail("123"), router.parseURL("myapp://items/123"))
    }

    @Test
    fun parseCatalogWithFilterReturnsCatalogFilter() {
        val router = router()

        assertEquals(Destination.Catalog("new"), router.parseURL("myapp://catalog?filter=new"))
    }

    @Test
    fun parseCatalogWithoutFilterReturnsCatalogNullFilter() {
        val router = router()

        assertEquals(Destination.Catalog(null), router.parseURL("myapp://catalog"))
    }

    @Test
    fun parseInviteReturnsInviteToken() {
        val router = router()

        assertEquals(Destination.Invite("ABC"), router.parseURL("myapp://invite/ABC"))
    }

    @Test
    fun parseHttpsItemsReturnsDetail() {
        val router = router()

        assertEquals(Destination.Detail("999"), router.parseURL("https://myapp.com/items/999"))
    }

    @Test
    fun parseHttpsPublicReturnsPublic() {
        val router = router()

        assertEquals(Destination.Public, router.parseURL("https://myapp.com/public"))
    }

    @Test
    fun handlePublicUrlDelegatesToNavigate() {
        val navigator = RecordingNavigator()
        val router = router(navigator)

        router.handle("https://myapp.com/public")

        assertEquals(listOf(Destination.Public), navigator.destinations)
    }

    @Test
    fun deepLinkUrlBuilderCreatesPublicUrl() {
        assertEquals("https://myapp.com/public", DeepLinkUrlBuilder.publicUrl())
    }

    @Test
    fun parseUnknownRouteReturnsNull() {
        val router = router()

        assertNull(router.parseURL("myapp://unknown/route"))
    }

    @Test
    fun parseEmptyStringReturnsNull() {
        val router = router()

        assertNull(router.parseURL(""))
    }

    @Test
    fun handleValidUrlDelegatesToNavigate() {
        val navigator = RecordingNavigator()
        val router = router(navigator)

        router.handle("myapp://items/123")

        assertEquals(listOf(Destination.Detail("123")), navigator.destinations)
    }

    @Test
    fun handleInvalidUrlDoesNotNavigate() {
        val navigator = RecordingNavigator()
        val router = router(navigator)

        router.handle("myapp://invalid")

        assertTrue(navigator.destinations.isEmpty())
    }

    @Test
    fun navigateUpdatesCurrentDestination() {
        val router = router()

        router.navigate(Destination.Detail("123"))

        assertEquals(Destination.Detail("123"), router.currentDestination.value)
    }

    @Test
    fun shareUrlBuilderCreatesCustomSchemeItemUrl() {
        assertEquals("myapp://items/abc-123", DeepLinkUrlBuilder.tripDetailUrl("abc-123"))
    }

    @Test
    fun mockDeepLinkRouterUsesSameHandleFlow() {
        val navigator = RecordingNavigator()
        val router = router(navigator)
        val mock = MockDeepLinkRouter(router)

        mock.open("myapp://catalog?filter=new")

        assertEquals(listOf(Destination.Catalog("new")), navigator.destinations)
    }

    @Test
    fun inviteDeepLinkWhenLoggedOutNavigatesToLoginAndKeepsPendingInvite() {
        val navigator = RecordingNavigator()
        val router = DeepLinkRouter(
            navigator = navigator,
            isAuthenticated = { false },
            logger = silentLogger
        )

        router.handle("myapp://invite/TOKEN")

        assertEquals(listOf(Destination.Login), navigator.destinations)
        assertEquals(Destination.Invite("TOKEN"), router.pendingDestination.value)
    }

    private fun router(navigator: RecordingNavigator = RecordingNavigator()) =
        DeepLinkRouter(navigator = navigator, logger = silentLogger)

    private class RecordingNavigator : DeepLinkNavigator {
        val destinations = mutableListOf<Destination>()

        override fun navigate(destination: Destination, resetBackStack: Boolean) {
            destinations += destination
        }
    }
}
