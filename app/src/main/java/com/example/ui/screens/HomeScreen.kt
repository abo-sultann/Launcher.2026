package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.CarScreen
import com.example.ui.components.WidgetFrame
import com.example.ui.components.WidgetLibraryDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.widgets.*

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val widgets by viewModel.widgets.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    var showLibraryDialog by remember { mutableStateOf(false) }
    var editingWidgetForStyle by remember { mutableStateOf<WidgetItem?>(null) }
    val columns = settings.homeGridColumns.coerceIn(2, 6)
    val height = settings.widgetHeightDp.coerceIn(90, 220)

    Box(modifier.fillMaxSize().background(backgroundBrush(settings))) {
        if (settings.backgroundType == BackgroundType.CUSTOM_IMAGE && !settings.customWallpaperPath.isNullOrBlank()) {
            AsyncImage(model = settings.customWallpaperPath, contentDescription = null, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (isDesignMode) {
                Surface(color = AmberRacing.copy(alpha = .15f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, AmberRacing), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("وضع التصميم: تحكم كامل بالودجات", color = AmberRacing, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { showLibraryDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp), modifier = Modifier.height(30.dp).testTag("btn_add_widget_banner")) { Icon(Icons.Default.Add, null, tint = CarbonDark, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة", color = CarbonDark) }
                            OutlinedButton(onClick = { viewModel.resetWidgetsToDefault() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp), modifier = Modifier.height(30.dp)) { Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text("افتراضي") }
                        }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(settings.gridHorizontalGapDp.dp), verticalArrangement = Arrangement.spacedBy(settings.gridVerticalGapDp.dp), contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(widgets.sortedBy { it.order }, key = { it.id }, span = { GridItemSpan(it.spanX.coerceIn(1, columns)) }) { item ->
                    WidgetFrame(item, isDesignMode, onChangeStyle = { editingWidgetForStyle = item }, onToggleSize = { viewModel.toggleWidgetSpan(item.id) }, onMoveForward = { viewModel.moveWidget(item.id, true) }, onMoveBackward = { viewModel.moveWidget(item.id, false) }, onDelete = { viewModel.removeWidget(item.id) }, modifier = Modifier.height(height.dp)) {
                        RenderWidgetContent(item, viewModel, settings, installedApps, playbackState, gpsTelemetry, tripData, activeMap)
                    }
                }
                if (isDesignMode) item(span = { GridItemSpan(1) }) {
                    Surface(color = CarbonSurface.copy(alpha = .6f), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = .4f)), modifier = Modifier.height(height.dp).clip(RoundedCornerShape(14.dp)).clickable { showLibraryDialog = true }.testTag("card_add_widget")) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.Add, "إضافة ودجت", tint = CyanNeon, modifier = Modifier.size(32.dp)); Text("إضافة ودجت", color = CyanNeon, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        if (showLibraryDialog) WidgetLibraryDialog(isStyleChangerMode = false, onDismiss = { showLibraryDialog = false }, onSelectStyle = { viewModel.addWidget(it.type, it); showLibraryDialog = false })
        editingWidgetForStyle?.let { target -> WidgetLibraryDialog(initialType = target.type, isStyleChangerMode = true, onDismiss = { editingWidgetForStyle = null }, onSelectStyle = { viewModel.updateWidgetStyle(target.id, it); editingWidgetForStyle = null }) }
    }
}

private fun backgroundBrush(s: LauncherSettings): Brush = when (s.backgroundType) {
    BackgroundType.CYBER_CYAN -> Brush.linearGradient(listOf(Color(0xFF06131B), Color(0xFF102C3A)))
    BackgroundType.AMBER_RACING -> Brush.linearGradient(listOf(Color(0xFF1A1007), Color(0xFF30200C)))
    BackgroundType.DEEP_SPACE -> Brush.linearGradient(listOf(Color(0xFF05060A), Color(0xFF15182A)))
    BackgroundType.LUXURY_ONYX -> Brush.linearGradient(listOf(Color.Black, Color(0xFF171717)))
    else -> Brush.linearGradient(listOf(CarbonDark, Color(0xFF101722)))
}

@Composable
private fun RenderWidgetContent(widgetItem: WidgetItem, viewModel: MainViewModel, settings: LauncherSettings, installedApps: List<AppItem>, playbackState: MusicPlaybackState, gpsTelemetry: GpsTelemetry, tripData: TripData, activeMap: MapItem?) {
    when (widgetItem.type) {
        WidgetType.CLOCK -> ClockWidget(widgetItem.style, settings.is24HourFormat)
        WidgetType.SPEEDOMETER -> SpeedWidget(widgetItem.style, gpsTelemetry, tripData, settings.speedUnit)
        WidgetType.DATE -> DateWidget(widgetItem.style)
        WidgetType.GPS -> GpsWidget(widgetItem.style, gpsTelemetry)
        WidgetType.MUSIC -> MusicWidget(widgetItem.style, playbackState, { viewModel.togglePlayPause() }, { viewModel.playNext() }, { viewModel.playPrevious() }, { viewModel.seekTo(it) })
        WidgetType.MAP -> MapWidget(widgetItem.style, gpsTelemetry, tripData, activeMap) { viewModel.navigateTo(CarScreen.MAP) }
        WidgetType.TRIP -> TripWidget(widgetItem.style, tripData, { viewModel.startTrip() }, { viewModel.pauseTrip() }, { viewModel.resetTrip() })
        WidgetType.APPS -> AppsWidget(widgetItem.style, installedApps, { viewModel.launchApp(it) }) { viewModel.navigateTo(CarScreen.APPS) }
        WidgetType.CONTROLS -> ControlsWidget(widgetItem.style, playbackState, { viewModel.adjustVolume(it) }, { viewModel.toggleMute() }, { viewModel.togglePlayPause() }, { viewModel.playNext() }, { viewModel.playPrevious() })
    }
}
