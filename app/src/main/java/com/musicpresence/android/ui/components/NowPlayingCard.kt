package com.musicpresence.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicpresence.android.media.MediaInfo
import com.musicpresence.android.ui.theme.*

/**
 * Card showing the currently playing track with album art,
 * styled to look like a Discord Rich Presence preview.
 */
@Composable
fun NowPlayingCard(
    mediaInfo: MediaInfo?,
    albumArtUrl: String?,
    modifier: Modifier = Modifier
) {
    val hasMedia = mediaInfo != null && mediaInfo.title.isNotBlank()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        AnimatedContent(
            targetState = hasMedia,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "nowPlayingContent"
        ) { isPlaying ->
            if (isPlaying && mediaInfo != null) {
                PlayingContent(mediaInfo, albumArtUrl)
            } else {
                IdleContent()
            }
        }
    }
}

@Composable
private fun PlayingContent(
    mediaInfo: MediaInfo,
    albumArtUrl: String?
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // "NOW PLAYING" label
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelSmall,
            color = AccentPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (albumArtUrl != null) {
                    AsyncImage(
                        model = albumArtUrl,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Track info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mediaInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = mediaInfo.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (mediaInfo.album.isNotBlank() && mediaInfo.album != mediaInfo.title) {
                    Text(
                        text = mediaInfo.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Player name badge
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                AccentPrimary.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mediaInfo.playerName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                    }
                }
            }
        }

        // Progress bar (if duration is known)
        if (mediaInfo.duration > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            val progress = (mediaInfo.position.toFloat() / mediaInfo.duration.toFloat())
                .coerceIn(0f, 1f)

            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentPrimary,
                    trackColor = DarkSurfaceVariant,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(mediaInfo.position),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = formatDuration(mediaInfo.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated music bars
            MusicBarsAnimation()

            Text(
                text = "No music playing",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )

            Text(
                text = "Play a song and it will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MusicBarsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "musicBars")

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(32.dp)
    ) {
        repeat(4) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600 + (index * 150),
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentPrimary.copy(alpha = 0.6f),
                                AccentPrimary.copy(alpha = 0.2f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Format milliseconds to mm:ss.
 */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
