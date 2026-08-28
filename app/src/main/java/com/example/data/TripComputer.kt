package com.example.data

import android.location.Location
import android.util.Log
import com.example.model.GpsTelemetry
import com.example.model.SavedTrip
import com.example.model.TripData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class TripComputer(private val preferencesManager: PreferencesManager) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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

    init {
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
        _tripData.value = if (prior.isRunning && prior.isPaused) {
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
    }

    fun finishTrip(name: String? = null): SavedTrip? {
        val current = _tripData.value
        if (!current.isRunning) return null
        val now = System.currentTimeMillis()
        val shouldSave = current.distanceKm >= 0.10f || current.elapsedMovingTimeSec >= 30L
        val saved = if (shouldSave) {
            SavedTrip(
                id = UUID.randomUUID().toString(),
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
                placesSavedCount = current.placesSavedCount
            )
        } else null
        if (saved != null) {
            _history.value = (listOf(saved) + _history.value).take(100)
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
                next = next.copy(
                    lastLatitude = telemetry.latitude,
                    lastLongitude = telemetry.longitude,
                    validGpsSamples = next.validGpsSamples + 1
                )
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
        if (autoTrip && stoppedSince > 0L && now - stoppedSince >= AUTO_FINISH_STOP_MS && next.distanceKm >= 0.5f) {
            finishTrip()
        }
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

    private fun saveCurrentTrip() {
        try { preferencesManager.saveTripData(_tripData.value) }
        catch (e: Exception) { Log.e(TAG, "Error persisting trip data", e) }
    }

    fun release() {
        tripTickerJob?.cancel()
        saveCurrentTrip()
        scope.cancel()
    }

    companion object {
        private const val TAG = "TripComputer"
        private const val AUTO_FINISH_STOP_MS = 15L * 60L * 1000L
    }
}
