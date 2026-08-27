package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.*
import com.example.ui.theme.CarbonCardBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.widgets.*

@Composable
fun ScreenSaverOverlay(
    viewModel: MainViewModel,
    settings: LauncherSettings,
    widgets: List<WidgetItem>,
    apps: List<AppItem>,
    playbackState: MusicPlaybackState,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (settings.screenSaverUseWallpaper) LauncherBackground(settings)

        val selected = settings.screenSaverWidgetTypes.take(4).toList().ifEmpty { listOf(WidgetType.CLOCK) }
        val rows = selected.chunked(2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 54.dp, vertical = 42.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rows.forEach { rowTypes ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowTypes.forEach { type ->
                        val source = widgets.firstOrNull { it.type == type }
                        val style = source?.style ?: defaultScreenSaverStyle(type)
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            color = Color.Black.copy(alpha = .58f),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                        ) {
                            RenderScreenSaverWidget(type, style, viewModel, settings, apps, playbackState, gpsTelemetry, tripData, activeMap)
                        }
                    }
                    if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Text(
            "المس الشاشة للعودة",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )

        // Transparent interaction layer: screensaver widgets are display-only.
        Box(
            Modifier
                .fillMaxSize()
                .zIndex(1000f)
                .clickable(onClick = onDismiss)
        )
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
