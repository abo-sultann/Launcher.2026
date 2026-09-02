package com.example.ui.screens

import android.content.Context
import android.location.Location
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.bearingToArabicDirection
import kotlinx.coroutines.delay
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Rotation
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.mbtiles.MBTilesFile
import org.mapsforge.map.android.mbtiles.TileMBTilesLayer
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.InMemoryTileCache
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.FixedPixelCircle
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import org.mapsforge.map.view.InputListener
import java.io.File
import java.util.Locale
import kotlin.math.abs

private data class UiOffroadPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: OffroadPlaceKind,
    val notes: String,
    val favorite: Boolean,
    val createdAt: Long,
    val legacy: Boolean
)

@Composable
fun EnhancedOfflineMapScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapStore = remember { EnhancedMapStore(context.applicationContext) }
    val mapUi by mapStore.ui.collectAsState()
    val extraPlaces by mapStore.extraPlaces.collectAsState()
    val placeKinds by mapStore.placeKinds.collectAsState()

    val gps by viewModel.gpsTelemetry.collectAsState()
    val trip by viewModel.tripData.collectAsState()
    val maps by viewModel.mapsList.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    val mapError by viewModel.mapError.collectAsState()
    val trackPoints by viewModel.offroadTrackPoints.collectAsState()
    val legacyPlaces by viewModel.savedOffroadPlaces.collectAsState()
    val navTarget by viewModel.offroadNavigationTarget.collectAsState()
    val storedMapState by viewModel.offroadMapState.collectAsState()
    val searchResults by viewModel.offlineSearchResults.collectAsState()
    val searchInProgress by viewModel.offlineSearchInProgress.collectAsState()
    val transferMessage by viewModel.offroadTransferMessage.collectAsState()
    val fileImportStatus by viewModel.fileImportStatus.collectAsState()
    val launcherSettings by viewModel.settings.collectAsState()
    val interfaceAccent = Color(launcherSettings.interfaceAccent.argb)

    var showTools by remember { mutableStateOf(false) }
    var showPlaces by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showMaps by remember { mutableStateOf(false) }
    var showSaveCurrent by remember { mutableStateOf(false) }
    var showLongPressActions by remember { mutableStateOf(false) }
    var longPressPoint by remember { mutableStateOf<LatLong?>(null) }
    var savePoint by remember { mutableStateOf<LatLong?>(null) }
    var saveName by remember { mutableStateOf("") }
    var saveNotes by remember { mutableStateOf("") }
    var saveKind by remember { mutableStateOf(OffroadPlaceKind.FLAG) }
    var saveFavorite by remember { mutableStateOf(false) }
    var navigateAfterSave by remember { mutableStateOf(false) }
    var saveIsCurrent by remember { mutableStateOf(false) }

    var followGps by remember { mutableStateOf(storedMapState.followGps) }
    var orientationMode by remember { mutableStateOf(storedMapState.orientationMode) }
    var measureA by remember { mutableStateOf<LatLong?>(null) }
    var measureB by remember { mutableStateOf<LatLong?>(null) }

    var smoothBearing by remember { mutableFloatStateOf(gps.bearingDegrees) }
    LaunchedEffect(gps.bearingDegrees) {
        smoothBearing = smoothAngle(smoothBearing, gps.bearingDegrees, .28f)
    }

    var autoZoom by remember { mutableIntStateOf(autoZoomForSpeedEnhanced(gps.speedKmH)) }
    val latestSpeed by rememberUpdatedState(gps.speedKmH)
    LaunchedEffect(followGps) {
        while (followGps) {
            val target = autoZoomForSpeedEnhanced(latestSpeed)
            autoZoom = when {
                target > autoZoom -> autoZoom + 1
                target < autoZoom -> autoZoom - 1
                else -> autoZoom
            }.coerceIn(11, 17)
            delay(2200L)
        }
    }

    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(viewModel::importMapUri) }
    val gpxImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importGpxUri) }
    val gpxExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri -> uri?.let(viewModel::exportGpxUri) }
    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importOffroadBackupUri) }
    val backupExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportOffroadBackupUri) }
    val launchMapPicker = {
        viewModel.prepareForExternalPicker()
        mapPicker.launch("*/*")
    }

    val allPlaces = remember(legacyPlaces, extraPlaces, placeKinds) {
        (legacyPlaces.map { p ->
            UiOffroadPlace(p.id, p.name, p.latitude, p.longitude, placeKinds[p.id] ?: OffroadPlaceKind.FLAG, "", false, p.createdAt, true)
        } + extraPlaces.map { p -> UiOffroadPlace(p.id, p.name, p.latitude, p.longitude, p.kind, p.notes, p.favorite, p.createdAt, false) })
            .sortedWith(compareByDescending<UiOffroadPlace> { it.favorite }.thenByDescending { it.createdAt })
    }

    val renderedTrack = remember(trackPoints) { sampleTrack(trackPoints, 3200) }
    val nearestTrack = remember(renderedTrack, gps.latitude, gps.longitude, gps.hasGpsFix) {
        if (!gps.hasGpsFix || renderedTrack.isEmpty()) null else nearestTrackPoint(renderedTrack, gps.latitude, gps.longitude)
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color(0xFF10151C))) {
        when {
            activeMap == null -> EnhancedEmptyMapState(launchMapPicker)
            activeMap!!.filePath.isSupportedOfflineMap() -> {
                EnhancedMapsforgeMap(
                    mapItem = activeMap!!,
                    gps = gps,
                    smoothedBearing = smoothBearing,
                    followGps = followGps,
                    orientationMode = orientationMode,
                    autoZoom = autoZoom,
                    initialState = storedMapState,
                    trackPoints = renderedTrack,
                    trackVisible = mapUi.trackVisible,
                    trackWidth = mapUi.trackWidth,
                    navigationTarget = navTarget,
                    measureA = measureA,
                    measureB = measureB,
                    detailedTheme = mapUi.detailedTheme,
                    drivingView = mapUi.drivingView,
                    onManualInteraction = { lat, lon, zoom ->
                        if (followGps) followGps = false
                        viewModel.updateOffroadMapState(OffroadMapState(lat, lon, zoom, false, orientationMode))
                    },
                    onLongPress = { point ->
                        longPressPoint = point
                        showLongPressActions = true
                    },
                    onRenderError = viewModel::reportMapError,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> EnhancedUnsupportedMapState(launchMapPicker)
        }

        if (mapUi.nightMap) {
            Box(Modifier.fillMaxSize().background(Color(0x66020A12)))
        }

        if (gps.hasGpsFix && activeMap?.filePath?.isSupportedOfflineMap() == true && followGps) {
            val yOffset = if (mapUi.drivingView) 86.dp else 0.dp
            Surface(
                color = CarbonDark.copy(alpha = .78f),
                shape = CircleShape,
                border = BorderStroke(2.dp, interfaceAccent),
                modifier = Modifier.align(Alignment.Center).offset(y = yOffset).size(48.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Navigation,
                        "موقع السيارة",
                        tint = interfaceAccent,
                        modifier = Modifier.size(32.dp).rotate(if (orientationMode == MapOrientationMode.HEADING_UP) 0f else smoothBearing)
                    )
                }
            }
        }

        if (mapUi.showPrimaryActions) {
            Surface(
                modifier = mapOverlayModifier(mapUi.primaryActionsSlot),
                color = CarbonDark.copy(alpha = .78f),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, CarbonCardBorder.copy(alpha = .65f)),
                shadowElevation = 3.dp
            ) {
                Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    MapPrimaryActionButton(Icons.Default.BookmarkAdd, "حفظ موقعي", interfaceAccent) {
                        if (gps.hasGpsFix) {
                            saveIsCurrent = true
                            savePoint = LatLong(gps.latitude, gps.longitude)
                            saveName = ""
                            saveNotes = ""
                            saveKind = OffroadPlaceKind.FLAG
                            saveFavorite = false
                            navigateAfterSave = false
                            showSaveCurrent = true
                        }
                    }
                    MapPrimaryActionButton(Icons.Default.Place, "المحفوظة", interfaceAccent) { showPlaces = true }
                    MapPrimaryActionButton(Icons.Default.Navigation, "بحث وتوجيه", interfaceAccent) { showSearch = true }
                    MapPrimaryActionButton(Icons.Default.Tune, "إعدادات", interfaceAccent) { showTools = true }
                }
            }
        }

        Surface(
            modifier = mapOverlayModifier(mapUi.dockSlot),
            color = CarbonDark.copy(alpha = .88f),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, CarbonCardBorder),
            shadowElevation = 5.dp
        ) {
            Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MapDockButton(
                    if (followGps) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                    if (followGps) "تتبع موقعي" else "العودة لموقعي",
                    interfaceAccent,
                    selected = followGps
                ) {
                    followGps = true
                    autoZoom = autoZoomForSpeedEnhanced(gps.speedKmH)
                    if (gps.hasGpsFix) viewModel.updateOffroadMapState(OffroadMapState(gps.latitude, gps.longitude, autoZoom, true, orientationMode))
                }
                MapDockButton(
                    if (orientationMode == MapOrientationMode.HEADING_UP) Icons.Default.Explore else Icons.Default.North,
                    orientationMode.arabicName,
                    interfaceAccent,
                    selected = orientationMode == MapOrientationMode.HEADING_UP
                ) {
                    orientationMode = if (orientationMode == MapOrientationMode.NORTH_UP) MapOrientationMode.HEADING_UP else MapOrientationMode.NORTH_UP
                    viewModel.updateOffroadMapState(storedMapState.copy(followGps = followGps, orientationMode = orientationMode))
                }
                MapDockButton(Icons.Default.Tune, "إعدادات الخريطة", interfaceAccent) { showTools = true }
            }
        }

        if (mapUi.showTelemetry) {
            EnhancedTelemetryCard(
                gps = gps,
                modifier = mapOverlayModifier(mapUi.telemetrySlot)
            )
        }

        navTarget?.let { target ->
            NavigationTargetStrip(
                target = target,
                distance = viewModel.offroadDistanceToTargetMeters(),
                bearing = viewModel.offroadBearingToTarget()?.let { targetBearing ->
                    if (orientationMode == MapOrientationMode.HEADING_UP) shortestAngleDelta(gps.bearingDegrees, targetBearing) else targetBearing
                },
                onStop = viewModel::stopOffroadNavigation,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
            )
        }

        if (mapUi.showMapScale) {
            Surface(
                modifier = mapOverlayModifier(mapUi.scaleSlot),
                color = CarbonDark.copy(alpha = .76f),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, CarbonCardBorder.copy(alpha = .65f))
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.width(48.dp).height(3.dp).background(interfaceAccent))
                    Text(scaleLabel(autoZoom), color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        nearestTrack?.let { nearest ->
            if (nearest.second > 80f) {
                Surface(
                    onClick = {
                        viewModel.navigateToSearchResult(OfflineMapSearchResult("nearest_track", "أقرب نقطة للمسار", nearest.first.latitude, nearest.first.longitude, "أثر المسار"))
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                    color = AmberRacing.copy(alpha = .92f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Route, null, tint = CarbonDark, modifier = Modifier.size(18.dp))
                        Text("ارجع للمسار ${formatDistanceEnhanced(nearest.second)}", color = CarbonDark, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
        }

        if (measureA != null || measureB != null) {
            MeasurementCard(
                a = measureA,
                b = measureB,
                onClear = { measureA = null; measureB = null },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 65.dp)
            )
        }

        mapError?.let { error ->
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 105.dp), color = HighContrastRed.copy(alpha = .92f), shape = RoundedCornerShape(9.dp)) {
                Text(error, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 10.sp)
            }
        }

        transferMessage?.let { msg ->
            LaunchedEffect(msg) { delay(3500L); viewModel.clearOffroadTransferMessage() }
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 105.dp), color = CarbonDark.copy(alpha = .95f), shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, EmeraldSafe)) {
                Text(msg, color = EmeraldSafe, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }

        fileImportStatus?.let { status ->
            val inProgress = status.startsWith("جارٍ")
            LaunchedEffect(status) {
                if (!inProgress) {
                    delay(4500L)
                    viewModel.clearFileImportStatus()
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 142.dp),
                color = CarbonDark.copy(alpha = .96f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (status.startsWith("تم")) EmeraldSafe else if (inProgress) interfaceAccent else HighContrastRed)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (inProgress) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = interfaceAccent)
                    else Icon(if (status.startsWith("تم")) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = if (status.startsWith("تم")) EmeraldSafe else HighContrastRed, modifier = Modifier.size(18.dp))
                    Text(status, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showSaveCurrent) {
            EnhancedSavePlaceDialog(
                point = savePoint,
                name = saveName,
                notes = saveNotes,
                kind = saveKind,
                favorite = saveFavorite,
                navigateAfterSave = navigateAfterSave,
                onName = { saveName = it.take(40) },
                onNotes = { saveNotes = it.take(120) },
                onKind = { saveKind = it },
                onFavorite = { saveFavorite = it },
                onNavigateAfterSave = { navigateAfterSave = it },
                onSave = {
                    savePoint?.let { point ->
                        val saved = mapStore.saveExtraPlace(
                            saveName.ifBlank { if (saveIsCurrent) "موقعي الحالي" else "نقطة محفوظة" },
                            point.latitude,
                            point.longitude,
                            saveKind,
                            saveNotes,
                            saveFavorite
                        )
                        if (navigateAfterSave) {
                            viewModel.navigateToSearchResult(OfflineMapSearchResult(saved.id, saved.name, saved.latitude, saved.longitude, "موقع محفوظ"))
                        }
                    }
                    showSaveCurrent = false
                },
                onDismiss = { showSaveCurrent = false }
            )
        }

        if (showLongPressActions) {
            longPressPoint?.let { point ->
                LongPressMapPointDialog(
                    point = point,
                    onSave = {
                        saveIsCurrent = false
                        savePoint = point
                        saveName = ""
                        saveNotes = ""
                        saveKind = OffroadPlaceKind.FLAG
                        saveFavorite = false
                        navigateAfterSave = false
                        showLongPressActions = false
                        showSaveCurrent = true
                    },
                    onNavigate = {
                        viewModel.navigateToSearchResult(OfflineMapSearchResult("map_point_${System.currentTimeMillis()}", "نقطة على الخريطة", point.latitude, point.longitude, "نقطة مختارة"))
                        showLongPressActions = false
                    },
                    onMeasureA = { measureA = point; showLongPressActions = false },
                    onMeasureB = { measureB = point; showLongPressActions = false },
                    onDismiss = { showLongPressActions = false }
                )
            }
        }

        if (showPlaces) {
            EnhancedPlacesDialog(
                places = allPlaces,
                onNavigate = { p ->
                    if (p.legacy) viewModel.navigateToSavedOffroadPlace(p.id)
                    else viewModel.navigateToSearchResult(OfflineMapSearchResult(p.id, p.name, p.latitude, p.longitude, "موقع محفوظ"))
                    showPlaces = false
                },
                onUpdate = { p, name, kind, notes, favorite ->
                    if (p.legacy) {
                        viewModel.renameSavedOffroadPlace(p.id, name)
                        mapStore.setPlaceKind(p.id, kind)
                    } else {
                        mapStore.updateExtraPlace(p.id, name, kind, notes, favorite)
                    }
                },
                onDelete = { p -> if (p.legacy) viewModel.deleteSavedOffroadPlace(p.id) else mapStore.deleteExtraPlace(p.id) },
                onClose = { showPlaces = false }
            )
        }

        if (showSearch) {
            EnhancedSearchDialog(
                results = searchResults,
                extras = extraPlaces,
                isSearching = searchInProgress,
                onSearch = viewModel::searchOfflineMap,
                onNavigate = { result -> viewModel.navigateToSearchResult(result); showSearch = false },
                onClose = { viewModel.clearOfflineMapSearch(); showSearch = false }
            )
        }

        if (showMaps) {
            EnhancedMapManagerDialog(
                maps = maps,
                onAdd = { showMaps = false; launchMapPicker() },
                onActivate = viewModel::setActiveMap,
                onDelete = viewModel::deleteMap,
                onClose = { showMaps = false }
            )
        }

        if (showTools) {
            EnhancedOffroadToolsDialog(
                ui = mapUi,
                trackKm = viewModel.offroadTrackDistanceKm(),
                hasTrack = trackPoints.isNotEmpty(),
                nearestDistance = nearestTrack?.second,
                onUiChange = mapStore::updateUi,
                onMaps = { showTools = false; showMaps = true },
                onImportGpx = {
                    showTools = false
                    viewModel.prepareForExternalPicker()
                    gpxImport.launch(arrayOf("application/gpx+xml", "text/xml", "application/xml", "*/*"))
                },
                onExportGpx = { gpxExport.launch("Launcher-2026-track.gpx") },
                onImportBackup = {
                    showTools = false
                    viewModel.prepareForExternalPicker()
                    backupImport.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onExportBackup = { backupExport.launch("Launcher-2026-offroad-backup.json") },
                onNavigateStart = { viewModel.navigateToTrackStart(); showTools = false },
                onNearestTrack = {
                    nearestTrack?.let { n -> viewModel.navigateToSearchResult(OfflineMapSearchResult("nearest_track", "أقرب نقطة للمسار", n.first.latitude, n.first.longitude, "أثر المسار")) }
                    showTools = false
                },
                onClearTrack = { viewModel.clearOffroadTrack(); showTools = false },
                onClose = { showTools = false }
            )
        }
    }
}

private fun BoxWithConstraintsScope.mapOverlayModifier(slot: MapOverlaySlot): Modifier {
    // المواقع هنا مطلقة بصريًا؛ لا تنعكس بسبب اتجاه اللغة العربية.
    val alignment = when (slot) {
        MapOverlaySlot.TOP_START -> AbsoluteAlignment.TopLeft
        MapOverlaySlot.TOP_CENTER -> Alignment.TopCenter
        MapOverlaySlot.TOP_END -> AbsoluteAlignment.TopRight
        MapOverlaySlot.CENTER_START -> AbsoluteAlignment.CenterLeft
        MapOverlaySlot.CENTER_END -> AbsoluteAlignment.CenterRight
        MapOverlaySlot.BOTTOM_START -> AbsoluteAlignment.BottomLeft
        MapOverlaySlot.BOTTOM_CENTER -> Alignment.BottomCenter
        MapOverlaySlot.BOTTOM_END -> AbsoluteAlignment.BottomRight
    }
    val bottomInset = if (slot.name.startsWith("BOTTOM")) 62.dp else 10.dp
    return Modifier.align(alignment).padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = bottomInset)
}

private fun nextMapOverlaySlot(current: MapOverlaySlot): MapOverlaySlot {
    val slots = MapOverlaySlot.entries
    return slots[(current.ordinal + 1) % slots.size]
}

@Composable
private fun EnhancedTelemetryCard(
    gps: GpsTelemetry,
    modifier: Modifier = Modifier
) {
    // السرعة على الخريطة رقم فقط؛ بلا بطاقة مرنة كي لا تتحول إلى شريط داكن.
    Box(
        modifier = modifier.size(82.dp, 62.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (gps.hasGpsFix && gps.isSpeedReliable) gps.speedKmH.toInt().toString() else "--",
            color = Color.Black,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            maxLines = 1
        )
    }
}
@Composable
private fun NavigationTargetStrip(
    target: OffroadNavigationTarget,
    distance: Float?,
    bearing: Float?,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 340.dp),
        color = Color.White.copy(alpha = .88f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AmberRacing)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = AmberRacing, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Navigation,
                        "اتجاه الهدف",
                        tint = Color.Black,
                        modifier = Modifier.size(31.dp).rotate(bearing ?: 0f)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(target.name, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
                Text(
                    buildString {
                        if (distance != null) append(formatDistanceEnhanced(distance))
                        if (bearing != null) append(" • ${bearingToArabicDirection(bearing)}")
                    },
                    color = Color(0xFF146B3A),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, "إيقاف التوجيه", tint = HighContrastRed)
            }
        }
    }
}
@Composable
private fun MeasurementCard(a: LatLong?, b: LatLong?, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val distance = if (a != null && b != null) distanceMetersEnhanced(a.latitude, a.longitude, b.latitude, b.longitude) else null
    Surface(modifier = modifier, color = CarbonDark.copy(alpha = .92f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CyanNeon)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Straighten, null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Text(if (distance != null) "المسافة ${formatDistanceEnhanced(distance)}" else "حدد نقطتي القياس بالضغط المطول", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            IconButton(onClick = onClear, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Close, "مسح القياس", tint = TextMuted) }
        }
    }
}

@Composable
private fun MapDockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    accentColor: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) accentColor.copy(alpha = .18f) else Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        border = if (selected) BorderStroke(1.dp, accentColor.copy(alpha = .55f)) else null,
        modifier = Modifier.size(44.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = if (selected) accentColor else TextPrimary, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun MapPrimaryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, label, tint = accentColor, modifier = Modifier.size(19.dp))
            Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun EnhancedSavePlaceDialog(
    point: LatLong?,
    name: String,
    notes: String,
    kind: OffroadPlaceKind,
    favorite: Boolean,
    navigateAfterSave: Boolean,
    onName: (String) -> Unit,
    onNotes: (String) -> Unit,
    onKind: (OffroadPlaceKind) -> Unit,
    onFavorite: (Boolean) -> Unit,
    onNavigateAfterSave: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حفظ الموقع", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                point?.let {
                    Surface(color = CarbonSurface, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude), color = TextSecondary, fontSize = 9.sp)
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = onName, label = { Text("اسم واضح للموقع") }, placeholder = { Text("مثال: مخيم الشتاء") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = onNotes, label = { Text("ملاحظة اختيارية") }, placeholder = { Text("طريق الدخول أو وصف المكان") }, maxLines = 2)
                Text("نوع العلامة", fontWeight = FontWeight.Bold)
                OffroadPlaceKind.entries.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { item -> FilterChip(selected = kind == item, onClick = { onKind(item) }, label = { Text(item.arabicName, fontSize = 9.sp) }) }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("تثبيت في أعلى المحفوظات", modifier = Modifier.weight(1f), fontSize = 10.sp)
                    Switch(checked = favorite, onCheckedChange = onFavorite)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("بدء التوجيه بعد الحفظ", modifier = Modifier.weight(1f), fontSize = 10.sp)
                    Switch(checked = navigateAfterSave, onCheckedChange = onNavigateAfterSave)
                }
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = point != null) { Text("حفظ الموقع") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun LongPressMapPointDialog(point: LatLong, onSave: () -> Unit, onNavigate: () -> Unit, onMeasureA: () -> Unit, onMeasureB: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نقطة على الخريطة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(String.format(Locale.US, "%.6f, %.6f", point.latitude, point.longitude), color = CyanNeon, fontWeight = FontWeight.Bold)
                TextButton(onClick = onSave) { Icon(Icons.Default.BookmarkAdd, null); Spacer(Modifier.width(6.dp)); Text("حفظ الموقع") }
                TextButton(onClick = onNavigate) { Icon(Icons.Default.Navigation, null); Spacer(Modifier.width(6.dp)); Text("التوجيه لهذه النقطة") }
                TextButton(onClick = onMeasureA) { Icon(Icons.Default.LooksOne, null); Spacer(Modifier.width(6.dp)); Text("تعيين كنقطة القياس الأولى") }
                TextButton(onClick = onMeasureB) { Icon(Icons.Default.LooksTwo, null); Spacer(Modifier.width(6.dp)); Text("تعيين كنقطة القياس الثانية") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun EnhancedPlacesDialog(
    places: List<UiOffroadPlace>,
    onNavigate: (UiOffroadPlace) -> Unit,
    onUpdate: (UiOffroadPlace, String, OffroadPlaceKind, String, Boolean) -> Unit,
    onDelete: (UiOffroadPlace) -> Unit,
    onClose: () -> Unit
) {
    var editing by remember { mutableStateOf<UiOffroadPlace?>(null) }
    var pendingDelete by remember { mutableStateOf<UiOffroadPlace?>(null) }
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon)) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("المواقع المحفوظة", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("المثبتة أولًا • تعديل الاسم والوصف والعلامة", color = TextSecondary, fontSize = 9.sp)
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                if (places.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لا توجد مواقع محفوظة", color = TextSecondary) }
                else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(places, key = { it.id }) { p ->
                        Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, if (p.favorite) AmberRacing else CarbonCardBorder)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(if (p.favorite) Icons.Default.Star else placeKindIcon(p.kind), p.kind.arabicName, tint = if (p.favorite) AmberRacing else CyanNeon)
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(if (p.notes.isNotBlank()) p.notes else p.kind.arabicName, color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { editing = p }) { Icon(Icons.Default.Edit, "تعديل", tint = TextSecondary) }
                                Button(onClick = { onNavigate(p) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("توجيه") }
                                IconButton(onClick = { pendingDelete = p }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                            }
                        }
                    }
                }
            }
        }
    }
    editing?.let { p ->
        var editName by remember(p.id) { mutableStateOf(p.name) }
        var editNotes by remember(p.id) { mutableStateOf(p.notes) }
        var kind by remember(p.id) { mutableStateOf(p.kind) }
        var favorite by remember(p.id) { mutableStateOf(p.favorite) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("تعديل الموقع") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it.take(40) }, label = { Text("اسم الموقع") }, singleLine = true)
                    OutlinedTextField(value = editNotes, onValueChange = { editNotes = it.take(120) }, label = { Text("ملاحظة") }, maxLines = 2, enabled = !p.legacy)
                    OffroadPlaceKind.entries.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { k -> AssistChip(onClick = { kind = k }, label = { Text(k.arabicName, fontSize = 8.sp) }, leadingIcon = { if (kind == k) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (p.legacy) "التثبيت متاح للمواقع الجديدة" else "تثبيت في الأعلى", modifier = Modifier.weight(1f), fontSize = 9.sp, color = TextSecondary)
                        Switch(checked = favorite, onCheckedChange = { favorite = it }, enabled = !p.legacy)
                    }
                    Text(String.format(Locale.US, "%.6f, %.6f", p.latitude, p.longitude), color = TextMuted, fontSize = 9.sp)
                }
            },
            confirmButton = { Button(onClick = { onUpdate(p, editName, kind, editNotes, favorite); editing = null }) { Text("حفظ التعديل") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } }
        )
    }
    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف الموقع؟") },
            text = { Text("سيُحذف «${p.name}» من المواقع المحفوظة.") },
            confirmButton = { Button(onClick = { onDelete(p); pendingDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)) { Text("حذف", color = Color.White) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun EnhancedSearchDialog(
    results: List<OfflineMapSearchResult>,
    extras: List<EnhancedSavedPlace>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onNavigate: (OfflineMapSearchResult) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val categories = remember {
        listOf(
            Triple("وقود", "محطة وقود", Icons.Default.LocalGasStation),
            Triple("مطاعم", "مطعم", Icons.Default.Restaurant),
            Triple("مقاهي", "مقهى", Icons.Default.LocalCafe),
            Triple("مستشفيات", "مستشفى", Icons.Default.LocalHospital),
            Triple("صيدليات", "صيدلية", Icons.Default.LocalPharmacy),
            Triple("تموينات", "تموينات", Icons.Default.Store),
            Triple("صراف", "صراف", Icons.Default.AccountBalance),
            Triple("سيارات", "خدمات سيارات", Icons.Default.DirectionsCar),
            Triple("تسوق", "محل تجاري", Icons.Default.ShoppingBag),
            Triple("مساجد", "مسجد", Icons.Default.Mosque)
        )
    }
    val local = remember(query, extras) {
        if (query.trim().length < 2) emptyList() else extras
            .filter { it.name.contains(query.trim(), true) || it.notes.contains(query.trim(), true) }
            .map { OfflineMapSearchResult(it.id, it.name, it.latitude, it.longitude, "موقع محفوظ") }
    }
    val merged = remember(results, local) { (local + results).distinctBy { it.id }.take(60) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.84f),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = BorderStroke(1.dp, CyanNeon)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("بحث الخريطة", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    Text("مدن • قرى • طرق • خدمات", color = TextSecondary, fontSize = 9.sp)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.trim().length >= 2) onSearch(it) else onSearch("")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    label = { Text("اكتب اسم مدينة، قرية، محل أو موقع") }
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.first }) { category ->
                        FilterChip(
                            selected = query == category.second,
                            onClick = {
                                query = category.second
                                onSearch(category.second)
                            },
                            label = { Text(category.first, fontSize = 9.sp) },
                            leadingIcon = { Icon(category.third, null, modifier = Modifier.size(17.dp)) }
                        )
                    }
                }
                if (query.trim().length < 2) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("اختر اختصارًا أو اكتب حرفين على الأقل", color = TextSecondary)
                    }
                } else if (isSearching) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("جاري البحث حول موقع السيارة…", color = TextSecondary)
                        Text("قد تستغرق الفهرسة الأولى عدة ثوانٍ", color = TextMuted, fontSize = 9.sp)
                    }
                } else if (merged.isEmpty()) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("لم تُعثر على نتائج قريبة", color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("جرّب اسمًا آخر؛ بعض بيانات الخريطة قد لا تحمل أسماء", color = TextMuted, fontSize = 9.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(merged, key = { it.id }) { result ->
                            Surface(
                                onClick = { onNavigate(result) },
                                color = CarbonSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CarbonCardBorder)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(mapSearchResultIcon(result.source), null, tint = CyanNeon)
                                    Column(Modifier.weight(1f)) {
                                        Text(result.name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                        val distanceText = result.distanceMeters?.let { distance ->
                                            if (distance < 1000f) "${distance.toInt()} م" else String.format(Locale.US, "%.1f كم", distance / 1000f)
                                        }
                                        Text(
                                            listOfNotNull(result.source, distanceText).joinToString(" • "),
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Icon(Icons.Default.Navigation, "توجيه", tint = EmeraldSafe, modifier = Modifier.size(21.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun mapSearchResultIcon(source: String) = when {
    source.contains("وقود") -> Icons.Default.LocalGasStation
    source.contains("مطعم") -> Icons.Default.Restaurant
    source.contains("مقهى") -> Icons.Default.LocalCafe
    source.contains("مستشفى") || source.contains("عيادة") -> Icons.Default.LocalHospital
    source.contains("صيدلية") -> Icons.Default.LocalPharmacy
    source.contains("مسجد") -> Icons.Default.Mosque
    source.contains("محل") || source.contains("سوق") -> Icons.Default.ShoppingBag
    source.contains("مدينة") || source.contains("بلدة") || source.contains("قرية") -> Icons.Default.LocationCity
    source.contains("محفوظ") -> Icons.Default.Bookmark
    else -> Icons.Default.Place
}

@Composable
private fun EnhancedOffroadToolsDialog(
    ui: EnhancedMapUiPreferences,
    trackKm: Double,
    hasTrack: Boolean,
    nearestDistance: Float?,
    onUiChange: ((EnhancedMapUiPreferences) -> EnhancedMapUiPreferences) -> Unit,
    onMaps: () -> Unit,
    onImportGpx: () -> Unit,
    onExportGpx: () -> Unit,
    onImportBackup: () -> Unit,
    onExportBackup: () -> Unit,
    onNavigateStart: () -> Unit,
    onNearestTrack: () -> Unit,
    onClearTrack: () -> Unit,
    onClose: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.90f).fillMaxHeight(.86f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon)) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("إعدادات وأدوات الخريطة", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { MapToolToggle(Icons.Default.DarkMode, "الوضع الليلي للخريطة", "ألوان أهدأ ووهج أقل أثناء القيادة الليلية", ui.nightMap) { onUiChange { it.copy(nightMap = !it.nightMap) } } }
                    item { MapToolToggle(Icons.Default.DirectionsCar, "وضع القيادة", "يضع السيارة أسفل منتصف الشاشة ليظهر أمامك مجال أكبر", ui.drivingView) { onUiChange { it.copy(drivingView = !it.drivingView) } } }
                    item { MapToolToggle(Icons.Default.Label, "تفاصيل وأسماء أكبر", "تكبير أسماء المدن والقرى والمعالم الموجودة في ملف الخريطة", ui.detailedTheme) { onUiChange { it.copy(detailedTheme = !it.detailedTheme) } } }
                    item { Text("عناصر الخريطة ومواقعها", color = AmberRacing, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) }
                    item { MapToolToggle(Icons.Default.Speed, "إظهار السرعة وبيانات GPS", "يمكن إخفاؤها أو نقلها إلى أي طرف", ui.showTelemetry) { onUiChange { it.copy(showTelemetry = !it.showTelemetry) } } }
                    if (ui.showTelemetry) item { MapToolRow(Icons.Default.OpenWith, "مكان السرعة والبيانات", ui.telemetrySlot.arabicName) { onUiChange { it.copy(telemetrySlot = nextMapOverlaySlot(it.telemetrySlot)) } } }
                    item { MapToolToggle(Icons.Default.DashboardCustomize, "إظهار اختصارات الخريطة", "حفظ الموقع والمحفوظات والبحث", ui.showPrimaryActions) { onUiChange { it.copy(showPrimaryActions = !it.showPrimaryActions) } } }
                    if (ui.showPrimaryActions) item { MapToolRow(Icons.Default.OpenWith, "مكان اختصارات الخريطة", ui.primaryActionsSlot.arabicName) { onUiChange { it.copy(primaryActionsSlot = nextMapOverlaySlot(it.primaryActionsSlot)) } } }
                    item { MapToolRow(Icons.Default.OpenWith, "مكان أزرار التتبع والاتجاه", ui.dockSlot.arabicName) { onUiChange { it.copy(dockSlot = nextMapOverlaySlot(it.dockSlot)) } } }
                    item { MapToolToggle(Icons.Default.Straighten, "إظهار مقياس الخريطة", "مقياس تقريبي حسب مستوى التقريب", ui.showMapScale) { onUiChange { it.copy(showMapScale = !it.showMapScale) } } }
                    if (ui.showMapScale) item { MapToolRow(Icons.Default.OpenWith, "مكان مقياس الخريطة", ui.scaleSlot.arabicName) { onUiChange { it.copy(scaleSlot = nextMapOverlaySlot(it.scaleSlot)) } } }
                    item { MapToolToggle(Icons.Default.Route, "إظهار أثر المسار", "إخفاء الأثر لا يمسحه من الذاكرة", ui.trackVisible) { onUiChange { it.copy(trackVisible = !it.trackVisible) } } }
                    item {
                        Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text("سماكة أثر المسار الزهري ${ui.trackWidth.toInt()}", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Slider(value = ui.trackWidth, onValueChange = { value -> onUiChange { it.copy(trackWidth = value) } }, valueRange = 3f..12f, steps = 8)
                            }
                        }
                    }
                    item { MapToolRow(Icons.Default.Map, "إدارة الخرائط", "إضافة أو تفعيل Mapsforge وMBTiles") { onMaps() } }
                    if (hasTrack) {
                        item { MapToolRow(Icons.Default.Flag, "العودة لبداية المسار", "توجيه مباشر لأول نقطة في الأثر") { onNavigateStart() } }
                        item { MapToolRow(Icons.Default.Route, "ارجع لأقرب نقطة من المسار", nearestDistance?.let { "تبعد ${formatDistanceEnhanced(it)}" } ?: "يتطلب إشارة GPS") { onNearestTrack() } }
                    }
                    item { MapToolRow(Icons.Default.FileOpen, "استيراد GPX", "استيراد مسارات ونقاط محفوظة") { onImportGpx() } }
                    item { MapToolRow(Icons.Default.SaveAlt, "تصدير GPX", "تصدير الأثر الحالي • ${String.format(Locale.US, "%.1f", trackKm)} كم") { onExportGpx() } }
                    item { MapToolRow(Icons.Default.Restore, "استيراد نسخة احتياطية", "استعادة بيانات البر") { onImportBackup() } }
                    item { MapToolRow(Icons.Default.Backup, "نسخة احتياطية", "تصدير بيانات البر إلى JSON") { onExportBackup() } }
                    if (hasTrack) item { MapToolRow(Icons.Default.DeleteSweep, "مسح الأثر", "يحذف أثر المسار فقط", true) { confirmClear = true } }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("مسح أثر المسار؟") },
        text = { Text("المواقع المحفوظة والخريطة لن تُحذف.") },
        confirmButton = { Button(onClick = { confirmClear = false; onClearTrack() }, colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)) { Text("مسح") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } }
    )
}

@Composable
private fun MapToolToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(onClick = onToggle, color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder)) {
        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = CyanNeon)
            Column(Modifier.weight(1f)) { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold); Text(subtitle, color = TextSecondary, fontSize = 9.sp) }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun MapToolRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, destructive: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, if (destructive) HighContrastRed.copy(alpha = .6f) else CarbonCardBorder)) {
        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = if (destructive) HighContrastRed else CyanNeon)
            Column(Modifier.weight(1f)) { Text(title, color = if (destructive) HighContrastRed else TextPrimary, fontWeight = FontWeight.Bold); Text(subtitle, color = TextSecondary, fontSize = 9.sp) }
            Icon(Icons.Default.ChevronLeft, null, tint = TextMuted)
        }
    }
}

@Composable
private fun EnhancedMapManagerDialog(maps: List<MapItem>, onAdd: () -> Unit, onActivate: (String) -> Unit, onDelete: (String) -> Unit, onClose: () -> Unit) {
    var pendingDelete by remember { mutableStateOf<MapItem?>(null) }
    Dialog(onDismissRequest = onClose) {
        Card(modifier = Modifier.fillMaxWidth(.90f).fillMaxHeight(.80f), colors = CardDefaults.cardColors(containerColor = CarbonDark), border = BorderStroke(1.dp, CyanNeon)) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("إدارة الخرائط", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Text("إضافة") }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(maps, key = { it.id }) { map ->
                        Surface(color = if (map.isActive) CyanNeon.copy(alpha = .13f) else CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, if (map.isActive) CyanNeon else CarbonCardBorder)) {
                            Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Default.Map, null, tint = CyanNeon)
                                Column(Modifier.weight(1f)) { Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold); Text(map.fileSizeFormatted, color = TextSecondary, fontSize = 9.sp) }
                                if (!map.isActive) Button(onClick = { onActivate(map.id) }) { Text("عرض") }
                                IconButton(onClick = { pendingDelete = map }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { map ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف الخريطة؟") },
            text = { Text("سيُحذف ملف «${map.name}» نهائيًا من Launcher.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(map.id); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)
                ) { Text("حذف", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun EnhancedMapsforgeMap(
    mapItem: MapItem,
    gps: GpsTelemetry,
    smoothedBearing: Float,
    followGps: Boolean,
    orientationMode: MapOrientationMode,
    autoZoom: Int,
    initialState: OffroadMapState,
    trackPoints: List<OffroadTrackPoint>,
    trackVisible: Boolean,
    trackWidth: Float,
    navigationTarget: OffroadNavigationTarget?,
    measureA: LatLong?,
    measureB: LatLong?,
    detailedTheme: Boolean,
    drivingView: Boolean,
    onManualInteraction: (Double, Double, Int) -> Unit,
    onLongPress: (LatLong) -> Unit,
    onRenderError: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val rendererKey = remember(mapItem.id, detailedTheme, trackWidth) {
        "${mapItem.id}:$detailedTheme:${trackWidth.toInt()}"
    }
    var holderRef by remember(rendererKey) { mutableStateOf<EnhancedMapHolder?>(null) }
    val latestManual by rememberUpdatedState(onManualInteraction)
    val latestLongPress by rememberUpdatedState(onLongPress)
    val latestRenderError by rememberUpdatedState(onRenderError)

    key(rendererKey) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val holder = try {
                    createEnhancedMapView(ctx, mapItem, gps, autoZoom, initialState, trackPoints, trackVisible, trackWidth, navigationTarget, measureA, measureB, detailedTheme, drivingView).also {
                        it.mapView.post { latestRenderError(null) }
                    }
                } catch (t: Throwable) {
                    Log.e(MAP_LOG_TAG, "Offline map renderer failed", t)
                    createEmptyMapHolder(ctx).also {
                        it.mapView.post { latestRenderError("تعذر عرض الخريطة؛ جرّب ملفًا آخر من إدارة الخرائط") }
                    }
                }
                holder.also {
                    holderRef = holder
                    holder.mapView.addInputListener(object : InputListener {
                        private fun notifyGesture() {
                            holder.mapView.postDelayed({
                                val pos = holder.mapView.model.mapViewPosition.mapPosition ?: return@postDelayed
                                latestManual(pos.latLong.latitude, pos.latLong.longitude, pos.zoomLevel.toInt())
                            }, 180L)
                        }
                        override fun onMoveEvent() = notifyGesture()
                        override fun onZoomEvent() = notifyGesture()
                    })
                    val detector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDown(e: MotionEvent): Boolean = true
                        override fun onLongPress(e: MotionEvent) {
                            holder.mapView.mapViewProjection.fromPixels(e.x.toDouble(), e.y.toDouble())?.let(latestLongPress)
                        }
                    })
                    holder.mapView.setOnTouchListener { _, event -> detector.onTouchEvent(event); false }
                }.mapView
            },
            update = { mapView ->
                mapView.setMapViewCenterY(if (followGps && drivingView) .65f else .50f)
                if (gps.hasGpsFix && followGps) {
                    mapView.setCenter(LatLong(gps.latitude, gps.longitude))
                    val minZoom = holderRef?.zoomMin ?: 3
                    val maxZoom = holderRef?.zoomMax ?: 20
                    val requestedZoom = autoZoom.coerceIn(minZoom, maxZoom)
                    if (mapView.model.mapViewPosition.zoomLevel.toInt() != requestedZoom) {
                        mapView.setZoomLevel(requestedZoom.toByte())
                    }
                }
                val rotation = if (orientationMode == MapOrientationMode.HEADING_UP && gps.hasGpsFix) -smoothedBearing else 0f
                val rotationDelta = shortestAngleDelta(holderRef?.appliedRotation ?: 0f, rotation)
                if (mapView.width > 0 && mapView.height > 0 && abs(rotationDelta) >= MAP_ROTATION_STEP_DEGREES) {
                    mapView.rotate(Rotation(rotation, mapView.width * .5f, mapView.height * .5f))
                    holderRef?.appliedRotation = rotation
                }
                holderRef?.trackLayer?.setPoints(if (trackVisible) trackPoints.map { LatLong(it.latitude, it.longitude) } else emptyList())
                holderRef?.navigationLayer?.setPoints(if (gps.hasGpsFix && navigationTarget != null) listOf(LatLong(gps.latitude, gps.longitude), LatLong(navigationTarget.latitude, navigationTarget.longitude)) else emptyList())
                holderRef?.measureLayer?.setPoints(if (measureA != null && measureB != null) listOf(measureA, measureB) else emptyList())
                holderRef?.vehicleMarkerLayer?.setLatLong(if (gps.hasGpsFix) LatLong(gps.latitude, gps.longitude) else null)
                holderRef?.targetMarkerLayer?.setLatLong(navigationTarget?.let { LatLong(it.latitude, it.longitude) })
                mapView.repaint()
            }
        )
    }

    DisposableEffect(rendererKey) {
        onDispose {
            try { holderRef?.mapView?.destroyAll() } catch (_: Exception) { }
            try { holderRef?.mapFile?.close() } catch (_: Exception) { }
            holderRef = null
        }
    }
}

private fun addFixedMarker(
    mapView: MapView,
    position: LatLong?,
    radiusPx: Float,
    fillColor: Int,
    strokeColor: Int
): FixedPixelCircle {
    val fill = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = fillColor
        setStyle(Style.FILL)
    }
    val stroke = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = strokeColor
        strokeWidth = 2f
        setStyle(Style.STROKE)
    }
    return FixedPixelCircle(position, radiusPx, fill, stroke).also {
        mapView.layerManager.layers.add(it)
    }
}

private fun createEmptyMapHolder(context: Context): EnhancedMapHolder {
    AndroidGraphicFactory.createInstance(context.applicationContext)
    val mapView = MapView(context).apply {
        setBuiltInZoomControls(false)
        isClickable = true
        model.frameBufferModel.setOverdrawFactor(MAP_FRAMEBUFFER_OVERDRAW)
        setCenter(LatLong(DEFAULT_MAP_LATITUDE, DEFAULT_MAP_LONGITUDE))
        setZoomLevel(8.toByte())
    }
    fun overlay(color: Int, width: Float) = Polyline(
        AndroidGraphicFactory.INSTANCE.createPaint().apply {
            this.color = color
            strokeWidth = width
            setStyle(Style.STROKE)
        },
        AndroidGraphicFactory.INSTANCE
    ).also { mapView.layerManager.layers.add(it) }

    val targetMarker = addFixedMarker(
        mapView, null, 12f,
        AndroidGraphicFactory.INSTANCE.createColor(255, 255, 184, 0),
        AndroidGraphicFactory.INSTANCE.createColor(255, 20, 20, 20)
    )
    val vehicleMarker = addFixedMarker(
        mapView, null, 9f,
        AndroidGraphicFactory.INSTANCE.createColor(255, 0, 205, 255),
        AndroidGraphicFactory.INSTANCE.createColor(255, 255, 255, 255)
    )
    return EnhancedMapHolder(
        mapView = mapView,
        mapFile = null,
        trackLayer = overlay(AndroidGraphicFactory.INSTANCE.createColor(255, 255, 70, 155), 6f),
        navigationLayer = overlay(AndroidGraphicFactory.INSTANCE.createColor(240, 255, 184, 0), 5f),
        measureLayer = overlay(AndroidGraphicFactory.INSTANCE.createColor(230, 0, 220, 255), 4f),
        targetMarkerLayer = targetMarker,
        vehicleMarkerLayer = vehicleMarker
    )
}

private data class EnhancedMapHolder(
    val mapView: MapView,
    val mapFile: MapFile?,
    val trackLayer: Polyline,
    val navigationLayer: Polyline,
    val measureLayer: Polyline,
    val targetMarkerLayer: FixedPixelCircle,
    val vehicleMarkerLayer: FixedPixelCircle,
    val zoomMin: Int = 3,
    val zoomMax: Int = 20,
    var appliedRotation: Float = 0f
)

private fun createEnhancedMapView(
    context: Context,
    mapItem: MapItem,
    gps: GpsTelemetry,
    autoZoom: Int,
    initialState: OffroadMapState,
    trackPoints: List<OffroadTrackPoint>,
    trackVisible: Boolean,
    trackWidth: Float,
    navigationTarget: OffroadNavigationTarget?,
    measureA: LatLong?,
    measureB: LatLong?,
    detailedTheme: Boolean,
    drivingView: Boolean
): EnhancedMapHolder {
    AndroidGraphicFactory.createInstance(context.applicationContext)
    val mapView = MapView(context).apply {
        setBuiltInZoomControls(false)
        isClickable = true
        setMapViewCenterY(if (initialState.followGps && drivingView) .65f else .50f)
        // Mapsforge rotation needs a larger framebuffer than the visible screen.
        // Without it, older GPUs can expose dark/empty rectangles around rendered tiles.
        model.frameBufferModel.setOverdrawFactor(MAP_FRAMEBUFFER_OVERDRAW)
    }
    var mapFile: MapFile? = null
    var sourceCenter: LatLong? = null
    var sourceZoomMin = 3
    var sourceZoomMax = 20

    when {
        mapItem.filePath.endsWith(".map", true) -> {
            val vectorMap = MapFile(File(mapItem.filePath), MAP_LANGUAGE_ARABIC)
            mapFile = vectorMap
            val tileCache: TileCache = AndroidUtil.createTileCache(
                context,
                "launcher_car_${mapItem.id}_${if (detailedTheme) "detail" else "clear"}",
                mapView.model.displayModel.tileSize,
                MAP_TILE_CACHE_SCREEN_RATIO,
                mapView.model.frameBufferModel.overdrawFactor
            )
            val renderer = TileRendererLayer(tileCache, vectorMap, mapView.model.mapViewPosition, AndroidGraphicFactory.INSTANCE).apply {
                // OSMARender exposes road hierarchy, surrounding names and POIs. The
                // larger scale is deliberate for a 1024x600 dashboard viewed at distance.
                setXmlRenderTheme(MapsforgeThemes.OSMARENDER)
                textScale = if (detailedTheme) 1.62f else 1.34f
            }
            mapView.layerManager.layers.add(renderer)
            sourceCenter = vectorMap.startPosition()
        }
        mapItem.filePath.endsWith(".mbtiles", true) -> {
            val mbTiles = MBTilesFile(File(mapItem.filePath))
            sourceZoomMin = mbTiles.zoomLevelMin.coerceIn(0, 20)
            sourceZoomMax = mbTiles.zoomLevelMax.coerceIn(sourceZoomMin, 22)
            sourceCenter = mbTiles.boundingBox?.centerPoint
            mapView.model.mapViewPosition.setZoomLevelMin(sourceZoomMin.toByte())
            mapView.model.mapViewPosition.setZoomLevelMax(sourceZoomMax.toByte())
            mapView.layerManager.layers.add(
                TileMBTilesLayer(
                    InMemoryTileCache(MBTILES_MEMORY_CACHE_TILES),
                    mapView.model.mapViewPosition,
                    false,
                    mbTiles,
                    AndroidGraphicFactory.INSTANCE
                )
            )
        }
        else -> throw IllegalArgumentException("Unsupported offline map format")
    }

    val pink = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(255, 255, 70, 155)
        strokeWidth = trackWidth
        setStyle(Style.STROKE)
    }
    val track = Polyline(pink, AndroidGraphicFactory.INSTANCE).apply {
        setPoints(if (trackVisible) trackPoints.map { LatLong(it.latitude, it.longitude) } else emptyList())
    }
    mapView.layerManager.layers.add(track)

    val navPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(240, 255, 184, 0)
        strokeWidth = 5f
        setStyle(Style.STROKE)
    }
    val nav = Polyline(navPaint, AndroidGraphicFactory.INSTANCE).apply {
        if (gps.hasGpsFix && navigationTarget != null) setPoints(listOf(LatLong(gps.latitude, gps.longitude), LatLong(navigationTarget.latitude, navigationTarget.longitude)))
    }
    mapView.layerManager.layers.add(nav)

    val measurePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(230, 0, 220, 255)
        strokeWidth = 4f
        setStyle(Style.STROKE)
    }
    val measure = Polyline(measurePaint, AndroidGraphicFactory.INSTANCE).apply {
        if (measureA != null && measureB != null) setPoints(listOf(measureA, measureB))
    }
    mapView.layerManager.layers.add(measure)

    // Saved places are drawn by the persistent overlay; do not rebuild Mapsforge here.
    val targetMarker = addFixedMarker(
        mapView = mapView,
        position = navigationTarget?.let { LatLong(it.latitude, it.longitude) },
        radiusPx = 13f,
        fillColor = AndroidGraphicFactory.INSTANCE.createColor(255, 255, 184, 0),
        strokeColor = AndroidGraphicFactory.INSTANCE.createColor(255, 20, 20, 20)
    )
    val vehicleMarker = addFixedMarker(
        mapView = mapView,
        position = if (gps.hasGpsFix) LatLong(gps.latitude, gps.longitude) else null,
        radiusPx = 9f,
        fillColor = AndroidGraphicFactory.INSTANCE.createColor(255, 0, 205, 255),
        strokeColor = AndroidGraphicFactory.INSTANCE.createColor(255, 255, 255, 255)
    )

    val stored = initialState.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }?.let { LatLong(it.latitude, it.longitude) }
    val start = when {
        gps.hasGpsFix && initialState.followGps -> LatLong(gps.latitude, gps.longitude)
        stored != null -> stored
        trackPoints.lastOrNull() != null -> trackPoints.last().let { LatLong(it.latitude, it.longitude) }
        else -> sourceCenter
    }
    if (start != null) mapView.setCenter(start)
    mapView.setZoomLevel(
        (if (gps.hasGpsFix && initialState.followGps) autoZoom else initialState.zoomLevel)
            .coerceIn(sourceZoomMin, sourceZoomMax)
            .toByte()
    )
    return EnhancedMapHolder(
        mapView = mapView,
        mapFile = mapFile,
        trackLayer = track,
        navigationLayer = nav,
        measureLayer = measure,
        targetMarkerLayer = targetMarker,
        vehicleMarkerLayer = vehicleMarker,
        zoomMin = sourceZoomMin,
        zoomMax = sourceZoomMax
    )
}

@Composable
private fun EnhancedEmptyMapState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Map, null, tint = TextMuted, modifier = Modifier.size(58.dp))
            Text("لا توجد خريطة مفعلة", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("Mapsforge للأسماء والبحث • MBTiles للعرض الصوري", color = TextSecondary, fontSize = 10.sp)
            Button(onClick = onAdd) { Text("إضافة خريطة") }
        }
    }
}

@Composable
private fun EnhancedUnsupportedMapState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(color = CarbonDark.copy(alpha = .95f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AmberRacing)) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الصيغة غير مدعومة؛ اختر Mapsforge (.map) أو MBTiles صورية", color = TextPrimary, fontWeight = FontWeight.Bold)
                Button(onClick = onAdd) { Text("اختيار خريطة") }
            }
        }
    }
}

private fun placeKindIcon(kind: OffroadPlaceKind) = when (kind) {
    OffroadPlaceKind.CAMP -> Icons.Default.Home
    OffroadPlaceKind.BIRD -> Icons.Default.FlutterDash
    OffroadPlaceKind.CAR -> Icons.Default.DirectionsCar
    OffroadPlaceKind.WATER -> Icons.Default.WaterDrop
    OffroadPlaceKind.WELL -> Icons.Default.Opacity
    OffroadPlaceKind.HOME -> Icons.Default.Home
    OffroadPlaceKind.IMPORTANT -> Icons.Default.Star
    OffroadPlaceKind.FLAG -> Icons.Default.Flag
}

private fun gpsStatusColor(gps: GpsTelemetry): Color = when {
    !gps.hasGpsFix -> HighContrastRed
    gps.accuracyMeters > 35f || gps.satellitesCount in 0..3 -> AmberRacing
    else -> EmeraldSafe
}

private fun autoZoomForSpeedEnhanced(speed: Float): Int = when {
    speed < 3f -> 16
    speed < 20f -> 15
    speed < 60f -> 14
    speed < 100f -> 13
    else -> 13
}

private fun scaleLabel(zoom: Int): String = when {
    zoom >= 17 -> "100 م"
    zoom == 16 -> "200 م"
    zoom == 15 -> "500 م"
    zoom == 14 -> "1 كم"
    zoom == 13 -> "2 كم"
    zoom == 12 -> "5 كم"
    else -> "10+ كم"
}

private fun smoothAngle(old: Float, new: Float, factor: Float): Float {
    val delta = ((new - old + 540f) % 360f) - 180f
    return ((old + delta * factor) % 360f + 360f) % 360f
}

private fun shortestAngleDelta(old: Float, new: Float): Float = ((new - old + 540f) % 360f) - 180f

private fun sampleTrack(points: List<OffroadTrackPoint>, maxPoints: Int): List<OffroadTrackPoint> {
    if (points.size <= maxPoints) return points
    val step = (points.size.toFloat() / maxPoints).toInt().coerceAtLeast(1)
    val sampled = points.filterIndexed { index, _ -> index % step == 0 }.toMutableList()
    points.lastOrNull()?.let { if (sampled.lastOrNull() != it) sampled += it }
    return sampled
}

private fun nearestTrackPoint(points: List<OffroadTrackPoint>, lat: Double, lon: Double): Pair<OffroadTrackPoint, Float>? {
    var best: OffroadTrackPoint? = null
    var bestDistance = Float.MAX_VALUE
    points.forEach { p ->
        val d = distanceMetersEnhanced(lat, lon, p.latitude, p.longitude)
        if (d < bestDistance) { bestDistance = d; best = p }
    }
    return best?.let { it to bestDistance }
}

private fun distanceMetersEnhanced(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val result = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, result)
    return result[0]
}

private fun formatDistanceEnhanced(meters: Float): String = if (meters < 1000f) "${meters.toInt()} م" else String.format(Locale.US, "%.1f كم", meters / 1000f)

private const val MAP_LANGUAGE_ARABIC = "ar"
private const val MAP_LOG_TAG = "LauncherOfflineMap"
private const val MAP_FRAMEBUFFER_OVERDRAW = 1.7
private const val MAP_TILE_CACHE_SCREEN_RATIO = 2f
private const val MAP_ROTATION_STEP_DEGREES = 4.5f
private const val MBTILES_MEMORY_CACHE_TILES = 48
private const val DEFAULT_MAP_LATITUDE = 24.7136
private const val DEFAULT_MAP_LONGITUDE = 46.6753

private fun String.isSupportedOfflineMap(): Boolean =
    endsWith(".map", true) || endsWith(".mbtiles", true)
