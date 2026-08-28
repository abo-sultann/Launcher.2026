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
    val lastUpdateTimestamp: Long = 0L,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val validGpsSamples: Int = 0,
    val placesSavedCount: Int = 0
) {
    val totalTimeSec: Long
        get() = elapsedMovingTimeSec + elapsedStopTimeSec
}

data class TripRoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class SavedTrip(
    val id: String,
    val name: String,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
    val distanceKm: Float,
    val movingTimeSec: Long,
    val stopTimeSec: Long,
    val maxSpeedKmH: Float,
    val averageSpeedKmH: Float,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val placesSavedCount: Int = 0,
    val route: List<TripRoutePoint> = emptyList()
)

data class GpsTelemetry(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val speedKmH: Float = 0f,
    val bearingDegrees: Float = 0f,
    val accuracyMeters: Float = 0f,
    val hasGpsFix: Boolean = false,
    val satellitesCount: Int = 0,
    val statusArabic: String = "في انتظار إشارة GPS...",
    val isSpeedReliable: Boolean = false,
    val fixAgeMs: Long = Long.MAX_VALUE,
    val providerName: String = "",
    val rejectedReason: String = ""
)
