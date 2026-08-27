package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.widgets.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ScreenSaverOverlay(
    viewModel: MainViewModel,
    settings: LauncherSettings,
    widgets: List<WidgetItem>,
    layouts: List<ScreenSaverWidgetLayout>,
    apps: List<AppItem>,
    playbackState: MusicPlaybackState,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editMode by remember { mutableStateOf(false) }
    var showLayoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(settings.screenSaverNightMode, settings.screenSaverNightBrightnessPercent) {
        val activity = context as? Activity
        val oldBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        if (activity != null && settings.screenSaverNightMode) {
            val attrs = activity.window.attributes
            attrs.screenBrightness = settings.screenSaverNightBrightnessPercent.coerceIn(5, 40) / 100f
            activity.window.attributes = attrs
        }
        onDispose {
            if (activity != null) {
                val attrs = activity.window.attributes
                attrs.screenBrightness = oldBrightness
                activity.window.attributes = attrs
            }
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (settings.screenSaverUseWallpaper) LauncherBackground(settings)
        if (settings.screenSaverNightMode) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .34f)).zIndex(2f))

        ScreenSaverCanvas(
            viewModel = viewModel,
            settings = settings,
            widgets = widgets,
            layouts = layouts,
            apps = apps,
            playbackState = playbackState,
            gpsTelemetry = gpsTelemetry,
            tripData = tripData,
            activeMap = activeMap,
            editMode = editMode,
            modifier = Modifier.fillMaxSize().zIndex(3f)
        )

        if (editMode) {
            Surface(
                color = CarbonDark.copy(alpha = .94f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyanNeon.copy(alpha = .7f)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).zIndex(1500f)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("تحكم حر", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FilledTonalButton(
                        onClick = { showLayoutDialog = true },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AmberRacing, contentColor = CarbonDark)
                    ) {
                        Icon(Icons.Default.ViewQuilt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("ترتيب", fontSize = 10.sp)
                    }
                    IconButton(onClick = { viewModel.resetScreenSaverLayout() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.RestartAlt, "إعادة الترتيب", tint = AmberRacing)
                    }
                    Button(
                        onClick = { viewModel.commitScreenSaverLayout(); editMode = false },
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حفظ")
                    }
                }
            }
        } else {
            Box(
                Modifier.fillMaxSize().zIndex(1000f).combinedClickable(
                    onClick = onDismiss,
                    onLongClick = { editMode = true }
                )
            )

            FilledTonalButton(
                onClick = { viewModel.updateSettings(settings.copy(screenSaverNightMode = !settings.screenSaverNightMode)) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).height(34.dp).zIndex(1200f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CarbonDark.copy(alpha = .90f),
                    contentColor = if (settings.screenSaverNightMode) AmberRacing else TextPrimary
                )
            ) {
                Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (settings.screenSaverNightMode) "الوضع الليلي مفعل" else "وضع القيادة الليلي", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showLayoutDialog) {
            WidgetLayoutDialog(viewModel, WidgetLayoutTarget.SCREEN_SAVER) { showLayoutDialog = false }
        }
    }
}

@Composable
fun ScreenSaverEditorScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val layouts by viewModel.screenSaverLayouts.collectAsState()
    val apps by viewModel.installedApps.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    val trip by viewModel.tripData.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    var showLayoutDialog by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (settings.screenSaverUseWallpaper) LauncherBackground(settings)

        ScreenSaverCanvas(
            viewModel, settings, widgets, layouts, apps, playback, gps, trip, activeMap, true, Modifier.fillMaxSize()
        )

        Surface(
            color = CarbonDark.copy(alpha = .94f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = .7f)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).zIndex(1500f)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("تحكم كامل", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = { showLayoutDialog = true }, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)) {
                    Icon(Icons.Default.ViewQuilt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("ترتيب", fontSize = 10.sp)
                }
                IconButton(onClick = { viewModel.resetScreenSaverLayout() }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.RestartAlt, "إعادة الترتيب", tint = AmberRacing) }
                Button(
                    onClick = { viewModel.commitScreenSaverLayout(); onDone() },
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تم")
                }
            }
        }

        if (showLayoutDialog) WidgetLayoutDialog(viewModel, WidgetLayoutTarget.SCREEN_SAVER) { showLayoutDialog = false }
    }
}

@Composable
private fun ScreenSaverCanvas(
    viewModel: MainViewModel,
    settings: LauncherSettings,
    widgets: List<WidgetItem>,
    layouts: List<ScreenSaverWidgetLayout>,
    apps: List<AppItem>,
    playbackState: MusicPlaybackState,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    editMode: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedTypes = WidgetType.values().filter { it in settings.screenSaverWidgetTypes }.take(4)
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val canvasWidth = maxWidth
        val canvasHeight = maxHeight
        val widthPx = with(density) { canvasWidth.toPx().coerceAtLeast(1f) }
        val heightPx = with(density) { canvasHeight.toPx().coerceAtLeast(1f) }

        selectedTypes.forEachIndexed { index, type ->
            val layout = layouts.firstOrNull { it.type == type } ?: ScreenSaverWidgetLayout.defaultFor(type, index)
            val source = widgets.firstOrNull { it.type == type }
            val style = layout.style?.takeIf { it.type == type } ?: source?.style ?: defaultScreenSaverStyle(type)

            Surface(
                color = Color.Black.copy(alpha = if (editMode) .64f else .55f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(if (editMode) 2.dp else 1.dp, if (editMode) AmberRacing else CarbonCardBorder),
                modifier = Modifier
                    .offset(x = canvasWidth * layout.xFraction, y = canvasHeight * layout.yFraction)
                    .size(width = canvasWidth * layout.widthFraction.coerceIn(.16f, 1f), height = canvasHeight * layout.heightFraction.coerceIn(.16f, 1f))
                    .alpha(layout.opacity)
                    .zIndex(layout.zIndex.toFloat())
            ) {
                Box(Modifier.fillMaxSize()) {
                    RenderScreenSaverWidget(type, style, viewModel, settings, apps, playbackState, gpsTelemetry, tripData, activeMap)

                    if (editMode) {
                        Surface(
                            color = CarbonDark.copy(alpha = .95f),
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, AmberRacing),
                            modifier = Modifier.align(Alignment.TopStart).padding(5.dp).pointerInput(type) {
                                detectDragGestures(
                                    onDragStart = { viewModel.bringScreenSaverWidgetToFront(type) },
                                    onDragEnd = { viewModel.commitScreenSaverLayout() },
                                    onDragCancel = { viewModel.commitScreenSaverLayout() }
                                ) { _, drag -> viewModel.previewScreenSaverMove(type, drag.x / widthPx, drag.y / heightPx) }
                            }
                        ) {
                            Row(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.DragIndicator, null, tint = AmberRacing, modifier = Modifier.size(16.dp))
                                Text("تحريك", color = AmberRacing, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.cycleScreenSaverStyle(type) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CarbonDark.copy(alpha = .95f))
                        ) {
                            Icon(Icons.Default.Palette, null, tint = CyanNeon, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(style.arabicName, color = TextPrimary, fontSize = 8.sp, maxLines = 1)
                        }

                        Surface(
                            color = CyanNeon,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).size(40.dp).pointerInput(type) {
                                detectDragGestures(
                                    onDragStart = { viewModel.bringScreenSaverWidgetToFront(type) },
                                    onDragEnd = { viewModel.commitScreenSaverLayout() },
                                    onDragCancel = { viewModel.commitScreenSaverLayout() }
                                ) { _, drag -> viewModel.previewScreenSaverResize(type, drag.x / widthPx, drag.y / heightPx) }
                            }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.OpenInFull, "تغيير الحجم", tint = CarbonDark, modifier = Modifier.size(21.dp)) }
                        }

                        Surface(color = CarbonDark.copy(alpha = .95f), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.setScreenSaverOpacity(type, layout.opacity - .10f) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Remove, "شفافية أقل", tint = TextPrimary, modifier = Modifier.size(16.dp)) }
                                Text("${(layout.opacity * 100).toInt()}%", color = CyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.setScreenSaverOpacity(type, layout.opacity + .10f) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Add, "شفافية أكثر", tint = TextPrimary, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderScreenSaverWidget(
    type: WidgetType,
    style: WidgetStyle,
    vm: MainViewModel,
    settings: LauncherSettings,
    apps: List<AppItem>,
    playback: MusicPlaybackState,
    gps: GpsTelemetry,
    trip: TripData,
    map: MapItem?
) {
    when (type) {
        WidgetType.CLOCK -> ClockWidget(style, settings.is24HourFormat)
        WidgetType.SPEEDOMETER -> SpeedWidget(style, gps, trip, settings.speedUnit)
        WidgetType.DATE -> DateWidget(style)
        WidgetType.GPS -> GpsWidget(style, gps)
        WidgetType.MUSIC -> MusicWidget(style, playback, {}, {}, {}, {})
        WidgetType.MAP -> MapWidget(style, gps, trip, map, onOpenFullMap = {})
        WidgetType.TRIP -> TripWidget(style, trip, {}, {}, {})
        WidgetType.APPS -> AppsWidget(style, apps, onOpenAppDrawer = {}, onLaunchApp = {})
        WidgetType.CONTROLS -> ControlsWidget(style, playback, {}, {}, {}, {}, {})
    }
}

private fun defaultScreenSaverStyle(type: WidgetType): WidgetStyle = when (type) {
    WidgetType.CLOCK -> WidgetStyle.CLOCK_AUTOMOTIVE_LARGE
    WidgetType.SPEEDOMETER -> WidgetStyle.SPEED_DIGITAL_LARGE
    WidgetType.DATE -> WidgetStyle.DATE_DAY_DATE
    WidgetType.GPS -> WidgetStyle.GPS_CARD
    WidgetType.MUSIC -> WidgetStyle.MUSIC_COMPACT
    WidgetType.MAP -> WidgetStyle.MAP_MEDIUM
    WidgetType.TRIP -> WidgetStyle.TRIP_CARD
    WidgetType.APPS -> WidgetStyle.APPS_ICONS_ONLY
    WidgetType.CONTROLS -> WidgetStyle.CONTROLS_HORIZONTAL_BAR
}
