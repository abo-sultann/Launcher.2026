package com.example.model

import android.graphics.Bitmap

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val customOrder: Int = 0,
    // Small pre-scaled bitmap prepared on the IO thread. Keeping Drawable conversion
    // out of Compose prevents old Android head units from freezing when opening Apps.
    val iconBitmap: Bitmap? = null
)
