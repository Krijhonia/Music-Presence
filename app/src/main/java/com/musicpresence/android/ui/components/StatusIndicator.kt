package com.musicpresence.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.musicpresence.android.discord.DiscordGateway
import com.musicpresence.android.ui.theme.*

/**
 * Animated status indicator showing the Discord connection state.
 * Features a pulsing dot and descriptive text.
 */
@Composable
fun StatusIndicator(
    connectionState: DiscordGateway.ConnectionState,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = when (connectionState) {
            DiscordGateway.ConnectionState.CONNECTED -> StatusOnline
            DiscordGateway.ConnectionState.CONNECTING -> StatusIdle
            DiscordGateway.ConnectionState.RECONNECTING -> StatusIdle
            DiscordGateway.ConnectionState.DISCONNECTED -> StatusOffline
        },
        animationSpec = tween(durationMillis = 300),
        label = "statusColor"
    )

    val statusText = when (connectionState) {
        DiscordGateway.ConnectionState.CONNECTED -> "Connected to Discord"
        DiscordGateway.ConnectionState.CONNECTING -> "Connecting…"
        DiscordGateway.ConnectionState.RECONNECTING -> "Reconnecting…"
        DiscordGateway.ConnectionState.DISCONNECTED -> "Disconnected"
    }

    // Pulse animation for connecting states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isAnimating = connectionState == DiscordGateway.ConnectionState.CONNECTING ||
            connectionState == DiscordGateway.ConnectionState.RECONNECTING

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Pulse ring (only when connecting)
            if (isAnimating) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.3f))
                )
            }

            // Solid dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = dotColor
        )
    }
}
