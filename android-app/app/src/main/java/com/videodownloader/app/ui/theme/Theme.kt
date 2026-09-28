package com.videodownloader.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val M3GlassColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = M3BackgroundDark,
    primaryContainer = NeonViolet,
    onPrimaryContainer = TextPrimary,
    secondary = NeonPurple,
    onSecondary = TextPrimary,
    secondaryContainer = M3SurfaceContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonEmerald,
    onTertiary = M3BackgroundDark,
    background = M3BackgroundDark,
    onBackground = TextPrimary,
    surface = M3SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = M3SurfaceContainer,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderStart
)

@Composable
fun NovaGetTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = M3BackgroundDark.toArgb()
                window.navigationBarColor = M3BackgroundDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = M3GlassColorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for compatibility
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) = NovaGetTheme(content = content)

@Composable
fun VideoDownloaderTheme(content: @Composable () -> Unit) = NovaGetTheme(content = content)
