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
