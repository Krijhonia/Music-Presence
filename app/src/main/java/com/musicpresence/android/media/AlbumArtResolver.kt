package com.musicpresence.android.media

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Resolves album art URLs using the iTunes Search API.
 * This provides high-quality album art URLs that Discord can display.
 *
 * Uses an LRU memory cache to avoid repeated network lookups.
 */
class AlbumArtResolver {
    companion object {
        private const val TAG = "AlbumArtResolver"
        private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
        private const val CACHE_MAX_SIZE = 100
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Simple LRU cache: query -> artworkUrl
    private val cache = LinkedHashMap<String, String?>(CACHE_MAX_SIZE, 0.75f, true)

    /**
     * Resolve an album art URL for the given media info.
     * Returns a URL string or null if not found.
     */
    suspend fun resolveArtUrl(mediaInfo: MediaInfo): String? = withContext(Dispatchers.IO) {
        val cacheKey = mediaInfo.cacheKey()

        // Check cache first
        synchronized(cache) {
            if (cache.containsKey(cacheKey)) {
                return@withContext cache[cacheKey]
            }
        }

        // Try searching iTunes
        val artUrl = searchItunes(mediaInfo)

        // Cache the result (even if null, to avoid repeated failed lookups)
        synchronized(cache) {
            if (cache.size >= CACHE_MAX_SIZE) {
                val oldestKey = cache.keys.firstOrNull()
                if (oldestKey != null) cache.remove(oldestKey)
            }
            cache[cacheKey] = artUrl
        }

        return@withContext artUrl
    }

    /**
     * Search the iTunes API for album artwork.
     */
    private fun searchItunes(mediaInfo: MediaInfo): String? {
        try {
            // Build search query: "artist album" or "artist title"
            val query = if (mediaInfo.album.isNotBlank()) {
                "${mediaInfo.artist} ${mediaInfo.album}"
            } else {
                "${mediaInfo.artist} ${mediaInfo.title}"
            }.trim()

            if (query.isBlank()) return null

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$ITUNES_SEARCH_URL?term=$encodedQuery&media=music&entity=album&limit=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MusicPresence/1.0")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "iTunes search failed: ${response.code}")
                return null
            }

            val body = response.body?.string() ?: return null
            val json = JsonParser.parseString(body).asJsonObject
            val resultCount = json.get("resultCount")?.asInt ?: 0

            if (resultCount == 0) {
                Log.d(TAG, "No iTunes results for: $query")
                return null
            }

            val results = json.getAsJsonArray("results")
            val firstResult = results[0].asJsonObject

            // Get artwork URL and upscale to 512x512
            val artworkUrl = firstResult.get("artworkUrl100")?.asString
            val highResUrl = artworkUrl?.replace("100x100bb", "512x512bb")

            Log.d(TAG, "Found album art for '$query': $highResUrl")
            return highResUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error searching iTunes: ${e.message}", e)
            return null
        }
    }

    /**
     * Clear the artwork cache.
     */
    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
    }
}
