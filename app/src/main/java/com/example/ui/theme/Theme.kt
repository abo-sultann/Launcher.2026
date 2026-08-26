package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val CarColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = CarbonDark,
    primaryContainer = CarbonCard,
    onPrimaryContainer = CyanNeon,
    secondary = AmberRacing,
    onSecondary = CarbonDark,
    secondaryContainer = CarbonSurface,
    onSecondaryContainer = AmberRacing,
    tertiary = CrimsonSport,
    background = CarbonDark,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = TextSecondary,
    outline = CarbonCardBorder
)

@Composable
fun Launcher2026Theme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = CarColorScheme,
            typography = Typography,
            content = content
        )
    }
}
