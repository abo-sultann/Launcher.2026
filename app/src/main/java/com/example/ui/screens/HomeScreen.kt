package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.CarScreen
import com.example.ui.components.WidgetFrame
import com.example.ui.components.WidgetLibraryDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.widgets.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
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

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Design Mode Banner (if active)
            if (isDesignMode) {
                Surface(
                    color = AmberRacing.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberRacing),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "وضع التصميم نشط: يمكنك تخصيص الودجات، تبديل الأحجام، وتحريك العناصر",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberRacing
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showLibraryDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).testTag("btn_add_widget_banner")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CarbonDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة ودجت", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CarbonDark)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetWidgetsToDefault() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إعادة الضبط", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Main Widgets Vertical Grid (4 columns in automotive landscape)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                items(
                    items = widgets,
                    key = { it.id },
                    span = { item -> GridItemSpan(item.spanX.coerceIn(1, 4)) }
                ) { widgetItem ->
                    WidgetFrame(
                        widgetItem = widgetItem,
                        isDesignMode = isDesignMode,
                        onChangeStyle = { editingWidgetForStyle = widgetItem },
                        onToggleSize = { viewModel.toggleWidgetSpan(widgetItem.id) },
                        onMoveForward = { viewModel.moveWidget(widgetItem.id, true) },
                        onMoveBackward = { viewModel.moveWidget(widgetItem.id, false) },
                        onDelete = { viewModel.removeWidget(widgetItem.id) },
                        modifier = Modifier.height(138.dp)
                    ) {
                        RenderWidgetContent(
                            widgetItem = widgetItem,
                            viewModel = viewModel,
                            settings = settings,
                            installedApps = installedApps,
                            playbackState = playbackState,
                            gpsTelemetry = gpsTelemetry,
                            tripData = tripData,
                            activeMap = activeMap
                        )
                    }
                }

                // In Design Mode: append "+ إضافة ودجت جديد" card
                if (isDesignMode) {
                    item(span = { GridItemSpan(1) }) {
                        Surface(
                            color = CarbonSurface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .height(138.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showLibraryDialog = true }
                                .testTag("card_add_widget")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة ودجت", tint = CyanNeon, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "إضافة ودجت",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CyanNeon
                                )
                            }
                        }
                    }
                }
            }
        }

        // Widget Library Dialog (Add New)
        if (showLibraryDialog) {
            WidgetLibraryDialog(
                isStyleChangerMode = false,
                onDismiss = { showLibraryDialog = false },
                onSelectStyle = { selectedStyle ->
                    viewModel.addWidget(selectedStyle.type, selectedStyle)
                    showLibraryDialog = false
                }
            )
        }

        // Widget Style Switcher Dialog (Edit Existing)
        if (editingWidgetForStyle != null) {
            val target = editingWidgetForStyle!!
            WidgetLibraryDialog(
                initialType = target.type,
                isStyleChangerMode = true,
                onDismiss = { editingWidgetForStyle = null },
                onSelectStyle = { newStyle ->
                    viewModel.updateWidgetStyle(target.id, newStyle)
                    editingWidgetForStyle = null
                }
            )
        }
    }
}

@Composable
private fun RenderWidgetContent(
    widgetItem: WidgetItem,
    viewModel: MainViewModel,
    settings: LauncherSettings,
    installedApps: List<AppItem>,
    playbackState: MusicPlaybackState,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?
) {
    when (widgetItem.type) {
        WidgetType.CLOCK -> {
            ClockWidget(
                style = widgetItem.style,
                is24Hour = settings.is24HourFormat
            )
        }

        WidgetType.SPEEDOMETER -> {
            SpeedWidget(
                style = widgetItem.style,
                gpsTelemetry = gpsTelemetry,
                tripData = tripData,
                speedUnit = settings.speedUnit
            )
        }

        WidgetType.DATE -> {
            DateWidget(style = widgetItem.style)
        }

        WidgetType.GPS -> {
            GpsWidget(style = widgetItem.style, gpsTelemetry = gpsTelemetry)
        }

        WidgetType.MUSIC -> {
            MusicWidget(
                style = widgetItem.style,
                playbackState = playbackState,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeek = { pos -> viewModel.seekTo(pos) }
            )
        }

        WidgetType.MAP -> {
            MapWidget(
                style = widgetItem.style,
                gpsTelemetry = gpsTelemetry,
                tripData = tripData,
                activeMap = activeMap,
                onOpenFullMap = { viewModel.navigateTo(CarScreen.MAP) }
            )
        }

        WidgetType.TRIP -> {
            TripWidget(
                style = widgetItem.style,
                tripData = tripData,
                onStartTrip = { viewModel.startTrip() },
                onPauseTrip = { viewModel.pauseTrip() },
                onResetTrip = { viewModel.resetTrip() }
            )
        }

        WidgetType.APPS -> {
            AppsWidget(
                style = widgetItem.style,
                installedApps = installedApps,
                onLaunchApp = { pkg -> viewModel.launchApp(pkg) },
                onOpenAppDrawer = { viewModel.navigateTo(CarScreen.APPS) }
            )
        }

        WidgetType.CONTROLS -> {
            ControlsWidget(
                style = widgetItem.style,
                playbackState = playbackState,
                onVolumeAdjust = { delta -> viewModel.adjustVolume(delta) },
                onToggleMute = { viewModel.toggleMute() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() }
            )
        }
    }
}
