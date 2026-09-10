package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

private enum class AppDrawerFilter(val title: String) { ALL("الكل"), FAVORITES("المفضلة"), HIDDEN("المخفية") }

@Composable
fun AppDrawerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val apps by viewModel.installedApps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppDrawerFilter.ALL) }
    var managing by rememberSaveable { mutableStateOf(false) }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedApp = apps.firstOrNull { it.packageName == selectedPackage }
    BackHandler(managing || selectedPackage != null) { selectedPackage = null; managing = false; if (filter == AppDrawerFilter.HIDDEN) filter = AppDrawerFilter.ALL }
    val filtered = remember(apps, query, filter) {
        apps.filter { app ->
            val include = when (filter) {
                AppDrawerFilter.ALL -> !app.isHidden
                AppDrawerFilter.FAVORITES -> app.isFavorite && !app.isHidden
                AppDrawerFilter.HIDDEN -> app.isHidden
            }
            include && (query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true))
        }
    }
    // Some car firmware does not expose the system settings activity in the app query.
    val showSystemShortcut = apps.none { it.packageName == "com.android.settings" } && filter == AppDrawerFilter.ALL &&
        (query.isBlank() || "إعدادات أندرويد settings".contains(query, true))
    Column(modifier.fillMaxSize().background(CarbonDark).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DarbakPageHeading("التطبيقات") {
            DarbakSearch(query, { query = it }, "بحث عن تطبيق", "apps_search", Modifier.width(290.dp))
            TextButton(onClick = { managing = !managing; if (!managing && filter == AppDrawerFilter.HIDDEN) filter = AppDrawerFilter.ALL },
                modifier = Modifier.heightIn(min = 52.dp).testTag("apps_manage")) {
                Icon(if (managing) Icons.Default.Check else Icons.Default.Tune, null)
                Spacer(Modifier.width(8.dp))
                Text(if (managing) "تم" else "إدارة", fontSize = 16.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppDrawerFilter.values().filter { managing || it != AppDrawerFilter.HIDDEN }.forEach { option ->
                FilterChip(selected = filter == option, onClick = { filter = option },
                    label = { Text(option.title, fontSize = 16.sp) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("apps_filter_${option.name}"),
                    shape = RoundedCornerShape(16.dp), border = null,
                    colors = FilterChipDefaults.filterChipColors(containerColor = CarbonSurface,
                        selectedContainerColor = CarbonCard, selectedLabelColor = CyanNeon))
            }
            Spacer(Modifier.weight(1f))
            Text(if (managing) "اختر تطبيقًا لإدارته" else "${filtered.size} تطبيق", color = TextSecondary, fontSize = 14.sp)
        }
        if (filtered.isEmpty() && !showSystemShortcut) {
            DarbakEmptyState(Icons.Default.Apps, if (query.isNotBlank()) "لا توجد نتائج" else "لا توجد تطبيقات هنا", Modifier.fillMaxSize())
        } else LazyVerticalGrid(
            columns = GridCells.Fixed(settings.appDrawerColumns.coerceIn(2, 8)),
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("apps_grid"),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
        ) {
            if (showSystemShortcut) item(key = "__android_system_settings__") {
                AppTile(label = "إعدادات أندرويد", iconSizeDp = settings.iconSizeDp, showLabel = settings.showAppLabels,
                    tag = "app_card_android_settings", launch = viewModel::launchAndroidSettings,
                    onManage = viewModel::launchAndroidSettings) { size ->
                    Icon(Icons.Default.Settings, null, tint = CyanNeon, modifier = Modifier.size(size))
                }
            }
            items(filtered, key = { it.packageName }) { app ->
                val bitmap = remember(app.iconBitmap) { app.iconBitmap?.asImageBitmap() }
                AppTile(label = app.label, iconSizeDp = settings.iconSizeDp, showLabel = settings.showAppLabels,
                    tag = "app_card_${app.packageName}", hidden = app.isHidden,
                    launch = { if (managing) selectedPackage = app.packageName else viewModel.launchApp(app.packageName) },
                    onManage = { selectedPackage = app.packageName }) { size ->
                    if (bitmap != null) Image(bitmap, null, modifier = Modifier.size(size))
                    else Icon(Icons.Default.Android, null, tint = CyanNeon, modifier = Modifier.size(size))
                }
            }
        }
    }
    if (selectedApp != null) AlertDialog(
        onDismissRequest = { selectedPackage = null },
        title = { Text(selectedApp.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { viewModel.toggleAppFavorite(selectedApp.packageName) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("app_action_favorite")) {
                    Icon(if (selectedApp.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (selectedApp.isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة", fontSize = 16.sp)
                }
                FilledTonalButton(onClick = { viewModel.toggleAppHidden(selectedApp.packageName) },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("app_action_hidden")) {
                    Icon(if (selectedApp.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (selectedApp.isHidden) "إظهار التطبيق" else "إخفاء التطبيق", fontSize = 16.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { selectedPackage = null }) { Text("تم") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(label: String, iconSizeDp: Int, showLabel: Boolean, tag: String, launch: () -> Unit,
    onManage: () -> Unit, hidden: Boolean = false, icon: @Composable (androidx.compose.ui.unit.Dp) -> Unit) {
    // Constrain oversized saved icons to the cell, without changing the saved preference.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val size = minOf(iconSizeDp.coerceIn(40, 110).dp, (maxWidth - 24.dp).coerceAtLeast(32.dp))
        Column(Modifier.fillMaxWidth().height(size + if (showLabel) 66.dp else 28.dp)
            .clip(RoundedCornerShape(22.dp)).background(CarbonSurface.copy(alpha = if (hidden) .5f else 1f))
            .combinedClickable(onClick = launch, onLongClick = onManage).testTag(tag)
            .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            icon(size)
            if (showLabel) {
                Spacer(Modifier.height(10.dp))
                Text(label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}
