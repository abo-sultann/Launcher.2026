package com.example.data

import android.content.Context
import android.location.Location
import android.util.AtomicFile
import android.util.Xml
import com.example.model.GpsTelemetry
import com.example.model.MapOrientationMode
import com.example.model.OffroadMapState
import com.example.model.OffroadNavigationTarget
import com.example.model.OffroadTrackPoint
import com.example.model.SavedOffroadPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.util.UUID
import kotlin.math.max

class OffroadTrackManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("offroad_navigation_2026", Context.MODE_PRIVATE)
    private val trackFile = File(context.filesDir, "offroad_track_rolling.json")
    private val trackAtomicFile = AtomicFile(trackFile)
    private val persistLock = Any()
    private var pendingTrackSnapshot: List<OffroadTrackPoint>? = null
    private var persistJob: Job? = null

    private val _trackPoints = MutableStateFlow<List<OffroadTrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<OffroadTrackPoint>> = _trackPoints.asStateFlow()

    private val _savedPlaces = MutableStateFlow<List<SavedOffroadPlace>>(emptyList())
    val savedPlaces: StateFlow<List<SavedOffroadPlace>> = _savedPlaces.asStateFlow()

    private val _navigationTarget = MutableStateFlow<OffroadNavigationTarget?>(null)
    val navigationTarget: StateFlow<OffroadNavigationTarget?> = _navigationTarget.asStateFlow()

    private val _mapState = MutableStateFlow(loadMapState())
    val mapState: StateFlow<OffroadMapState> = _mapState.asStateFlow()

    private var totalTrackKm = 0.0
    private var lastPersistAt = 0L

    init {
        loadTrack()
        loadPlaces()
        restoreNavigationTarget()
    }

    fun record(telemetry: GpsTelemetry) {
        if (!telemetry.hasGpsFix || telemetry.latitude == 0.0 || telemetry.longitude == 0.0) return

        val now = System.currentTimeMillis()
        val newPoint = OffroadTrackPoint(telemetry.latitude, telemetry.longitude, now)
        val current = _trackPoints.value
        val last = current.lastOrNull()

        if (last != null) {
            val meters = distanceMeters(last.latitude, last.longitude, newPoint.latitude, newPoint.longitude)
            val ageMs = now - last.timestamp
            val minDistance = if (telemetry.speedKmH < 25f) 15f else 35f
            if (meters < minDistance && ageMs < 20_000L) return
        }

        val updated = current.toMutableList()
        if (last != null) totalTrackKm += distanceMeters(last.latitude, last.longitude, newPoint.latitude, newPoint.longitude) / 1000.0
        updated += newPoint
        trimRollingTrack(updated)

        _trackPoints.value = updated
        if (now - lastPersistAt > TRACK_PERSIST_INTERVAL_MS || updated.size % TRACK_PERSIST_POINT_INTERVAL == 0) {
            lastPersistAt = now
            persistTrackAsync(updated)
        }
    }

    fun trackDistanceKm(): Double = totalTrackKm

    fun clearTrack() {
        totalTrackKm = 0.0
        _trackPoints.value = emptyList()
        persistTrackAsync(emptyList())
    }

    fun saveCurrentPlace(telemetry: GpsTelemetry, requestedName: String? = null): SavedOffroadPlace? {
        if (!telemetry.hasGpsFix) return null
        return savePlace(
            requestedName?.trim().takeUnless { it.isNullOrBlank() } ?: "موقع محفوظ ${_savedPlaces.value.size + 1}",
            telemetry.latitude,
            telemetry.longitude
        )
    }

    fun savePlace(name: String, latitude: Double, longitude: Double): SavedOffroadPlace {
        val clean = name.trim().ifBlank { "موقع محفوظ ${_savedPlaces.value.size + 1}" }
        val place = SavedOffroadPlace(
            id = UUID.randomUUID().toString(),
            name = clean,
            latitude = latitude,
            longitude = longitude,
            createdAt = System.currentTimeMillis()
        )
        _savedPlaces.value = _savedPlaces.value + place
        persistPlaces()
        return place
    }

    fun renamePlace(id: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        _savedPlaces.value = _savedPlaces.value.map { if (it.id == id) it.copy(name = clean) else it }
        val target = _navigationTarget.value
        if (target?.id == id) {
            _navigationTarget.value = target.copy(name = clean)
            persistNavigationTarget(_navigationTarget.value!!)
        }
        persistPlaces()
    }

    fun deletePlace(id: String) {
        _savedPlaces.value = _savedPlaces.value.filterNot { it.id == id }
        if (_navigationTarget.value?.id == id) stopNavigation()
        persistPlaces()
    }

    fun navigateTo(place: SavedOffroadPlace) {
        navigateToCoordinates(place.id, place.name, place.latitude, place.longitude)
    }

    fun navigateToCoordinates(id: String, name: String, latitude: Double, longitude: Double) {
        val target = OffroadNavigationTarget(id, name, latitude, longitude)
        _navigationTarget.value = target
        persistNavigationTarget(target)
    }

    fun navigateToTrackStart() {
        val start = _trackPoints.value.firstOrNull() ?: return
        navigateToCoordinates("track_start", "بداية المسار", start.latitude, start.longitude)
    }

    fun stopNavigation() {
        _navigationTarget.value = null
        prefs.edit().remove("navigation_target").apply()
    }

    fun distanceToTargetMeters(current: GpsTelemetry): Float? {
        val target = _navigationTarget.value ?: return null
        if (!current.hasGpsFix) return null
        return distanceMeters(current.latitude, current.longitude, target.latitude, target.longitude)
    }

    fun bearingToTarget(current: GpsTelemetry): Float? {
        val target = _navigationTarget.value ?: return null
        if (!current.hasGpsFix) return null
        return bearing(current.latitude, current.longitude, target.latitude, target.longitude)
    }

    fun distanceToTrackStartMeters(current: GpsTelemetry): Float? {
        val start = _trackPoints.value.firstOrNull() ?: return null
        if (!current.hasGpsFix) return null
        return distanceMeters(current.latitude, current.longitude, start.latitude, start.longitude)
    }

    fun bearingToTrackStart(current: GpsTelemetry): Float? {
        val start = _trackPoints.value.firstOrNull() ?: return null
        if (!current.hasGpsFix) return null
        return bearing(current.latitude, current.longitude, start.latitude, start.longitude)
    }

    fun renderPoints(maxPoints: Int = 6000): List<OffroadTrackPoint> {
        val points = _trackPoints.value
        if (points.size <= maxPoints) return points
        val step = (points.size.toDouble() / maxPoints.toDouble()).toInt().coerceAtLeast(1)
        val sampled = points.filterIndexed { index, _ -> index % step == 0 }.toMutableList()
        points.lastOrNull()?.let { if (sampled.lastOrNull() != it) sampled += it }
        return sampled
    }

    fun saveMapState(state: OffroadMapState) {
        val sanitized = state.copy(zoomLevel = state.zoomLevel.coerceIn(3, 20))
        _mapState.value = sanitized
        prefs.edit().putString("map_state", JSONObject().apply {
            put("lat", sanitized.latitude)
            put("lon", sanitized.longitude)
            put("zoom", sanitized.zoomLevel)
            put("follow", sanitized.followGps)
            put("orientation", sanitized.orientationMode.name)
        }.toString()).apply()
    }

    fun exportGpx(): String {
        val sb = StringBuilder(1024 + _trackPoints.value.size * 60)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Launcher 2026\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        _savedPlaces.value.forEach { p ->
            sb.append("  <wpt lat=\"").append(p.latitude).append("\" lon=\"").append(p.longitude).append("\"><name>")
                .append(xmlEscape(p.name)).append("</name></wpt>\n")
        }
        sb.append("  <trk><name>أثر Launcher 2026</name><trkseg>\n")
        _trackPoints.value.forEach { p ->
            sb.append("    <trkpt lat=\"").append(p.latitude).append("\" lon=\"").append(p.longitude).append("\" />\n")
        }
        sb.append("  </trkseg></trk>\n</gpx>\n")
        return sb.toString()
    }

    fun importGpx(raw: String): Int {
        val importedPoints = mutableListOf<OffroadTrackPoint>()
        val importedPlaces = mutableListOf<SavedOffroadPlace>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(raw))
            var event = parser.eventType
            var pendingWptLat: Double? = null
            var pendingWptLon: Double? = null
            var pendingWptName: String? = null
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name.lowercase()) {
                        "trkpt", "rtept" -> {
                            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            if (lat != null && lon != null && importedPoints.size < MAX_TRACK_POINTS) {
                                importedPoints += OffroadTrackPoint(lat, lon, System.currentTimeMillis() + importedPoints.size)
                            }
                        }
                        "wpt" -> {
                            pendingWptLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            pendingWptLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            pendingWptName = null
                        }
                        "name" -> if (pendingWptLat != null) pendingWptName = parser.nextText()
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name.equals("wpt", true)) {
                    val lat = pendingWptLat
                    val lon = pendingWptLon
                    if (lat != null && lon != null && importedPlaces.size < MAX_IMPORTED_PLACES) {
                        importedPlaces += SavedOffroadPlace(
                            id = UUID.randomUUID().toString(),
                            name = pendingWptName?.trim().takeUnless { it.isNullOrBlank() } ?: "نقطة GPX",
                            latitude = lat,
                            longitude = lon,
                            createdAt = System.currentTimeMillis()
                        )
                    }
                    pendingWptLat = null
                    pendingWptLon = null
                    pendingWptName = null
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return 0
        }

        if (importedPoints.isNotEmpty()) {
            val merged = (_trackPoints.value + importedPoints).toMutableList()
            totalTrackKm = calculateDistanceKm(merged)
            trimRollingTrack(merged)
            _trackPoints.value = merged
            persistTrackAsync(merged)
        }
        if (importedPlaces.isNotEmpty()) {
            _savedPlaces.value = _savedPlaces.value + importedPlaces
            persistPlaces()
        }
        return importedPoints.size + importedPlaces.size
    }

    fun exportBackupJson(): String = JSONObject().apply {
        put("version", 2)
        put("createdAt", System.currentTimeMillis())
        put("track", JSONArray().apply {
            _trackPoints.value.forEach { p -> put(JSONObject().apply { put("lat", p.latitude); put("lon", p.longitude); put("time", p.timestamp) }) }
        })
        put("places", JSONArray().apply {
            _savedPlaces.value.forEach { p -> put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("lat", p.latitude); put("lon", p.longitude); put("time", p.createdAt)
            }) }
        })
        _navigationTarget.value?.let { t -> put("navigation", JSONObject().apply {
            put("id", t.id); put("name", t.name); put("lat", t.latitude); put("lon", t.longitude)
        }) }
        put("mapState", JSONObject().apply {
            val s = _mapState.value
            put("lat", s.latitude); put("lon", s.longitude); put("zoom", s.zoomLevel); put("follow", s.followGps); put("orientation", s.orientationMode.name)
        })
    }.toString(2)

    fun importBackupJson(raw: String): Int {
        return try {
            val root = JSONObject(raw)
            val importedTrack = mutableListOf<OffroadTrackPoint>()
            root.optJSONArray("track")?.let { a ->
                for (i in 0 until minOf(a.length(), MAX_TRACK_POINTS)) {
                    val o = a.getJSONObject(i)
                    importedTrack += OffroadTrackPoint(o.getDouble("lat"), o.getDouble("lon"), o.optLong("time", System.currentTimeMillis()))
                }
            }
            if (importedTrack.isNotEmpty()) {
                val merged = (_trackPoints.value + importedTrack).distinctBy { "${it.latitude}:${it.longitude}:${it.timestamp}" }.sortedBy { it.timestamp }.toMutableList()
                totalTrackKm = calculateDistanceKm(merged)
                trimRollingTrack(merged)
                _trackPoints.value = merged
                persistTrackAsync(merged)
            }

            var placeCount = 0
            root.optJSONArray("places")?.let { a ->
                val merged = _savedPlaces.value.associateBy { it.id }.toMutableMap()
                for (i in 0 until minOf(a.length(), MAX_IMPORTED_PLACES)) {
                    val o = a.getJSONObject(i)
                    val p = SavedOffroadPlace(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", "موقع محفوظ"),
                        latitude = o.getDouble("lat"),
                        longitude = o.getDouble("lon"),
                        createdAt = o.optLong("time", System.currentTimeMillis())
                    )
                    merged[p.id] = p
                    placeCount++
                }
                _savedPlaces.value = merged.values.sortedBy { it.createdAt }
                persistPlaces()
            }

            root.optJSONObject("navigation")?.let { o ->
                navigateToCoordinates(o.optString("id", "backup_target"), o.optString("name", "هدف محفوظ"), o.getDouble("lat"), o.getDouble("lon"))
            }
            root.optJSONObject("mapState")?.let { o ->
                val mode = try { MapOrientationMode.valueOf(o.optString("orientation", MapOrientationMode.NORTH_UP.name)) } catch (_: Exception) { MapOrientationMode.NORTH_UP }
                saveMapState(OffroadMapState(o.optDouble("lat", 0.0), o.optDouble("lon", 0.0), o.optInt("zoom", 13), o.optBoolean("follow", true), mode))
            }
            importedTrack.size + placeCount
        } catch (_: Exception) { 0 }
    }

    fun release() {
        val finalSnapshot = _trackPoints.value.toList()
        val activeJob = synchronized(persistLock) {
            pendingTrackSnapshot = null
            persistJob
        }
        runBlocking { activeJob?.join() }
        persistTrackNow(finalSnapshot)
        scope.cancel()
    }

    private fun loadTrack() {
        try {
            if (!trackFile.exists()) return
            val array = trackAtomicFile.openRead().bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
            val points = ArrayList<OffroadTrackPoint>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                points += OffroadTrackPoint(o.getDouble("lat"), o.getDouble("lon"), o.optLong("time", 0L))
            }
            totalTrackKm = calculateDistanceKm(points)
            val trimmed = points.toMutableList()
            trimRollingTrack(trimmed)
            _trackPoints.value = trimmed
            if (trimmed.size != points.size) persistTrackAsync(trimmed)
        } catch (_: Exception) {
            _trackPoints.value = emptyList()
            totalTrackKm = 0.0
        }
    }

    private fun trimRollingTrack(points: MutableList<OffroadTrackPoint>) {
        while ((totalTrackKm > MAX_TRACK_KM || points.size > MAX_TRACK_POINTS) && points.size > 2) {
            val a = points[0]
            val b = points[1]
            totalTrackKm = max(0.0, totalTrackKm - distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) / 1000.0)
            points.removeAt(0)
        }
    }

    private fun persistTrackAsync(points: List<OffroadTrackPoint>) {
        val snapshot = points.toList()
        synchronized(persistLock) {
            pendingTrackSnapshot = snapshot
            if (persistJob?.isActive == true) return
            persistJob = scope.launch {
                while (true) {
                    val next = synchronized(persistLock) {
                        val queued = pendingTrackSnapshot
                        pendingTrackSnapshot = null
                        if (queued == null) persistJob = null
                        queued
                    } ?: break
                    persistTrackNow(next)
                }
            }
        }
    }

    private fun persistTrackNow(points: List<OffroadTrackPoint>) {
        val array = JSONArray()
        points.forEach { point ->
            array.put(JSONObject().apply {
                put("lat", point.latitude)
                put("lon", point.longitude)
                put("time", point.timestamp)
            })
        }
        var output: FileOutputStream? = null
        try {
            output = trackAtomicFile.startWrite()
            output.write(array.toString().toByteArray(Charsets.UTF_8))
            output.fd.sync()
            trackAtomicFile.finishWrite(output)
        } catch (_: Exception) {
            output?.let { trackAtomicFile.failWrite(it) }
        }
    }

    private fun loadPlaces() {
        try {
            val raw = prefs.getString("saved_places", null) ?: return
            val array = JSONArray(raw)
            val places = mutableListOf<SavedOffroadPlace>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                places += SavedOffroadPlace(o.getString("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"), o.optLong("time", 0L))
            }
            _savedPlaces.value = places
        } catch (_: Exception) { }
    }

    private fun persistPlaces() {
        val array = JSONArray()
        _savedPlaces.value.forEach { p -> array.put(JSONObject().apply {
            put("id", p.id); put("name", p.name); put("lat", p.latitude); put("lon", p.longitude); put("time", p.createdAt)
        }) }
        prefs.edit().putString("saved_places", array.toString()).apply()
    }

    private fun persistNavigationTarget(target: OffroadNavigationTarget) {
        prefs.edit().putString("navigation_target", JSONObject().apply {
            put("id", target.id); put("name", target.name); put("lat", target.latitude); put("lon", target.longitude)
        }.toString()).apply()
    }

    private fun restoreNavigationTarget() {
        try {
            val raw = prefs.getString("navigation_target", null) ?: return
            val o = JSONObject(raw)
            _navigationTarget.value = OffroadNavigationTarget(o.getString("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"))
        } catch (_: Exception) { }
    }

    private fun loadMapState(): OffroadMapState {
        return try {
            val raw = prefs.getString("map_state", null) ?: return OffroadMapState()
            val o = JSONObject(raw)
            val mode = try { MapOrientationMode.valueOf(o.optString("orientation", MapOrientationMode.NORTH_UP.name)) } catch (_: Exception) { MapOrientationMode.NORTH_UP }
            OffroadMapState(o.optDouble("lat", 0.0), o.optDouble("lon", 0.0), o.optInt("zoom", 13), o.optBoolean("follow", true), mode)
        } catch (_: Exception) { OffroadMapState() }
    }

    private fun calculateDistanceKm(points: List<OffroadTrackPoint>): Double {
        var total = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            total += distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) / 1000.0
        }
        return total
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(2)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return ((results[1] % 360f) + 360f) % 360f
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    companion object {
        private const val MAX_TRACK_KM = 1000.0
        private const val MAX_TRACK_POINTS = 24_000
        private const val MAX_IMPORTED_PLACES = 2_000
        private const val TRACK_PERSIST_INTERVAL_MS = 30_000L
        private const val TRACK_PERSIST_POINT_INTERVAL = 20
    }
}
