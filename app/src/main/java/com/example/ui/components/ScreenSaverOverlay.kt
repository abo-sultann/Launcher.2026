package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.WidgetVisualStore
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
    val context = LocalContext.current
    val visualStore = remember { WidgetVisualStore(context.applicationContext) }
    var appearanceVersion by remember { mutableIntStateOf(0) }
    var selectedType by remember { mutableStateOf(selectedTypes.firstOrNull()) }
    val interfaceAccent = Color(settings.interfaceAccent.argb)

    LaunchedEffect(selectedTypes) {
        if (selectedType !in selectedTypes) selectedType = selectedTypes.firstOrNull()
    }

    Box(modifier) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(
                top = if (editMode) 54.dp else 0.dp,
                bottom = if (editMode) 92.dp else 0.dp
            )
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight
            val widthPx = with(density) { canvasWidth.toPx().coerceAtLeast(1f) }
            val heightPx = with(density) { canvasHeight.toPx().coerceAtLeast(1f) }

            selectedTypes.forEachIndexed { index, type ->
                val layout = layouts.firstOrNull { it.type == type } ?: ScreenSaverWidgetLayout.defaultFor(type, index)
                val source = widgets.firstOrNull { it.type == type }
                val style = layout.style?.takeIf { it.type == type } ?: source?.style ?: defaultScreenSaverStyle(type)
                val visualId = screenSaverVisualId(type)
                val surfaceStyle = remember(type, appearanceVersion) {
                    visualStore.getSurface(visualId, WidgetItem.defaultSurfaceFor(type))
                }
                val showBorder = remember(type, appearanceVersion) { visualStore.getBorder(visualId) }
                val surfaceOpacity = remember(type, appearanceVersion) { visualStore.getSurfaceOpacity(visualId) }
                val foreground = remember(type, appearanceVersion) { visualStore.getForegroundColorArgb(visualId)?.let(::Color) }
                val accent = remember(type, appearanceVersion) { visualStore.getAccentColorArgb(visualId)?.let(::Color) }
                val isSelected = editMode && selectedType == type
                val shape = screenSaverShape(type)
                val background = when (surfaceStyle) {
                    WidgetSurfaceStyle.TRANSPARENT -> Color.Transparent
                    WidgetSurfaceStyle.GLASS -> CarbonDark.copy(alpha = (.18f + .46f * surfaceOpacity).coerceAtMost(.72f))
                    WidgetSurfaceStyle.CARD -> CarbonCard.copy(alpha = (.55f + .40f * surfaceOpacity).coerceAtMost(.97f))
                }
                val borderColor = when {
                    isSelected -> interfaceAccent
                    editMode -> AmberRacing.copy(alpha = .28f)
                    showBorder -> CarbonCardBorder.copy(alpha = .88f)
                    else -> Color.Transparent
                }
                val borderWidth = if (isSelected) 2.dp else if (editMode || showBorder) 1.dp else 0.dp

                Surface(
                    color = background,
                    shape = shape,
                    border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
                    modifier = Modifier
                        .offset(x = canvasWidth * layout.xFraction, y = canvasHeight * layout.yFraction)
                        .size(width = canvasWidth * layout.widthFraction.coerceIn(.16f, 1f), height = canvasHeight * layout.heightFraction.coerceIn(.16f, 1f))
                        .alpha(layout.opacity)
                        .zIndex(layout.zIndex.toFloat())
                        .then(if (editMode) Modifier.clickable { selectedType = type } else Modifier)
                ) {
                    CompositionLocalProvider(
                        LocalWidgetVisualTokens provides WidgetVisualTokens(foreground, accent),
                        LocalWidgetForegroundColor provides foreground
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            RenderScreenSaverWidget(type, style, viewModel, settings, apps, playbackState, gpsTelemetry, tripData, activeMap)

                            if (isSelected) {
                                Surface(
                                    color = CarbonDark.copy(alpha = .95f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AmberRacing),
                                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp).pointerInput(type) {
                                        detectDragGestures(
                                            onDragStart = { viewModel.bringScreenSaverWidgetToFront(type) },
                                            onDragEnd = { viewModel.commitScreenSaverLayout() },
                                            onDragCancel = { viewModel.commitScreenSaverLayout() }
                                        ) { change, drag ->
                                            change.consume()
                                            viewModel.previewScreenSaverMove(type, drag.x / widthPx, drag.y / heightPx)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.DragIndicator, "تحريك", tint = AmberRacing, modifier = Modifier.padding(7.dp).size(18.dp))
                                }

                                Surface(
                                    color = interfaceAccent,
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).size(40.dp).pointerInput(type) {
                                        detectDragGestures(
                                            onDragStart = { viewModel.bringScreenSaverWidgetToFront(type) },
                                            onDragEnd = { viewModel.commitScreenSaverLayout() },
                                            onDragCancel = { viewModel.commitScreenSaverLayout() }
                                        ) { change, drag ->
                                            change.consume()
                                            viewModel.previewScreenSaverResize(type, drag.x / widthPx, drag.y / heightPx)
                                        }
                                    }
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.OpenInFull, "تغيير الحجم", tint = CarbonDark, modifier = Modifier.size(21.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (editMode) {
            selectedType?.let { type ->
                val layout = layouts.firstOrNull { it.type == type } ?: ScreenSaverWidgetLayout.defaultFor(type, 0)
                val source = widgets.firstOrNull { it.type == type }
                val style = layout.style?.takeIf { it.type == type } ?: source?.style ?: defaultScreenSaverStyle(type)
                ScreenSaverAppearanceDock(
                    type = type,
                    style = style,
                    widgetOpacity = layout.opacity,
                    visualStore = visualStore,
                    appearanceVersion = appearanceVersion,
                    onAppearanceChanged = { appearanceVersion++ },
                    onCycleStyle = { viewModel.cycleScreenSaverStyle(type) },
                    onOpacityChange = { viewModel.setScreenSaverOpacity(type, it) },
                    accentColor = interfaceAccent,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 6.dp).zIndex(2000f)
                )
            }
        }
    }
}

@Composable
private fun ScreenSaverAppearanceDock(
    type: WidgetType,
    style: WidgetStyle,
    widgetOpacity: Float,
    visualStore: WidgetVisualStore,
    appearanceVersion: Int,
    onAppearanceChanged: () -> Unit,
    onCycleStyle: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val visualId = screenSaverVisualId(type)
    val surface = remember(visualId, appearanceVersion) { visualStore.getSurface(visualId, WidgetItem.defaultSurfaceFor(type)) }
    val border = remember(visualId, appearanceVersion) { visualStore.getBorder(visualId) }
    val foreground = remember(visualId, appearanceVersion) { visualStore.getForegroundColorArgb(visualId) }
    val widgetAccent = remember(visualId, appearanceVersion) { visualStore.getAccentColorArgb(visualId) }
    val backgroundOpacity = remember(visualId, appearanceVersion) { visualStore.getSurfaceOpacity(visualId) }

    Surface(
        color = CarbonDark.copy(alpha = .97f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = .55f)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(type.arabicTitle, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                FilledTonalButton(
                    onClick = onCycleStyle,
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Palette, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(style.arabicName, fontSize = 8.sp, maxLines = 1)
                }

                WidgetSurfaceStyle.values().forEach { option ->
                    Surface(
                        onClick = {
                            visualStore.setSurface(visualId, option)
                            onAppearanceChanged()
                        },
                        color = if (surface == option) accentColor else CarbonSurface,
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, if (surface == option) accentColor else CarbonCardBorder)
                    ) {
                        Text(option.arabicName, color = if (surface == option) CarbonDark else TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp))
                    }
                }

                IconButton(
                    onClick = {
                        visualStore.setBorder(visualId, !border)
                        onAppearanceChanged()
                    },
                    modifier = Modifier.size(30.dp)
                ) { Icon(Icons.Default.BorderStyle, "الإطار", tint = if (border) AmberRacing else TextSecondary, modifier = Modifier.size(17.dp)) }

                Spacer(Modifier.weight(1f))
                Text("شفافية الودجت", color = TextSecondary, fontSize = 8.sp)
                IconButton(onClick = { onOpacityChange(widgetOpacity - .10f) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, "شفافية الودجت أقل", tint = TextPrimary, modifier = Modifier.size(15.dp)) }
                Text("${(widgetOpacity * 100).toInt()}%", color = accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onOpacityChange(widgetOpacity + .10f) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, "شفافية الودجت أكثر", tint = TextPrimary, modifier = Modifier.size(15.dp)) }

                Text("الخلفية", color = TextSecondary, fontSize = 8.sp)
                IconButton(
                    onClick = { visualStore.setSurfaceOpacity(visualId, backgroundOpacity - .10f); onAppearanceChanged() },
                    modifier = Modifier.size(28.dp)
                ) { Icon(Icons.Default.Remove, "الخلفية أخف", tint = TextPrimary, modifier = Modifier.size(15.dp)) }
                Text("${(backgroundOpacity * 100).toInt()}%", color = accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { visualStore.setSurfaceOpacity(visualId, backgroundOpacity + .10f); onAppearanceChanged() },
                    modifier = Modifier.size(28.dp)
                ) { Icon(Icons.Default.Add, "الخلفية أوضح", tint = TextPrimary, modifier = Modifier.size(15.dp)) }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("لون النص", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                ScreenSaverColorChoice(null, foreground == null, accentColor) {
                    visualStore.setForegroundColorArgb(visualId, it); onAppearanceChanged()
                }
                SCREEN_SAVER_TEXT_COLORS.forEach { color ->
                    ScreenSaverColorChoice(color, foreground == color, accentColor) {
                        visualStore.setForegroundColorArgb(visualId, it); onAppearanceChanged()
                    }
                }

                VerticalDivider(Modifier.height(22.dp), color = CarbonCardBorder)
                Text("اللون المميّز", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                ScreenSaverColorChoice(null, widgetAccent == null, accentColor) {
                    visualStore.setAccentColorArgb(visualId, it); onAppearanceChanged()
                }
                SCREEN_SAVER_ACCENT_COLORS.forEach { color ->
                    ScreenSaverColorChoice(color, widgetAccent == color, accentColor) {
                        visualStore.setAccentColorArgb(visualId, it); onAppearanceChanged()
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenSaverColorChoice(argb: Int?, selected: Boolean, selectionColor: Color, onSelect: (Int?) -> Unit) {
    val color = argb?.let(::Color) ?: CarbonSurface
    Surface(
        onClick = { onSelect(argb) },
        color = color,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) selectionColor else CarbonCardBorder),
        modifier = Modifier.size(21.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (argb == null) Text("A", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            else if (selected) Icon(Icons.Default.Check, null, tint = if (argb == SCREEN_SAVER_DARK_TEXT) Color.White else Color.Black, modifier = Modifier.size(12.dp))
        }
    }
}

private fun screenSaverVisualId(type: WidgetType) = "screensaver_${type.name.lowercase()}"

private fun screenSaverShape(type: WidgetType) = when (type) {
    WidgetType.CLOCK -> RoundedCornerShape(28.dp)
    WidgetType.SPEEDOMETER -> RoundedCornerShape(8.dp)
    WidgetType.DATE -> RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)
    WidgetType.GPS -> RoundedCornerShape(10.dp)
    WidgetType.MUSIC -> RoundedCornerShape(24.dp)
    WidgetType.MAP -> RoundedCornerShape(18.dp)
    WidgetType.TRIP -> RoundedCornerShape(12.dp)
    WidgetType.APPS -> RoundedCornerShape(22.dp)
    WidgetType.CONTROLS -> RoundedCornerShape(20.dp)
}

private const val SCREEN_SAVER_DARK_TEXT = -15724528 // 0xFF101010
private val SCREEN_SAVER_TEXT_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    SCREEN_SAVER_DARK_TEXT,
    0xFFB7C0CC.toInt(),
    0xFF59E6F2.toInt(),
    0xFFFFD166.toInt()
)
private val SCREEN_SAVER_ACCENT_COLORS = listOf(
    0xFF00E5FF.toInt(),
    0xFFFFB84D.toInt(),
    0xFF37E6A1.toInt(),
    0xFFB892FF.toInt(),
    0xFFFF5C75.toInt()
)

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
