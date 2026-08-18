package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// White & Neon Purple Color Palette
// - Background: Pure White / Off-White (#FFFFFF / #FAFAFC)
// - Container & Chat Boxes: Light Gray (#F3F4F6 / #E5E7EB)
// - Accent Colors: ONLY Neon Purple (#9333EA / #A855F7 / #7C3AED / #8B5CF6)
// - Text Colors: Deep Charcoal (#1E1B4B) for high-contrast readability on white

val WhiteBackground = Color(0xFFFFFFFF)
val OffWhiteCanvas = Color(0xFFFAFAFC)
val DarkBackground = Color(0xFFFFFFFF) // Mapped to White

val LightContainer = Color(0xFFF3F4F6)       // Chat input & light card container
val LightContainerElevated = Color(0xFFF5F3FF)// Soft purple-tinted container
val LightBorder = Color(0xFFE5E7EB)
val LightBorderVibrant = Color(0xFFA855F7)   // Neon purple border

val GlassSurface = Color(0xFFF3F4F6)
val GlassSurfaceElevated = Color(0xFFF5F3FF)
val GlassBorder = Color(0xFFE5E7EB)
val GlassBorderVibrant = Color(0xFFA855F7)
val GlassWhiteBorder = Color(0xFFE5E7EB)
val GlassWhiteBorderVibrant = Color(0xFFA855F7)
val GlassWhiteSurface = Color(0xFFF3F4F6)

val NeonPurplePrimary = Color(0xFF9333EA)    // Pure Neon Purple
val NeonPurpleLight = Color(0xFFA855F7)      // Light Neon Purple
val NeonPurpleDark = Color(0xFF7C3AED)       // Deep Neon Purple
val ElectricViolet = Color(0xFF8B5CF6)       // Vivid Neon Purple Accent

val GlowingCyan = Color(0xFF9333EA)          // Mapped to Neon Purple to maintain theme consistency
val NeonPink = Color(0xFFA855F7)             // Mapped to Neon Purple to maintain theme consistency

val TextPrimary = Color(0xFF1E1B4B)          // Deep charcoal/navy text for high contrast on white
val TextSecondary = Color(0xFF4C1D95)        // Soft dark purple text
val TextMuted = Color(0xFF6B7280)            // Muted gray text

val UserBubbleGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF9333EA), Color(0xFFA855F7))
)

val GlassCardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF3F4F6), Color(0xFFF9FAFB))
)

val GlassCardElevatedGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE))
)

val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFAFAFC))
)
