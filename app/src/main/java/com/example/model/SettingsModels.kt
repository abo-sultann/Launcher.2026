package com.example.model

enum class BackgroundType(val arabicName: String) {
    DARK_CARBON("كربون رياضي فاخر"),
    CYBER_CYAN("سايبر نيون أزرق"),
    AMBER_RACING("عنبر سباق ديناميكي"),
    DEEP_SPACE("فضاء داكن"),
    LUXURY_ONYX("عقيق أسود مونوكروم"),
    CUSTOM_IMAGE("صورة مخصصة من الجهاز")
}

data class LauncherSettings(
    val safeArea: SafeAreaConfig = SafeAreaConfig.DEFAULT,
    val backgroundType: BackgroundType = BackgroundType.DARK_CARBON,
    val customWallpaperPath: String? = null,
    val iconSizeDp: Int = 64,
    val showAppNames: Boolean = true,
    val showAppLabels: Boolean = true,
    val appDrawerColumns: Int = 5,
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
    val autoLogTrips: Boolean = true
)
