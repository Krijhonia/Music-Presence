package com.musicpresence.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * NotificationListenerService that enables the app to access MediaSessions.
 *
 * Android requires a NotificationListenerService to be declared and granted
 * permission before MediaSessionManager.getActiveSessions() can be used.
 * This service doesn't need to process notifications itself — its primary
 * purpose is to grant the permission needed for media session access.
 */
class MediaDetectionService : NotificationListenerService() {

    companion object {
        private const val TAG = "MediaDetectionService"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // We don't need to process notifications directly.
        // Media detection happens via MediaSessionManager in DiscordPresenceService.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for media detection.
    }
}
