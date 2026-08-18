package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightNeonPurpleColorScheme = lightColorScheme(
    primary = NeonPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = LightContainerElevated,
    onPrimaryContainer = NeonPurpleDark,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = LightContainer,
    onSecondaryContainer = TextSecondary,
    tertiary = NeonPurpleLight,
    onTertiary = Color.White,
    background = WhiteBackground,
    onBackground = TextPrimary,
    surface = OffWhiteCanvas,
    onSurface = TextPrimary,
    surfaceVariant = LightContainer,
    onSurfaceVariant = TextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorderVibrant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightNeonPurpleColorScheme,
        typography = Typography,
        content = content
    )
}
