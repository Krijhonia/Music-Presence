package com.musicpresence.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.musicpresence.android.data.PreferencesManager
import com.musicpresence.android.service.DiscordPresenceService

/**
 * Receives BOOT_COMPLETED broadcast to auto-start the service.
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = PreferencesManager(context)

            if (prefs.autoStart && prefs.isConfigured()) {
                Log.i(TAG, "Boot completed, auto-starting Music Presence service")
                DiscordPresenceService.start(context)
            } else {
                Log.d(TAG, "Boot completed, but auto-start is disabled or not configured")
            }
        }
    }
}
