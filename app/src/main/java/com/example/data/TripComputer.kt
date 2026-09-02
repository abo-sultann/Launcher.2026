package com.example.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.CarLauncherApp
import com.example.model.GpsTelemetry
import com.example.model.SavedTrip
import com.example.model.TripData
import com.example.model.TripRoutePoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class TripComputer(private val preferencesManager: PreferencesManager) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val routePrefs = CarLauncherApp.instance.getSharedPreferences("launcher_trip_routes_2026", Context.MODE_PRIVATE)
    private val _tripData = MutableStateFlow(preferencesManager.getTripData())
    val tripData: StateFlow<TripData> = _tripData.asStateFlow()
    private val _history = MutableStateFlow(preferencesManager.getSavedTrips())
    val history: StateFlow<List<SavedTrip>> = _history.asStateFlow()

    private var tripTickerJob: Job? = null
    private val speedSamples = ArrayDeque<Float>()
    private var lastTelemetry: GpsTelemetry? = null
    private var lastAcceptedTripLocation: Location? = null
    private var lastDistanceUpdateTime = 0L
    private var autoTrip = false
    private var movementCandidateStartedAt = 0L
    private var movementCandidateSamples = 0
    private var movementCandidateDistanceM = 0f
    private var movementCandidateLocation: Location? = null
    private var stoppedSince = 0L
    private val currentRoute = mutableListOf<TripRoutePoint>()

    init {
        if (_tripData.value.isRunning) currentRoute += loadRoute(CURRENT_ROUTE_KEY)
        if (_tripData.value.isRunning && !_tripData.value.isPaused) startTicker()
    }

    fun startTrip() {
        val telemetry = lastTelemetry
        startTripInternal(
            automatic = false,
            startLat = telemetry?.latitude ?: _tripData.value.lastLatitude,
            startLon = telemetry?.longitude ?: _tripData.value.lastLongitude,
            startTime = System.currentTimeMillis()
        )
    }

    private fun startTripInternal(automatic: Boolean, startLat: Double, startLon: Double, startTime: Long) {
        if (_tripData.value.isRunning && !_tripData.value.isPaused) return
        autoTrip = automatic
        val prior = _tripData.value
        val resuming = prior.isRunning && prior.isPaused
        _tripData.value = if (resuming) {
            prior.copy(isPaused = false, lastUpdateTimestamp = System.currentTimeMillis())
        } else {
            TripData(
                isRunning = true,
                isPaused = false,
                startTimeStamp = startTime,
                lastUpdateTimestamp = System.currentTimeMillis(),
                startLatitude = startLat,
                startLongitude = startLon,
                lastLatitude = startLat,
                lastLongitude = startLon
            )
        }
        speedSamples.clear()
        if (!resuming) {
            currentRoute.clear()
            if (startLat != 0.0 || startLon != 0.0) currentRoute += TripRoutePoint(startLat, startLon, startTime)
            persistRoute(CURRENT_ROUTE_KEY, currentRoute)
        }
        lastAcceptedTripLocation = telemetryToLocation(lastTelemetry)?.takeIf { startLat != 0.0 || startLon != 0.0 }
        lastDistanceUpdateTime = System.currentTimeMillis()
        stoppedSince = 0L
        resetMovementCandidate()
        startTicker()
        saveCurrentTrip()
    }

    fun pauseTrip() {
        _tripData.value = _tripData.value.copy(isPaused = true, currentSpeedKmH = 0f)
        tripTickerJob?.cancel()
        saveCurrentTrip()
        persistRoute(CURRENT_ROUTE_KEY, currentRoute)
    }

    fun finishTrip(name: String? = null): SavedTrip? {
        val current = _tripData.value
        if (!current.isRunning) return null
        val now = System.currentTimeMillis()
        val shouldSave = current.distanceKm >= 0.10f || current.elapsedMovingTimeSec >= 30L
        val id = UUID.randomUUID().toString()
        val saved = if (shouldSave) {
            SavedTrip(
                id = id,
                name = name?.trim()?.takeIf { it.isNotBlank() } ?: defaultTripName(current.startTimeStamp),
                startTimeStamp = current.startTimeStamp,
                endTimeStamp = now,
                distanceKm = current.distanceKm,
                movingTimeSec = current.elapsedMovingTimeSec,
                stopTimeSec = current.elapsedStopTimeSec,
                maxSpeedKmH = current.maxSpeedKmH,
                averageSpeedKmH = current.averageSpeedKmH,
                startLatitude = current.startLatitude,
                startLongitude = current.startLongitude,
                endLatitude = current.lastLatitude,
                endLongitude = current.lastLongitude,
                placesSavedCount = current.placesSavedCount,
                route = currentRoute.toList()
            )
        } else null
        if (saved != null) {
            persistRoute(saved.id, saved.route)
            _history.value = (listOf(saved.copy(route = emptyList())) + _history.value).take(100)
            preferencesManager.saveSavedTrips(_history.value)
        }
        clearCurrentTrip()
        return saved
    }

    fun resetTrip() = clearCurrentTrip()

    private fun clearCurrentTrip() {
        tripTickerJob?.cancel()
        speedSamples.clear()
        autoTrip = false
        lastAcceptedTripLocation = null
        stoppedSince = 0L
        resetMovementCandidate()
        currentRoute.clear()
        routePrefs.edit().remove(CURRENT_ROUTE_KEY).apply()
        _tripData.value = TripData()
        preferencesManager.saveTripData(_tripData.value)
    }

    fun updateTelemetry(telemetry: GpsTelemetry, autoLogTrips: Boolean) {
        lastTelemetry = telemetry
        val safeSpeed = if (telemetry.hasGpsFix && telemetry.isSpeedReliable && telemetry.fixAgeMs <= 8_000L) telemetry.speedKmH.coerceAtLeast(0f) else 0f
        val current = _tripData.value

        if (!current.isRunning) {
            _tripData.value = current.copy(currentSpeedKmH = safeSpeed)
            if (autoLogTrips) evaluateAutoStart(telemetry, safeSpeed) else resetMovementCandidate()
            return
        }
        if (current.isPaused) {
            _tripData.value = current.copy(currentSpeedKmH = 0f)
            return
        }

        val now = System.currentTimeMillis()
        var next = current.copy(currentSpeedKmH = safeSpeed, lastUpdateTimestamp = now)
        if (telemetry.hasGpsFix && telemetry.isSpeedReliable && telemetry.accuracyMeters in 0f..40f && telemetry.fixAgeMs <= 8_000L) {
            val newLoc = telemetryToLocation(telemetry)
            val oldLoc = lastAcceptedTripLocation
            if (newLoc != null) {
                if (oldLoc != null) {
                    val dtSec = ((newLoc.time - oldLoc.time) / 1000f).takeIf { it > 0f } ?: ((now - lastDistanceUpdateTime) / 1000f)
                    val distanceM = oldLoc.distanceTo(newLoc)
                    val impliedSpeed = if (dtSec > 0f) distanceM / dtSec * 3.6f else 0f
                    val movementThreshold = max(3f, (telemetry.accuracyMeters + oldLoc.accuracy) * 0.22f)
                    val validDistance = dtSec in 0.5f..8f && distanceM >= movementThreshold && impliedSpeed <= 180f && safeSpeed >= 2.2f
                    if (validDistance) next = next.copy(distanceKm = next.distanceKm + distanceM / 1000f)
                }
                lastAcceptedTripLocation = newLoc
                lastDistanceUpdateTime = now
                next = next.copy(lastLatitude = telemetry.latitude, lastLongitude = telemetry.longitude, validGpsSamples = next.validGpsSamples + 1)

                // Route shape is intentionally independent from trip-distance acceptance.
                // A legitimate turn may contain short GPS steps that should not add distance noise,
                // but those points are still needed to draw the road instead of a straight chord.
                maybeAppendRoutePoint(telemetry, now)
            }
        }

        if (safeSpeed >= 2.2f) {
            speedSamples.addLast(safeSpeed)
            while (speedSamples.size > 1800) speedSamples.removeFirst()
            next = next.copy(
                maxSpeedKmH = maxOf(next.maxSpeedKmH, safeSpeed),
                averageSpeedKmH = if (speedSamples.isNotEmpty()) speedSamples.average().toFloat() else next.averageSpeedKmH
            )
            stoppedSince = 0L
        } else if (stoppedSince == 0L) {
            stoppedSince = now
        }

        _tripData.value = next
        if (autoTrip && stoppedSince > 0L && now - stoppedSince >= AUTO_FINISH_STOP_MS && next.distanceKm >= 0.5f) finishTrip()
    }

    private fun maybeAppendRoutePoint(telemetry: GpsTelemetry, now: Long) {
        val last = currentRoute.lastOrNull()
        val append = if (last == null) true else {
            val result = FloatArray(1)
            Location.distanceBetween(last.latitude, last.longitude, telemetry.latitude, telemetry.longitude, result)
            val gapMs = now - last.timestamp
            val routeThreshold = max(6f, telemetry.accuracyMeters * .30f).coerceAtMost(14f)
            (result[0] >= routeThreshold && gapMs >= 2_000L) || gapMs >= 8_000L
        }
        if (!append) return

        currentRoute += TripRoutePoint(telemetry.latitude, telemetry.longitude, now)
        if (currentRoute.size > MAX_ROUTE_POINTS) {
            val compacted = currentRoute.filterIndexed { index, _ -> index % 2 == 0 }.takeLast(MAX_ROUTE_POINTS).toList()
            currentRoute.clear()
            currentRoute.addAll(compacted)
        }
        if (currentRoute.size % ROUTE_PERSIST_POINT_INTERVAL == 0) persistRoute(CURRENT_ROUTE_KEY, currentRoute)
    }

    private fun evaluateAutoStart(telemetry: GpsTelemetry, speed: Float) {
        if (!telemetry.hasGpsFix || !telemetry.isSpeedReliable || telemetry.accuracyMeters !in 0f..35f || speed < 4f) {
            if (speed == 0f) resetMovementCandidate()
            return
        }
        val loc = telemetryToLocation(telemetry) ?: return
        if (movementCandidateStartedAt == 0L) {
            movementCandidateStartedAt = System.currentTimeMillis()
            movementCandidateSamples = 1
            movementCandidateDistanceM = 0f
            movementCandidateLocation = loc
            return
        }
        movementCandidateSamples++
        movementCandidateLocation?.let { old ->
            val distance = old.distanceTo(loc)
            if (distance in 0f..120f) movementCandidateDistanceM += distance
        }
        movementCandidateLocation = loc
        val elapsed = System.currentTimeMillis() - movementCandidateStartedAt
        if (movementCandidateSamples >= 8 && elapsed >= 8_000L && (movementCandidateDistanceM >= 60f || elapsed >= 18_000L)) {
            startTripInternal(true, loc.latitude, loc.longitude, movementCandidateStartedAt)
        }
    }

    private fun startTicker() {
        tripTickerJob?.cancel()
        tripTickerJob = scope.launch {
            while (isActive && _tripData.value.isRunning && !_tripData.value.isPaused) {
                delay(1000L)
                val current = _tripData.value
                val moving = current.currentSpeedKmH >= 2.2f
                _tripData.value = current.copy(
                    elapsedMovingTimeSec = current.elapsedMovingTimeSec + if (moving) 1 else 0,
                    elapsedStopTimeSec = current.elapsedStopTimeSec + if (moving) 0 else 1,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )
                if ((_tripData.value.totalTimeSec % 10L) == 0L) saveCurrentTrip()
            }
        }
    }

    fun noteSavedPlace() {
        val current = _tripData.value
        if (!current.isRunning) return
        _tripData.value = current.copy(placesSavedCount = current.placesSavedCount + 1)
        saveCurrentTrip()
    }

    fun renameSavedTrip(id: String, name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        _history.value = _history.value.map { if (it.id == id) it.copy(name = clean.take(50)) else it }
        preferencesManager.saveSavedTrips(_history.value)
    }

    fun deleteSavedTrip(id: String) {
        _history.value = _history.value.filterNot { it.id == id }
        preferencesManager.saveSavedTrips(_history.value)
        routePrefs.edit().remove(routeKey(id)).apply()
    }

    private fun telemetryToLocation(t: GpsTelemetry?): Location? {
        t ?: return null
        if (!t.hasGpsFix || (t.latitude == 0.0 && t.longitude == 0.0)) return null
        return Location("trip").apply {
            latitude = t.latitude
            longitude = t.longitude
            accuracy = t.accuracyMeters.coerceAtLeast(1f)
            time = System.currentTimeMillis() - t.fixAgeMs.coerceIn(0L, 8_000L)
        }
    }

    private fun resetMovementCandidate() {
        movementCandidateStartedAt = 0L
        movementCandidateSamples = 0
        movementCandidateDistanceM = 0f
        movementCandidateLocation = null
    }

    private fun defaultTripName(startTime: Long): String {
        val sdf = SimpleDateFormat("dd/MM - HH:mm", Locale("ar"))
        return "رحلة ${sdf.format(Date(startTime.takeIf { it > 0L } ?: System.currentTimeMillis()))}"
    }

    private fun persistRoute(id: String, points: List<TripRoutePoint>) {
        try {
            val array = JSONArray()
            points.takeLast(MAX_ROUTE_POINTS).forEach { p -> array.put(JSONObject().apply { put("lat", p.latitude); put("lon", p.longitude); put("t", p.timestamp) }) }
            routePrefs.edit().putString(routeKey(id), array.toString()).apply()
        } catch (e: Exception) { Log.w(TAG, "Unable to persist trip route", e) }
    }

    fun hasSavedTripRoute(id: String): Boolean =
        routePrefs.getString(routeKey(id), null)?.let { it.length > 2 } == true

    fun loadSavedTripWithRoute(id: String): SavedTrip? {
        val saved = _history.value.firstOrNull { it.id == id } ?: return null
        val route = loadRoute(id)
        return saved.copy(route = route).takeIf { route.isNotEmpty() }
    }

    private fun loadRoute(id: String): List<TripRoutePoint> {
        return try {
            val array = JSONArray(routePrefs.getString(routeKey(id), "[]") ?: "[]")
            List(minOf(array.length(), MAX_ROUTE_POINTS)) { i ->
                array.getJSONObject(i).let {
                    TripRoutePoint(it.getDouble("lat"), it.getDouble("lon"), it.optLong("t", 0L))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun routeKey(id: String) = if (id == CURRENT_ROUTE_KEY) CURRENT_ROUTE_KEY else "route_$id"

    private fun saveCurrentTrip() {
        try { preferencesManager.saveTripData(_tripData.value) }
        catch (e: Exception) { Log.e(TAG, "Error persisting trip data", e) }
    }

    fun release() {
        tripTickerJob?.cancel()
        saveCurrentTrip()
        if (_tripData.value.isRunning) persistRoute(CURRENT_ROUTE_KEY, currentRoute)
        scope.cancel()
    }

    companion object {
        private const val TAG = "TripComputer"
        private const val AUTO_FINISH_STOP_MS = 15L * 60L * 1000L
        private const val MAX_ROUTE_POINTS = 2500
        private const val ROUTE_PERSIST_POINT_INTERVAL = 24
        private const val CURRENT_ROUTE_KEY = "current_route"
    }
}
