package com.musicpresence.android.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log

/**
 * Detects and extracts media information from active MediaSessions.
 * Uses MediaSessionManager to monitor all active media playback on the device.
 */
class MediaSessionDetector(private val context: Context) {
    companion object {
        private const val TAG = "MediaSessionDetector"
    }

    private val mediaSessionManager: MediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private var activeController: MediaController? = null
    private var callback: MediaController.Callback? = null
    private var onMediaChanged: ((MediaInfo?) -> Unit)? = null

    /**
     * Start listening for media session changes.
     *
     * @param listenerComponent The ComponentName of the NotificationListenerService
     * @param onChange Callback invoked whenever media info changes (null = stopped)
     */
    fun startListening(
        listenerComponent: ComponentName,
        onChange: (MediaInfo?) -> Unit
    ) {
        onMediaChanged = onChange

        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                { controllers -> onActiveSessionsChanged(controllers) },
                listenerComponent
            )

            // Check for already active sessions
            val activeSessions = mediaSessionManager.getActiveSessions(listenerComponent)
            onActiveSessionsChanged(activeSessions)

            Log.i(TAG, "Started listening for media sessions")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification listener permission not granted", e)
        }
    }

    /**
     * Stop listening for media changes.
     */
    fun stopListening() {
        detachFromController()
        onMediaChanged = null
        Log.i(TAG, "Stopped listening for media sessions")
    }

    /**
     * Get the current media info, or null if nothing is playing.
     */
    fun getCurrentMedia(): MediaInfo? {
        val controller = activeController ?: return null
        return extractMediaInfo(controller)
    }

    private fun onActiveSessionsChanged(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            Log.d(TAG, "No active media sessions")
            detachFromController()
            onMediaChanged?.invoke(null)
            return
        }

        // Find the first playing session, or the first session if none are playing
        val playingController = controllers.firstOrNull { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.first()

        attachToController(playingController)
    }

    private fun attachToController(controller: MediaController) {
        // Detach from previous
        detachFromController()

        activeController = controller
        Log.i(TAG, "Attached to media session: ${controller.packageName}")

        // Create and register callback
        callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                val info = extractMediaInfo(controller)
                onMediaChanged?.invoke(info)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                val info = extractMediaInfo(controller)
                if (state?.state == PlaybackState.STATE_PLAYING) {
                    onMediaChanged?.invoke(info)
                } else if (state?.state == PlaybackState.STATE_PAUSED ||
                    state?.state == PlaybackState.STATE_STOPPED ||
                    state?.state == PlaybackState.STATE_NONE
                ) {
                    onMediaChanged?.invoke(null)
                }
            }

            override fun onSessionDestroyed() {
                Log.d(TAG, "Media session destroyed")
                onMediaChanged?.invoke(null)
            }
        }

        controller.registerCallback(callback!!)

        // Emit current state immediately
        val info = extractMediaInfo(controller)
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            onMediaChanged?.invoke(info)
        }
    }

    private fun detachFromController() {
        callback?.let { cb ->
            try {
                activeController?.unregisterCallback(cb)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering callback", e)
            }
        }
        callback = null
        activeController = null
    }

    private fun extractMediaInfo(controller: MediaController): MediaInfo {
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        val position = playbackState?.position ?: 0L

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        // Get human-readable player name from package name
        val packageName = controller.packageName ?: ""
        val playerName = getPlayerName(packageName)

        return MediaInfo(
            title = title,
            artist = artist,
            album = album,
            albumArt = albumArt,
            duration = duration,
            position = position,
            isPlaying = isPlaying,
            playerName = playerName,
            playerPackage = packageName
        )
    }

    /**
     * Map package names to human-readable player names.
     */
    private fun getPlayerName(packageName: String): String {
        return when {
            packageName.contains("apple.android.music", ignoreCase = true) -> "Apple Music"
            packageName.contains("spotify", ignoreCase = true) -> "Spotify"
            packageName.contains("youtube.music", ignoreCase = true) -> "YouTube Music"
            packageName.contains("google.android.music", ignoreCase = true) -> "Google Play Music"
            packageName.contains("amazon.mp3", ignoreCase = true) -> "Amazon Music"
            packageName.contains("deezer", ignoreCase = true) -> "Deezer"
            packageName.contains("tidal", ignoreCase = true) -> "TIDAL"
            packageName.contains("pandora", ignoreCase = true) -> "Pandora"
            packageName.contains("soundcloud", ignoreCase = true) -> "SoundCloud"
            packageName.contains("samsung.android.app.music", ignoreCase = true) -> "Samsung Music"
            packageName.contains("poweramp", ignoreCase = true) -> "Poweramp"
            packageName.contains("vlc", ignoreCase = true) -> "VLC"
            packageName.contains("foobar", ignoreCase = true) -> "foobar2000"
            packageName.contains("musicolet", ignoreCase = true) -> "Musicolet"
            packageName.contains("retro.musicplayer", ignoreCase = true) -> "Retro Music"
            packageName.contains("blackplayer", ignoreCase = true) -> "BlackPlayer"
            else -> {
                // Try to extract a readable name from package
                try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName.substringAfterLast(".")
                        .replaceFirstChar { it.uppercase() }
                }
            }
        }
    }
}
