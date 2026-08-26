package com.example.data

import android.util.Log
import com.example.model.TripData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripComputer(private val preferencesManager: PreferencesManager) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _tripData = MutableStateFlow(preferencesManager.getTripData())
    val tripData: StateFlow<TripData> = _tripData.asStateFlow()

    private var tripTickerJob: Job? = null
    private var speedSamples = mutableListOf<Float>()

    fun startTrip() {
        if (_tripData.value.isRunning && !_tripData.value.isPaused) return

        _tripData.value = _tripData.value.copy(
            isRunning = true,
            isPaused = false,
            startTimeStamp = if (_tripData.value.startTimeStamp == 0L) System.currentTimeMillis() else _tripData.value.startTimeStamp,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
        startTicker()
    }

    fun pauseTrip() {
        _tripData.value = _tripData.value.copy(isPaused = true)
        tripTickerJob?.cancel()
        saveCurrentTrip()
    }

    fun resetTrip() {
        tripTickerJob?.cancel()
        speedSamples.clear()
        val emptyTrip = TripData(
            currentSpeedKmH = 0f,
            maxSpeedKmH = 0f,
            averageSpeedKmH = 0f,
            distanceKm = 0f,
            elapsedMovingTimeSec = 0L,
            elapsedStopTimeSec = 0L,
            isRunning = false,
            isPaused = false,
            startTimeStamp = 0L,
            lastUpdateTimestamp = 0L
        )
        _tripData.value = emptyTrip
        preferencesManager.saveTripData(emptyTrip)
    }

    fun updateSpeed(currentSpeedKmH: Float) {
        val current = _tripData.value
        val safeSpeed = currentSpeedKmH.coerceAtLeast(0f)

        if (!current.isRunning || current.isPaused) {
            _tripData.value = current.copy(currentSpeedKmH = safeSpeed)
            return
        }

        val newMax = maxOf(current.maxSpeedKmH, safeSpeed)
        speedSamples.add(safeSpeed)
        if (speedSamples.size > 3600) {
            speedSamples.removeAt(0)
        }
        val newAvg = if (speedSamples.isNotEmpty()) speedSamples.average().toFloat() else 0f

        _tripData.value = current.copy(
            currentSpeedKmH = safeSpeed,
            maxSpeedKmH = newMax,
            averageSpeedKmH = newAvg
        )
    }

    private fun startTicker() {
        tripTickerJob?.cancel()
        tripTickerJob = scope.launch {
            while (isActive && _tripData.value.isRunning && !_tripData.value.isPaused) {
                delay(1000L)
                val current = _tripData.value
                val isMoving = current.currentSpeedKmH > 1.5f

                val newMoving = if (isMoving) current.elapsedMovingTimeSec + 1 else current.elapsedMovingTimeSec
                val newStopped = if (!isMoving) current.elapsedStopTimeSec + 1 else current.elapsedStopTimeSec

                // Distance delta in km: speed (km/h) / 3600 per second
                val distDeltaKm = if (isMoving) current.currentSpeedKmH / 3600f else 0f
                val newDist = current.distanceKm + distDeltaKm

                _tripData.value = current.copy(
                    elapsedMovingTimeSec = newMoving,
                    elapsedStopTimeSec = newStopped,
                    distanceKm = newDist,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )

                // Save periodically every 10 seconds
                if ((newMoving + newStopped) % 10L == 0L) {
                    saveCurrentTrip()
                }
            }
        }
    }

    private fun saveCurrentTrip() {
        try {
            preferencesManager.saveTripData(_tripData.value)
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting trip data", e)
        }
    }

    fun release() {
        tripTickerJob?.cancel()
        saveCurrentTrip()
        scope.cancel()
    }

    companion object {
        private const val TAG = "TripComputer"
    }
}
