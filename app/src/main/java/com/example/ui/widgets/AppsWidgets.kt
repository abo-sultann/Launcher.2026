package com.example.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.components.resolvedWidgetSurface
import com.example.ui.theme.*

@Composable
fun AppsWidget(
    style: WidgetStyle,
    installedApps: List<AppItem>,
    onLaunchApp: (String) -> Unit,
    onOpenAppDrawer: () -> Unit,
    interactionEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val widgetColors = resolvedWidgetColors()
    val displayApps = installedApps.filter { !it.isHidden }
    val favoriteApps = displayApps.filter { it.isFavorite }.ifEmpty { displayApps.take(8) }

    Box(
        modifier = modifier.fillMaxSize().padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.APPS_HORIZONTAL_DOCK -> {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(favoriteApps.take(10), key = { it.packageName }) { app ->
                        AppShortcutItem(app, true, interactionEnabled) { onLaunchApp(app.packageName) }
                    }
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = interactionEnabled) { onOpenAppDrawer() }.padding(4.dp).testTag("btn_widget_all_apps")
                        ) {
                            Surface(
                                color = widgetColors.accent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.accent),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Apps, "كل التطبيقات", tint = widgetColors.accent, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text("التطبيقات", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = widgetColors.primary)
                        }
                    }
                }
            }

            WidgetStyle.APPS_ICONS_ONLY -> {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    favoriteApps.take(5).forEach { app -> AppShortcutItem(app, false, interactionEnabled) { onLaunchApp(app.packageName) } }
                }
            }

            WidgetStyle.APPS_GRID_2X2 -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    val apps4 = favoriteApps.take(4)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        apps4.take(2).forEach { app -> AppShortcutItem(app, true, interactionEnabled) { onLaunchApp(app.packageName) } }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        apps4.drop(2).take(2).forEach { app -> AppShortcutItem(app, true, interactionEnabled) { onLaunchApp(app.packageName) } }
                    }
                }
            }

            WidgetStyle.APPS_GRID_3X2 -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                    val apps6 = favoriteApps.take(6)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        apps6.take(3).forEach { app -> AppShortcutItem(app, false, interactionEnabled) { onLaunchApp(app.packageName) } }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        apps6.drop(3).take(3).forEach { app -> AppShortcutItem(app, false, interactionEnabled) { onLaunchApp(app.packageName) } }
                    }
                }
            }

            WidgetStyle.APPS_FAVORITES_CARD -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.secondary.copy(alpha = .45f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("المفضلة السريعة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = widgetColors.secondary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            favoriteApps.take(4).forEach { app -> AppShortcutItem(app, true, interactionEnabled) { onLaunchApp(app.packageName) } }
                        }
                    }
                }
            }

            else -> {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    favoriteApps.take(4).forEach { app -> AppShortcutItem(app, true, interactionEnabled) { onLaunchApp(app.packageName) } }
                }
            }
        }
    }
}

@Composable
private fun AppShortcutItem(app: AppItem, showLabel: Boolean, interactionEnabled: Boolean, onClick: () -> Unit) {
    val widgetColors = resolvedWidgetColors()
    val imageBitmap = remember(app.packageName, app.iconBitmap) { app.iconBitmap?.asImageBitmap() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = interactionEnabled, onClick = onClick).padding(4.dp).testTag("app_shortcut_${app.packageName}")
    ) {
        Surface(
            color = resolvedWidgetSurface(CarbonSurface),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.secondary.copy(alpha = .45f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (imageBitmap != null) {
                    Image(bitmap = imageBitmap, contentDescription = app.label, modifier = Modifier.size(34.dp))
                } else {
                    Icon(Icons.Default.Android, contentDescription = app.label, tint = widgetColors.accent, modifier = Modifier.size(24.dp))
                }
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(2.dp))
            Text(app.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = widgetColors.primary, maxLines = 1)
        }
    }
}
