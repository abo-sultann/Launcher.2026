package com.example.model

data class TripData(
    val currentSpeedKmH: Float = 0f,
    val maxSpeedKmH: Float = 0f,
    val averageSpeedKmH: Float = 0f,
    val distanceKm: Float = 0f,
    val elapsedMovingTimeSec: Long = 0L,
    val elapsedStopTimeSec: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val startTimeStamp: Long = 0L,
    val lastUpdateTimestamp: Long = 0L
) {
    val totalTimeSec: Long
        get() = elapsedMovingTimeSec + elapsedStopTimeSec
}

data class GpsTelemetry(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val speedKmH: Float = 0f,
    val bearingDegrees: Float = 0f,
    val accuracyMeters: Float = 0f,
    val hasGpsFix: Boolean = false,
    val satellitesCount: Int = 0,
    val statusArabic: String = "في انتظار إشارة GPS..."
)
