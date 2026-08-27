package com.example.model

enum class WidgetLayoutPreset(val arabicName: String) {
    AUTO("تلقائي"),
    CENTER_ROW("صف في المنتصف"),
    EVEN_ROW("صف متساوي"),
    TOP_ROW("صف علوي"),
    BOTTOM_ROW("صف سفلي"),
    LEFT_CENTER_RIGHT("يمين • وسط • يسار"),
    CENTER_COLUMN("عمود في المنتصف"),
    GRID_2X2("شبكة 2×2"),
    GRID_3X2("شبكة 3×2")
}

enum class WidgetLayoutTarget {
    HOME,
    SCREEN_SAVER
}
