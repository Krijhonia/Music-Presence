package com.musicpresence.android.media

import android.graphics.Bitmap

/**
 * Represents information about the currently playing media track.
 * Extracted from Android's MediaSession metadata.
 */
data class MediaInfo(
    val title: String,
    val artist: String,
    val album: String,
    val albumArt: Bitmap? = null,
    val duration: Long = 0L,       // milliseconds
    val position: Long = 0L,       // milliseconds
    val isPlaying: Boolean = false,
    val playerName: String = "",
    val playerPackage: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Returns a search-friendly string for looking up album art externally. */
    fun searchQuery(): String = "$artist $album".trim()

    /** Unique key for caching purposes. */
    fun cacheKey(): String = "$artist|$album|$title".lowercase()

    companion object {
        val EMPTY = MediaInfo(
            title = "",
            artist = "",
            album = ""
        )
    }
}
