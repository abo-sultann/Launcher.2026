package com.example.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val customOrder: Int = 0,
    val icon: Drawable? = null
)
