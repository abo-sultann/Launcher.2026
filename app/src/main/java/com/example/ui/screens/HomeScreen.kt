package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.*
import com.example.ui.components.CarScreen
import com.example.ui.components.WidgetFrame
import com.example.ui.components.WidgetLayoutDialog
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
    val navigationTarget by viewModel.offroadNavigationTarget.collectAsState()

    val visualWidgets = widgets

    var showLibraryDialog by remember { mutableStateOf(false) }
    var showLayoutDialog by remember { mutableStateOf(false) }
    var editingWidgetForStyle by remember { mutableStateOf<WidgetItem?>(null) }
    var selectedWidgetId by remember { mutableStateOf<String?>(null) }
    var undoSnapshot by remember { mutableStateOf<List<WidgetItem>?>(null) }
    val density = LocalDensity.current

    fun rememberUndoPoint() {
        undoSnapshot = visualWidgets.map { it.copy() }
    }

    fun restoreUndoPoint() {
        val snapshot = undoSnapshot ?: return
        viewModel.replaceWidgets(snapshot)
        undoSnapshot = null
    }

    fun applyBackgroundFocusLayout() {
        rememberUndoPoint()
        visualWidgets.filter { it.isVisible }.forEach { item ->
            val target = backgroundFocusGeometry(item.type)
            viewModel.replaceWidgetGeometry(item.id, target[0], target[1], target[2], target[3])
            val recommendedStyle = backgroundFocusStyle(item.type)
            if (item.style != recommendedStyle) viewModel.updateWidgetStyle(item.id, recommendedStyle)
            viewModel.setWidgetSurface(item.id, WidgetItem.defaultSurfaceFor(item.type))
            if (item.showBorder) viewModel.toggleWidgetBorder(item.id)
        }
        selectedWidgetId = null
    }

    LaunchedEffect(isDesignMode) {
        if (!isDesignMode) selectedWidgetId = null
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        visualWidgets
            .filter { it.isVisible }
            .sortedBy { it.zIndex }
            .forEach { item ->
                val normalized = WidgetItem.withDefaultGeometry(item)
                val width = maxWidth * normalized.widthFraction.coerceIn(.07f, 1f)
                val height = maxHeight * normalized.heightFraction.coerceIn(.07f, 1f)
                val x = maxWidth * normalized.xFraction.coerceIn(0f, 1f)
                val y = maxHeight * normalized.yFraction.coerceIn(0f, 1f)

                WidgetFrame(
                    widgetItem = normalized,
                    isDesignMode = isDesignMode,
                    isSelected = selectedWidgetId == normalized.id,
                    onSelect = {
                        if (selectedWidgetId != normalized.id) rememberUndoPoint()
                        selectedWidgetId = normalized.id
                    },
                    onChangeStyle = { rememberUndoPoint(); selectedWidgetId = normalized.id; editingWidgetForStyle = normalized },
                    onMoveBy = { dxPx, dyPx ->
                        selectedWidgetId = normalized.id
                        viewModel.previewWidgetMove(normalized.id, dxPx / canvasWidthPx, dyPx / canvasHeightPx)
                    },
                    onResizeBy = { dwPx, dhPx ->
                        selectedWidgetId = normalized.id
                        viewModel.previewWidgetResize(normalized.id, dwPx / canvasWidthPx, dhPx / canvasHeightPx)
                    },
                    onTransformFinished = { viewModel.commitWidgetLayout() },
                    onOpacityChange = { viewModel.setWidgetOpacity(normalized.id, it) },
                    onToggleLock = { rememberUndoPoint(); viewModel.toggleWidgetLock(normalized.id) },
                    onBringToFront = { viewModel.bringWidgetToFront(normalized.id) },
                    onDelete = {
                        rememberUndoPoint()
                        viewModel.removeWidget(normalized.id)
                        if (selectedWidgetId == normalized.id) selectedWidgetId = null
                    },
                    onDuplicate = {
                        rememberUndoPoint()
                        viewModel.addWidget(normalized.type, normalized.style)
                    },
                    onSetSizePreset = { preset ->
                        rememberUndoPoint()
                        viewModel.setWidgetSizePreset(normalized.id, preset)
                    },
                    onSurfaceChange = { surface ->
                        rememberUndoPoint()
                        viewModel.setWidgetSurface(normalized.id, surface)
                    },
                    onToggleBorder = {
                        rememberUndoPoint()
                        viewModel.toggleWidgetBorder(normalized.id)
                    },
                    onToneChange = { rememberUndoPoint(); viewModel.setWidgetTone(normalized.id, it) },
                    onSurfaceOpacityChange = { rememberUndoPoint(); viewModel.setWidgetSurfaceOpacity(normalized.id, it) },
                    onResetWidget = { rememberUndoPoint(); viewModel.resetWidget(normalized.id) },
                    modifier = Modifier.offset(x = x, y = y).size(width = width, height = height).zIndex(normalized.zIndex.toFloat())
                ) {
                    RenderWidgetContent(
                        normalized,
                        viewModel,
                        settings,
                        installedApps,
                        playbackState,
                        gpsTelemetry,
                        tripData,
                        isDesignMode,
                        activeMap,
                        navigationTarget,
                        if (navigationTarget != null) viewModel.offroadDistanceToTargetMeters() else null,
                        if (navigationTarget != null) viewModel.offroadBearingToTarget() else null
                    )
                }
            }

        if (isDesignMode) {
            Surface(
                color = CarbonDark.copy(alpha = .92f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanNeon.copy(alpha = .55f)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).zIndex(10000f)
            ) {
                Row(
                    Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Widgets, null, tint = CyanNeon, modifier = Modifier.size(17.dp))
                    Text("تصميم الودجت", color = CyanNeon, fontWeight = FontWeight.Black)
                    FilledTonalButton(
                        onClick = { applyBackgroundFocusLayout() },
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CarbonSurface)
                    ) {
                        Icon(Icons.Default.Wallpaper, null, tint = AmberRacing, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("إبراز الخلفية", color = TextPrimary, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                    IconButton(onClick = { restoreUndoPoint() }, enabled = undoSnapshot != null, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Undo, "تراجع", tint = if (undoSnapshot != null) TextPrimary else TextMuted, modifier = Modifier.size(17.dp))
                    }
                    Button(
                        onClick = { showLayoutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberRacing),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.ViewQuilt, null, tint = CarbonDark, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("ترتيب", color = CarbonDark, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                    Button(
                        onClick = { showLibraryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp).testTag("btn_add_widget_banner")
                    ) {
                        Icon(Icons.Default.Add, null, tint = CarbonDark, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("إضافة", color = CarbonDark, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                    OutlinedButton(
                        onClick = {
                            rememberUndoPoint()
                            viewModel.resetWidgetsToDefault()
                            selectedWidgetId = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(13.dp))
                        Text("افتراضي", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                    IconButton(onClick = { viewModel.toggleDesignMode() }, modifier = Modifier.size(31.dp)) {
                        Icon(Icons.Default.CheckCircle, "إنهاء التصميم", tint = EmeraldSafe, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (selectedWidgetId == null) {
                Surface(
                    color = CarbonDark.copy(alpha = .68f),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).zIndex(10000f)
                ) {
                    Text("اختر الودجت ثم حدّد: أبيض أو أسود • شفاف أو زجاجي أو بطاقة", color = TextSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }

            Surface(
                color = CarbonSurface.copy(alpha = .72f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanNeon.copy(alpha = .45f)),
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).clip(RoundedCornerShape(12.dp)).clickable { showLibraryDialog = true }.testTag("card_add_widget").zIndex(10000f)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, "إضافة ودجت", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ودجت", color = CyanNeon, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showLibraryDialog) {
            WidgetLibraryDialog(
                isStyleChangerMode = false,
                onDismiss = { showLibraryDialog = false },
                onSelectStyle = { viewModel.addWidget(it.type, it); showLibraryDialog = false }
            )
        }

        if (showLayoutDialog) {
            WidgetLayoutDialog(
                viewModel = viewModel,
                target = WidgetLayoutTarget.HOME,
                onBackgroundFocus = { applyBackgroundFocusLayout() },
                onDismiss = { showLayoutDialog = false }
            )
        }

        editingWidgetForStyle?.let { target ->
            WidgetLibraryDialog(
                initialType = target.type,
                initialStyle = target.style,
                isStyleChangerMode = true,
                onDismiss = { editingWidgetForStyle = null },
                onSelectStyle = {
                    rememberUndoPoint()
                    viewModel.updateWidgetStyle(target.id, it)
                    editingWidgetForStyle = null
                }
            )
        }
    }
}

private fun backgroundFocusGeometry(type: WidgetType): FloatArray = when (type) {
    WidgetType.SPEEDOMETER -> floatArrayOf(.025f, .05f, .17f, .24f)
    WidgetType.CLOCK -> floatArrayOf(.40f, .015f, .20f, .12f)
    WidgetType.DATE -> floatArrayOf(.39f, .14f, .22f, .10f)
    WidgetType.GPS -> floatArrayOf(.025f, .30f, .16f, .13f)
    WidgetType.MUSIC -> floatArrayOf(.70f, .035f, .27f, .17f)
    WidgetType.MAP -> floatArrayOf(.025f, .48f, .22f, .23f)
    WidgetType.TRIP -> floatArrayOf(.80f, .39f, .18f, .27f)
    WidgetType.APPS -> floatArrayOf(.31f, .79f, .38f, .17f)
    WidgetType.CONTROLS -> floatArrayOf(.72f, .75f, .25f, .17f)
    WidgetType.MAINTENANCE -> floatArrayOf(.835f, .025f, .15f, .92f)
}

private fun backgroundFocusStyle(type: WidgetType): WidgetStyle = when (type) {
    WidgetType.CLOCK -> WidgetStyle.CLOCK_MINIMAL
    WidgetType.SPEEDOMETER -> WidgetStyle.SPEED_GAUGE_SEMI
    WidgetType.DATE -> WidgetStyle.DATE_MINIMAL
    WidgetType.GPS -> WidgetStyle.GPS_INDICATOR_MINI
    WidgetType.MUSIC -> WidgetStyle.MUSIC_MINIMAL
    WidgetType.MAP -> WidgetStyle.MAP_MINI
    WidgetType.TRIP -> WidgetStyle.TRIP_CARD
    WidgetType.APPS -> WidgetStyle.APPS_HORIZONTAL_DOCK
    WidgetType.CONTROLS -> WidgetStyle.CONTROLS_HORIZONTAL_BAR
    WidgetType.MAINTENANCE -> WidgetStyle.MAINTENANCE_VERTICAL
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
    isDesignMode: Boolean,
    map: MapItem?,
    navigationTarget: OffroadNavigationTarget?,
    targetDistanceMeters: Float?,
    targetBearing: Float?
) {
    when (w.type) {
        WidgetType.CLOCK -> ClockWidget(w.style, s.is24HourFormat)
        WidgetType.SPEEDOMETER -> SpeedWidget(w.style, gps, trip, s.speedUnit)
        WidgetType.DATE -> DateWidget(w.style)
        WidgetType.GPS -> GpsWidget(w.style, gps)
        WidgetType.MUSIC -> MusicWidget(w.style, p, { vm.togglePlayPause() }, { vm.playNext() }, { vm.playPrevious() }, { vm.seekTo(it) })
        WidgetType.MAP -> MapWidget(w.style, gps, trip, map, navigationTarget, targetDistanceMeters, targetBearing, onOpenFullMap = { vm.navigateTo(CarScreen.MAP) }, interactionEnabled = !isDesignMode)
        WidgetType.TRIP -> TripWidget(w.style, trip, { vm.startTrip() }, { vm.pauseTrip() }, { vm.resetTrip() })
        WidgetType.APPS -> AppsWidget(w.style, apps, onOpenAppDrawer = { vm.navigateTo(CarScreen.APPS) }, onLaunchApp = { vm.launchApp(it) }, interactionEnabled = !isDesignMode)
        WidgetType.CONTROLS -> ControlsWidget(w.style, p, { vm.adjustVolume(it) }, { vm.toggleMute() }, { vm.togglePlayPause() }, { vm.playNext() }, { vm.playPrevious() })
        WidgetType.MAINTENANCE -> MaintenanceWidget(w.style, interactionEnabled = !isDesignMode)
    }
}
