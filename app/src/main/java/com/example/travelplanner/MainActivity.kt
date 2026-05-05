package com.example.travelplanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.travelplanner.data.sync.NetworkSyncMonitor
import com.example.travelplanner.data.sync.TripSyncWorker
import com.example.travelplanner.navigation.AppNavigation
import com.example.travelplanner.security.SharedPreferencesSecurityPreferences
import com.example.travelplanner.ui.theme.TravelPlannerTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var networkSyncMonitor: NetworkSyncMonitor? = null
    private val deepLinkEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val logoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val securityPreferences by lazy { SharedPreferencesSecurityPreferences(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var lastPausedAt: Long? = null
    private val logoutRunnable = Runnable {
        lifecycleScope.launch {
            logoutEvents.emit(Unit)
        }
    }
    private fun startInactivityTimer() {
        handler.removeCallbacks(logoutRunnable)
        val timeoutMillis = securityPreferences.lockAfterSeconds() * 1000L
        handler.postDelayed(logoutRunnable, timeoutMillis)
    }

    private fun stopInactivityTimer() {
        handler.removeCallbacks(logoutRunnable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDeepLink = intent?.dataString
        TripSyncWorker.schedule(applicationContext)
        TripSyncWorker.triggerImmediateSync(applicationContext)
        networkSyncMonitor = NetworkSyncMonitor(applicationContext).also { it.start() }
        enableEdgeToEdge()
        setContent {
            TravelPlannerTheme {
                AppNavigation(
                    initialDeepLink = initialDeepLink,
                    deepLinkEvents = deepLinkEvents,
                    logoutEvents = logoutEvents,
                    forceLogoutOnStart = savedInstanceState == null
                )
            }
        }
        startInactivityTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        startInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
        lastPausedAt = System.currentTimeMillis()
        stopInactivityTimer()
    }

    override fun onResume() {
        super.onResume()
        lastPausedAt?.let { pausedAt ->
            val timeoutMillis = securityPreferences.lockAfterSeconds() * 1000L
            if (System.currentTimeMillis() - pausedAt >= timeoutMillis) {
                lifecycleScope.launch {
                    logoutEvents.emit(Unit)
                }
            }
        }
        startInactivityTimer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { url ->
            lifecycleScope.launch {
                deepLinkEvents.emit(url)
            }
        }
        startInactivityTimer()
    }
    override fun onDestroy() {
        stopInactivityTimer()
        networkSyncMonitor?.stop()
        super.onDestroy()
    }
}
