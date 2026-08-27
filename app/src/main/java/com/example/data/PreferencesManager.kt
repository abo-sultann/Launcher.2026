package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("car_launcher_preferences_2026", Context.MODE_PRIVATE)

    fun getSafeArea(): SafeAreaConfig = try {
        SafeAreaConfig(
            topDp = prefs.getInt("safe_top", 0),
            bottomDp = prefs.getInt("safe_bottom", 0),
            leftDp = prefs.getInt("safe_left", 0),
            rightDp = prefs.getInt("safe_right", 0)
        )
    } catch (e: Exception) { SafeAreaConfig.DEFAULT }

    fun getSafeAreaConfig(): SafeAreaConfig = getSafeArea()

    fun saveSafeArea(config: SafeAreaConfig) {
        prefs.edit()
            .putInt("safe_top", config.topDp.coerceIn(0, 250))
            .putInt("safe_bottom", config.bottomDp.coerceIn(0, 150))
            .putInt("safe_left", config.leftDp.coerceIn(0, 150))
            .putInt("safe_right", config.rightDp.coerceIn(0, 150))
            .apply()
    }
    fun saveSafeAreaConfig(config: SafeAreaConfig) = saveSafeArea(config)

    fun getSettings(): LauncherSettings = try {
        val bgName = prefs.getString("bg_type", BackgroundType.DARK_CARBON.name) ?: BackgroundType.DARK_CARBON.name
        val bgType = try { BackgroundType.valueOf(bgName) } catch (_: Exception) { BackgroundType.DARK_CARBON }
        val screenSaverTypes = (prefs.getStringSet("screensaver_widgets", null)
            ?: setOf(WidgetType.CLOCK.name, WidgetType.SPEEDOMETER.name))
            .mapNotNull { name -> try { WidgetType.valueOf(name) } catch (_: Exception) { null } }
            .toSet()
            .ifEmpty { setOf(WidgetType.CLOCK) }

        LauncherSettings(
            safeArea = getSafeArea(),
            backgroundType = bgType,
            customWallpaperPath = prefs.getString("custom_wallpaper_path", null),
            wallpaperDimPercent = prefs.getInt("wallpaper_dim", 10).coerceIn(0, 80),
            iconSizeDp = prefs.getInt("icon_size", 64),
            showAppNames = prefs.getBoolean("show_app_names", true),
            showAppLabels = prefs.getBoolean("show_app_labels", true),
            appDrawerColumns = prefs.getInt("app_columns", 5),
            homeGridColumns = prefs.getInt("home_columns", 4),
            widgetHeightDp = prefs.getInt("widget_height", 138),
            gridHorizontalGapDp = prefs.getInt("grid_h_gap", 8),
            gridVerticalGapDp = prefs.getInt("grid_v_gap", 8),
            is24HourClock = prefs.getBoolean("clock_24h", true),
            is24HourFormat = prefs.getBoolean("clock_24h", true),
            showSeconds = prefs.getBoolean("clock_seconds", false),
            speedUnit = prefs.getString("speed_unit", "كم/س") ?: "كم/س",
            autoStartEnabled = prefs.getBoolean("auto_start", true),
            autoStartOnBoot = prefs.getBoolean("auto_start", true),
            resumeMusicPlayback = prefs.getBoolean("resume_music", true),
            safeModeActive = prefs.getBoolean("safe_mode", false),
            showTopBar = prefs.getBoolean("show_top_bar", true),
            showBottomBar = prefs.getBoolean("show_bottom_bar", true),
            highContrastMode = prefs.getBoolean("high_contrast", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            autoLogTrips = prefs.getBoolean("auto_log_trips", true),
            childUnlockHoldSeconds = prefs.getInt("child_unlock_hold", 3).coerceIn(2, 6),
            screenSaverEnabled = prefs.getBoolean("screensaver_enabled", false),
            screenSaverTimeoutSeconds = prefs.getInt("screensaver_timeout", 120).coerceIn(30, 1800),
            screenSaverUseWallpaper = prefs.getBoolean("screensaver_wallpaper", true),
            screenSaverWidgetTypes = screenSaverTypes,
            screenSaverNightMode = prefs.getBoolean("screensaver_night_mode", false),
            screenSaverNightBrightnessPercent = prefs.getInt("screensaver_night_brightness", 14).coerceIn(5, 40)
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error reading settings", e)
        LauncherSettings()
    }

    fun saveSettings(settings: LauncherSettings) {
        try {
            saveSafeArea(settings.safeArea)
            prefs.edit()
                .putString("bg_type", settings.backgroundType.name)
                .putString("custom_wallpaper_path", settings.customWallpaperPath)
                .putInt("wallpaper_dim", settings.wallpaperDimPercent.coerceIn(0, 80))
                .putInt("icon_size", settings.iconSizeDp.coerceIn(40, 110))
                .putBoolean("show_app_names", settings.showAppNames)
                .putBoolean("show_app_labels", settings.showAppLabels)
                .putInt("app_columns", settings.appDrawerColumns.coerceIn(2, 8))
                .putInt("home_columns", settings.homeGridColumns.coerceIn(2, 6))
                .putInt("widget_height", settings.widgetHeightDp.coerceIn(90, 220))
                .putInt("grid_h_gap", settings.gridHorizontalGapDp.coerceIn(2, 24))
                .putInt("grid_v_gap", settings.gridVerticalGapDp.coerceIn(2, 24))
                .putBoolean("clock_24h", settings.is24HourFormat)
                .putBoolean("clock_seconds", settings.showSeconds)
                .putString("speed_unit", settings.speedUnit)
                .putBoolean("auto_start", settings.autoStartOnBoot)
                .putBoolean("resume_music", settings.resumeMusicPlayback)
                .putBoolean("safe_mode", settings.safeModeActive)
                .putBoolean("show_top_bar", settings.showTopBar)
                .putBoolean("show_bottom_bar", settings.showBottomBar)
                .putBoolean("high_contrast", settings.highContrastMode)
                .putBoolean("keep_screen_on", settings.keepScreenOn)
                .putBoolean("auto_log_trips", settings.autoLogTrips)
                .putInt("child_unlock_hold", settings.childUnlockHoldSeconds.coerceIn(2, 6))
                .putBoolean("screensaver_enabled", settings.screenSaverEnabled)
                .putInt("screensaver_timeout", settings.screenSaverTimeoutSeconds.coerceIn(30, 1800))
                .putBoolean("screensaver_wallpaper", settings.screenSaverUseWallpaper)
                .putStringSet("screensaver_widgets", settings.screenSaverWidgetTypes.map { it.name }.toSet())
                .putBoolean("screensaver_night_mode", settings.screenSaverNightMode)
                .putInt("screensaver_night_brightness", settings.screenSaverNightBrightnessPercent.coerceIn(5, 40))
                .apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving settings", e) }
    }

    fun getWidgets(): List<WidgetItem> {
        val raw = prefs.getString("widgets_json", null) ?: return WidgetItem.createDefaultList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<WidgetItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val type = try { WidgetType.valueOf(obj.getString("type")) } catch (_: Exception) { WidgetType.CLOCK }
                val style = try { WidgetStyle.valueOf(obj.getString("style")) } catch (_: Exception) {
                    when (type) {
                        WidgetType.CLOCK -> WidgetStyle.CLOCK_AUTOMOTIVE_LARGE
                        WidgetType.SPEEDOMETER -> WidgetStyle.SPEED_GAUGE_CIRCULAR
                        WidgetType.DATE -> WidgetStyle.DATE_DAY_DATE
                        WidgetType.GPS -> WidgetStyle.GPS_CARD
                        WidgetType.MUSIC -> WidgetStyle.MUSIC_COVER
                        WidgetType.MAP -> WidgetStyle.MAP_MEDIUM
                        WidgetType.TRIP -> WidgetStyle.TRIP_CARD
                        WidgetType.APPS -> WidgetStyle.APPS_HORIZONTAL_DOCK
                        WidgetType.CONTROLS -> WidgetStyle.CONTROLS_CARD
                    }
                }

                val base = WidgetItem(
                    id = id,
                    type = type,
                    style = style,
                    spanX = obj.optInt("spanX", 1),
                    spanY = obj.optInt("spanY", 1),
                    isVisible = obj.optBoolean("isVisible", true),
                    order = obj.optInt("order", i),
                    xFraction = obj.optDouble("xFraction", -1.0).toFloat(),
                    yFraction = obj.optDouble("yFraction", -1.0).toFloat(),
                    widthFraction = obj.optDouble("widthFraction", 0.0).toFloat(),
                    heightFraction = obj.optDouble("heightFraction", 0.0).toFloat(),
                    opacity = obj.optDouble("opacity", 0.92).toFloat().coerceIn(0.20f, 1f),
                    isLocked = obj.optBoolean("isLocked", false),
                    zIndex = obj.optInt("zIndex", obj.optInt("order", i))
                )
                list.add(WidgetItem.withDefaultGeometry(base))
            }
            if (list.isEmpty()) WidgetItem.createDefaultList() else list
        } catch (e: Exception) {
            Log.e(TAG, "Error reading widgets", e)
            WidgetItem.createDefaultList()
        }
    }

    fun saveWidgets(widgets: List<WidgetItem>) {
        try {
            val array = JSONArray()
            widgets.forEach { item ->
                array.put(JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type.name)
                    put("style", item.style.name)
                    put("spanX", item.spanX)
                    put("spanY", item.spanY)
                    put("isVisible", item.isVisible)
                    put("order", item.order)
                    put("xFraction", item.xFraction.toDouble())
                    put("yFraction", item.yFraction.toDouble())
                    put("widthFraction", item.widthFraction.toDouble())
                    put("heightFraction", item.heightFraction.toDouble())
                    put("opacity", item.opacity.toDouble())
                    put("isLocked", item.isLocked)
                    put("zIndex", item.zIndex)
                })
            }
            prefs.edit().putString("widgets_json", array.toString()).apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving widgets", e) }
    }
    fun resetToDefaultWidgets() = saveWidgets(WidgetItem.createDefaultList())

    fun getFavorites(): Set<String> = prefs.getStringSet("favorite_apps", emptySet()) ?: emptySet()
    fun saveFavorites(favorites: Set<String>) = prefs.edit().putStringSet("favorite_apps", favorites).apply()
    fun getHiddenApps(): Set<String> = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
    fun saveHiddenApps(hidden: Set<String>) = prefs.edit().putStringSet("hidden_apps", hidden).apply()

    fun getLastMusicPath(): String? = prefs.getString("last_music_path", null)
    fun getLastMusicPosition(): Long = prefs.getLong("last_music_position", 0L)
    fun saveMusicResumeState(path: String?, positionMs: Long) {
        prefs.edit().putString("last_music_path", path).putLong("last_music_position", positionMs.coerceAtLeast(0L)).apply()
    }

    fun getTripData(): TripData = try {
        TripData(0f, prefs.getFloat("trip_max_speed", 0f), prefs.getFloat("trip_avg_speed", 0f), prefs.getFloat("trip_distance", 0f), prefs.getLong("trip_moving_time", 0L), prefs.getLong("trip_stop_time", 0L), false, false)
    } catch (_: Exception) { TripData() }
    fun saveTripData(data: TripData) { prefs.edit().putFloat("trip_max_speed", data.maxSpeedKmH).putFloat("trip_avg_speed", data.averageSpeedKmH).putFloat("trip_distance", data.distanceKm).putLong("trip_moving_time", data.elapsedMovingTimeSec).putLong("trip_stop_time", data.elapsedStopTimeSec).apply() }

    fun getSavedMaps(): List<MapItem> {
        val raw = prefs.getString("maps_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                MapItem(o.getString("id"), o.getString("name"), o.getString("filePath"), o.optString("fileSizeFormatted", "--"), o.optBoolean("isActive", false), o.optString("dateAdded", ""))
            }
        } catch (_: Exception) { emptyList() }
    }
    fun saveMaps(maps: List<MapItem>) {
        try {
            val array = JSONArray()
            maps.forEach { item -> array.put(JSONObject().apply { put("id", item.id); put("name", item.name); put("filePath", item.filePath); put("fileSizeFormatted", item.fileSizeFormatted); put("isActive", item.isActive); put("dateAdded", item.dateAdded) }) }
            prefs.edit().putString("maps_json", array.toString()).apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving maps", e) }
    }

    companion object { private const val TAG = "PreferencesManager" }
}
