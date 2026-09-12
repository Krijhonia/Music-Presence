# Proguard rules for Music Presence

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.musicpresence.android.discord.** { *; }
-keep class com.musicpresence.android.media.MediaInfo { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
