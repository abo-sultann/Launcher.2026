package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.bearingToArabicDirection
import kotlinx.coroutines.delay
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Rotation
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import org.mapsforge.map.view.InputListener
import java.io.File
import java.util.Locale

@Composable
fun OfflineMapScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    val mapsList by viewModel.mapsList.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    val mapError by viewModel.mapError.collectAsState()
    val trackPoints by viewModel.offroadTrackPoints.collectAsState()
    val savedPlaces by viewModel.savedOffroadPlaces.collectAsState()
    val navigationTarget by viewModel.offroadNavigationTarget.collectAsState()
    val storedMapState by viewModel.offroadMapState.collectAsState()
    val searchResults by viewModel.offlineSearchResults.collectAsState()
    val transferMessage by viewModel.offroadTransferMessage.collectAsState()

    var showManager by remember { mutableStateOf(false) }
    var showPlaces by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showClearTrackConfirm by remember { mutableStateOf(false) }
    var showSavePlaceDialog by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf("") }
    var mapUiVisible by remember { mutableStateOf(true) }
    var mapInteractionToken by remember { mutableStateOf(System.currentTimeMillis()) }
    var followGps by remember { mutableStateOf(storedMapState.followGps) }
    var orientationMode by remember { mutableStateOf(storedMapState.orientationMode) }
    var offroadTheme by remember { mutableStateOf(false) }

    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importMapUri) }
    val gpxImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importGpxUri) }
    val gpxExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri -> uri?.let(viewModel::exportGpxUri) }
    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importOffroadBackupUri) }
    val backupExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportOffroadBackupUri) }

    LaunchedEffect(mapInteractionToken, showManager, showPlaces, showSearch, showTools, showSavePlaceDialog) {
        if (!showManager && !showPlaces && !showSearch && !showTools && !showSavePlaceDialog) {
            delay(4500L)
            mapUiVisible = false
        }
    }

    LaunchedEffect(activeMap?.id) {
        if (activeMap == null) viewModel.clearOfflineMapSearch()
    }

    val renderedTrack = remember(trackPoints) {
        if (trackPoints.size <= 6000) trackPoints else {
            val step = (trackPoints.size.toFloat() / 6000f).toInt().coerceAtLeast(1)
            trackPoints.filterIndexed { index, _ -> index % step == 0 }.let { sampled -> if (sampled.lastOrNull() == trackPoints.lastOrNull()) sampled else sampled + trackPoints.last() }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFF10151C)).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed && !it.previousPressed }) {
                        mapUiVisible = true
                        mapInteractionToken = System.currentTimeMillis()
                    }
                }
            }
        }
    ) {
        when {
            activeMap == null -> EmptyMapState(onAdd = { mapPicker.launch(arrayOf("application/*", "*/*")) })
            activeMap!!.filePath.endsWith(".map", ignoreCase = true) -> {
                MapsforgeFullScreenMap(
                    mapItem = activeMap!!,
                    gpsLat = gpsTelemetry.latitude,
                    gpsLon = gpsTelemetry.longitude,
                    gpsSpeed = gpsTelemetry.speedKmH,
                    gpsBearing = gpsTelemetry.bearingDegrees,
                    hasGpsFix = gpsTelemetry.hasGpsFix,
                    followGps = followGps,
                    orientationMode = orientationMode,
                    initialState = storedMapState,
                    trackPoints = renderedTrack,
                    navigationTarget = navigationTarget,
                    offroadTheme = offroadTheme,
                    onManualInteraction = { lat, lon, zoom ->
                        if (followGps) followGps = false
                        viewModel.updateOffroadMapState(OffroadMapState(lat, lon, zoom, false, orientationMode))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> UnsupportedMapState(onAdd = { mapPicker.launch(arrayOf("application/*", "*/*")) })
        }

        if (gpsTelemetry.hasGpsFix && activeMap?.filePath?.endsWith(".map", true) == true && followGps) {
            Surface(
                color = CarbonDark.copy(alpha = .72f),
                shape = CircleShape,
                border = BorderStroke(2.dp, CyanNeon),
                modifier = Modifier.align(Alignment.Center).size(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Navigation,
                        "السيارة",
                        tint = CyanNeon,
                        modifier = Modifier.size(34.dp).rotate(if (orientationMode == MapOrientationMode.HEADING_UP) 0f else gpsTelemetry.bearingDegrees)
                    )
                }
            }
        }

        if (mapUiVisible) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SmallMapFab(Icons.Default.Layers, "الخرائط") { showManager = true }
                SmallMapFab(Icons.Default.BookmarkAdd, "علّم موقعي") {
                    if (gpsTelemetry.hasGpsFix) {
                        placeName = ""
                        showSavePlaceDialog = true
                    }
                }
                SmallMapFab(Icons.Default.Place, "المواقع المحفوظة") { showPlaces = true }
                SmallMapFab(Icons.Default.Search, "بحث أوفلاين") { showSearch = true }
                SmallMapFab(Icons.Default.Build, "أدوات البر") { showTools = true }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SmallMapFab(if (followGps) Icons.Default.GpsFixed else Icons.Default.MyLocation, if (followGps) "تتبع تلقائي" else "العودة لموقعي") {
                    followGps = true
                    val z = autoZoomForSpeed(gpsTelemetry.speedKmH).toInt()
                    viewModel.updateOffroadMapState(OffroadMapState(gpsTelemetry.latitude, gpsTelemetry.longitude, z, true, orientationMode))
                    mapInteractionToken = System.currentTimeMillis()
                }
                SmallMapFab(
                    if (orientationMode == MapOrientationMode.HEADING_UP) Icons.Default.Explore else Icons.Default.North,
                    orientationMode.arabicName
                ) {
                    orientationMode = if (orientationMode == MapOrientationMode.NORTH_UP) MapOrientationMode.HEADING_UP else MapOrientationMode.NORTH_UP
                    viewModel.updateOffroadMapState(storedMapState.copy(followGps = followGps, orientationMode = orientationMode))
                    mapInteractionToken = System.currentTimeMillis()
                }
                if (navigationTarget != null) SmallMapFab(Icons.Default.Close, "إيقاف التوجيه") { viewModel.stopOffroadNavigation() }
            }

            MapTelemetryCard(
                gps = gpsTelemetry,
                trip = tripData,
                target = navigationTarget,
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )

            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                color = CarbonDark.copy(alpha = .82f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CarbonCardBorder)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(54.dp).height(3.dp).background(CyanNeon))
                    Text(scaleLabelForZoom(if (followGps) autoZoomForSpeed(gpsTelemetry.speedKmH).toInt() else storedMapState.zoomLevel), color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (trackPoints.isNotEmpty() && gpsTelemetry.hasGpsFix) {
                        val startDistance = viewModel.offroadDistanceToTrackStartMeters()
                        val startBearing = viewModel.offroadBearingToTrackStart()
                        if (startDistance != null && startBearing != null) {
                            Text("• البداية ${formatDistance(startDistance)} ${bearingToArabicDirection(startBearing)}", color = AmberRacing, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        GpsQualityWarning(gpsTelemetry, Modifier.align(Alignment.CenterStart).padding(start = 12.dp))

        if (mapError != null && mapUiVisible) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp), color = HighContrastRed.copy(alpha = .92f), shape = RoundedCornerShape(9.dp)) {
                Text(mapError!!, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        transferMessage?.let { message ->
            LaunchedEffect(message) {
                delay(3500L)
                viewModel.clearOffroadTransferMessage()
            }
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 78.dp), color = CarbonDark.copy(alpha = .94f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, EmeraldSafe)) {
                Text(message, color = EmeraldSafe, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        if (showManager) {
            MapManagerDialog(mapsList, onAdd = { showManager = false; mapPicker.launch(arrayOf("application/*", "*/*")) }, onActivate = viewModel::setActiveMap, onDelete = viewModel::deleteMap, onClose = { showManager = false; mapInteractionToken = System.currentTimeMillis() })
        }
        if (showPlaces) {
            SavedPlacesDialog(
                places = savedPlaces,
                onNavigate = {
                    viewModel.navigateToSavedOffroadPlace(it)
                    savedPlaces.firstOrNull { p -> p.id == it }?.let { p ->
                        followGps = false
                        viewModel.updateOffroadMapState(OffroadMapState(p.latitude, p.longitude, 15, false, orientationMode))
                    }
                    showPlaces = false
                },
                onRename = viewModel::renameSavedOffroadPlace,
                onDelete = viewModel::deleteSavedOffroadPlace,
                onClose = { showPlaces = false; mapInteractionToken = System.currentTimeMillis() }
            )
        }
        if (showSearch) {
            OfflineSearchDialog(
                results = searchResults,
                onSearch = viewModel::searchOfflineMap,
                onNavigate = { result ->
                    viewModel.navigateToSearchResult(result)
                    followGps = false
                    showSearch = false
                },
                onClose = { viewModel.clearOfflineMapSearch(); showSearch = false; mapInteractionToken = System.currentTimeMillis() }
            )
        }
        if (showTools) {
            OffroadToolsDialog(
                offroadTheme = offroadTheme,
                trackDistanceKm = viewModel.offroadTrackDistanceKm(),
                hasTrack = trackPoints.isNotEmpty(),
                onToggleTheme = { offroadTheme = !offroadTheme },
                onImportGpx = { showTools = false; gpxImport.launch(arrayOf("application/gpx+xml", "text/xml", "application/xml", "*/*")) },
                onExportGpx = { gpxExport.launch("Launcher-2026-track.gpx") },
                onImportBackup = { showTools = false; backupImport.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onExportBackup = { backupExport.launch("Launcher-2026-offroad-backup.json") },
                onNavigateStart = { viewModel.navigateToTrackStart(); showTools = false },
                onClearTrack = { showTools = false; showClearTrackConfirm = true },
                onClose = { showTools = false; mapInteractionToken = System.currentTimeMillis() }
            )
        }
        if (showClearTrackConfirm) {
            ConfirmClearTrackDialog(
                onConfirm = { viewModel.clearOffroadTrack(); showClearTrackConfirm = false },
                onDismiss = { showClearTrackConfirm = false }
            )
        }
        if (showSavePlaceDialog) {
            SavePlaceNameDialog(
                name = placeName,
                onNameChange = { placeName = it.take(40) },
                onSave = {
                    viewModel.saveCurrentOffroadPlace(placeName.ifBlank { null })
                    showSavePlaceDialog = false
                },
                onDismiss = { showSavePlaceDialog = false }
            )
        }
    }
}

@Composable
private fun MapTelemetryCard(gps: GpsTelemetry, trip: TripData, target: OffroadNavigationTarget?, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = CarbonDark.copy(alpha = .88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (gps.hasGpsFix) EmeraldSafe else CarbonCardBorder)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(if (gps.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, null, tint = if (gps.hasGpsFix) EmeraldSafe else TextMuted)
                Text("${gps.speedKmH.toInt()} كم/س", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Text(if (gps.hasGpsFix) bearingToArabicDirection(gps.bearingDegrees) else "الاتجاه --", color = AmberRacing, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("رحلة ${String.format(Locale.US, "%.2f", trip.distanceKm)} كم • أثر ${String.format(Locale.US, "%.1f", viewModel.offroadTrackDistanceKm())} كم", color = TextSecondary, fontSize = 9.sp)
            target?.let {
                HorizontalDivider(color = CarbonCardBorder, modifier = Modifier.padding(vertical = 5.dp))
                val distance = viewModel.offroadDistanceToTargetMeters()
                val bearing = viewModel.offroadBearingToTarget()
                Text("إلى: ${it.name}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(buildString { if (distance != null) append(formatDistance(distance)); if (bearing != null) append(" • ${bearingToArabicDirection(bearing)}") }, color = EmeraldSafe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GpsQualityWarning(gps: GpsTelemetry, modifier: Modifier = Modifier) {
    val message = when {
        !gps.hasGpsFix -> "GPS مفقود"
        gps.accuracyMeters > 35f -> "دقة GPS ضعيفة ±${gps.accuracyMeters.toInt()}م"
        gps.satellitesCount in 0..3 -> "إشارة GPS ضعيفة"
        else -> null
    }
    if (message != null) {
        Surface(modifier = modifier, color = if (!gps.hasGpsFix) HighContrastRed.copy(alpha = .90f) else AmberRacing.copy(alpha = .90f), shape = RoundedCornerShape(9.dp)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(17.dp))
                Text(message, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SmallMapFab(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, containerColor = CarbonDark.copy(alpha = .92f), contentColor = CyanNeon, modifier = Modifier.size(44.dp)) {
        Icon(icon, description, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun MapsforgeFullScreenMap(
    mapItem: MapItem,
    gpsLat: Double,
    gpsLon: Double,
    gpsSpeed: Float,
    gpsBearing: Float,
    hasGpsFix: Boolean,
    followGps: Boolean,
    orientationMode: MapOrientationMode,
    initialState: OffroadMapState,
    trackPoints: List<OffroadTrackPoint>,
    navigationTarget: OffroadNavigationTarget?,
    offroadTheme: Boolean,
    onManualInteraction: (Double, Double, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapViewRef by remember(mapItem.id, offroadTheme) { mutableStateOf<MapView?>(null) }
    var mapFileRef by remember(mapItem.id, offroadTheme) { mutableStateOf<MapFile?>(null) }
    var trackLayerRef by remember(mapItem.id, offroadTheme) { mutableStateOf<Polyline?>(null) }
    var navigationLayerRef by remember(mapItem.id, offroadTheme) { mutableStateOf<Polyline?>(null) }
    val latestManualCallback by rememberUpdatedState(onManualInteraction)

    key("${mapItem.id}:$offroadTheme") {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                createMapsforgeView(ctx, mapItem, hasGpsFix, gpsLat, gpsLon, gpsSpeed, initialState, trackPoints, navigationTarget, offroadTheme).also { holder ->
                    mapViewRef = holder.mapView
                    mapFileRef = holder.mapFile
                    trackLayerRef = holder.trackLayer
                    navigationLayerRef = holder.navigationLayer
                    holder.mapView.addInputListener(object : InputListener {
                        private fun notifyAfterGesture() {
                            holder.mapView.postDelayed({
                                val pos = holder.mapView.model.mapViewPosition.mapPosition
                                latestManualCallback(pos.latLong.latitude, pos.latLong.longitude, pos.zoomLevel.toInt())
                            }, 180L)
                        }
                        override fun onMoveEvent() = notifyAfterGesture()
                        override fun onZoomEvent() = notifyAfterGesture()
                    })
                }.mapView
            },
            update = { mapView ->
                if (hasGpsFix && followGps) {
                    mapView.setCenter(LatLong(gpsLat, gpsLon))
                    mapView.setZoomLevel(autoZoomForSpeed(gpsSpeed))
                }
                val rotationDegrees = if (orientationMode == MapOrientationMode.HEADING_UP && hasGpsFix) -gpsBearing else 0f
                mapView.rotate(Rotation(rotationDegrees, mapView.width * .5f, mapView.height * .5f))
                trackLayerRef?.setPoints(trackPoints.map { LatLong(it.latitude, it.longitude) })
                val navPoints = if (hasGpsFix && navigationTarget != null) listOf(LatLong(gpsLat, gpsLon), LatLong(navigationTarget.latitude, navigationTarget.longitude)) else emptyList()
                navigationLayerRef?.setPoints(navPoints)
                mapView.repaint()
            }
        )
    }

    DisposableEffect(mapItem.id, offroadTheme) {
        onDispose {
            try { mapViewRef?.destroyAll() } catch (_: Exception) { }
            try { mapFileRef?.close() } catch (_: Exception) { }
            mapViewRef = null; mapFileRef = null; trackLayerRef = null; navigationLayerRef = null
        }
    }
}

private data class MapHolder(val mapView: MapView, val mapFile: MapFile, val trackLayer: Polyline, val navigationLayer: Polyline)

private fun createMapsforgeView(
    context: Context,
    mapItem: MapItem,
    hasGpsFix: Boolean,
    gpsLat: Double,
    gpsLon: Double,
    gpsSpeed: Float,
    initialState: OffroadMapState,
    trackPoints: List<OffroadTrackPoint>,
    navigationTarget: OffroadNavigationTarget?,
    offroadTheme: Boolean
): MapHolder {
    AndroidGraphicFactory.createInstance(context.applicationContext)
    val mapView = MapView(context).apply { setBuiltInZoomControls(false); isClickable = true }
    val mapFile = MapFile(File(mapItem.filePath))
    val tileCache: TileCache = AndroidUtil.createTileCache(context, "launcher_map_${mapItem.id}_${if (offroadTheme) "offroad" else "detail"}", mapView.model.displayModel.tileSize, 1f, mapView.model.frameBufferModel.overdrawFactor)

    val renderer = TileRendererLayer(tileCache, mapFile, mapView.model.mapViewPosition, AndroidGraphicFactory.INSTANCE).apply {
        setXmlRenderTheme(if (offroadTheme) MapsforgeThemes.MOTORIDER else MapsforgeThemes.DEFAULT)
        textScale = 1.12f
    }
    mapView.layerManager.layers.add(renderer)

    val pinkPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(255, 255, 70, 155)
        strokeWidth = 6f
        setStyle(Style.STROKE)
    }
    val trackLayer = Polyline(pinkPaint, AndroidGraphicFactory.INSTANCE).apply { setPoints(trackPoints.map { LatLong(it.latitude, it.longitude) }) }
    mapView.layerManager.layers.add(trackLayer)

    val navigationPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(235, 255, 184, 0)
        strokeWidth = 4f
        setStyle(Style.STROKE)
    }
    val navigationLayer = Polyline(navigationPaint, AndroidGraphicFactory.INSTANCE).apply {
        if (hasGpsFix && navigationTarget != null) setPoints(listOf(LatLong(gpsLat, gpsLon), LatLong(navigationTarget.latitude, navigationTarget.longitude)))
    }
    mapView.layerManager.layers.add(navigationLayer)

    val fallbackTrack = trackPoints.lastOrNull()?.let { LatLong(it.latitude, it.longitude) }
    val stored = initialState.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }?.let { LatLong(it.latitude, it.longitude) }
    val start = when {
        hasGpsFix && initialState.followGps -> LatLong(gpsLat, gpsLon)
        stored != null -> stored
        fallbackTrack != null -> fallbackTrack
        else -> mapFile.startPosition()
    }
    if (start != null) mapView.setCenter(start)
    val zoom = if (hasGpsFix && initialState.followGps) autoZoomForSpeed(gpsSpeed) else initialState.zoomLevel.coerceIn(3, 20).toByte()
    mapView.setZoomLevel(zoom)
    return MapHolder(mapView, mapFile, trackLayer, navigationLayer)
}

private fun autoZoomForSpeed(speedKmH: Float): Byte = when {
    speedKmH < 5f -> 16
    speedKmH < 20f -> 15
    speedKmH < 50f -> 14
    speedKmH < 90f -> 13
    else -> 12
}.toByte()

private fun scaleLabelForZoom(zoom: Int): String = when {
    zoom >= 17 -> "100 م"
    zoom == 16 -> "200 م"
    zoom == 15 -> "500 م"
    zoom == 14 -> "1 كم"
    zoom == 13 -> "2 كم"
    zoom == 12 -> "5 كم"
    zoom == 11 -> "10 كم"
    else -> "20 كم"
}

@Composable
private fun SavePlaceNameDialog(name: String, onNameChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("علّم موقعي") },
        text = { OutlinedTextField(value = name, onValueChange = onNameChange, singleLine = true, label = { Text("اسم الموقع — مثال: المخيم، البئر") }) },
        confirmButton = { Button(onClick = onSave) { Text("حفظ الموقع") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ConfirmClearTrackDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = HighContrastRed) },
        title = { Text("مسح أثر المسار الحالي؟") },
        text = { Text("سيتم مسح الخط الزهري الحالي فقط. المواقع المحفوظة والخريطة لن تُحذف.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)) { Text("مسح الأثر") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun OffroadToolsDialog(
    offroadTheme: Boolean,
    trackDistanceKm: Double,
    hasTrack: Boolean,
    onToggleTheme: () -> Unit,
    onImportGpx: () -> Unit,
    onExportGpx: () -> Unit,
    onImportBackup: () -> Unit,
    onExportBackup: () -> Unit,
    onNavigateStart: () -> Unit,
    onClearTrack: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.88f).fillMaxHeight(.82f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("أدوات البر", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ToolRow(Icons.Default.Terrain, if (offroadTheme) "ثيم المسارات مفعل" else "ثيم التفاصيل والمدن", "بدّل بين إبراز الطرق والمسارات وبين تفاصيل الأسماء") { onToggleTheme() }
                    }
                    item { ToolRow(Icons.Default.FileOpen, "استيراد GPX", "إضافة مسار ونقاط محفوظة من ملف GPX") { onImportGpx() } }
                    item { ToolRow(Icons.Default.SaveAlt, "تصدير GPX", "حفظ الأثر الحالي والمواقع المحفوظة • ${String.format(Locale.US, "%.1f", trackDistanceKm)} كم") { onExportGpx() } }
                    item { ToolRow(Icons.Default.Restore, "استيراد نسخة احتياطية", "استعادة المسارات والمواقع والتوجيه وحالة الخريطة") { onImportBackup() } }
                    item { ToolRow(Icons.Default.Backup, "نسخة احتياطية كاملة", "تصدير بيانات البر المحلية إلى ملف JSON") { onExportBackup() } }
                    if (hasTrack) {
                        item { ToolRow(Icons.Default.Flag, "التوجيه لبداية المسار", "اتجاه ومسافة مباشرة للعودة لأول نقطة") { onNavigateStart() } }
                        item { ToolRow(Icons.Default.DeleteSweep, "مسح أثر المسار الحالي", "يتطلب تأكيدًا ولا يمس المواقع المحفوظة", destructive = true) { onClearTrack() } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, destructive: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, color = CarbonSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, if (destructive) HighContrastRed.copy(alpha = .6f) else CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, tint = if (destructive) HighContrastRed else CyanNeon)
            Column(Modifier.weight(1f)) {
                Text(title, color = if (destructive) HighContrastRed else TextPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = TextMuted)
        }
    }
}

@Composable
private fun OfflineSearchDialog(results: List<OfflineMapSearchResult>, onSearch: (String) -> Unit, onNavigate: (OfflineMapSearchResult) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.90f).fillMaxHeight(.80f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("بحث أوفلاين", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; if (it.trim().length >= 2) onSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("مدينة، قرية، وادي، طريق، أو موقع محفوظ") }
                )
                Text("يبحث داخل أسماء ملف Mapsforge المحلي والمواقع المحفوظة، بدون إنترنت.", color = TextMuted, fontSize = 9.sp)
                if (query.trim().length < 2) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("اكتب حرفين على الأقل", color = TextSecondary) }
                } else if (results.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لا توجد نتيجة حتى الآن", color = TextSecondary) }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(results, key = { it.id }) { result ->
                            Surface(onClick = { onNavigate(result) }, color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Place, null, tint = if (result.source == "موقع محفوظ") AmberRacing else CyanNeon)
                                    Column(Modifier.weight(1f)) {
                                        Text(result.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(result.source, color = TextSecondary, fontSize = 9.sp)
                                    }
                                    Text("توجيه", color = EmeraldSafe, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlacesDialog(
    places: List<SavedOffroadPlace>,
    onNavigate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    var editing by remember { mutableStateOf<SavedOffroadPlace?>(null) }
    var editName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.90f).fillMaxHeight(.78f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المواقع المحفوظة", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                if (places.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لا توجد مواقع محفوظة بعد", color = TextSecondary) }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(places, key = { it.id }) { place ->
                            Surface(color = CarbonSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CarbonCardBorder)) {
                                Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Place, null, tint = AmberRacing)
                                    Column(Modifier.weight(1f)) {
                                        Text(place.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude), color = TextSecondary, fontSize = 9.sp)
                                    }
                                    TextButton(onClick = { editing = place; editName = place.name }) { Text("تسمية") }
                                    Button(onClick = { onNavigate(place.id) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("توجيه") }
                                    IconButton(onClick = { onDelete(place.id) }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { place ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("تسمية الموقع") },
            text = { OutlinedTextField(value = editName, onValueChange = { editName = it.take(40) }, singleLine = true) },
            confirmButton = { Button(onClick = { onRename(place.id, editName); editing = null }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun MapManagerDialog(maps: List<MapItem>, onAdd: () -> Unit, onActivate: (String) -> Unit, onDelete: (String) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إدارة الخرائط", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Row {
                        Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Icon(Icons.Default.Add, null); Text("إضافة") }
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                    }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(maps, key = { it.id }) { map ->
                        Surface(color = if (map.isActive) CyanNeon.copy(alpha = .14f) else CarbonSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, if (map.isActive) CyanNeon else CarbonCardBorder)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Map, null, tint = if (map.isActive) CyanNeon else TextSecondary)
                                Column(Modifier.weight(1f)) {
                                    Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("${map.fileSizeFormatted} • ${map.dateAdded}", color = TextSecondary, fontSize = 10.sp)
                                    Text(if (map.filePath.endsWith(".map", true)) "Mapsforge • جاهزة للعرض" else "MBTiles • محفوظة", color = if (map.filePath.endsWith(".map", true)) EmeraldSafe else AmberRacing, fontSize = 10.sp)
                                }
                                if (!map.isActive) Button(onClick = { onActivate(map.id) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("عرض") }
                                IconButton(onClick = { onDelete(map.id) }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsupportedMapState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = CarbonDark.copy(alpha = .94f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, AmberRacing), modifier = Modifier.fillMaxWidth(.72f)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Map, null, tint = AmberRacing, modifier = Modifier.size(46.dp))
                Text("الخريطة محفوظة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("للعرض الكامل على هذه الشاشة اختر خريطة Mapsforge بامتداد .map.", color = TextSecondary, fontSize = 13.sp)
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)) { Text("إضافة خريطة .map", color = CarbonDark, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun EmptyMapState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Map, null, tint = TextMuted, modifier = Modifier.size(64.dp))
            Text("لا توجد خريطة مفعلة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)) {
                Icon(Icons.Default.AddLocationAlt, null, tint = CarbonDark); Spacer(Modifier.width(6.dp)); Text("إضافة خريطة", color = CarbonDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatDistance(meters: Float): String = if (meters < 1000f) "${meters.toInt()} م" else String.format(Locale.US, "%.1f كم", meters / 1000f)
