package com.example.util

fun bearingToArabicDirection(bearingDegrees: Float): String {
    val normalized = ((bearingDegrees % 360f) + 360f) % 360f
    return when {
        normalized < 22.5f || normalized >= 337.5f -> "شمال"
        normalized < 67.5f -> "شمال شرقي"
        normalized < 112.5f -> "شرق"
        normalized < 157.5f -> "جنوب شرقي"
        normalized < 202.5f -> "جنوب"
        normalized < 247.5f -> "جنوب غربي"
        normalized < 292.5f -> "غرب"
        else -> "شمال غربي"
    }
}
