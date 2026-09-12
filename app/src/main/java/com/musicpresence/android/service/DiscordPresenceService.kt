package com.musicpresence.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.musicpresence.android.MainActivity
import com.musicpresence.android.R
import com.musicpresence.android.data.PreferencesManager
import com.musicpresence.android.discord.DiscordGateway
import com.musicpresence.android.discord.PresenceBuilder
import com.musicpresence.android.media.AlbumArtResolver
import com.musicpresence.android.media.MediaInfo
import com.musicpresence.android.media.MediaSessionDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that orchestrates the music presence system.
 *
 * Responsibilities:
 * - Runs as a foreground service with a persistent notification
 * - Manages the Discord Gateway connection
 * - Listens for media session changes
 * - Resolves album art via iTunes Search API
 * - Sends presence updates to Discord
 */
class DiscordPresenceService : Service() {

    companion object {
        private const val TAG = "DiscordPresenceService"
        private const val NOTIFICATION_CHANNEL_ID = "music_presence_service"
        private const val NOTIFICATION_ID = 1001

        private val _currentMedia = MutableStateFlow<MediaInfo?>(null)
        val currentMedia: StateFlow<MediaInfo?> = _currentMedia.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private var gatewayInstance: DiscordGateway? = null

        val connectionState get() = gatewayInstance?.connectionState

        fun start(context: Context) {
            val intent = Intent(context, DiscordPresenceService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DiscordPresenceService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var prefs: PreferencesManager
    private lateinit var mediaDetector: MediaSessionDetector
    private lateinit var albumArtResolver: AlbumArtResolver
    private var gateway: DiscordGateway? = null
    private var presenceBuilder: PresenceBuilder? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var artResolveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        prefs = PreferencesManager(this)
        mediaDetector = MediaSessionDetector(this)
        albumArtResolver = AlbumArtResolver()

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service starting")

        // Start as foreground service immediately
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notification_text_idle)))

        // Initialize Discord connection
        initializeDiscord()

        // Start media detection
        startMediaDetection()

        _isRunning.value = true

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")

        _isRunning.value = false

        // Stop media detection
        mediaDetector.stopListening()

        // Disconnect from Discord
        gateway?.clearPresence()
        gateway?.disconnect()
        gatewayInstance = null

        // Cancel coroutines
        serviceScope.cancel()

        _currentMedia.value = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeDiscord() {
        val token = prefs.discordToken
        val appId = prefs.applicationId

        if (token.isBlank() || appId.isBlank()) {
            Log.e(TAG, "Discord token or application ID not configured")
            stopSelf()
            return
        }

        gateway = DiscordGateway(token, serviceScope)
        gatewayInstance = gateway
        presenceBuilder = PresenceBuilder(appId)

        gateway?.connect()
    }

    private fun startMediaDetection() {
        val listenerComponent = ComponentName(this, MediaDetectionService::class.java)

        mediaDetector.startListening(listenerComponent) { mediaInfo ->
            handleMediaChange(mediaInfo)
        }
    }

    private fun handleMediaChange(mediaInfo: MediaInfo?) {
        _currentMedia.value = mediaInfo

        if (mediaInfo == null || !mediaInfo.isPlaying || mediaInfo.title.isBlank()) {
            // Nothing playing — clear presence
            gateway?.clearPresence()
            updateNotification(getString(R.string.notification_text_idle))
            return
        }

        // Check if this player is enabled
        if (!prefs.isPlayerEnabled(mediaInfo.playerPackage)) {
            Log.d(TAG, "Player ${mediaInfo.playerPackage} is disabled, skipping")
            return
        }

        // Update notification
        updateNotification("♪ ${mediaInfo.title} — ${mediaInfo.artist}")

        // Resolve album art and update presence
        artResolveJob?.cancel()
        artResolveJob = serviceScope.launch {
            val artUrl = if (prefs.showAlbumArt) {
                albumArtResolver.resolveArtUrl(mediaInfo)
            } else {
                null
            }

            val presence = presenceBuilder?.buildPresence(
                mediaInfo = mediaInfo,
                activityType = prefs.activityType,
                showTimestamp = prefs.showTimestamp,
                showAlbumArt = prefs.showAlbumArt,
                albumArtUrl = artUrl
            )

            if (presence != null) {
                gateway?.updatePresence(presence)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
