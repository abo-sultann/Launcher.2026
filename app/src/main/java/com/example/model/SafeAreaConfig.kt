package com.example.model

data class SafeAreaConfig(
    val topDp: Int = 8,
    val bottomDp: Int = 8,
    val leftDp: Int = 8,
    val rightDp: Int = 8
) {
    companion object {
        val DEFAULT = SafeAreaConfig(topDp = 8, bottomDp = 8, leftDp = 8, rightDp = 8)
        val MINIMAL = SafeAreaConfig(topDp = 0, bottomDp = 0, leftDp = 0, rightDp = 0)
        val CAR_STANDARD = SafeAreaConfig(topDp = 24, bottomDp = 12, leftDp = 12, rightDp = 12)
    }
}
