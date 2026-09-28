package com.videodownloader.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Pure Material 3 Deep Obsidian & Neutral Dark Palette (No blue tint)
val M3BackgroundDark = Color(0xFF0C0D12) // Pure dark obsidian background
val M3SurfaceDark = Color(0xFF13141C)    // Dark graphite surface
val M3SurfaceContainer = Color(0xFF1A1C26) // Elevated container
val M3SurfaceContainerHigh = Color(0xFF262836) // High container

val GlassBackgroundDark = M3BackgroundDark
val GlassSurfaceDark = M3SurfaceDark
val GlassSurfaceElevated = M3SurfaceContainer

// Frosted Acrylic Colors (Pure white translucent layers over dark canvas)
val GlassAcrylicBase = Color(0x18FFFFFF) // 10% frosted glass
val GlassAcrylicElevated = Color(0x24FFFFFF) // 14% frosted glass
val GlassAcrylicHighlight = Color(0x32FFFFFF) // 20% frosted glass
val GlassAcrylicModal = Color(0xF613141C) // 96% deep obsidian glass modal

// Specular Glass Border Gradients (Frosted white light sheen)
val GlassBorderStart = Color(0x60FFFFFF) // Specular highlight rim matching NovaPill
val GlassBorderEnd = Color(0x20FFFFFF)   // Subtle glass edge matching NovaPill
val GlassBorderCyan = Color(0x60FFFFFF)  // Neutral glass sheen
val GlassBorderViolet = Color(0x708B5CF6) // Elegant glass violet

// Accents (Refined Material 3 palette)
val NeonCyan = Color(0xFF8B5CF6)         // Refined violet accent
val NeonCyanGlow = Color(0xFFA855F7)     // Soft violet glow
val NeonViolet = Color(0xFF8B5CF6)       // Signature violet
val NeonPurple = Color(0xFFA855F7)       // Lavender
val NeonPink = Color(0xFFEC4899)         // Rose
val NeonRose = Color(0xFFF43F5E)
val NeonEmerald = Color(0xFF10B981)      // Success green
val NeonAmber = Color(0xFFF59E0B)        // Active amber

// High contrast typography
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// Gradient Brushes
val GlassLinearBorderBrush = Brush.linearGradient(
    colors = listOf(GlassBorderStart, GlassBorderEnd)
)

val NeonAccentBrush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
)

val NeonWarmBrush = Brush.horizontalGradient(
    colors = listOf(NeonPink, NeonAmber)
)

val GlassCardBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x28FFFFFF),
        Color(0x0EFFFFFF)
    )
)
