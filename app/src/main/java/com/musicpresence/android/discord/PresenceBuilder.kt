package com.musicpresence.android.discord

import com.musicpresence.android.media.MediaInfo

/**
 * Builds Discord Rich Presence payloads from media information.
 * Converts MediaInfo into the Discord Activity format.
 */
class PresenceBuilder(
    private val applicationId: String
) {
    /**
     * Build a presence update from the current media info.
     *
     * @param mediaInfo The currently playing media information
     * @param activityType Activity type (0 = Playing, 2 = Listening)
     * @param showTimestamp Whether to show the playback position
     * @param showAlbumArt Whether to include album art
     * @param albumArtUrl External URL for the album art (from iTunes API etc.)
     */
    fun buildPresence(
        mediaInfo: MediaInfo,
        activityType: Int = Activity.TYPE_LISTENING,
        showTimestamp: Boolean = true,
        showAlbumArt: Boolean = true,
        albumArtUrl: String? = null
    ): PresenceUpdate {
        val activity = buildActivity(
            mediaInfo = mediaInfo,
            activityType = activityType,
            showTimestamp = showTimestamp,
            showAlbumArt = showAlbumArt,
            albumArtUrl = albumArtUrl
        )

        return PresenceUpdate(
            activities = listOf(activity),
            status = "online",
            afk = false
        )
    }

    /**
     * Build an empty presence (clears the activity).
     */
    fun buildEmptyPresence(): PresenceUpdate {
        return PresenceUpdate(
            activities = emptyList(),
            status = "online",
            afk = false
        )
    }

    private fun buildActivity(
        mediaInfo: MediaInfo,
        activityType: Int,
        showTimestamp: Boolean,
        showAlbumArt: Boolean,
        albumArtUrl: String?
    ): Activity {
        // Build timestamps for playback position
        val timestamps = if (showTimestamp && mediaInfo.duration > 0 && mediaInfo.isPlaying) {
            val now = System.currentTimeMillis()
            val startTime = now - mediaInfo.position
            val endTime = startTime + mediaInfo.duration
            ActivityTimestamps(
                start = startTime,
                end = endTime
            )
        } else {
            null
        }

        // Build assets for album art
        val assets = if (showAlbumArt && albumArtUrl != null) {
            ActivityAssets(
                largeImage = albumArtUrl,
                largeText = mediaInfo.album.ifEmpty { mediaInfo.title },
                smallImage = null,
                smallText = mediaInfo.playerName.ifEmpty { null }
            )
        } else {
            null
        }

        // Build the activity
        // details = song title, state = artist name
        val details = mediaInfo.title.take(128).ifEmpty { null }
        val state = buildStateString(mediaInfo).take(128).ifEmpty { null }

        return Activity(
            name = if (activityType == Activity.TYPE_LISTENING) {
                mediaInfo.playerName.ifEmpty { "Music" }
            } else {
                mediaInfo.playerName.ifEmpty { "Music" }
            },
            type = activityType,
            details = details,
            state = state,
            timestamps = timestamps,
            assets = assets,
            applicationId = applicationId
        )
    }

    /**
     * Build the state string (artist - album format).
     */
    private fun buildStateString(mediaInfo: MediaInfo): String {
        val parts = mutableListOf<String>()
        if (mediaInfo.artist.isNotBlank()) {
            parts.add(mediaInfo.artist)
        }
        // Only add album if it differs from the title
        if (mediaInfo.album.isNotBlank() && mediaInfo.album != mediaInfo.title) {
            parts.add("on ${mediaInfo.album}")
        }
        return parts.joinToString(" ")
    }
}
