plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.videodownloader.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.videodownloader.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

// Force transitive dependencies to versions that have AARs in the local maven repo.
// appcompat:1.6.1 transitively requires drawerlayout:1.0.0 and (via coordinatorlayout)
// customview:1.0.0, but only POMs exist for those versions locally.
configurations.all {
    resolutionStrategy {
        force("androidx.drawerlayout:drawerlayout:1.1.1")
        force("androidx.customview:customview:1.1.0")
    }
}

dependencies {
    // Core AndroidX — versions from local repo
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")

    // Compose — version 1.3.0 (local repo)
    implementation("androidx.compose.ui:ui:1.3.0")
    implementation("androidx.compose.ui:ui-graphics:1.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.3.0")
    implementation("androidx.compose.foundation:foundation:1.3.0")
    implementation("androidx.compose.material:material:1.3.0")
    implementation("androidx.compose.material:material-icons-core:1.3.0")
    implementation("androidx.compose.animation:animation:1.3.0")
    implementation("androidx.compose.runtime:runtime:1.3.0")

    // Material3 — 1.0.0 (local repo)
    implementation("androidx.compose.material3:material3:1.0.0")

    // Navigation Compose — 2.5.3 (local repo)
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.navigation:navigation-runtime-ktx:2.5.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // Standalone yt-dlp & FFmpeg for Android with Python 3.12 (Runs 100% on Phone CPU)
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:common:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.11.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.11.1")
    implementation("commons-io:commons-io:2.16.1")
    implementation("org.apache.commons:commons-compress:1.21")

    // Gson (for JSON parsing)
    implementation("com.google.code.gson:gson:2.10.1")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.3.0")

    // Test
    testImplementation("junit:junit:4.13.2")
}
