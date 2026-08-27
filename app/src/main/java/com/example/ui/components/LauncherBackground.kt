package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.model.BackgroundType
import com.example.model.LauncherSettings
import com.example.ui.theme.CarbonDark
import java.io.File

@Composable
fun LauncherBackground(
    settings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(backgroundBrush(settings.backgroundType))) {
        if (settings.backgroundType == BackgroundType.CUSTOM_IMAGE && !settings.customWallpaperPath.isNullOrBlank()) {
            val path = settings.customWallpaperPath
            val model: Any = if (path.startsWith("content://") || path.startsWith("file://")) path else File(path)
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val dim = settings.wallpaperDimPercent.coerceIn(0, 80) / 100f
            if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
    }
}

private fun backgroundBrush(type: BackgroundType): Brush = when (type) {
    BackgroundType.CYBER_CYAN -> Brush.linearGradient(listOf(Color(0xFF06131B), Color(0xFF102C3A)))
    BackgroundType.AMBER_RACING -> Brush.linearGradient(listOf(Color(0xFF1A1007), Color(0xFF30200C)))
    BackgroundType.DEEP_SPACE -> Brush.linearGradient(listOf(Color(0xFF05060A), Color(0xFF15182A)))
    BackgroundType.LUXURY_ONYX -> Brush.linearGradient(listOf(Color.Black, Color(0xFF171717)))
    else -> Brush.linearGradient(listOf(CarbonDark, Color(0xFF101722)))
}
