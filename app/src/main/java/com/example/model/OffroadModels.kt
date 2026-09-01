package com.example.model

data class OffroadTrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class SavedOffroadPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long
)

data class OffroadNavigationTarget(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

enum class MapOrientationMode(val arabicName: String) {
    NORTH_UP("الشمال للأعلى"),
    HEADING_UP("اتجاه السير للأعلى")
}

data class OffroadMapState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val zoomLevel: Int = 13,
    val followGps: Boolean = true,
    val orientationMode: MapOrientationMode = MapOrientationMode.NORTH_UP
)

data class OfflineMapSearchResult(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val source: String,
    val distanceMeters: Float? = null
)
