package com.musicpresence.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicpresence.android.discord.DiscordGateway
import com.musicpresence.android.media.MediaInfo
import com.musicpresence.android.service.DiscordPresenceService
import com.musicpresence.android.ui.components.NowPlayingCard
import com.musicpresence.android.ui.components.StatusIndicator
import com.musicpresence.android.ui.theme.*

/**
 * Main home screen showing connection status, now-playing info, and service controls.
 */
@Composable
fun HomeScreen(
    isServiceRunning: Boolean,
    connectionState: DiscordGateway.ConnectionState,
    currentMedia: MediaInfo?,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Music Presence",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusIndicator(connectionState = connectionState)
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Service control card
        ServiceControlCard(
            isRunning = isServiceRunning,
            connectionState = connectionState,
            onStart = onStartService,
            onStop = onStopService
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Now playing card
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        NowPlayingCard(
            mediaInfo = currentMedia,
            albumArtUrl = null  // Album art URL is resolved in the service
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Discord preview card
        if (currentMedia != null && currentMedia.title.isNotBlank()) {
            Text(
                text = "DISCORD PREVIEW",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            DiscordPreviewCard(mediaInfo = currentMedia)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ServiceControlCard(
    isRunning: Boolean,
    connectionState: DiscordGateway.ConnectionState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Subtle gradient overlay when connected
            if (isRunning && connectionState == DiscordGateway.ConnectionState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AccentPrimary, AccentSecondary, AccentTertiary)
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status icon
                val iconTint = if (isRunning) AccentPrimary else TextMuted

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isRunning) AccentPrimary.copy(alpha = 0.1f)
                            else DarkSurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRunning) "Sharing your music" else "Service is stopped",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isRunning) "Your Discord status is being updated"
                    else "Tap the button below to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Start/Stop button
                Button(
                    onClick = if (isRunning) onStop else onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) DiscordRed.copy(alpha = 0.9f)
                        else AccentPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Stop Sharing" else "Start Sharing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Shows a preview of how the presence looks on Discord.
 */
@Composable
private fun DiscordPreviewCard(mediaInfo: MediaInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Simulated Discord profile card header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile picture placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DiscordBlurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = DiscordWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Listening to ${mediaInfo.playerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = DarkSurfaceVariant
            )

            // Activity details
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = mediaInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (mediaInfo.artist.isNotBlank()) {
                    Text(
                        text = "by ${mediaInfo.artist}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                if (mediaInfo.album.isNotBlank() && mediaInfo.album != mediaInfo.title) {
                    Text(
                        text = "on ${mediaInfo.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
