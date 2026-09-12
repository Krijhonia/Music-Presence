package com.musicpresence.android.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musicpresence.android.data.PreferencesManager
import com.musicpresence.android.service.MediaDetectionService
import com.musicpresence.android.ui.theme.*

/**
 * Setup wizard screen that guides the user through initial configuration:
 * 1. Welcome
 * 2. Discord token
 * 3. Application ID
 * 4. Notification access permission
 * 5. Complete
 */
@Composable
fun SetupScreen(
    prefs: PreferencesManager,
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkSurface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Step indicator
            StepIndicator(
                currentStep = currentStep,
                totalSteps = 4
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step content with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "setupStep"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(onNext = { currentStep = 1 })
                    1 -> TokenStep(
                        prefs = prefs,
                        onNext = { currentStep = 2 },
                        onBack = { currentStep = 0 }
                    )
                    2 -> AppIdStep(
                        prefs = prefs,
                        onNext = { currentStep = 3 },
                        onBack = { currentStep = 1 }
                    )
                    3 -> NotificationStep(
                        context = context,
                        onNext = {
                            prefs.setupComplete = true
                            onSetupComplete()
                        },
                        onBack = { currentStep = 2 }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val width by animateDpAsState(
                targetValue = if (index == currentStep) 32.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.8f),
                label = "stepWidth"
            )

            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(width)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) AccentPrimary
                        else DarkSurfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated icon
        val infiniteTransition = rememberInfiniteTransition(label = "welcome")
        val iconScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "iconPulse"
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AccentPrimary, AccentSecondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = DiscordWhite,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Music Presence",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Show your friends on Discord what you're listening to",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature highlights
        FeatureItem(Icons.Default.Album, "Works with any music app", "Apple Music, Spotify, YouTube Music, and more")
        FeatureItem(Icons.Default.Image, "Album art display", "Shows the correct cover art automatically")
        FeatureItem(Icons.Default.Timer, "Playback position", "Shows real-time progress on your status")

        Spacer(modifier = Modifier.height(32.dp))

        GradientButton(
            text = "Get Started",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun TokenStep(
    prefs: PreferencesManager,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var token by remember { mutableStateOf(prefs.discordToken) }
    var showToken by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "Discord Token",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your token is stored encrypted on your device and is only used to connect to Discord.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Token input
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Discord Token") },
            placeholder = { Text("Paste your token here") },
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showToken) "Hide" else "Show"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedContainerColor = DarkCardElevated,
                unfocusedContainerColor = DarkCard
            )
        )

        // Help card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How to get your token:",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Open Discord in a web browser\n2. Press F12 to open Developer Tools\n3. Go to the Network tab\n4. Send any message in a channel\n5. Click any request and find the \"Authorization\" header\n6. Copy the token value",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Back")
            }

            GradientButton(
                text = "Next",
                onClick = {
                    prefs.discordToken = token.trim()
                    onNext()
                },
                enabled = token.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppIdStep(
    prefs: PreferencesManager,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var appId by remember { mutableStateOf(prefs.applicationId) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = AccentSecondary,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "Application ID",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Create a Discord Application to use as your presence identity. The application name becomes your status name.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = appId,
            onValueChange = { appId = it.filter { c -> c.isDigit() } },
            label = { Text("Application ID") },
            placeholder = { Text("e.g. 1234567890123456789") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentSecondary,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedContainerColor = DarkCardElevated,
                unfocusedContainerColor = DarkCard
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardElevated)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How to create an application:",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Go to discord.com/developers/applications\n2. Click \"New Application\"\n3. Name it (e.g. \"Music\" or \"Apple Music\")\n4. Copy the Application ID from the General Information page",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Back")
            }

            GradientButton(
                text = "Next",
                onClick = {
                    prefs.applicationId = appId.trim()
                    onNext()
                },
                enabled = appId.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NotificationStep(
    context: Context,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val hasPermission = remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            tint = AccentTertiary,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = "Notification Access",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Music Presence needs notification access to detect what music you're playing. This is required for the app to work.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (hasPermission.value) {
            // Permission granted
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StatusOnline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusOnline,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Notification access granted!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = StatusOnline
                    )
                }
            }
        } else {
            // Need to grant permission
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Notification Settings")
            }

            Text(
                text = "Find \"Music Presence\" in the list and enable it",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            // Refresh button
            TextButton(
                onClick = { hasPermission.value = isNotificationListenerEnabled(context) }
            ) {
                Text("I've granted access — check again", color = AccentPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Back")
            }

            GradientButton(
                text = if (hasPermission.value) "Complete Setup" else "Skip for Now",
                onClick = onNext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Gradient-filled primary button used throughout the setup flow.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPrimary,
            disabledContainerColor = AccentPrimary.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Check if the notification listener service is enabled.
 */
fun isNotificationListenerEnabled(context: Context): Boolean {
    val componentName = ComponentName(context, MediaDetectionService::class.java)
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabledListeners.contains(componentName.flattenToString())
}
