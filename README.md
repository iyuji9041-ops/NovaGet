# ⚡ NovaGet — Modern Android Video & Audio Downloader

<p align="center">
  <img src="android-app/app/src/main/res/drawable/ic_app_logo.png" width="128" height="128" alt="NovaGet Logo" />
</p>

<p align="center">
  <b>Fast, Private, and 100% Standalone Video & Audio Downloader for Android.</b><br>
  Built natively with Jetpack Compose, Kotlin Coroutines, and embedded yt-dlp + FFmpeg runtime.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_1.9-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a_(~53MB)-FF6F00" alt="Arch" />
  <img src="https://img.shields.io/badge/License-GPL_v3.0-blue.svg" alt="License" />
</p>

---

## ⚖️ Legal Disclaimer & Terms of Use (Must Read)

> [!CAUTION]
> **USER RESPONSIBILITY NOTICE:**  
> **NovaGet is developed strictly for educational, personal archival, and fair-use research purposes.**  
> - NovaGet **does NOT host, store, index, or distribute** any media files, video streams, or copyrighted data.
> - All media extractions, parsing, and downloads are executed **100% locally on the user's device** directly between the user's phone and the target public URL.
> - The developers and contributors of NovaGet accept **NO RESPONSIBILITY OR LIABILITY** for any misuse of this application.
> - **End users are solely responsible** for ensuring that their downloads comply with all applicable copyright laws in their jurisdiction and the Terms of Service of respective content providers (YouTube, Instagram, TikTok, Facebook, Twitter/X, etc.).

---

## ✨ Features

- 🚀 **100% Standalone (Zero Server Dependency)**: Embedded Python 3.12 + yt-dlp and static FFmpeg run directly on your phone's CPU. No external proxy servers or backend APIs required.
- ⚡ **True Multi-Threaded Acceleration**: Direct HTTP chunk downloads powered by `RandomAccessFile("rw")` across 2–4 parallel streams with persistent `.parts` pause & resume synchronization.
- 🛡️ **3-Layer Hybrid AdBlocker**: Built-in WebView browser features:
  1. *Network Layer*: Intercepts and drops known ad/tracking domains (`doubleclick`, `googlesyndication`, `googleadservices`, `an.facebook.com`, etc.) with empty 200 OK responses.
  2. *Payload Layer*: Injected JavaScript hooks `JSON.parse` and `fetch` to recursively strip YouTube ad placement objects.
  3. *DOM Layer*: Cosmetic CSS filters hiding promotional banners plus an automated video ad fast-forwarder and skip-button clicker.
- 🎨 **Cyber Glassmorphism UI**: Beautiful, fluid dark-mode design crafted with Jetpack Compose, acrylic surfaces, and neon accents.
- 🎯 **Smart Format Selector**: Proportional resolution hierarchy supporting 4K, 2K, 1080p, 720p, 480p, 360p, alongside audio extraction (MP3 @ 320k/192k, M4A, OPUS).
- 📲 **1-Tap Share Target**: Share any video link directly from YouTube, Instagram, or Twitter into NovaGet to instantly open download options.
- 🔔 **Background Download Service**: Persistent Android Foreground Service with real-time speed, progress bar, and ETA notifications.
- 📦 **Compact Binary Size**: Clean single-architecture (`arm64-v8a`) release build weighing only **~53 MB** with safe R8 optimization.

---

## 📁 Project Architecture & Structure

```
downloader/
├── android-app/                       ← Native Android Project
│   ├── app/src/main/
│   │   ├── java/com/videodownloader/app/
│   │   │   ├── MainActivity.kt        ← App entry point & Intent Share Target handler
│   │   │   ├── data/
│   │   │   │   ├── download/          ← DownloadManager, Multi-threaded engine, ForegroundService
│   │   │   │   └── local/             ← Room Database (Entity, DAO, migrations)
│   │   │   ├── engine/                ← Embedded yt-dlp & FFmpeg Native Core
│   │   │   └── ui/                    ← Jetpack Compose UI
│   │   │       ├── screens/           ← HomeScreen, DownloadsScreen, BrowserScreen, SettingsScreen
│   │   │       ├── components/        ← GlassCard, Dialogs, Capsule Bar
│   │   │       └── theme/             ← Cyber neon color schemes & typography
│   │   ├── res/                       ← App launcher icons, drawables, strings
│   │   ├── AndroidManifest.xml        ← Permissions, services, share intent filters
│   │   ├── build.gradle.kts           ← Gradle config (arm64-v8a filter, R8 rules)
│   │   └── proguard-rules.pro         ← Crash-prevention keep rules for reflection
│   ├── gradle.properties
│   └── settings.gradle.kts
├── keystore.properties.example        ← Release signing credentials template
├── LICENSE                            ← GNU General Public License v3.0
└── README.md
```

---

## 🛠️ Building From Source

NovaGet is designed to be effortlessly compiled by any developer with zero setup friction.

### Prerequisites
- **JDK 17** or higher
- **Android SDK** (API 26+)
- **Android Studio Hedgehog** or later (optional, for IDE development)

### Quick Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/<your-username>/NovaGet.git
   cd NovaGet/android-app
   ```

2. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   *The built APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.*

3. **Build Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   *The release APK will be generated at `app/build/outputs/apk/release/app-release.apk` (~53 MB).*

> [!NOTE]
> **Graceful Signing Fallback:**  
> Contributors do not need a private signing keystore. If `keystore.properties` is absent, Gradle automatically falls back to standard debug signing so anyone can build and test immediately without errors.

---

## 🔐 Keystore & Open-Source Security

If you are a maintainer building official release binaries:
1. Copy `keystore.properties.example` to `keystore.properties`.
2. Fill in your private `.jks` path and credentials.
3. `keystore.properties` and `*.jks` files are ignored by `.gitignore` and will never be committed to GitHub.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.  
See the [LICENSE](LICENSE) file for full details.
