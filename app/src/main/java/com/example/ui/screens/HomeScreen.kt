package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        widgets
            .filter { it.isVisible }
            .sortedBy { it.zIndex }
            .forEach { item ->
                val normalized = WidgetItem.withDefaultGeometry(item)
                val width = maxWidth * normalized.widthFraction.coerceIn(.12f, 1f)
                val height = maxHeight * normalized.heightFraction.coerceIn(.14f, 1f)
                val x = maxWidth * normalized.xFraction.coerceIn(0f, 1f)
                val y = maxHeight * normalized.yFraction.coerceIn(0f, 1f)

                WidgetFrame(
                    widgetItem = normalized,
                    isDesignMode = isDesignMode,
                    onChangeStyle = { editingWidgetForStyle = normalized },
                    onMoveBy = { dxPx, dyPx ->
                        viewModel.previewWidgetMove(normalized.id, dxPx / canvasWidthPx, dyPx / canvasHeightPx)
                    },
                    onResizeBy = { dwPx, dhPx ->
                        viewModel.previewWidgetResize(normalized.id, dwPx / canvasWidthPx, dhPx / canvasHeightPx)
                    },
                    onTransformFinished = { viewModel.commitWidgetLayout() },
                    onOpacityChange = { viewModel.setWidgetOpacity(normalized.id, it) },
                    onToggleLock = { viewModel.toggleWidgetLock(normalized.id) },
                    onBringToFront = { viewModel.bringWidgetToFront(normalized.id) },
                    onDelete = { viewModel.removeWidget(normalized.id) },
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(width = width, height = height)
                        .zIndex(normalized.zIndex.toFloat())
                ) {
                    RenderWidgetContent(
                        normalized,
                        viewModel,
                        settings,
                        installedApps,
                        playbackState,
                        gpsTelemetry,
                        tripData,
                        activeMap
                    )
                }
            }

        if (isDesignMode) {
            Surface(
                color = CarbonDark.copy(alpha = .96f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberRacing),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .zIndex(10000f)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("وضع التصميم الحر", color = AmberRacing, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showLibraryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                        modifier = Modifier.height(31.dp).testTag("btn_add_widget_banner")
                    ) {
                        Icon(Icons.Default.Add, null, tint = CarbonDark, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("إضافة", color = CarbonDark)
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetWidgetsToDefault() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(31.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("افتراضي")
                    }
                }
            }

            Surface(
                color = CarbonSurface.copy(alpha = .82f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanNeon.copy(alpha = .55f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showLibraryDialog = true }
                    .testTag("card_add_widget")
                    .zIndex(10000f)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, "إضافة ودجت", tint = CyanNeon)
                    Spacer(Modifier.width(5.dp))
                    Text("إضافة ودجت", color = CyanNeon, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showLibraryDialog) {
            WidgetLibraryDialog(
                isStyleChangerMode = false,
                onDismiss = { showLibraryDialog = false },
                onSelectStyle = {
                    viewModel.addWidget(it.type, it)
                    showLibraryDialog = false
                }
            )
        }

        editingWidgetForStyle?.let { target ->
            WidgetLibraryDialog(
                initialType = target.type,
                isStyleChangerMode = true,
                onDismiss = { editingWidgetForStyle = null },
                onSelectStyle = {
                    viewModel.updateWidgetStyle(target.id, it)
                    editingWidgetForStyle = null
                }
            )
        }
    }
}

@Composable
private fun RenderWidgetContent(
    w: WidgetItem,
    vm: MainViewModel,
    s: LauncherSettings,
    apps: List<AppItem>,
    p: MusicPlaybackState,
    gps: GpsTelemetry,
    trip: TripData,
    map: MapItem?
) {
    when (w.type) {
        WidgetType.CLOCK -> ClockWidget(w.style, s.is24HourFormat)
        WidgetType.SPEEDOMETER -> SpeedWidget(w.style, gps, trip, s.speedUnit)
        WidgetType.DATE -> DateWidget(w.style)
        WidgetType.GPS -> GpsWidget(w.style, gps)
        WidgetType.MUSIC -> MusicWidget(w.style, p, { vm.togglePlayPause() }, { vm.playNext() }, { vm.playPrevious() }, { vm.seekTo(it) })
        WidgetType.MAP -> MapWidget(w.style, gps, trip, map, onOpenFullMap = { vm.navigateTo(CarScreen.MAP) })
        WidgetType.TRIP -> TripWidget(w.style, trip, { vm.startTrip() }, { vm.pauseTrip() }, { vm.resetTrip() })
        WidgetType.APPS -> AppsWidget(w.style, apps, onOpenAppDrawer = { vm.navigateTo(CarScreen.APPS) }, onLaunchApp = { vm.launchApp(it) })
        WidgetType.CONTROLS -> ControlsWidget(w.style, p, { vm.adjustVolume(it) }, { vm.toggleMute() }, { vm.togglePlayPause() }, { vm.playNext() }, { vm.playPrevious() })
    }
}
