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
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File
import java.util.Locale

@Composable
fun OfflineMapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    val mapsList by viewModel.mapsList.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    val mapError by viewModel.mapError.collectAsState()
    val trackPoints by viewModel.offroadTrackPoints.collectAsState()
    val savedPlaces by viewModel.savedOffroadPlaces.collectAsState()
    val navigationTarget by viewModel.offroadNavigationTarget.collectAsState()

    var showManager by remember { mutableStateOf(false) }
    var showPlaces by remember { mutableStateOf(false) }
    var mapUiVisible by remember { mutableStateOf(true) }
    var mapInteractionToken by remember { mutableStateOf(System.currentTimeMillis()) }
    var followGps by remember { mutableStateOf(true) }

    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importMapUri)
    }

    LaunchedEffect(mapInteractionToken, showManager, showPlaces) {
        if (!showManager && !showPlaces) {
            delay(4500L)
            mapUiVisible = false
        }
    }

    val renderedTrack = remember(trackPoints) {
        if (trackPoints.size <= 6000) trackPoints else {
            val step = (trackPoints.size.toFloat() / 6000f).toInt().coerceAtLeast(1)
            trackPoints.filterIndexed { index, _ -> index % step == 0 }.let { sampled ->
                if (sampled.lastOrNull() == trackPoints.lastOrNull()) sampled else sampled + trackPoints.last()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF10151C))
            .pointerInput(Unit) {
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
                    hasGpsFix = gpsTelemetry.hasGpsFix,
                    followGps = followGps,
                    trackPoints = renderedTrack,
                    navigationTarget = navigationTarget,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> UnsupportedMapState(onAdd = { mapPicker.launch(arrayOf("application/*", "*/*")) })
        }

        if (gpsTelemetry.hasGpsFix && followGps && activeMap?.filePath?.endsWith(".map", true) == true) {
            Surface(
                color = CarbonDark.copy(alpha = .76f),
                shape = CircleShape,
                border = BorderStroke(2.dp, CyanNeon),
                modifier = Modifier.align(Alignment.Center).size(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Navigation,
                        "السيارة",
                        tint = CyanNeon,
                        modifier = Modifier.size(34.dp).rotate(gpsTelemetry.bearingDegrees)
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
                SmallMapFab(Icons.Default.BookmarkAdd, "حفظ الموقع") { viewModel.saveCurrentOffroadPlace() }
                SmallMapFab(Icons.Default.Place, "المواقع المحفوظة") { showPlaces = true }
                if (trackPoints.isNotEmpty()) {
                    SmallMapFab(Icons.Default.Flag, "العودة للبداية") { viewModel.navigateToTrackStart() }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SmallMapFab(if (followGps) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, if (followGps) "إيقاف تتبع السيارة" else "تتبع السيارة") {
                    followGps = !followGps
                    mapInteractionToken = System.currentTimeMillis()
                }
                if (navigationTarget != null) {
                    SmallMapFab(Icons.Default.Close, "إيقاف التوجيه") { viewModel.stopOffroadNavigation() }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                color = CarbonDark.copy(alpha = .88f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (gpsTelemetry.hasGpsFix) EmeraldSafe else CarbonCardBorder)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, null, tint = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted)
                        Text("${gpsTelemetry.speedKmH.toInt()} كم/س", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Text(
                        if (gpsTelemetry.hasGpsFix) bearingToArabicDirection(gpsTelemetry.bearingDegrees) else "الاتجاه --",
                        color = AmberRacing,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text("رحلة ${String.format(Locale.US, "%.2f", tripData.distanceKm)} كم • أثر ${trackPoints.size} نقطة", color = TextSecondary, fontSize = 9.sp)
                    navigationTarget?.let { target ->
                        HorizontalDivider(color = CarbonCardBorder, modifier = Modifier.padding(vertical = 5.dp))
                        val distance = viewModel.offroadDistanceToTargetMeters()
                        val bearing = viewModel.offroadBearingToTarget()
                        Text("إلى: ${target.name}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            buildString {
                                if (distance != null) append(formatDistance(distance))
                                if (bearing != null) append(" • ${bearingToArabicDirection(bearing)}")
                            },
                            color = EmeraldSafe,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (mapError != null && mapUiVisible) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp),
                color = HighContrastRed.copy(alpha = .92f),
                shape = RoundedCornerShape(9.dp)
            ) {
                Text(mapError!!, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        if (showManager) {
            MapManagerDialog(
                maps = mapsList,
                onAdd = { showManager = false; mapPicker.launch(arrayOf("application/*", "*/*")) },
                onActivate = viewModel::setActiveMap,
                onDelete = viewModel::deleteMap,
                onClose = { showManager = false; mapInteractionToken = System.currentTimeMillis() }
            )
        }

        if (showPlaces) {
            SavedPlacesDialog(
                places = savedPlaces,
                onNavigate = { viewModel.navigateToSavedOffroadPlace(it); showPlaces = false },
                onDelete = viewModel::deleteSavedOffroadPlace,
                onClose = { showPlaces = false; mapInteractionToken = System.currentTimeMillis() }
            )
        }
    }
}

@Composable
private fun SmallMapFab(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = CarbonDark.copy(alpha = .92f),
        contentColor = CyanNeon,
        modifier = Modifier.size(44.dp)
    ) { Icon(icon, description, modifier = Modifier.size(21.dp)) }
}

@Composable
private fun MapsforgeFullScreenMap(
    mapItem: MapItem,
    gpsLat: Double,
    gpsLon: Double,
    hasGpsFix: Boolean,
    followGps: Boolean,
    trackPoints: List<OffroadTrackPoint>,
    navigationTarget: OffroadNavigationTarget?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember(mapItem.id) { mutableStateOf<MapView?>(null) }
    var mapFileRef by remember(mapItem.id) { mutableStateOf<MapFile?>(null) }
    var trackLayerRef by remember(mapItem.id) { mutableStateOf<Polyline?>(null) }
    var navigationLayerRef by remember(mapItem.id) { mutableStateOf<Polyline?>(null) }

    key(mapItem.id) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                createMapsforgeView(ctx, mapItem, hasGpsFix, gpsLat, gpsLon, trackPoints, navigationTarget).also { holder ->
                    mapViewRef = holder.mapView
                    mapFileRef = holder.mapFile
                    trackLayerRef = holder.trackLayer
                    navigationLayerRef = holder.navigationLayer
                }.mapView
            },
            update = { mapView ->
                if (hasGpsFix && followGps) mapView.setCenter(LatLong(gpsLat, gpsLon))
                trackLayerRef?.setPoints(trackPoints.map { LatLong(it.latitude, it.longitude) })
                val navPoints = if (hasGpsFix && navigationTarget != null) {
                    listOf(LatLong(gpsLat, gpsLon), LatLong(navigationTarget.latitude, navigationTarget.longitude))
                } else emptyList()
                navigationLayerRef?.setPoints(navPoints)
                mapView.repaint()
            }
        )
    }

    DisposableEffect(mapItem.id) {
        onDispose {
            try { mapViewRef?.destroyAll() } catch (_: Exception) { }
            try { mapFileRef?.close() } catch (_: Exception) { }
            mapViewRef = null
            mapFileRef = null
            trackLayerRef = null
            navigationLayerRef = null
        }
    }
}

private data class MapHolder(
    val mapView: MapView,
    val mapFile: MapFile,
    val trackLayer: Polyline,
    val navigationLayer: Polyline
)

private fun createMapsforgeView(
    context: Context,
    mapItem: MapItem,
    hasGpsFix: Boolean,
    gpsLat: Double,
    gpsLon: Double,
    trackPoints: List<OffroadTrackPoint>,
    navigationTarget: OffroadNavigationTarget?
): MapHolder {
    AndroidGraphicFactory.createInstance(context.applicationContext)

    val mapView = MapView(context).apply {
        setBuiltInZoomControls(false)
        isClickable = true
    }

    val mapFile = MapFile(File(mapItem.filePath))
    val tileCache: TileCache = AndroidUtil.createTileCache(
        context,
        "launcher_map_${mapItem.id}",
        mapView.model.displayModel.tileSize,
        1f,
        mapView.model.frameBufferModel.overdrawFactor
    )

    val renderer = TileRendererLayer(
        tileCache,
        mapFile,
        mapView.model.mapViewPosition,
        AndroidGraphicFactory.INSTANCE
    ).apply {
        // DEFAULT shows more settlement and POI labels than the motorcycle theme.
        setXmlRenderTheme(MapsforgeThemes.DEFAULT)
    }
    mapView.layerManager.layers.add(renderer)

    val pinkPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(255, 255, 70, 155)
        strokeWidth = 6f
        style = Style.STROKE
    }
    val trackLayer = Polyline(pinkPaint, AndroidGraphicFactory.INSTANCE).apply {
        setPoints(trackPoints.map { LatLong(it.latitude, it.longitude) })
    }
    mapView.layerManager.layers.add(trackLayer)

    val navigationPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
        color = AndroidGraphicFactory.INSTANCE.createColor(230, 255, 184, 0)
        strokeWidth = 4f
        style = Style.STROKE
    }
    val navigationLayer = Polyline(navigationPaint, AndroidGraphicFactory.INSTANCE).apply {
        if (hasGpsFix && navigationTarget != null) {
            setPoints(listOf(LatLong(gpsLat, gpsLon), LatLong(navigationTarget.latitude, navigationTarget.longitude)))
        }
    }
    mapView.layerManager.layers.add(navigationLayer)

    val fallback = trackPoints.lastOrNull()?.let { LatLong(it.latitude, it.longitude) }
    val start = if (hasGpsFix) LatLong(gpsLat, gpsLon) else fallback ?: mapFile.startPosition()
    if (start != null) mapView.setCenter(start)
    mapView.setZoomLevel((mapFile.startZoomLevel() ?: 12).coerceAtLeast(11))
    return MapHolder(mapView, mapFile, trackLayer, navigationLayer)
}

@Composable
private fun UnsupportedMapState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = CarbonDark.copy(alpha = .94f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AmberRacing),
            modifier = Modifier.fillMaxWidth(.72f)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Map, null, tint = AmberRacing, modifier = Modifier.size(46.dp))
                Text("الخريطة محفوظة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("للعرض الكامل على هذه الشاشة اختر خريطة Mapsforge بامتداد .map.", color = TextSecondary, fontSize = 13.sp)
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)) {
                    Text("إضافة خريطة .map", color = CarbonDark, fontWeight = FontWeight.Bold)
                }
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
                Icon(Icons.Default.AddLocationAlt, null, tint = CarbonDark)
                Spacer(Modifier.width(6.dp))
                Text("إضافة خريطة", color = CarbonDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SavedPlacesDialog(
    places: List<SavedOffroadPlace>,
    onNavigate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth(.90f).fillMaxHeight(.78f),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = BorderStroke(1.dp, CyanNeon),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المواقع المحفوظة", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }
                if (places.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("لا توجد مواقع محفوظة بعد", color = TextSecondary)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(places, key = { it.id }) { place ->
                            Surface(color = CarbonSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CarbonCardBorder)) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Place, null, tint = AmberRacing)
                                    Column(Modifier.weight(1f)) {
                                        Text(place.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(String.format(Locale.US, "%.5f, %.5f", place.latitude, place.longitude), color = TextSecondary, fontSize = 10.sp)
                                    }
                                    Button(onClick = { onNavigate(place.id) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("توجيه") }
                                    IconButton(onClick = { onDelete(place.id) }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
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
private fun MapManagerDialog(
    maps: List<MapItem>,
    onAdd: () -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth(.92f).fillMaxHeight(.82f),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = BorderStroke(1.dp, CyanNeon),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("إدارة الخرائط", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Row {
                        Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Default.Add, null)
                            Text("إضافة")
                        }
                        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                    }
                }

                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(maps, key = { it.id }) { map ->
                        Surface(
                            color = if (map.isActive) CyanNeon.copy(alpha = .14f) else CarbonSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (map.isActive) CyanNeon else CarbonCardBorder)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Map, null, tint = if (map.isActive) CyanNeon else TextSecondary)
                                Column(Modifier.weight(1f)) {
                                    Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("${map.fileSizeFormatted} • ${map.dateAdded}", color = TextSecondary, fontSize = 10.sp)
                                    Text(if (map.filePath.endsWith(".map", true)) "Mapsforge • جاهزة للعرض" else "MBTiles • محفوظة", color = if (map.filePath.endsWith(".map", true)) EmeraldSafe else AmberRacing, fontSize = 10.sp)
                                }
                                if (!map.isActive) {
                                    Button(onClick = { onActivate(map.id) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("عرض") }
                                }
                                IconButton(onClick = { onDelete(map.id) }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDistance(meters: Float): String = if (meters < 1000f) {
    "${meters.toInt()} م"
} else {
    String.format(Locale.US, "%.1f كم", meters / 1000f)
}
