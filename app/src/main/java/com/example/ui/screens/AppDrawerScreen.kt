package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

private enum class AppDrawerFilter(val title: String) { ALL("الكل"), FAVORITES("المفضلة ⭐"), HIDDEN("المخفية 👁️") }

@Composable
fun AppDrawerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val apps by viewModel.installedApps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppDrawerFilter.ALL) }

    val filtered = remember(apps, query, filter) {
        apps.filter { a ->
            val f = when (filter) {
                AppDrawerFilter.ALL -> !a.isHidden
                AppDrawerFilter.FAVORITES -> a.isFavorite && !a.isHidden
                AppDrawerFilter.HIDDEN -> a.isHidden
            }
            f && (query.isBlank() || a.label.contains(query, true) || a.packageName.contains(query, true))
        }
    }
    val showAndroidSettings = filter == AppDrawerFilter.ALL &&
        (query.isBlank() || "إعدادات أندرويد".contains(query, true) || "settings".contains(query, true))

    Column(modifier.fillMaxSize().padding(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).height(50.dp),
                singleLine = true,
                placeholder = { Text("بحث عن تطبيق") }
            )
            AppDrawerFilter.values().forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.title) })
            }
        }

        Spacer(Modifier.height(7.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.appDrawerColumns.coerceIn(2, 8)),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (showAndroidSettings) {
                item(key = "__android_system_settings__") {
                    AndroidSettingsCard(
                        launch = viewModel::launchAndroidSettings,
                        showLabel = settings.showAppLabels,
                        iconSizeDp = settings.iconSizeDp
                    )
                }
            }

            items(filtered, key = { it.packageName }) { app ->
                AppDrawerCard(
                    app = app,
                    launch = { viewModel.launchApp(app.packageName) },
                    favorite = { viewModel.toggleAppFavorite(app.packageName) },
                    hide = { viewModel.toggleAppHidden(app.packageName) },
                    showLabel = settings.showAppLabels,
                    iconSizeDp = settings.iconSizeDp
                )
            }
        }
    }
}

@Composable
private fun AndroidSettingsCard(launch: () -> Unit, showLabel: Boolean, iconSizeDp: Int) {
    val iconSize = iconSizeDp.coerceIn(40, 110)
    val cardHeight = (iconSize + if (showLabel) 50 else 30).coerceIn(92, 160)
    Card(
        Modifier
            .fillMaxWidth()
            .height(cardHeight.dp)
            .border(1.dp, CyanNeon.copy(alpha = .65f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = launch)
            .testTag("app_card_android_settings"),
        colors = CardDefaults.cardColors(containerColor = CarbonCard)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Settings, "إعدادات أندرويد", tint = CyanNeon, modifier = Modifier.size(iconSize.dp))
            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text("إعدادات أندرويد", color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AppDrawerCard(
    app: AppItem,
    launch: () -> Unit,
    favorite: () -> Unit,
    hide: () -> Unit,
    showLabel: Boolean,
    iconSizeDp: Int
) {
    val imageBitmap = remember(app.packageName, app.iconBitmap) { app.iconBitmap?.asImageBitmap() }
    val iconSize = iconSizeDp.coerceIn(40, 110)
    val cardHeight = (iconSize + if (showLabel) 50 else 30).coerceIn(92, 160)

    Card(
        Modifier
            .fillMaxWidth()
            .height(cardHeight.dp)
            .border(1.dp, if (app.isFavorite) AmberRacing else CarbonCardBorder, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = launch)
            .testTag("app_card_${app.packageName}"),
        colors = CardDefaults.cardColors(containerColor = CarbonCard)
    ) {
        Box(Modifier.fillMaxSize().padding(5.dp)) {
            Row(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = favorite, modifier = Modifier.size(25.dp)) {
                    Icon(
                        if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        null,
                        tint = if (app.isFavorite) AmberRacing else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = hide, modifier = Modifier.size(25.dp)) {
                    Icon(Icons.Default.VisibilityOff, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (imageBitmap != null) {
                    Image(bitmap = imageBitmap, contentDescription = app.label, modifier = Modifier.size(iconSize.dp))
                } else {
                    Icon(Icons.Default.Android, app.label, tint = CyanNeon, modifier = Modifier.size((iconSize - 4).coerceAtLeast(36).dp))
                }
                if (showLabel) {
                    Spacer(Modifier.height(4.dp))
                    Text(app.label, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
