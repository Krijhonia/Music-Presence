# Music Presence for Android

Show your friends on Discord what music you're listening to — from any Android music player.

The Android equivalent of [Music Presence for Desktop](https://github.com/ungive/discord-music-presence).

## Features

- **Works with any music player** — Apple Music, Spotify, YouTube Music, Deezer, TIDAL, and more
- **Album art display** — Automatically finds and displays correct album covers via iTunes Search API
- **Playback position** — Shows real-time progress on your Discord status
- **Per-player control** — Enable or disable specific music apps
- **Auto-start on boot** — Optionally start sharing when your phone starts
- **Secure** — Discord token is encrypted with Android Keystore

## Setup

### Prerequisites

1. **Discord Developer Application**
   - Go to [discord.com/developers/applications](https://discord.com/developers/applications)
   - Click "New Application"
   - Name it (e.g., "Music" or "Apple Music") — this name appears as your status
   - Copy the **Application ID** from the General Information page

2. **Discord Token**
   - Open Discord in a web browser
   - Press F12 → Network tab
   - Send a message and look for the "Authorization" header value

### Installation

1. Open the project in Android Studio
2. Build and install on your device:
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
3. Open the app and follow the setup wizard
4. Grant **Notification Access** when prompted
5. Tap **Start Sharing** and play some music!

## Architecture

```
┌─────────────────────────────────────────────┐
│                   Android                    │
│                                             │
│  ┌─────────────┐    ┌────────────────────┐  │
│  │ Music Player │───▶│ MediaDetectionSvc  │  │
│  │ (any app)    │    │ (NotificationLsnr) │  │
│  └─────────────┘    └────────┬───────────┘  │
│                              │               │
│                    ┌─────────▼───────────┐   │
│                    │ DiscordPresenceSvc   │   │
│                    │ (Foreground Service) │   │
│                    └─────────┬───────────┘   │
│                              │               │
│                    ┌─────────▼───────────┐   │
│                    │  Discord Gateway     │   │
│                    │  (WebSocket Client)  │   │
│                    └─────────┬───────────┘   │
└──────────────────────────────┼───────────────┘
                               │ wss://
                    ┌──────────▼───────────┐
                    │   Discord Servers     │
                    │   (Rich Presence)     │
                    └──────────────────────┘
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Network**: OkHttp WebSocket
- **Serialization**: Gson
- **Image Loading**: Coil
- **Security**: EncryptedSharedPreferences (Android Keystore)

## Project Structure

```
app/src/main/java/com/musicpresence/android/
├── MainActivity.kt              # Main entry point with navigation
├── MusicPresenceApp.kt          # Application class
├── data/
│   └── PreferencesManager.kt    # Encrypted prefs for token + settings
├── discord/
│   ├── DiscordGateway.kt        # WebSocket connection to Discord
│   ├── DiscordModels.kt         # Gateway payload data classes
│   └── PresenceBuilder.kt       # Builds Rich Presence from media info
├── media/
│   ├── AlbumArtResolver.kt      # iTunes Search API for cover art URLs
│   ├── MediaInfo.kt             # Track info data class
│   └── MediaSessionDetector.kt  # MediaSession monitoring
├── receiver/
│   └── BootReceiver.kt          # Auto-start on boot
├── service/
│   ├── DiscordPresenceService.kt # Foreground service orchestrator
│   └── MediaDetectionService.kt  # NotificationListenerService
└── ui/
    ├── components/
    │   ├── NowPlayingCard.kt     # Now-playing display card
    │   └── StatusIndicator.kt    # Connection status dot
    ├── screens/
    │   ├── HomeScreen.kt         # Main dashboard
    │   ├── SettingsScreen.kt     # App settings
    │   └── SetupScreen.kt       # First-run setup wizard
    └── theme/
        ├── Color.kt              # Discord-inspired color palette
        ├── Theme.kt              # Material 3 dark theme
        └── Type.kt               # Typography scale
```

## ⚠️ Important Notes

- This app uses your Discord **user token** to set your status via the Gateway API. This is technically against Discord's Terms of Service (self-botting). Use at your own risk.
- Your token is encrypted with AES-256-GCM via Android Keystore and never leaves your device.
- The app requires **Notification Access** permission to detect media sessions from other apps.

## License

See [LICENSE](LICENSE) for details.
