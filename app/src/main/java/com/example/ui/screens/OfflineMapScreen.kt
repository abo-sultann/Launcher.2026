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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.model.MapItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
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

    var showManager by remember { mutableStateOf(false) }
    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importMapUri)
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF10151C))) {
        when {
            activeMap == null -> EmptyMapState(onAdd = { mapPicker.launch(arrayOf("application/*", "*/*")) })
            activeMap!!.filePath.endsWith(".map", ignoreCase = true) -> {
                MapsforgeFullScreenMap(
                    mapItem = activeMap!!,
                    gpsLat = gpsTelemetry.latitude,
                    gpsLon = gpsTelemetry.longitude,
                    hasGpsFix = gpsTelemetry.hasGpsFix,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Existing MBTiles files stay imported and manageable. On this Android 7.1
                // head unit the lightweight renderer uses Mapsforge .map for reliable display.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = CarbonDark.copy(alpha = .94f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AmberRacing),
                        modifier = Modifier.fillMaxWidth(.72f)
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Map, null, tint = AmberRacing, modifier = Modifier.size(46.dp))
                            Text("الخريطة مضافة بنجاح", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(
                                "ملف MBTiles الحالي محفوظ، لكن العرض المباشر على هذه الشاشة يستخدم ملف Mapsforge (.map) لتجنب التعليق.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { mapPicker.launch(arrayOf("application/*", "*/*")) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                            ) {
                                Text("إضافة خريطة للعرض", color = CarbonDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Map controls are overlays; the map itself remains full screen.
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { showManager = true },
                containerColor = CarbonDark.copy(alpha = .92f),
                contentColor = CyanNeon,
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Default.Layers, "إدارة الخرائط") }

            if (activeMap != null) {
                Surface(
                    color = CarbonDark.copy(alpha = .88f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CarbonCardBorder)
                ) {
                    Text(
                        activeMap!!.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        maxLines = 1
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            color = CarbonDark.copy(alpha = .88f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (gpsTelemetry.hasGpsFix) EmeraldSafe else CarbonCardBorder)
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    null,
                    tint = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted
                )
                Column {
                    Text("${gpsTelemetry.speedKmH.toInt()} كم/س", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("${String.format(Locale.US, "%.2f", tripData.distanceKm)} كم", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        if (mapError != null) {
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
                onAdd = {
                    showManager = false
                    mapPicker.launch(arrayOf("application/*", "*/*"))
                },
                onActivate = viewModel::setActiveMap,
                onDelete = viewModel::deleteMap,
                onClose = { showManager = false }
            )
        }
    }
}

@Composable
private fun MapsforgeFullScreenMap(
    mapItem: MapItem,
    gpsLat: Double,
    gpsLon: Double,
    hasGpsFix: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember(mapItem.id) { mutableStateOf<MapView?>(null) }
    var mapFileRef by remember(mapItem.id) { mutableStateOf<MapFile?>(null) }

    key(mapItem.id) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                createMapsforgeView(ctx, mapItem, hasGpsFix, gpsLat, gpsLon).also { pair ->
                    mapViewRef = pair.first
                    mapFileRef = pair.second
                }.first
            },
            update = { mapView ->
                if (hasGpsFix) {
                    // Do not constantly recenter while the user is manually panning.
                    // GPS is used for the initial position and the overlay above.
                    if (mapView.model.mapViewPosition.mapPosition == null) {
                        mapView.setCenter(LatLong(gpsLat, gpsLon))
                    }
                }
            }
        )
    }

    DisposableEffect(mapItem.id) {
        onDispose {
            try { mapViewRef?.destroyAll() } catch (_: Exception) { }
            try { mapFileRef?.close() } catch (_: Exception) { }
            mapViewRef = null
            mapFileRef = null
        }
    }
}

private fun createMapsforgeView(
    context: Context,
    mapItem: MapItem,
    hasGpsFix: Boolean,
    gpsLat: Double,
    gpsLon: Double
): Pair<MapView, MapFile> {
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

    val layer = TileRendererLayer(
        tileCache,
        mapFile,
        mapView.model.mapViewPosition,
        AndroidGraphicFactory.INSTANCE
    ).apply {
        setXmlRenderTheme(MapsforgeThemes.MOTORIDER)
    }
    mapView.layerManager.layers.add(layer)

    val start = if (hasGpsFix) LatLong(gpsLat, gpsLon) else mapFile.startPosition()
    if (start != null) mapView.setCenter(start)
    mapView.setZoomLevel(mapFile.startZoomLevel() ?: 12)
    return mapView to mapFile
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
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Map, null, tint = if (map.isActive) CyanNeon else TextSecondary)
                                Column(Modifier.weight(1f)) {
                                    Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text("${map.fileSizeFormatted} • ${map.dateAdded}", color = TextSecondary, fontSize = 10.sp)
                                    Text(
                                        if (map.filePath.endsWith(".map", true)) "Mapsforge • جاهزة للعرض" else "MBTiles • محفوظة",
                                        color = if (map.filePath.endsWith(".map", true)) EmeraldSafe else AmberRacing,
                                        fontSize = 10.sp
                                    )
                                }
                                if (!map.isActive) {
                                    Button(onClick = { onActivate(map.id) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text("عرض")
                                    }
                                }
                                IconButton(onClick = { onDelete(map.id) }) {
                                    Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
