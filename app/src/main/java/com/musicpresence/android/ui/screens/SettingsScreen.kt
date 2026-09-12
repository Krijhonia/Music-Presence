package com.musicpresence.android.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicpresence.android.data.PreferencesManager
import com.musicpresence.android.discord.Activity
import com.musicpresence.android.ui.theme.*

/**
 * Settings screen for configuring the app behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: PreferencesManager,
    onNavigateBack: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current

    var activityType by remember { mutableIntStateOf(prefs.activityType) }
    var showAlbumArt by remember { mutableStateOf(prefs.showAlbumArt) }
    var showTimestamp by remember { mutableStateOf(prefs.showTimestamp) }
    var autoStart by remember { mutableStateOf(prefs.autoStart) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkBackground,
                titleContentColor = TextPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // --- Display Section ---
            SectionHeader("Display")

            SettingsCard {
                // Activity type
                SettingsDropdown(
                    title = "Activity Type",
                    description = "How your status appears on Discord",
                    selectedValue = if (activityType == Activity.TYPE_LISTENING) "Listening to" else "Playing",
                    options = listOf("Listening to" to Activity.TYPE_LISTENING, "Playing" to Activity.TYPE_PLAYING),
                    onValueChanged = {
                        activityType = it
                        prefs.activityType = it
                    }
                )

                HorizontalDivider(color = DarkSurfaceVariant)

                // Album art toggle
                SettingsToggle(
                    title = "Show Album Art",
                    description = "Display album cover on your status",
                    icon = Icons.Default.Image,
                    checked = showAlbumArt,
                    onCheckedChange = {
                        showAlbumArt = it
                        prefs.showAlbumArt = it
                    }
                )

                HorizontalDivider(color = DarkSurfaceVariant)

                // Timestamp toggle
                SettingsToggle(
                    title = "Show Playback Position",
                    description = "Display elapsed / remaining time",
                    icon = Icons.Default.Timer,
                    checked = showTimestamp,
                    onCheckedChange = {
                        showTimestamp = it
                        prefs.showTimestamp = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Behavior Section ---
            SectionHeader("Behavior")

            SettingsCard {
                SettingsToggle(
                    title = "Auto-Start on Boot",
                    description = "Start sharing when your phone boots up",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = autoStart,
                    onCheckedChange = {
                        autoStart = it
                        prefs.autoStart = it
                    }
                )

                HorizontalDivider(color = DarkSurfaceVariant)

                // Notification access
                SettingsAction(
                    title = "Notification Access",
                    description = if (isNotificationListenerEnabled(context))
                        "Permission granted" else "Permission required",
                    icon = Icons.Default.Notifications,
                    actionIcon = Icons.Default.OpenInNew,
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Account Section ---
            SectionHeader("Account")

            SettingsCard {
                SettingsAction(
                    title = "Disconnect & Clear Data",
                    description = "Remove your Discord token and reset settings",
                    icon = Icons.Default.Logout,
                    tintColor = DiscordRed,
                    onClick = { showDisconnectDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- About Section ---
            SectionHeader("About")

            SettingsCard {
                SettingsInfo(
                    title = "Version",
                    value = "1.0.0"
                )

                HorizontalDivider(color = DarkSurfaceVariant)

                SettingsInfo(
                    title = "Inspired by",
                    value = "Music Presence for Desktop"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Disconnect confirmation dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove your Discord token and reset all settings. You'll need to set up the app again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectDialog = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DiscordRed)
                ) {
                    Text("Disconnect", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DiscordWhite,
                checkedTrackColor = AccentPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsAction(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: androidx.compose.ui.graphics.Color = AccentPrimary,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ChevronRight,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tintColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (tintColor == DiscordRed) DiscordRed else TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDropdown(
    title: String,
    description: String,
    selectedValue: String,
    options: List<Pair<String, Int>>,
    onValueChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentSecondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = AccentSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = selectedValue,
                    color = AccentSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = AccentSecondary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = DarkCardElevated
            ) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label, color = TextPrimary) },
                        onClick = {
                            onValueChanged(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsInfo(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}
