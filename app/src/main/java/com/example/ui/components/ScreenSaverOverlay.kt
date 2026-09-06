package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.widgets.*
import com.example.util.bearingToArabicDirection
import java.util.Locale

@Composable
fun ScreenSaverOverlay(
    viewModel: MainViewModel,
    settings: LauncherSettings,
    widgets: List<WidgetItem>,
    layouts: List<ScreenSaverWidgetLayout>,
    playbackState: MusicPlaybackState,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (settings.screenSaverUseWallpaper) LauncherBackground(settings)

        ScreenSaverCanvas(
            viewModel = viewModel,
            settings = settings,
            widgets = widgets,
            layouts = layouts,
            playbackState = playbackState,
            gpsTelemetry = gpsTelemetry,
            tripData = tripData,
            activeMap = activeMap,
            editMode = false,
            modifier = Modifier.fillMaxSize().zIndex(3f)
        )

        Box(Modifier.fillMaxSize().zIndex(1000f).clickable(onClick = onDismiss))
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
    val playback by viewModel.playbackState.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    val trip by viewModel.tripData.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    var showLayoutDialog by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (settings.screenSaverUseWallpaper) LauncherBackground(settings)

        ScreenSaverCanvas(
            viewModel, settings, widgets, layouts, playback, gps, trip, activeMap, true, Modifier.fillMaxSize()
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
                Text("محرر شاشة التوقف", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    playbackState: MusicPlaybackState,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    editMode: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedTypes = WidgetType.values().filter { it in settings.screenSaverWidgetTypes && it in SCREEN_SAVER_DISPLAY_WIDGET_TYPES }.take(4)
    val density = LocalDensity.current
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
                val surfaceStyle = layout.surfaceStyle
                val showBorder = layout.showBorder
                val surfaceOpacity = layout.surfaceOpacity
                val tone = WidgetTone.fromArgb(layout.foregroundColorArgb)
                val foreground = Color(tone.argb)
                val accent = Color(tone.argb)
                val isSelected = editMode && selectedType == type
                val shape = screenSaverShape(type)
                val background = when (surfaceStyle) {
                    WidgetSurfaceStyle.TRANSPARENT -> Color.Transparent
                    WidgetSurfaceStyle.GLASS -> if (tone == WidgetTone.BLACK) {
                        Color.White.copy(alpha = (.26f + .46f * surfaceOpacity).coerceAtMost(.74f))
                    } else {
                        Color.Black.copy(alpha = (.20f + .48f * surfaceOpacity).coerceAtMost(.76f))
                    }
                    WidgetSurfaceStyle.CARD -> if (tone == WidgetTone.BLACK) {
                        Color.White.copy(alpha = (.66f + .32f * surfaceOpacity).coerceAtMost(.98f))
                    } else {
                        Color.Black.copy(alpha = (.64f + .33f * surfaceOpacity).coerceAtMost(.97f))
                    }
                }
                val borderColor = when {
                    isSelected -> interfaceAccent
                    editMode -> AmberRacing.copy(alpha = .28f)
                    showBorder -> foreground.copy(alpha = .72f)
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
                        LocalWidgetVisualTokens provides WidgetVisualTokens(foreground, accent, background.takeIf { it != Color.Transparent }),
                        LocalWidgetForegroundColor provides foreground
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            RenderScreenSaverWidget(type, style, settings, playbackState, gpsTelemetry, tripData, activeMap)

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
                    layout = layout,
                    onSurface = { viewModel.setScreenSaverSurface(type, it) },
                    onBorder = { viewModel.toggleScreenSaverBorder(type) },
                    onTone = { viewModel.setScreenSaverTone(type, it) },
                    onSurfaceOpacity = { viewModel.setScreenSaverSurfaceOpacity(type, it) },
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
    layout: ScreenSaverWidgetLayout,
    onSurface: (WidgetSurfaceStyle) -> Unit,
    onBorder: () -> Unit,
    onTone: (WidgetTone) -> Unit,
    onSurfaceOpacity: (Float) -> Unit,
    onCycleStyle: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val surface = layout.surfaceStyle
    val border = layout.showBorder
    val tone = WidgetTone.fromArgb(layout.foregroundColorArgb)
    val backgroundOpacity = layout.surfaceOpacity

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
                            onSurface(option)
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
                        onBorder()
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
                    onClick = { onSurfaceOpacity(backgroundOpacity - .10f) },
                    modifier = Modifier.size(28.dp)
                ) { Icon(Icons.Default.Remove, "الخلفية أخف", tint = TextPrimary, modifier = Modifier.size(15.dp)) }
                Text("${(backgroundOpacity * 100).toInt()}%", color = accentColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { onSurfaceOpacity(backgroundOpacity + .10f) },
                    modifier = Modifier.size(28.dp)
                ) { Icon(Icons.Default.Add, "الخلفية أوضح", tint = TextPrimary, modifier = Modifier.size(15.dp)) }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("لون الودجت", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                WidgetTone.values().forEach { option ->
                    ScreenSaverToneChoice(option, tone == option, accentColor) { onTone(option) }
                }
                Text("الأبيض والأسود فقط؛ شكل الخلفية يصنع الفرق بين بسيط، فاخر وزجاجي.", color = TextMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ScreenSaverToneChoice(tone: WidgetTone, selected: Boolean, selectionColor: Color, onSelect: () -> Unit) {
    val color = Color(tone.argb)
    Surface(
        onClick = onSelect,
        color = color,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) selectionColor else CarbonCardBorder),
        modifier = Modifier.size(width = 54.dp, height = 25.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (selected) Icon(Icons.Default.Check, null, tint = if (tone == WidgetTone.BLACK) Color.White else Color.Black, modifier = Modifier.size(11.dp))
                Text(tone.arabicName, color = if (tone == WidgetTone.BLACK) Color.White else Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

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
    WidgetType.MAINTENANCE -> RoundedCornerShape(18.dp)
}

@Composable
private fun RenderScreenSaverWidget(
    type: WidgetType,
    style: WidgetStyle,
    settings: LauncherSettings,
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
        WidgetType.MUSIC -> ScreenSaverMusicSummary(style, playback)
        WidgetType.MAP -> ScreenSaverMapSummary(style, gps, trip, map)
        WidgetType.TRIP -> ScreenSaverTripSummary(style, trip)
        WidgetType.APPS, WidgetType.CONTROLS, WidgetType.MAINTENANCE -> Unit
    }
}

@Composable
private fun ScreenSaverMusicSummary(style: WidgetStyle, playback: MusicPlaybackState) {
    val colors = resolvedWidgetColors()
    val title = playback.currentTrack?.title ?: "لا توجد موسيقى"
    val artist = playback.currentTrack?.artist ?: ""
    val progress = if (playback.durationMs > 0L) {
        (playback.currentPositionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
    } else 0f
    Row(
        Modifier.fillMaxSize().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (style != WidgetStyle.MUSIC_MINIMAL) {
            Icon(Icons.Default.MusicNote, null, tint = colors.accent, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, color = colors.primary, fontWeight = FontWeight.Black, maxLines = 1)
            if (artist.isNotBlank()) Text(artist, color = colors.secondary, fontSize = 10.sp, maxLines = 1)
            if (style == WidgetStyle.MUSIC_COMPACT) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = colors.accent,
                    trackColor = colors.secondary.copy(alpha = .22f),
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                )
            }
        }
        Text(if (playback.isPlaying) "قيد التشغيل" else "متوقف", color = colors.secondary, fontSize = 9.sp)
    }
}

@Composable
private fun ScreenSaverTripSummary(style: WidgetStyle, trip: TripData) {
    val colors = resolvedWidgetColors()
    val minutes = trip.elapsedMovingTimeSec / 60
    val seconds = trip.elapsedMovingTimeSec % 60
    Row(
        Modifier.fillMaxSize().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ScreenSaverMetric("المسافة", String.format(Locale.US, "%.1f كم", trip.distanceKm), colors.primary)
        ScreenSaverMetric("المدة", String.format(Locale.US, "%02d:%02d", minutes, seconds), colors.accent)
        if (style != WidgetStyle.TRIP_SPEED_DISTANCE) {
            ScreenSaverMetric("المتوسط", "${trip.averageSpeedKmH.toInt()} كم/س", colors.secondary)
        }
    }
}

@Composable
private fun ScreenSaverMapSummary(style: WidgetStyle, gps: GpsTelemetry, trip: TripData, map: MapItem?) {
    val colors = resolvedWidgetColors()
    Row(
        Modifier.fillMaxSize().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (gps.hasGpsFix) Icons.Default.Navigation else Icons.Default.GpsNotFixed,
            null,
            tint = colors.accent,
            modifier = Modifier.size(32.dp).rotate(if (gps.hasGpsFix) gps.bearingDegrees else 0f)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(map?.name ?: "لا توجد خريطة", color = colors.primary, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                if (gps.hasGpsFix) "${bearingToArabicDirection(gps.bearingDegrees)} • دقة ±${gps.accuracyMeters.toInt()}م" else "بانتظار GPS",
                color = colors.secondary,
                fontSize = 9.sp,
                maxLines = 1
            )
            if (style == WidgetStyle.MAP_LARGE) {
                Text(String.format(Locale.US, "رحلة %.1f كم", trip.distanceKm), color = colors.accent, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ScreenSaverMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1)
        Text(label, color = resolvedWidgetColors().secondary, fontSize = 8.sp)
    }
}

private fun defaultScreenSaverStyle(type: WidgetType): WidgetStyle = when (type) {
    WidgetType.CLOCK -> WidgetStyle.CLOCK_AUTOMOTIVE_LARGE
    WidgetType.SPEEDOMETER -> WidgetStyle.SPEED_DIGITAL_LARGE
    WidgetType.DATE -> WidgetStyle.DATE_DAY_DATE
    WidgetType.GPS -> WidgetStyle.GPS_CARD
    WidgetType.MUSIC -> WidgetStyle.MUSIC_COMPACT
    WidgetType.MAP -> WidgetStyle.MAP_WITH_GPS
    WidgetType.TRIP -> WidgetStyle.TRIP_CARD
    WidgetType.APPS -> WidgetStyle.APPS_ICONS_ONLY
    WidgetType.CONTROLS -> WidgetStyle.CONTROLS_HORIZONTAL_BAR
    WidgetType.MAINTENANCE -> WidgetStyle.MAINTENANCE_VERTICAL
}
