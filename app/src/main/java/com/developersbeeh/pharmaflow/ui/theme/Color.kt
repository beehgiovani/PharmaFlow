package com.developersbeeh.pharmaflow.ui.theme

import androidx.compose.ui.graphics.Color

// --- PALETA PREMIUM PHARMAFLOW (Glassmorphism & Vibrant) ---

// Primary Gradient Strategy: Vivid Teal (#00E5FF) -> Deep Blue (#2979FF)
// We define the core solid colors here to be used in gradients or fallbacks.
val BrandTeal = Color(0xFF00E5FF)
val BrandBlue = Color(0xFF2979FF)
val BrandPurple = Color(0xFFD500F9)  // Secondary Accents
val BrandDarkNavy = Color(0xFF0A1929) // Deep Background

// --- LIGHT THEME (Clean & Airy) ---
val LightPrimary = Color(0xFF006CFF) // A solid blue for accessible text/icons
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0EAFF) // Very soft blue
val LightOnPrimaryContainer = Color(0xFF001F50)

val LightSecondary = Color(0xFF9C27B0) // Deep Purple
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFDE7FF)
val LightOnSecondaryContainer = Color(0xFF3B0046)

val LightTertiary = Color(0xFF00C853) // Success/Eco Green
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFB9F6CA)
val LightOnTertiaryContainer = Color(0xFF00210B)

val LightBackground = Color(0xFFF4F7FC) // Cool Grey-Blue tinted white
val LightSurface = Color(0xFFFFFFFF)    // Pure white for cards
val LightSurfaceVariant = Color(0xFFE1E6EF)
val LightOnSurface = Color(0xFF1A1C1E)
val LightOnSurfaceVariant = Color(0xFF44474E)
val LightError = Color(0xFFBA1A1A)

// --- DARK THEME (Deep & Neon) ---
val DarkPrimary = Color(0xFF40C4FF) // Cyan-Blue Neon
val DarkOnPrimary = Color(0xFF00344F)
val DarkPrimaryContainer = Color(0xFF004B70)
val DarkOnPrimaryContainer = Color(0xFFCAECFF)

val DarkSecondary = Color(0xFFE040FB) // Neon Purple
val DarkOnSecondary = Color(0xFF550064)
val DarkSecondaryContainer = Color(0xFF78008B)
val DarkOnSecondaryContainer = Color(0xFFFDD6FF)

val DarkTertiary = Color(0xFF69F0AE) // Neon Green
val DarkOnTertiary = Color(0xFF003914)

val DarkBackground = Color(0xFF0A1929) // Deep Navy (Not pure black)
val DarkSurface = Color(0xFF132F4C)    // Slightly lighter navy for cards
val DarkSurfaceVariant = Color(0xFF44474E)
val DarkOnSurface = Color(0xFFE2E2E6)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
val DarkError = Color(0xFFFFB4AB)

// --- CUSTOM GLASS COLORS ---
fun glassColor(isDark: Boolean): Color {
    return if (isDark) Color(0xCC132F4C) else Color(0xCCFFFFFF) // 80% Opacity
}