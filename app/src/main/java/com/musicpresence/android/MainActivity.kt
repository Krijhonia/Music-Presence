package com.musicpresence.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.musicpresence.android.data.PreferencesManager
import com.musicpresence.android.discord.DiscordGateway
import com.musicpresence.android.service.DiscordPresenceService
import com.musicpresence.android.ui.screens.HomeScreen
import com.musicpresence.android.ui.screens.SettingsScreen
import com.musicpresence.android.ui.screens.SetupScreen
import com.musicpresence.android.ui.theme.MusicPresenceTheme

/**
 * Main activity hosting the Compose navigation graph.
 * Routes between Setup, Home, and Settings screens.
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefs: PreferencesManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = PreferencesManager(this)

        // Request notification permission for Android 13+
        requestNotificationPermissionIfNeeded()

        setContent {
            MusicPresenceTheme {
                MusicPresenceNavigation(
                    prefs = prefs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun MusicPresenceNavigation(
    prefs: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val startDestination = if (prefs.setupComplete && prefs.isConfigured()) "home" else "setup"

    // Observe service state
    val isServiceRunning by DiscordPresenceService.isRunning.collectAsStateWithLifecycle()
    val currentMedia by DiscordPresenceService.currentMedia.collectAsStateWithLifecycle()
    val connectionState by (DiscordPresenceService.connectionState
        ?: MutableStateFlow(DiscordGateway.ConnectionState.DISCONNECTED))
        .collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("setup") {
            SetupScreen(
                prefs = prefs,
                onSetupComplete = {
                    navController.navigate("home") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                isServiceRunning = isServiceRunning,
                connectionState = connectionState,
                currentMedia = currentMedia,
                onStartService = {
                    DiscordPresenceService.start(navController.context)
                    prefs.serviceEnabled = true
                },
                onStopService = {
                    DiscordPresenceService.stop(navController.context)
                    prefs.serviceEnabled = false
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                prefs = prefs,
                onNavigateBack = { navController.popBackStack() },
                onDisconnect = {
                    // Stop service
                    DiscordPresenceService.stop(navController.context)
                    // Clear all data
                    prefs.clearAll()
                    // Navigate to setup
                    navController.navigate("setup") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
