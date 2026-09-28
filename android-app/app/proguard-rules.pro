# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data models (Gson serialization)
-keep class com.videodownloader.app.data.model.** { *; }

# Keep Retrofit interfaces
-keep interface com.videodownloader.app.data.api.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Coil
-dontwarn coil.**

# youtubedl-android & FFmpeg Native Runtime
-keep class com.yausername.youtubedl_android.** { *; }
-keep class io.github.junkfood02.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class io.github.junkfood02.ffmpeg.** { *; }
-dontwarn com.yausername.**
-dontwarn io.github.junkfood02.**

# Jackson reflection keep rules
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# App Models, Room Entities, DAOs, and Engine
-keep class com.videodownloader.app.data.** { *; }
-keep class com.videodownloader.app.engine.** { *; }

