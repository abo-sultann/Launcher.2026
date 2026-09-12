package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DisplayAutomationController
import com.example.ui.components.DarbakEmptyState
import com.example.ui.components.DarbakSearch
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

private enum class AppDrawerFilter(val title: String) { ALL("الكل"), FAVORITES("المفضلة"), HIDDEN("المخفية") }

@Composable
fun AppDrawerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val apps by viewModel.installedApps.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    val drivingConfig = remember(context) { DisplayAutomationController(context) }.readConfig()
    val drivingLocked = drivingConfig.safeDrivingEnabled && gps.hasGpsFix && gps.isSpeedReliable && gps.speedKmH >= drivingConfig.safeDrivingThresholdKmH
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppDrawerFilter.ALL) }
    var managing by rememberSaveable { mutableStateOf(false) }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedApp = apps.firstOrNull { it.packageName == selectedPackage }

    LaunchedEffect(drivingLocked) {
        if (drivingLocked) {
            managing = false
            selectedPackage = null
            if (filter == AppDrawerFilter.HIDDEN) filter = AppDrawerFilter.ALL
        }
    }

    BackHandler(managing || selectedPackage != null) {
        selectedPackage = null
        managing = false
        if (filter == AppDrawerFilter.HIDDEN) filter = AppDrawerFilter.ALL
    }

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
    val showSystemShortcut = apps.none { it.packageName == "com.android.settings" } && filter == AppDrawerFilter.ALL &&
        (query.isBlank() || "إعدادات أندرويد settings".contains(query, true))

    Column(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = CarbonSurface.copy(alpha = .30f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.width(135.dp)) {
                    Text("التطبيقات", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(if (drivingLocked) "إدارة التطبيقات مقفلة أثناء القيادة" else "${filtered.size} تطبيق", color = if (drivingLocked) AmberRacing else TextSecondary, fontSize = 10.sp, maxLines = 1)
                }
                DarbakSearch(query, { query = it }, "بحث", "apps_search", Modifier.weight(1f))
                AppDrawerFilter.values().filter { (managing && !drivingLocked) || it != AppDrawerFilter.HIDDEN }.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.title, fontSize = 12.sp) },
                        modifier = Modifier.height(40.dp).testTag("apps_filter_${option.name}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (filter == option) CyanNeon.copy(alpha = .40f) else Color.White.copy(alpha = .05f)),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Black.copy(alpha = .12f),
                            selectedContainerColor = CyanNeon.copy(alpha = .12f),
                            selectedLabelColor = TextPrimary,
                        ),
                    )
                }
                TextButton(
                    onClick = {
                        if (!drivingLocked) {
                            managing = !managing
                            if (!managing && filter == AppDrawerFilter.HIDDEN) filter = AppDrawerFilter.ALL
                        }
                    },
                    enabled = !drivingLocked,
                    modifier = Modifier.height(42.dp).testTag("apps_manage"),
                ) {
                    Icon(if (managing) Icons.Default.Check else if (drivingLocked) Icons.Default.Lock else Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (managing) "تم" else if (drivingLocked) "مقفل" else "إدارة", fontSize = 12.sp)
                }
                Surface(color = DarbakGold, shape = RoundedCornerShape(2.dp), modifier = Modifier.width(30.dp).height(3.dp)) {}
            }
        }

        if (filtered.isEmpty() && !showSystemShortcut) {
            DarbakEmptyState(Icons.Default.Apps, if (query.isNotBlank()) "لا توجد نتائج" else "لا توجد تطبيقات هنا", Modifier.fillMaxSize())
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("apps_grid"),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(top = 2.dp, bottom = 6.dp),
            ) {
                if (showSystemShortcut) item(key = "__android_system_settings__") {
                    CompactAppTile(
                        label = "الإعدادات",
                        showLabel = settings.showAppLabels,
                        tag = "app_card_android_settings",
                        launch = viewModel::launchAndroidSettings,
                        onManage = viewModel::launchAndroidSettings,
                    ) { size -> Icon(Icons.Default.Settings, null, tint = CyanNeon, modifier = Modifier.size(size)) }
                }
                items(filtered, key = { it.packageName }) { app ->
                    val bitmap = remember(app.iconBitmap) { app.iconBitmap?.asImageBitmap() }
                    CompactAppTile(
                        label = app.label,
                        showLabel = settings.showAppLabels,
                        tag = "app_card_${app.packageName}",
                        hidden = app.isHidden,
                        launch = { if (managing && !drivingLocked) selectedPackage = app.packageName else viewModel.launchApp(app.packageName) },
                        onManage = { if (!drivingLocked) selectedPackage = app.packageName },
                    ) { size ->
                        if (bitmap != null) Image(bitmap, null, modifier = Modifier.size(size))
                        else Icon(Icons.Default.Android, null, tint = CyanNeon, modifier = Modifier.size(size))
                    }
                }
            }
        }
    }

    if (selectedApp != null && !drivingLocked) AlertDialog(
        onDismissRequest = { selectedPackage = null },
        title = { Text(selectedApp.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { viewModel.toggleAppFavorite(selectedApp.packageName) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("app_action_favorite"),
                ) {
                    Icon(if (selectedApp.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedApp.isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة", fontSize = 14.sp)
                }
                FilledTonalButton(
                    onClick = { viewModel.toggleAppHidden(selectedApp.packageName) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("app_action_hidden"),
                ) {
                    Icon(if (selectedApp.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedApp.isHidden) "إظهار التطبيق" else "إخفاء التطبيق", fontSize = 14.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = { selectedPackage = null }) { Text("تم") } },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactAppTile(
    label: String,
    showLabel: Boolean,
    tag: String,
    launch: () -> Unit,
    onManage: () -> Unit,
    hidden: Boolean = false,
    icon: @Composable (androidx.compose.ui.unit.Dp) -> Unit,
) {
    Surface(
        color = CarbonSurface.copy(alpha = if (hidden) .22f else .32f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .055f)),
        modifier = Modifier.fillMaxWidth().height(if (showLabel) 88.dp else 70.dp)
            .combinedClickable(onClick = launch, onLongClick = onManage).testTag(tag),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon(40.dp)
            if (showLabel) {
                Spacer(Modifier.height(5.dp))
                Text(
                    label,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
