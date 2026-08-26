package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class CarScreen(val arabicTitle: String, val icon: ImageVector, val tag: String) {
    HOME("الرئيسية", Icons.Default.Home, "nav_tab_home"),
    APPS("التطبيقات", Icons.Default.Apps, "nav_tab_apps"),
    MUSIC("الموسيقى", Icons.Default.MusicNote, "nav_tab_music"),
    MAP("الخريطة", Icons.Default.Map, "nav_tab_map"),
    TRIP("الرحلة", Icons.Default.Speed, "nav_tab_trip"),
    SETTINGS("الإعدادات", Icons.Default.Settings, "nav_tab_settings")
}

@Composable
fun BottomCarNavBar(
    currentScreen: CarScreen,
    onScreenSelected: (CarScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = CarbonDark.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CarScreen.values().forEach { screen ->
                val isSelected = currentScreen == screen
                NavButton(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = { onScreenSelected(screen) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavButton(
    screen: CarScreen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) CyanNeon.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (isSelected) CyanNeon else TextSecondary
    val borderColor = if (isSelected) CyanNeon.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color.Transparent

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(screen.tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.arabicTitle,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = screen.arabicTitle,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                ),
                color = contentColor
            )
        }
    }
}
