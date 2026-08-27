package com.example.data

import android.content.Context
import android.location.Location
import com.example.model.GpsTelemetry
import com.example.model.OffroadNavigationTarget
import com.example.model.OffroadTrackPoint
import com.example.model.SavedOffroadPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.max

class OffroadTrackManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("offroad_navigation_2026", Context.MODE_PRIVATE)
    private val trackFile = File(context.filesDir, "offroad_track_rolling.json")

    private val _trackPoints = MutableStateFlow<List<OffroadTrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<OffroadTrackPoint>> = _trackPoints.asStateFlow()

    private val _savedPlaces = MutableStateFlow<List<SavedOffroadPlace>>(emptyList())
    val savedPlaces: StateFlow<List<SavedOffroadPlace>> = _savedPlaces.asStateFlow()

    private val _navigationTarget = MutableStateFlow<OffroadNavigationTarget?>(null)
    val navigationTarget: StateFlow<OffroadNavigationTarget?> = _navigationTarget.asStateFlow()

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

        // Garmin-style rolling breadcrumb: preserve approximately the newest 1000 km.
        while (totalTrackKm > MAX_TRACK_KM && updated.size > 2) {
            val a = updated[0]
            val b = updated[1]
            totalTrackKm -= distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) / 1000.0
            updated.removeAt(0)
        }

        // Additional safety cap for the 1 GB head unit. Distance remains the primary retention rule.
        while (updated.size > MAX_TRACK_POINTS) {
            val a = updated[0]
            val b = updated[1]
            totalTrackKm = max(0.0, totalTrackKm - distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) / 1000.0)
            updated.removeAt(0)
        }

        _trackPoints.value = updated
        if (now - lastPersistAt > 15_000L || updated.size % 8 == 0) {
            lastPersistAt = now
            persistTrackAsync(updated)
        }
    }

    fun clearTrack() {
        totalTrackKm = 0.0
        _trackPoints.value = emptyList()
        persistTrackAsync(emptyList())
    }

    fun saveCurrentPlace(telemetry: GpsTelemetry): SavedOffroadPlace? {
        if (!telemetry.hasGpsFix) return null
        val nextNumber = _savedPlaces.value.size + 1
        val place = SavedOffroadPlace(
            id = UUID.randomUUID().toString(),
            name = "موقع محفوظ $nextNumber",
            latitude = telemetry.latitude,
            longitude = telemetry.longitude,
            createdAt = System.currentTimeMillis()
        )
        _savedPlaces.value = _savedPlaces.value + place
        persistPlaces()
        return place
    }

    fun deletePlace(id: String) {
        _savedPlaces.value = _savedPlaces.value.filterNot { it.id == id }
        if (_navigationTarget.value?.id == id) stopNavigation()
        persistPlaces()
    }

    fun navigateTo(place: SavedOffroadPlace) {
        val target = OffroadNavigationTarget(place.id, place.name, place.latitude, place.longitude)
        _navigationTarget.value = target
        persistNavigationTarget(target)
    }

    fun navigateToTrackStart() {
        val start = _trackPoints.value.firstOrNull() ?: return
        val target = OffroadNavigationTarget("track_start", "بداية المسار", start.latitude, start.longitude)
        _navigationTarget.value = target
        persistNavigationTarget(target)
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
        val results = FloatArray(2)
        Location.distanceBetween(current.latitude, current.longitude, target.latitude, target.longitude, results)
        return ((results[1] % 360f) + 360f) % 360f
    }

    fun renderPoints(maxPoints: Int = 6000): List<OffroadTrackPoint> {
        val points = _trackPoints.value
        if (points.size <= maxPoints) return points
        val step = (points.size.toDouble() / maxPoints.toDouble()).toInt().coerceAtLeast(1)
        val sampled = points.filterIndexed { index, _ -> index % step == 0 }.toMutableList()
        points.lastOrNull()?.let { if (sampled.lastOrNull() != it) sampled += it }
        return sampled
    }

    fun release() {
        persistTrackAsync(_trackPoints.value)
        scope.cancel()
    }

    private fun loadTrack() {
        try {
            if (!trackFile.exists()) return
            val array = JSONArray(trackFile.readText())
            val points = ArrayList<OffroadTrackPoint>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                points += OffroadTrackPoint(o.getDouble("lat"), o.getDouble("lon"), o.optLong("time", 0L))
            }
            _trackPoints.value = points
            totalTrackKm = calculateDistanceKm(points)
            if (totalTrackKm > MAX_TRACK_KM || points.size > MAX_TRACK_POINTS) {
                val trimmed = points.toMutableList()
                while ((totalTrackKm > MAX_TRACK_KM || trimmed.size > MAX_TRACK_POINTS) && trimmed.size > 2) {
                    val a = trimmed[0]
                    val b = trimmed[1]
                    totalTrackKm = max(0.0, totalTrackKm - distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) / 1000.0)
                    trimmed.removeAt(0)
                }
                _trackPoints.value = trimmed
                persistTrackAsync(trimmed)
            }
        } catch (_: Exception) {
            _trackPoints.value = emptyList()
            totalTrackKm = 0.0
        }
    }

    private fun persistTrackAsync(points: List<OffroadTrackPoint>) {
        val snapshot = points.toList()
        scope.launch {
            try {
                val array = JSONArray()
                snapshot.forEach { point ->
                    array.put(JSONObject().apply {
                        put("lat", point.latitude)
                        put("lon", point.longitude)
                        put("time", point.timestamp)
                    })
                }
                trackFile.writeText(array.toString())
            } catch (_: Exception) { }
        }
    }

    private fun loadPlaces() {
        try {
            val raw = prefs.getString("saved_places", null) ?: return
            val array = JSONArray(raw)
            val places = mutableListOf<SavedOffroadPlace>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                places += SavedOffroadPlace(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    latitude = o.getDouble("lat"),
                    longitude = o.getDouble("lon"),
                    createdAt = o.optLong("time", 0L)
                )
            }
            _savedPlaces.value = places
        } catch (_: Exception) { }
    }

    private fun persistPlaces() {
        val array = JSONArray()
        _savedPlaces.value.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("lat", p.latitude)
                put("lon", p.longitude)
                put("time", p.createdAt)
            })
        }
        prefs.edit().putString("saved_places", array.toString()).apply()
    }

    private fun persistNavigationTarget(target: OffroadNavigationTarget) {
        val o = JSONObject().apply {
            put("id", target.id)
            put("name", target.name)
            put("lat", target.latitude)
            put("lon", target.longitude)
        }
        prefs.edit().putString("navigation_target", o.toString()).apply()
    }

    private fun restoreNavigationTarget() {
        try {
            val raw = prefs.getString("navigation_target", null) ?: return
            val o = JSONObject(raw)
            _navigationTarget.value = OffroadNavigationTarget(
                o.getString("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon")
            )
        } catch (_: Exception) { }
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

    companion object {
        private const val MAX_TRACK_KM = 1000.0
        private const val MAX_TRACK_POINTS = 24_000
    }
}
