package com.musicpresence.android.discord

import com.google.gson.annotations.SerializedName

/**
 * Discord Gateway payload wrapper.
 * All messages sent/received follow this structure.
 */
data class GatewayPayload(
    @SerializedName("op") val op: Int,
    @SerializedName("d") val d: Any? = null,
    @SerializedName("s") val s: Int? = null,
    @SerializedName("t") val t: String? = null
)

/**
 * Hello event data (opcode 10).
 * Contains the heartbeat interval.
 */
data class HelloData(
    @SerializedName("heartbeat_interval") val heartbeatInterval: Long
)

/**
 * Identify payload sent to authenticate (opcode 2).
 */
data class IdentifyPayload(
    @SerializedName("token") val token: String,
    @SerializedName("properties") val properties: IdentifyProperties,
    @SerializedName("presence") val presence: PresenceUpdate? = null
)

data class IdentifyProperties(
    @SerializedName("os") val os: String = "android",
    @SerializedName("browser") val browser: String = "Music Presence",
    @SerializedName("device") val device: String = "Music Presence"
)

/**
 * Presence Update payload (opcode 3).
 * Sent to update the user's activity/status.
 */
data class PresenceUpdate(
    @SerializedName("since") val since: Long? = null,
    @SerializedName("activities") val activities: List<Activity>,
    @SerializedName("status") val status: String = "online",
    @SerializedName("afk") val afk: Boolean = false
)

/**
 * Discord Activity object for Rich Presence.
 */
data class Activity(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: Int,
    @SerializedName("details") val details: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("timestamps") val timestamps: ActivityTimestamps? = null,
    @SerializedName("assets") val assets: ActivityAssets? = null,
    @SerializedName("application_id") val applicationId: String? = null
) {
    companion object {
        /** Activity type constants */
        const val TYPE_PLAYING = 0
        const val TYPE_STREAMING = 1
        const val TYPE_LISTENING = 2
        const val TYPE_WATCHING = 3
        const val TYPE_COMPETING = 5
    }
}

data class ActivityTimestamps(
    @SerializedName("start") val start: Long? = null,
    @SerializedName("end") val end: Long? = null
)

data class ActivityAssets(
    @SerializedName("large_image") val largeImage: String? = null,
    @SerializedName("large_text") val largeText: String? = null,
    @SerializedName("small_image") val smallImage: String? = null,
    @SerializedName("small_text") val smallText: String? = null
)

/**
 * Resume payload (opcode 6) for reconnection.
 */
data class ResumePayload(
    @SerializedName("token") val token: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("seq") val seq: Int
)

/** Gateway opcodes */
object GatewayOpcode {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val VOICE_STATE_UPDATE = 4
    const val RESUME = 6
    const val RECONNECT = 7
    const val REQUEST_GUILD_MEMBERS = 8
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}
