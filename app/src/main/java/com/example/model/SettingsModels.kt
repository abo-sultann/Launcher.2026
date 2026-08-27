package com.example.model

enum class BackgroundType(val arabicName: String) {
    DARK_CARBON("كربون رياضي فاخر"),
    CYBER_CYAN("سايبر نيون أزرق"),
    AMBER_RACING("عنبر سباق ديناميكي"),
    DEEP_SPACE("فضاء داكن"),
    LUXURY_ONYX("عقيق أسود مونوكروم"),
    CUSTOM_IMAGE("صورة مخصصة من الجهاز")
}

data class ScreenSaverWidgetLayout(
    val type: WidgetType,
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val opacity: Float = 0.90f,
    val zIndex: Int = 0
) {
    companion object {
        fun defaultFor(type: WidgetType, index: Int): ScreenSaverWidgetLayout {
            val slot = index.coerceIn(0, 3)
            val x = if (slot % 2 == 0) 0.05f else 0.53f
            val y = if (slot < 2) 0.12f else 0.55f
            return ScreenSaverWidgetLayout(
                type = type,
                xFraction = x,
                yFraction = y,
                widthFraction = 0.42f,
                heightFraction = 0.34f,
                opacity = 0.90f,
                zIndex = slot
            )
        }
    }
}

data class LauncherSettings(
    val safeArea: SafeAreaConfig = SafeAreaConfig.DEFAULT,
    val backgroundType: BackgroundType = BackgroundType.DARK_CARBON,
    val customWallpaperPath: String? = null,
    val wallpaperDimPercent: Int = 10,
    val iconSizeDp: Int = 64,
    val showAppNames: Boolean = true,
    val showAppLabels: Boolean = true,
    val appDrawerColumns: Int = 5,
    val homeGridColumns: Int = 4,
    val widgetHeightDp: Int = 138,
    val gridHorizontalGapDp: Int = 8,
    val gridVerticalGapDp: Int = 8,
    val is24HourClock: Boolean = true,
    val is24HourFormat: Boolean = true,
    val showSeconds: Boolean = false,
    val speedUnit: String = "كم/س",
    val autoStartEnabled: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val resumeMusicPlayback: Boolean = true,
    val safeModeActive: Boolean = false,
    val showTopBar: Boolean = true,
    val showBottomBar: Boolean = true,
    val highContrastMode: Boolean = false,
    val keepScreenOn: Boolean = true,
    val autoLogTrips: Boolean = true,
    val childUnlockHoldSeconds: Int = 3,
    val screenSaverEnabled: Boolean = false,
    val screenSaverTimeoutSeconds: Int = 120,
    val screenSaverUseWallpaper: Boolean = true,
    val screenSaverWidgetTypes: Set<WidgetType> = setOf(WidgetType.CLOCK, WidgetType.SPEEDOMETER)
)
