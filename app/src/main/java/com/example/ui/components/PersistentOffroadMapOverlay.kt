package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EnhancedMapStore
import com.example.data.OffroadPlaceKind
import com.example.data.SavedTripRouteBridge
import com.example.model.MapOrientationMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlin.math.*

private data class OverlaySavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: OffroadPlaceKind,
    val legacy: Boolean
)

private data class ProjectedSavedPlace(val place: OverlaySavedPlace, val x: Float, val y: Float)
private data class MarkerGroup(val items: List<ProjectedSavedPlace>, val x: Float, val y: Float)

/**
 * Lightweight Compose overlay above Mapsforge. It keeps saved locations visible without
 * rebuilding the vector-map renderer. At distant zoom levels close markers are grouped;
 * at close zoom levels the marker name is shown beside its category icon.
 */
@Composable
fun PersistentOffroadMapOverlay(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapStore = remember { EnhancedMapStore(context.applicationContext) }
    val extraPlaces by mapStore.extraPlaces.collectAsState()
    val placeKinds by mapStore.placeKinds.collectAsState()
    val legacyPlaces by viewModel.savedOffroadPlaces.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    val mapState by viewModel.offroadMapState.collectAsState()
    val ui by mapStore.ui.collectAsState()
    val routeSelection by SavedTripRouteBridge.selection.collectAsState()

    var selectedPlace by remember { mutableStateOf<OverlaySavedPlace?>(null) }

    val places = remember(legacyPlaces, extraPlaces, placeKinds) {
        legacyPlaces.map { p ->
            OverlaySavedPlace(p.id, p.name, p.latitude, p.longitude, placeKinds[p.id] ?: OffroadPlaceKind.FLAG, true)
        } + extraPlaces.map { p -> OverlaySavedPlace(p.id, p.name, p.latitude, p.longitude, p.kind, false) }
    }

    // A saved trip should open around its route, not immediately snap back to the car.
    LaunchedEffect(routeSelection?.tripId) {
        routeSelection?.points?.takeIf { it.isNotEmpty() }?.let { points ->
            val middle = points[points.size / 2]
            viewModel.updateOffroadMapState(
                mapState.copy(
                    latitude = middle.latitude,
                    longitude = middle.longitude,
                    zoomLevel = 13,
                    followGps = false
                )
            )
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val densityScale = density.density.coerceIn(1f, 2.5f)
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val follow = mapState.followGps && gps.hasGpsFix
        val zoom = if (follow) autoOverlayZoom(gps.speedKmH) else mapState.zoomLevel.coerceIn(3, 20)
        val centerLat = when {
            follow -> gps.latitude
            mapState.latitude != 0.0 || mapState.longitude != 0.0 -> mapState.latitude
            gps.hasGpsFix -> gps.latitude
            else -> 0.0
        }
        val centerLon = when {
            follow -> gps.longitude
            mapState.latitude != 0.0 || mapState.longitude != 0.0 -> mapState.longitude
            gps.hasGpsFix -> gps.longitude
            else -> 0.0
        }
        val centerY = if (follow && ui.drivingView) heightPx * .65f else heightPx * .50f
        val bearing = if (mapState.orientationMode == MapOrientationMode.HEADING_UP && gps.hasGpsFix) gps.bearingDegrees else 0f

        val projectedPlaces = remember(places, centerLat, centerLon, zoom, bearing, centerY, widthPx, heightPx, densityScale) {
            places.take(MAX_MARKERS).mapNotNull { p ->
                projectToScreen(p.latitude, p.longitude, centerLat, centerLon, zoom, widthPx, centerY, bearing, densityScale)
                    ?.takeIf { (x, y) -> x in -80f..(widthPx + 80f) && y in -80f..(heightPx + 80f) }
                    ?.let { (x, y) -> ProjectedSavedPlace(p, x, y) }
            }
        }

        val groups = remember(projectedPlaces, zoom, densityScale) {
            if (zoom >= 14) {
                projectedPlaces.map { MarkerGroup(listOf(it), it.x, it.y) }
            } else {
                val cell = 70f * densityScale
                projectedPlaces.groupBy { Pair(floor(it.x / cell).toInt(), floor(it.y / cell).toInt()) }
                    .values.map { list -> MarkerGroup(list, list.map { it.x }.average().toFloat(), list.map { it.y }.average().toFloat()) }
            }
        }

        val routePoints = routeSelection?.points.orEmpty()
        Canvas(Modifier.fillMaxSize()) {
            val topClip = 112.dp.toPx()
            val bottomClip = 66.dp.toPx()
            clipRect(0f, topClip, size.width, (size.height - bottomClip).coerceAtLeast(topClip)) {
                if (routePoints.size >= 2) {
                    val sampled = sampleRoute(routePoints, 1100)
                    var previous: Offset? = null
                    sampled.forEach { p ->
                        val projected = projectToScreen(
                            p.latitude, p.longitude, centerLat, centerLon, zoom,
                            size.width, centerY, bearing, densityScale
                        )
                        if (projected != null) {
                            val point = Offset(projected.first, projected.second)
                            previous?.let { old ->
                                if (abs(point.x - old.x) < size.width * .8f && abs(point.y - old.y) < size.height * .8f) {
                                    drawLine(AmberRacing.copy(alpha = .88f), old, point, strokeWidth = 4.dp.toPx())
                                }
                            }
                            previous = point
                        } else previous = null
                    }
                }
            }
        }

        val topSafePx = with(density) { 108.dp.toPx() }
        val bottomSafePx = heightPx - with(density) { 64.dp.toPx() }
        groups.forEach { group ->
            if (group.y !in topSafePx..bottomSafePx) return@forEach
            val xDp = with(density) { (group.x - 21f * densityScale).toDp() }
            val yDp = with(density) { (group.y - 24f * densityScale).toDp() }
            if (group.items.size > 1) {
                Surface(
                    color = CarbonDark.copy(alpha = .94f),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, AmberRacing),
                    modifier = Modifier.absoluteOffset(xDp, yDp).size(42.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(group.items.size.toString(), color = AmberRacing, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            } else {
                val item = group.items.first()
                Surface(
                    color = CarbonDark.copy(alpha = .92f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, markerTint(item.place.kind)),
                    modifier = Modifier
                        .absoluteOffset(xDp, yDp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedPlace = item.place }
                ) {
                    Row(
                        Modifier.padding(horizontal = if (zoom >= 15) 7.dp else 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(placeGlyph(item.place.kind), fontSize = 17.sp)
                        if (zoom >= 15) {
                            Text(
                                item.place.name,
                                color = TextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 105.dp)
                            )
                        }
                    }
                }
            }
        }

        routeSelection?.let { selected ->
            Surface(
                color = CarbonDark.copy(alpha = .94f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AmberRacing),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 68.dp)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("مسار: ${selected.name}", color = AmberRacing, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, modifier = Modifier.widthIn(max = 230.dp))
                    Spacer(Modifier.width(5.dp))
                    TextButton(onClick = { SavedTripRouteBridge.clear() }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                        Text("إغلاق", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }

    selectedPlace?.let { place ->
        var editName by remember(place.id) { mutableStateOf(place.name) }
        var editKind by remember(place.id) { mutableStateOf(place.kind) }
        AlertDialog(
            onDismissRequest = { selectedPlace = null },
            title = { Text(place.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${placeGlyph(place.kind)} ${place.kind.arabicName}", color = AmberRacing, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = editName, onValueChange = { editName = it.take(40) }, label = { Text("اسم الموقع") }, singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OffroadPlaceKind.entries.chunked(4).first().forEach { kind ->
                            AssistChip(onClick = { editKind = kind }, label = { Text(placeGlyph(kind), fontSize = 14.sp) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OffroadPlaceKind.entries.drop(4).forEach { kind ->
                            AssistChip(onClick = { editKind = kind }, label = { Text(placeGlyph(kind), fontSize = 14.sp) })
                        }
                    }
                    Text("${String.format(java.util.Locale.US, "%.5f", place.latitude)}, ${String.format(java.util.Locale.US, "%.5f", place.longitude)}", color = TextMuted, fontSize = 9.sp)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextButton(onClick = {
                        viewModel.navigateToSearchResult(com.example.model.OfflineMapSearchResult(place.id, editName.ifBlank { place.name }, place.latitude, place.longitude, "موقع محفوظ"))
                        selectedPlace = null
                    }) { Text("توجيه") }
                    Button(onClick = {
                        if (place.legacy) {
                            viewModel.renameSavedOffroadPlace(place.id, editName)
                            mapStore.setPlaceKind(place.id, editKind)
                        } else {
                            mapStore.renameExtraPlace(place.id, editName)
                            mapStore.updateExtraPlaceKind(place.id, editKind)
                        }
                        selectedPlace = null
                    }) { Text("حفظ") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (place.legacy) viewModel.deleteSavedOffroadPlace(place.id) else mapStore.deleteExtraPlace(place.id)
                    selectedPlace = null
                }) { Text("حذف", color = HighContrastRed) }
            }
        )
    }
}

private fun placeGlyph(kind: OffroadPlaceKind): String = when (kind) {
    OffroadPlaceKind.CAMP -> "⛺"
    OffroadPlaceKind.BIRD -> "🐦"
    OffroadPlaceKind.CAR -> "🚙"
    OffroadPlaceKind.WATER -> "💧"
    OffroadPlaceKind.WELL -> "◉"
    OffroadPlaceKind.HOME -> "⌂"
    OffroadPlaceKind.IMPORTANT -> "★"
    OffroadPlaceKind.FLAG -> "⚑"
}

private fun markerTint(kind: OffroadPlaceKind): Color = when (kind) {
    OffroadPlaceKind.BIRD -> AmberRacing
    OffroadPlaceKind.CAMP -> AmberRacing
    OffroadPlaceKind.WATER -> CyanNeon
    OffroadPlaceKind.IMPORTANT -> HighContrastRed
    else -> EmeraldSafe
}

private fun autoOverlayZoom(speed: Float): Int = when {
    speed < 3f -> 17
    speed < 15f -> 16
    speed < 35f -> 15
    speed < 65f -> 14
    speed < 100f -> 13
    else -> 12
}

private fun projectToScreen(
    latitude: Double,
    longitude: Double,
    centerLatitude: Double,
    centerLongitude: Double,
    zoom: Int,
    widthPx: Float,
    centerYPx: Float,
    bearingDegrees: Float,
    densityScale: Float
): Pair<Float, Float>? {
    if (!latitude.isFinite() || !longitude.isFinite() || !centerLatitude.isFinite() || !centerLongitude.isFinite()) return null
    val z = zoom.coerceIn(3, 20)
    val tileSize = 256.0 * densityScale
    val world = tileSize * 2.0.pow(z.toDouble())
    val centerXWorld = mercatorX(centerLongitude, world)
    val centerYWorld = mercatorY(centerLatitude, world)
    val xWorld = mercatorX(longitude, world)
    val yWorld = mercatorY(latitude, world)
    var dx = xWorld - centerXWorld
    if (dx > world / 2.0) dx -= world
    if (dx < -world / 2.0) dx += world
    val dy = yWorld - centerYWorld
    val angle = Math.toRadians(-bearingDegrees.toDouble())
    val rx = dx * cos(angle) - dy * sin(angle)
    val ry = dx * sin(angle) + dy * cos(angle)
    return (widthPx / 2f + rx.toFloat()) to (centerYPx + ry.toFloat())
}

private fun mercatorX(longitude: Double, worldSize: Double): Double = (longitude + 180.0) / 360.0 * worldSize

private fun mercatorY(latitude: Double, worldSize: Double): Double {
    val lat = latitude.coerceIn(-85.05112878, 85.05112878)
    val rad = Math.toRadians(lat)
    val normalized = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0
    return normalized * worldSize
}

private fun sampleRoute(points: List<com.example.model.TripRoutePoint>, maxPoints: Int): List<com.example.model.TripRoutePoint> {
    if (points.size <= maxPoints) return points
    val step = ceil(points.size.toDouble() / maxPoints.toDouble()).toInt().coerceAtLeast(1)
    val sampled = points.filterIndexed { index, _ -> index % step == 0 }.toMutableList()
    points.lastOrNull()?.let { if (sampled.lastOrNull() != it) sampled += it }
    return sampled
}

private const val MAX_MARKERS = 300
