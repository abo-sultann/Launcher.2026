package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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

/**
 * Compact floating dock designed for a wallpaper-first 1024x600 home screen.
 * Inactive destinations remain icon-only; only the active destination expands to show text.
 */
@Composable
fun BottomCarNavBar(
    currentScreen: CarScreen,
    onScreenSelected: (CarScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = CarbonDark.copy(alpha = .82f),
            shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, CarbonCardBorder.copy(alpha = .75f)),
            shadowElevation = 5.dp,
            modifier = Modifier.fillMaxWidth(.90f).height(45.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CarScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    DockButton(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = { onScreenSelected(screen) },
                        modifier = Modifier.weight(if (isSelected) 1.35f else .78f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockButton(
    screen: CarScreen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) CyanNeon.copy(alpha = .16f) else Color.Transparent
    val contentColor = if (isSelected) CyanNeon else TextSecondary
    val borderColor = if (isSelected) CyanNeon.copy(alpha = .45f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .then(if (isSelected) Modifier.background(CyanNeon.copy(alpha = .02f), RoundedCornerShape(13.dp)) else Modifier)
            .clickable { onClick() }
            .testTag(screen.tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isSelected) 29.dp else 31.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (isSelected) Modifier.background(CyanNeon.copy(alpha = .12f)) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.arabicTitle,
                    tint = contentColor,
                    modifier = Modifier.size(if (isSelected) 19.dp else 20.dp)
                )
            }
            if (isSelected) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = screen.arabicTitle,
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}
