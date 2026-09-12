package com.musicpresence.android.discord

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * Manages the WebSocket connection to Discord's Gateway API.
 *
 * Handles:
 * - Connection to wss://gateway.discord.gg
 * - Authentication via IDENTIFY (opcode 2)
 * - Heartbeat loop to keep connection alive
 * - Sending presence updates (opcode 3)
 * - Auto-reconnect with exponential backoff
 */
class DiscordGateway(
    private val token: String,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "DiscordGateway"
        private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
        private const val MAX_RECONNECT_DELAY = 60_000L // 60 seconds
        private const val INITIAL_RECONNECT_DELAY = 1_000L // 1 second
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var sequenceNumber: Int? = null
    private var sessionId: String? = null
    private var resumeGatewayUrl: String? = null
    private var reconnectAttempts = 0
    private var lastPresence: PresenceUpdate? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var isResuming = false

    /**
     * Connect to the Discord Gateway.
     */
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED) {
            Log.d(TAG, "Already connecting or connected, skipping")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        val url = resumeGatewayUrl ?: GATEWAY_URL

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, GatewayListener())
        Log.i(TAG, "Connecting to Discord Gateway: $url")
    }

    /**
     * Disconnect from the Gateway.
     */
    fun disconnect() {
        Log.i(TAG, "Disconnecting from Discord Gateway")
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        reconnectAttempts = 0
    }

    /**
     * Send a presence update with the given activities.
     */
    fun updatePresence(presence: PresenceUpdate) {
        lastPresence = presence

        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Not connected, queuing presence update")
            return
        }

        val payload = GatewayPayload(
            op = GatewayOpcode.PRESENCE_UPDATE,
            d = presence
        )

        sendPayload(payload)
        Log.d(TAG, "Sent presence update: ${presence.activities.firstOrNull()?.details}")
    }

    /**
     * Clear the presence (set no activity).
     */
    fun clearPresence() {
        val emptyPresence = PresenceUpdate(
            activities = emptyList(),
            status = "online"
        )
        lastPresence = emptyPresence
        updatePresence(emptyPresence)
    }

    private fun sendPayload(payload: GatewayPayload) {
        val json = gson.toJson(payload)
        webSocket?.send(json)
    }

    private fun handleMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val op = json.get("op").asInt
            val d = json.get("d")
            val s = json.get("s")

            // Update sequence number if present
            if (s != null && !s.isJsonNull) {
                sequenceNumber = s.asInt
            }

            when (op) {
                GatewayOpcode.HELLO -> handleHello(d.asJsonObject)
                GatewayOpcode.HEARTBEAT_ACK -> handleHeartbeatAck()
                GatewayOpcode.HEARTBEAT -> sendHeartbeat()
                GatewayOpcode.DISPATCH -> handleDispatch(json)
                GatewayOpcode.RECONNECT -> handleReconnect()
                GatewayOpcode.INVALID_SESSION -> handleInvalidSession(d)
                else -> Log.d(TAG, "Received opcode: $op")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: ${e.message}", e)
        }
    }

    private fun handleHello(data: JsonObject) {
        val heartbeatInterval = data.get("heartbeat_interval").asLong
        Log.i(TAG, "Received HELLO, heartbeat interval: ${heartbeatInterval}ms")

        // Start heartbeating
        startHeartbeat(heartbeatInterval)

        // Send IDENTIFY or RESUME
        if (isResuming && sessionId != null) {
            sendResume()
        } else {
            sendIdentify()
        }
    }

    private fun sendIdentify() {
        val identify = IdentifyPayload(
            token = token,
            properties = IdentifyProperties()
        )

        val payload = GatewayPayload(
            op = GatewayOpcode.IDENTIFY,
            d = identify
        )

        sendPayload(payload)
        Log.i(TAG, "Sent IDENTIFY")
    }

    private fun sendResume() {
        val resume = ResumePayload(
            token = token,
            sessionId = sessionId!!,
            seq = sequenceNumber ?: 0
        )

        val payload = GatewayPayload(
            op = GatewayOpcode.RESUME,
            d = resume
        )

        sendPayload(payload)
        Log.i(TAG, "Sent RESUME")
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            // Jitter: first heartbeat after interval * random(0, 1)
            delay((intervalMs * Math.random()).toLong())
            sendHeartbeat()

            while (isActive) {
                delay(intervalMs)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val payload = GatewayPayload(
            op = GatewayOpcode.HEARTBEAT,
            d = sequenceNumber
        )
        sendPayload(payload)
    }

    private fun handleHeartbeatAck() {
        Log.v(TAG, "Received heartbeat ACK")
    }

    private fun handleDispatch(json: JsonObject) {
        val eventName = json.get("t")?.asString
        val data = json.get("d")

        when (eventName) {
            "READY" -> handleReady(data?.asJsonObject)
            "RESUMED" -> handleResumed()
            else -> Log.v(TAG, "Dispatch event: $eventName")
        }
    }

    private fun handleReady(data: JsonObject?) {
        sessionId = data?.get("session_id")?.asString
        val resumeUrl = data?.get("resume_gateway_url")?.asString
        if (resumeUrl != null) {
            resumeGatewayUrl = resumeUrl
        }

        _connectionState.value = ConnectionState.CONNECTED
        reconnectAttempts = 0
        isResuming = false

        Log.i(TAG, "Successfully connected! Session: $sessionId")

        // Re-send last presence if we had one
        lastPresence?.let { updatePresence(it) }
    }

    private fun handleResumed() {
        _connectionState.value = ConnectionState.CONNECTED
        reconnectAttempts = 0
        isResuming = false
        Log.i(TAG, "Successfully resumed session")

        // Re-send last presence
        lastPresence?.let { updatePresence(it) }
    }

    private fun handleReconnect() {
        Log.i(TAG, "Server requested reconnect")
        isResuming = true
        webSocket?.close(4000, "Server requested reconnect")
        scheduleReconnect()
    }

    private fun handleInvalidSession(data: com.google.gson.JsonElement?) {
        val resumable = data?.asBoolean ?: false
        Log.w(TAG, "Invalid session, resumable: $resumable")

        if (resumable) {
            isResuming = true
        } else {
            // Clear session data and start fresh
            sessionId = null
            sequenceNumber = null
            resumeGatewayUrl = null
            isResuming = false
        }

        scope.launch {
            delay(if (resumable) 1000L else 5000L)
            connect()
        }
    }

    private fun scheduleReconnect() {
        if (_connectionState.value == ConnectionState.DISCONNECTED) {
            return // User explicitly disconnected
        }

        _connectionState.value = ConnectionState.RECONNECTING
        reconnectAttempts++

        val delay = (INITIAL_RECONNECT_DELAY * (1 shl minOf(reconnectAttempts, 6)))
            .coerceAtMost(MAX_RECONNECT_DELAY)

        Log.i(TAG, "Scheduling reconnect in ${delay}ms (attempt $reconnectAttempts)")

        scope.launch {
            delay(delay)
            if (_connectionState.value == ConnectionState.RECONNECTING) {
                connect()
            }
        }
    }

    private inner class GatewayListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket opened")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closing: $code $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closed: $code $reason")
            heartbeatJob?.cancel()

            if (_connectionState.value != ConnectionState.DISCONNECTED) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}", t)
            heartbeatJob?.cancel()

            if (_connectionState.value != ConnectionState.DISCONNECTED) {
                scheduleReconnect()
            }
        }
    }
}
