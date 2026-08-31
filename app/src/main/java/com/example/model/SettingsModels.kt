package com.example.model

enum class BackgroundType(val arabicName: String) {
    DARK_CARBON("كربون رياضي فاخر"),
    CYBER_CYAN("سايبر نيون أزرق"),
    AMBER_RACING("عنبر سباق ديناميكي"),
    DEEP_SPACE("فضاء داكن"),
    LUXURY_ONYX("عقيق أسود مونوكروم"),
    CUSTOM_IMAGE("صورة مخصصة من الجهاز")
}

/** Visual treatment for the floating navigation dock. */
enum class DockSurfaceStyle(val arabicName: String) {
    CLEAR("شفاف"),
    GLASS("زجاجي"),
    SOLID("داكن")
}

/** Small, offline-safe accent palette that remains readable on the car display. */
enum class InterfaceAccent(val arabicName: String, val argb: Int) {
    CYAN("سماوي", 0xFF00E5FF.toInt()),
    GOLD("ذهبي", 0xFFFFB84D.toInt()),
    WHITE("أبيض", 0xFFF5F7FA.toInt()),
    GREEN("أخضر", 0xFF37E6A1.toInt())
}

data class ScreenSaverWidgetLayout(
    val type: WidgetType,
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val opacity: Float = 0.90f,
    val zIndex: Int = 0,
    val style: WidgetStyle? = null,
    val surfaceStyle: WidgetSurfaceStyle = WidgetSurfaceStyle.TRANSPARENT,
    val showBorder: Boolean = false,
    val foregroundColorArgb: Int? = null,
    val accentColorArgb: Int? = null,
    val surfaceOpacity: Float = 1f
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
                zIndex = slot,
                style = screenSaverStylesFor(type).firstOrNull(),
                surfaceStyle = WidgetItem.defaultSurfaceFor(type),
                foregroundColorArgb = WidgetTone.WHITE.argb,
                accentColorArgb = WidgetTone.WHITE.argb
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
    val showAppLabels: Boolean = true,
    val appDrawerColumns: Int = 5,
    val homeGridColumns: Int = 4,
    val widgetHeightDp: Int = 138,
    val gridHorizontalGapDp: Int = 8,
    val gridVerticalGapDp: Int = 8,
    val is24HourFormat: Boolean = true,
    val showSeconds: Boolean = false,
    val speedUnit: String = "كم/س",
    val autoStartOnBoot: Boolean = true,
    val resumeMusicPlayback: Boolean = true,
    val safeModeActive: Boolean = false,
    val showTopBar: Boolean = true,
    val showBottomBar: Boolean = true,
    val bottomDockStyle: DockSurfaceStyle = DockSurfaceStyle.GLASS,
    val bottomDockOpacityPercent: Int = 76,
    val interfaceAccent: InterfaceAccent = InterfaceAccent.CYAN,
    val highContrastMode: Boolean = false,
    val keepScreenOn: Boolean = true,
    val autoLogTrips: Boolean = true,
    val childUnlockHoldSeconds: Int = 3,
    val screenSaverEnabled: Boolean = false,
    val screenSaverTimeoutSeconds: Int = 120,
    val screenSaverUseWallpaper: Boolean = true,
    val screenSaverWidgetTypes: Set<WidgetType> = setOf(WidgetType.CLOCK, WidgetType.SPEEDOMETER)
)

/** Screen saver widgets are informative only: no launchers or touch controls. */
val SCREEN_SAVER_DISPLAY_WIDGET_TYPES: Set<WidgetType> = linkedSetOf(
    WidgetType.CLOCK,
    WidgetType.SPEEDOMETER,
    WidgetType.DATE,
    WidgetType.GPS,
    WidgetType.MUSIC,
    WidgetType.MAP,
    WidgetType.TRIP
)

fun screenSaverStylesFor(type: WidgetType): List<WidgetStyle> = preferredWidgetStylesFor(type).filterNot { style ->
    style == WidgetStyle.MUSIC_COVER || style == WidgetStyle.TRIP_DASHBOARD
}
