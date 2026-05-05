package com.example.travelplanner.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.Context
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.travel.BuildConfig
import com.example.travelplanner.data.UserRepository
import com.example.travelplanner.data.local.TravelDatabase
import com.example.travelplanner.data.realtime.FakeSocketTransport
import com.example.travelplanner.data.realtime.OkHttpSocketTransport
import com.example.travelplanner.data.realtime.SocketManager
import com.example.travelplanner.data.remote.ApiConfig
import com.example.travelplanner.data.remote.HttpTripApiService
import com.example.travelplanner.data.remote.HttpUserApiService
import com.example.travelplanner.data.repository.OfflineFirstTripRepository
import com.example.travelplanner.domain.usecase.trip.DeleteRoutePointUseCase
import com.example.travelplanner.domain.usecase.trip.DeleteTripUseCase
import com.example.travelplanner.domain.usecase.trip.ObserveRoutePointsUseCase
import com.example.travelplanner.domain.usecase.trip.ObserveTripByIdUseCase
import com.example.travelplanner.domain.usecase.trip.ObserveTripsUseCase
import com.example.travelplanner.domain.usecase.trip.RefreshTripsUseCase
import com.example.travelplanner.domain.usecase.trip.SaveRoutePointUseCase
import com.example.travelplanner.domain.usecase.trip.SaveTripUseCase
import com.example.travelplanner.domain.usecase.trip.SyncPendingTripsUseCase
import com.example.travelplanner.domain.usecase.trip.TripUseCases
import com.example.travelplanner.domain.usecase.trip.UpdateRoutePointUseCase
import com.example.travelplanner.domain.usecase.trip.CreateInviteTokenUseCase
import com.example.travelplanner.domain.usecase.trip.GetInvitePreviewUseCase
import com.example.travelplanner.domain.usecase.trip.AcceptInviteUseCase
import com.example.travelplanner.screens.*
import com.example.travelplanner.security.AndroidBiometricManager
import com.example.travelplanner.security.SecurityViewModel
import com.example.travelplanner.security.SecurityViewModelFactory
import com.example.travelplanner.security.SharedPreferencesSecurityPreferences
import com.example.travelplanner.viewmodel.ProfileViewModel
import com.example.travelplanner.viewmodel.ProfileViewModelFactory
import com.example.travelplanner.presentation.trip.TripDetailViewModelFactory
import com.example.travelplanner.presentation.trip.TripListViewModelFactory
import com.example.travelplanner.presentation.realtime.RealtimeViewModel
import com.example.travelplanner.presentation.realtime.RealtimeViewModelFactory
import com.example.travelplanner.presentation.trip.detail.TripDetailViewModel
import com.example.travelplanner.presentation.trip.list.TripListViewModel
import com.example.travelplanner.viewmodel.AuthViewModel
import com.example.travelplanner.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.flow.SharedFlow

private const val KEY_FIRST_RUN_DONE = "first_run_done"

@Composable
fun AppNavigation(
    initialDeepLink: String? = null,
    deepLinkEvents: SharedFlow<String>? = null,
    logoutEvents: SharedFlow<Unit>? = null,
    forceLogoutOnStart: Boolean = false
) {
    val context = LocalContext.current
    val securityPreferences = remember(context) { SharedPreferencesSecurityPreferences(context) }
    val biometricManager = remember(context, securityPreferences) {
        AndroidBiometricManager(
            activity = context as? FragmentActivity,
            context = context,
            preferences = securityPreferences
        )
    }

    val db = remember(context) { TravelDatabase.getInstance(context) }
    val tripRepository = remember(db) {
        OfflineFirstTripRepository(
            context = context,
            tripDao = db.tripDao(),
            routePointDao = db.routePointDao(),
            userDao = db.userDao(),
            api = HttpTripApiService(ApiConfig.BASE_URL)
        )
    }
    val tripUseCases = remember(tripRepository) {
        TripUseCases(
            observeTrips = ObserveTripsUseCase(tripRepository),
            observeTripById = ObserveTripByIdUseCase(tripRepository),
            observeRoutePoints = ObserveRoutePointsUseCase(tripRepository),
            saveTrip = SaveTripUseCase(tripRepository),
            deleteTrip = DeleteTripUseCase(tripRepository),
            saveRoutePoint = SaveRoutePointUseCase(tripRepository),
            updateRoutePoint = UpdateRoutePointUseCase(tripRepository),
            deleteRoutePoint = DeleteRoutePointUseCase(tripRepository),
            syncPendingTrips = SyncPendingTripsUseCase(tripRepository),
            refreshTrips = RefreshTripsUseCase(tripRepository),
            createInviteToken = CreateInviteTokenUseCase(tripRepository),
            getInvitePreview = GetInvitePreviewUseCase(tripRepository),
            acceptInvite = AcceptInviteUseCase(tripRepository)
        )
    }
    val userRepository = remember(context, tripRepository, db) {
        UserRepository(
            context = context,
            tripRepository = tripRepository,
            userDao = db.userDao(),
            userApiService = HttpUserApiService(ApiConfig.BASE_URL)
        )
    }
    val authVm = viewModel<AuthViewModel>(
        factory = AuthViewModelFactory(userRepository)
    )
    LaunchedEffect(forceLogoutOnStart) {
        if (forceLogoutOnStart) {
            authVm.logout()
        }
    }
    val securityVm = viewModel<SecurityViewModel>(
        key = "security_vm",
        factory = SecurityViewModelFactory(biometricManager, securityPreferences)
    )
    val isLoggedIn by authVm.isLoggedIn.collectAsState()
    val hasRegisteredAccounts = authVm.hasRegisteredAccounts()
    val currentUserEmail by authVm.currentUserEmail.collectAsState()
    val profileVm = viewModel<ProfileViewModel>(
        key = "profile_${currentUserEmail ?: "guest"}",
        factory = ProfileViewModelFactory(userRepository)
    )
    val userProfile by profileVm.userProfile.collectAsState()
    val socketManager = remember {
        SocketManager(
            transportFactory = { scope ->
                if (ApiConfig.USE_FAKE_WS) FakeSocketTransport(scope)
                else OkHttpSocketTransport()
            }
        )
    }
    val realtimeVm = viewModel<RealtimeViewModel>(
        key = "realtime_vm",
        factory = RealtimeViewModelFactory(socketManager)
    )

    val navController = rememberNavController()
    val deepLinkRouter = remember(navController, isLoggedIn) {
        DeepLinkRouter(
            navigator = ComposeDeepLinkNavigator(navController),
            isAuthenticated = { isLoggedIn }
        )
    }
    val appPrefs = remember(context) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    val isFirstLaunch = remember {
        !appPrefs.getBoolean(KEY_FIRST_RUN_DONE, false)
    }
    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) {
            appPrefs.edit().putBoolean(KEY_FIRST_RUN_DONE, true).apply()
        }
    }
    val initialDeepLinkDestination = remember(initialDeepLink) {
        initialDeepLink?.let { deepLinkRouter.parseURL(it) }
    }
    val shouldShowDeferredOnboarding = initialDeepLinkDestination != null && isFirstLaunch
    val currentDeepLinkDestination by deepLinkRouter.currentDestination.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isAdmin = currentUserEmail?.let { userRepository.isAdmin(it) } == true

    LaunchedEffect(initialDeepLink, deepLinkRouter, shouldShowDeferredOnboarding) {
        if (!shouldShowDeferredOnboarding) {
            initialDeepLink?.let { deepLinkRouter.handle(it) }
        }
    }

    LaunchedEffect(shouldShowDeferredOnboarding, deepLinkRouter) {
        if (shouldShowDeferredOnboarding) {
            deepLinkRouter.navigate(Destination.DeferredOnboarding)
        }
    }

    LaunchedEffect(deepLinkEvents, deepLinkRouter) {
        deepLinkEvents?.collect { url ->
            deepLinkRouter.handle(url)
        }
    }
    LaunchedEffect(initialDeepLink, deepLinkRouter) {
        initialDeepLink?.let { deepLinkRouter.handle(it) }
    }
    LaunchedEffect(logoutEvents, navController) {
        logoutEvents?.collect {
            authVm.logout()
            navController.navigate(AppRoutes.Login.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val bottomNavRoutes = mutableListOf(
        AppRoutes.TripList.route,
        AppRoutes.Catalog.route,
        AppRoutes.Profile.route
    )
    if (BuildConfig.DEBUG && isAdmin) {
        bottomNavRoutes.add(AppRoutes.DebugDeepLink.route)
    }
    val showBottomBar = isLoggedIn && currentDestination?.route in bottomNavRoutes

    val initialRoute = when {
        shouldShowDeferredOnboarding -> AppRoutes.DeferredOnboarding.route
        isLoggedIn -> AppRoutes.TripList.route
        hasRegisteredAccounts -> AppRoutes.Login.route
        else -> AppRoutes.Register.route
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == AppRoutes.TripList.route } == true,
                        onClick = {
                            navController.navigate(AppRoutes.TripList.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Place, contentDescription = "Поїздки") },
                        label = { Text("Поїздки") }
                    )
                    if (BuildConfig.DEBUG && isAdmin) {
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == AppRoutes.DebugDeepLink.route } == true,
                            onClick = {
                                navController.navigate(AppRoutes.DebugDeepLink.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.BugReport, contentDescription = "Deep links") },
                            label = { Text("Debug") }
                        )
                    }
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == AppRoutes.Profile.route } == true,
                        onClick = {
                            navController.navigate(AppRoutes.Profile.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Профіль") },
                        label = { Text("Профіль") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.TripList.route) {
                val vm = viewModel<TripListViewModel>(
                    key = "trip_list_${currentUserEmail ?: "guest"}",
                    factory = TripListViewModelFactory(tripUseCases, socketManager, null)
                )
                TripListScreen(
                    viewModel = vm,
                    onTripClick = { tripId ->
                        navController.navigate(AppRoutes.TripDetail.createRoute(tripId))
                    },
                    onAddTripClick = {
                        navController.navigate(AppRoutes.AddTrip.route)
                    },
                    onDeleteTripClick = { tripId ->
                        navController.navigate(AppRoutes.CriticalAction.createRoute(tripId))
                    }
                )
            }

            composable(
                route = AppRoutes.Catalog.route,
                arguments = listOf(
                    navArgument("filter") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val filterParam = backStackEntry.arguments?.getString("filter")
                val vm = viewModel<TripListViewModel>(
                    key = "catalog_${currentUserEmail ?: "guest"}_$filterParam",
                    factory = TripListViewModelFactory(tripUseCases, socketManager, filterParam)
                )
                TripListScreen(
                    viewModel = vm,
                    onTripClick = { tripId ->
                        navController.navigate(AppRoutes.TripDetail.createRoute(tripId))
                    },
                    onAddTripClick = {
                        navController.navigate(AppRoutes.AddTrip.route)
                    },
                    onDeleteTripClick = { tripId ->
                        navController.navigate(AppRoutes.CriticalAction.createRoute(tripId))
                    }
                )
            }

            composable(AppRoutes.TripDetail.route) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                val vm = viewModel<TripDetailViewModel>(
                    key = "trip_detail_${currentUserEmail ?: "guest"}_$tripId",
                    factory = TripDetailViewModelFactory(tripUseCases, tripId)
                )
                TripDetailScreen(
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() },
                    onAddRoutePointClick = {
                        navController.navigate(AppRoutes.AddRoutePoint.createRoute(tripId))
                    }
                )
            }

            composable(AppRoutes.AddTrip.route) {
                val vm = viewModel<TripListViewModel>(
                    key = "add_trip_${currentUserEmail ?: "guest"}",
                    factory = TripListViewModelFactory(tripUseCases, socketManager, null)
                )
                AddTripScreen(
                    viewModel = vm,
                    preferredCurrency = userProfile.preferredCurrency,
                    onBackClick = { navController.popBackStack() },
                    onTripSaved = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.AddRoutePoint.route) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                val vm = viewModel<TripDetailViewModel>(
                    key = "add_point_${currentUserEmail ?: "guest"}_$tripId",
                    factory = TripDetailViewModelFactory(tripUseCases, tripId)
                )
                AddRoutePointScreen(
                    tripId = tripId,
                    viewModel = vm,
                    preferredCurrency = userProfile.preferredCurrency,
                    onBackClick = { navController.popBackStack() },
                    onPointSaved = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.Public.route) {
                PublicScreen(
                    onOpenFully = {
                        deepLinkRouter.navigate(Destination.Home)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.DeferredOnboarding.route) {
                DeferredOnboardingScreen(
                    deepLink = initialDeepLink.orEmpty(),
                    destination = initialDeepLinkDestination,
                    onOpenFully = {
                        initialDeepLink?.let { deepLinkRouter.handle(it) }
                    },
                    onSkip = {
                        if (isLoggedIn) {
                            navController.navigate(AppRoutes.TripList.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else if (hasRegisteredAccounts) {
                            navController.navigate(AppRoutes.Login.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(AppRoutes.Register.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(AppRoutes.Profile.route) {
                ProfileScreen(
                    viewModel = profileVm,
                    isLoggedIn = isLoggedIn,
                    onEditProfileClick = {
                        navController.navigate(AppRoutes.EditProfile.route)
                    },
                    onLoginClick = {
                        navController.navigate(AppRoutes.Login.route)
                    },
                    onRegisterClick = {
                        navController.navigate(AppRoutes.Register.route)
                    },
                    onRealtimeStatusClick = {
                        navController.navigate(AppRoutes.RealtimeStatus.route)
                    },
                    onRealtimeEventsClick = {
                        navController.navigate(AppRoutes.RealtimeEvents.route)
                    },
                    onSecurityClick = {
                        navController.navigate(AppRoutes.SecuritySettings.route)
                    },
                    onLogoutClick = {
                        authVm.logout()
                        navController.navigate(AppRoutes.Login.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoutes.EditProfile.route) {
                EditProfileScreen(
                    viewModel = profileVm,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.Login.route) {
                LoginScreen(
                    onLoginClick = { email, password -> authVm.login(email, password) },
                    onOpenRegister = { navController.navigate(AppRoutes.Register.route) },
                    showRegisterOnly = !hasRegisteredAccounts,
                    securityViewModel = securityVm,
                    onBiometricLogin = { authVm.unlockWithBiometrics() },
                    onLoginSuccess = {
                        deepLinkRouter.consumePendingAfterLogin()?.let { pending ->
                            deepLinkRouter.navigate(pending)
                        } ?: navController.navigate(AppRoutes.TripList.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoutes.Register.route) {
                RegisterScreen(
                    onRegisterClick = { email, password -> authVm.register(email, password) },
                    onBackClick = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(AppRoutes.TripList.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoutes.RealtimeStatus.route) {
                RealtimeStatusScreen(
                    viewModel = realtimeVm,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.RealtimeEvents.route) {
                RealtimeEventsScreen(
                    viewModel = realtimeVm,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.SecuritySettings.route) {
                SecuritySettingsScreen(
                    viewModel = securityVm,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.Invite.route) { backStackEntry ->
                val token = backStackEntry.arguments?.getString("token") ?: return@composable
                val vm = viewModel<TripListViewModel>(
                    key = "invite_${currentUserEmail ?: "guest"}_$token",
                    factory = TripListViewModelFactory(tripUseCases, socketManager, null)
                )
                
                LaunchedEffect(token) {
                    vm.loadInvitePreview(token)
                }
                
                val preview by vm.invitePreview.collectAsState()
                val scope = rememberCoroutineScope()
                var isAccepting by remember { mutableStateOf(false) }

                InviteScreen(
                    preview = preview,
                    isAccepting = isAccepting,
                    onAccept = {
                        scope.launch {
                            isAccepting = true
                            try {
                                vm.acceptInvite(token)
                                navController.navigate(AppRoutes.TripList.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                            } finally {
                                isAccepting = false
                            }
                        }
                    },
                    onDecline = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() }
                )
            }

            if (BuildConfig.DEBUG && isAdmin) {
                composable(AppRoutes.DebugDeepLink.route) {
                    DebugDeepLinkScreen(
                        onOpenUrl = { url -> deepLinkRouter.handle(url) }
                    )
                }
            }

            composable(AppRoutes.CriticalAction.route) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                val vm = viewModel<TripListViewModel>(
                    key = "critical_delete_${currentUserEmail ?: "guest"}",
                    factory = TripListViewModelFactory(tripUseCases, socketManager, null)
                )
                CriticalActionScreen(
                    tripId = tripId,
                    tripListViewModel = vm,
                    securityViewModel = securityVm,
                    onBackClick = { navController.popBackStack() },
                    onActionConfirmed = {
                        navController.popBackStack(AppRoutes.TripList.route, inclusive = false)
                    }
                )
            }
        }
    }
}

private class ComposeDeepLinkNavigator(
    private val navController: NavHostController
) : DeepLinkNavigator {
    override fun navigate(destination: Destination, resetBackStack: Boolean) {
        val route = when (destination) {
            Destination.Home -> AppRoutes.TripList.route
            is Destination.Detail -> AppRoutes.TripDetail.createRoute(destination.id)
            is Destination.Catalog -> AppRoutes.Catalog.createRoute(destination.filter)
            is Destination.Invite -> AppRoutes.Invite.createRoute(destination.token)
            Destination.Notifications -> AppRoutes.RealtimeEvents.route
            Destination.Debug -> AppRoutes.DebugDeepLink.route
            Destination.Public -> AppRoutes.Public.route
            Destination.DeferredOnboarding -> AppRoutes.DeferredOnboarding.route
            Destination.Login -> AppRoutes.Login.route
        }

        navController.navigate(route) {
            if (resetBackStack) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = destination == Destination.Home || destination == Destination.Login
                    saveState = false
                }
            }
            launchSingleTop = true
            restoreState = false
        }
    }
}
