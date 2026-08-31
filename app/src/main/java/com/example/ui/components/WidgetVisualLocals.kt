package com.example.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Color tokens shared by every widget type on Home and Screen Saver. */
data class WidgetVisualTokens(
    val foreground: Color? = null,
    val accent: Color? = null,
    val surface: Color? = null
) {
    fun primaryOr(default: Color): Color = foreground ?: default
    fun secondaryOr(default: Color): Color = foreground?.copy(alpha = .68f) ?: default
    fun accentOr(default: Color): Color = accent ?: default
}

val LocalWidgetVisualTokens = staticCompositionLocalOf { WidgetVisualTokens() }

data class ResolvedWidgetColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color
)

@Composable
fun resolvedWidgetColors(): ResolvedWidgetColors {
    val tokens = LocalWidgetVisualTokens.current
    return ResolvedWidgetColors(
        primary = tokens.primaryOr(TextPrimary),
        secondary = tokens.secondaryOr(TextSecondary),
        accent = tokens.accentOr(TextPrimary)
    )
}

@Composable
fun resolvedWidgetSurface(default: Color): Color = LocalWidgetVisualTokens.current.surface ?: default

/** Kept as a compatibility alias while older widget renderers migrate to the unified tokens. */
val LocalWidgetForegroundColor = staticCompositionLocalOf<Color?> { null }
