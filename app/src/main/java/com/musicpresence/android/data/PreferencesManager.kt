package com.musicpresence.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages app preferences with encrypted storage for sensitive data (Discord token).
 * Uses EncryptedSharedPreferences backed by Android Keystore.
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "music_presence_prefs"
        private const val ENCRYPTED_PREFS_NAME = "music_presence_encrypted_prefs"

        // Encrypted keys
        private const val KEY_DISCORD_TOKEN = "discord_token"

        // Regular keys
        private const val KEY_APPLICATION_ID = "application_id"
        private const val KEY_ACTIVITY_TYPE = "activity_type"
        private const val KEY_SHOW_ALBUM_ART = "show_album_art"
        private const val KEY_SHOW_TIMESTAMP = "show_timestamp"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_DISABLED_PLAYERS = "disabled_players"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // --- Discord Token (encrypted) ---

    var discordToken: String
        get() = encryptedPrefs.getString(KEY_DISCORD_TOKEN, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_DISCORD_TOKEN, value).apply()

    // --- Application ID ---

    var applicationId: String
        get() = prefs.getString(KEY_APPLICATION_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APPLICATION_ID, value).apply()

    // --- Activity Type (0 = Playing, 2 = Listening) ---

    var activityType: Int
        get() = prefs.getInt(KEY_ACTIVITY_TYPE, 2) // Default: Listening
        set(value) = prefs.edit().putInt(KEY_ACTIVITY_TYPE, value).apply()

    // --- Show Album Art ---

    var showAlbumArt: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ALBUM_ART, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ALBUM_ART, value).apply()

    // --- Show Timestamp ---

    var showTimestamp: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIMESTAMP, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TIMESTAMP, value).apply()

    // --- Auto-Start on Boot ---

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    // --- Setup Complete ---

    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    // --- Service Enabled ---

    var serviceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    // --- Disabled Players ---

    var disabledPlayers: Set<String>
        get() = prefs.getStringSet(KEY_DISABLED_PLAYERS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_DISABLED_PLAYERS, value).apply()

    /**
     * Check if all required configuration is present.
     */
    fun isConfigured(): Boolean {
        return discordToken.isNotBlank() && applicationId.isNotBlank()
    }

    /**
     * Check if a player package is enabled.
     */
    fun isPlayerEnabled(packageName: String): Boolean {
        return packageName !in disabledPlayers
    }

    /**
     * Toggle a player package.
     */
    fun togglePlayer(packageName: String, enabled: Boolean) {
        val current = disabledPlayers.toMutableSet()
        if (enabled) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        disabledPlayers = current
    }

    /**
     * Clear all stored data (logout).
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        prefs.edit().clear().apply()
    }
}
