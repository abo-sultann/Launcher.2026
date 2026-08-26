package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("car_launcher_preferences_2026", Context.MODE_PRIVATE)

    // Safe Area
    fun getSafeArea(): SafeAreaConfig {
        return try {
            SafeAreaConfig(
                topDp = prefs.getInt("safe_top", 8),
                bottomDp = prefs.getInt("safe_bottom", 8),
                leftDp = prefs.getInt("safe_left", 8),
                rightDp = prefs.getInt("safe_right", 8)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading safe area, fallback to default", e)
            SafeAreaConfig.DEFAULT
        }
    }

    fun getSafeAreaConfig(): SafeAreaConfig = getSafeArea()

    fun saveSafeArea(config: SafeAreaConfig) {
        try {
            prefs.edit()
                .putInt("safe_top", config.topDp)
                .putInt("safe_bottom", config.bottomDp)
                .putInt("safe_left", config.leftDp)
                .putInt("safe_right", config.rightDp)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving safe area", e)
        }
    }

    fun saveSafeAreaConfig(config: SafeAreaConfig) = saveSafeArea(config)

    // Settings
    fun getSettings(): LauncherSettings {
        return try {
            val bgName = prefs.getString("bg_type", BackgroundType.DARK_CARBON.name) ?: BackgroundType.DARK_CARBON.name
            val bgType = try { BackgroundType.valueOf(bgName) } catch (e: Exception) { BackgroundType.DARK_CARBON }
            LauncherSettings(
                safeArea = getSafeArea(),
                backgroundType = bgType,
                customWallpaperPath = prefs.getString("custom_wallpaper_path", null),
                iconSizeDp = prefs.getInt("icon_size", 64),
                showAppNames = prefs.getBoolean("show_app_names", true),
                appDrawerColumns = prefs.getInt("app_columns", 5),
                is24HourClock = prefs.getBoolean("clock_24h", true),
                showSeconds = prefs.getBoolean("clock_seconds", false),
                speedUnit = prefs.getString("speed_unit", "كم/س") ?: "كم/س",
                autoStartEnabled = prefs.getBoolean("auto_start", true),
                resumeMusicPlayback = prefs.getBoolean("resume_music", true),
                safeModeActive = prefs.getBoolean("safe_mode", false),
                showTopBar = prefs.getBoolean("show_top_bar", true),
                showBottomBar = prefs.getBoolean("show_bottom_bar", true)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading settings, fallback to defaults", e)
            LauncherSettings()
        }
    }

    fun saveSettings(settings: LauncherSettings) {
        try {
            saveSafeArea(settings.safeArea)
            prefs.edit()
                .putString("bg_type", settings.backgroundType.name)
                .putString("custom_wallpaper_path", settings.customWallpaperPath)
                .putInt("icon_size", settings.iconSizeDp)
                .putBoolean("show_app_names", settings.showAppNames)
                .putInt("app_columns", settings.appDrawerColumns)
                .putBoolean("clock_24h", settings.is24HourClock)
                .putBoolean("clock_seconds", settings.showSeconds)
                .putString("speed_unit", settings.speedUnit)
                .putBoolean("auto_start", settings.autoStartEnabled)
                .putBoolean("resume_music", settings.resumeMusicPlayback)
                .putBoolean("safe_mode", settings.safeModeActive)
                .putBoolean("show_top_bar", settings.showTopBar)
                .putBoolean("show_bottom_bar", settings.showBottomBar)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving settings", e)
        }
    }

    // Widgets list persistence
    fun getWidgets(): List<WidgetItem> {
        val raw = prefs.getString("widgets_json", null)
        if (raw.isNullOrBlank()) {
            return WidgetItem.createDefaultList()
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<WidgetItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val typeStr = obj.getString("type")
                val styleStr = obj.getString("style")
                val spanX = obj.optInt("spanX", 1)
                val spanY = obj.optInt("spanY", 1)
                val isVisible = obj.optBoolean("isVisible", true)
                val order = obj.optInt("order", i)

                val type = try { WidgetType.valueOf(typeStr) } catch (e: Exception) { WidgetType.CLOCK }
                val style = try { WidgetStyle.valueOf(styleStr) } catch (e: Exception) {
                    type.let {
                        when (it) {
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
                }
                list.add(WidgetItem(id, type, style, spanX, spanY, isVisible, order))
            }
            if (list.isEmpty()) WidgetItem.createDefaultList() else list
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing widgets, restoring default list", e)
            WidgetItem.createDefaultList()
        }
    }

    fun saveWidgets(widgets: List<WidgetItem>) {
        try {
            val array = JSONArray()
            widgets.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type.name)
                    put("style", item.style.name)
                    put("spanX", item.spanX)
                    put("spanY", item.spanY)
                    put("isVisible", item.isVisible)
                    put("order", item.order)
                }
                array.put(obj)
            }
            prefs.edit().putString("widgets_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving widgets", e)
        }
    }

    fun resetToDefaultWidgets() {
        saveWidgets(WidgetItem.createDefaultList())
    }

    // App Favorites & Custom Order
    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorite_apps", emptySet()) ?: emptySet()
    }

    fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putStringSet("favorite_apps", favorites).apply()
    }

    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
    }

    fun saveHiddenApps(hidden: Set<String>) {
        prefs.edit().putStringSet("hidden_apps", hidden).apply()
    }

    // Music Resume State
    fun getLastMusicPath(): String? {
        return prefs.getString("last_music_path", null)
    }

    fun getLastMusicPosition(): Long {
        return prefs.getLong("last_music_position", 0L)
    }

    fun saveMusicResumeState(path: String?, positionMs: Long) {
        prefs.edit()
            .putString("last_music_path", path)
            .putLong("last_music_position", positionMs)
            .apply()
    }

    // Trip Persistence
    fun getTripData(): TripData {
        return try {
            TripData(
                currentSpeedKmH = 0f,
                maxSpeedKmH = prefs.getFloat("trip_max_speed", 0f),
                averageSpeedKmH = prefs.getFloat("trip_avg_speed", 0f),
                distanceKm = prefs.getFloat("trip_distance", 0f),
                elapsedMovingTimeSec = prefs.getLong("trip_moving_time", 0L),
                elapsedStopTimeSec = prefs.getLong("trip_stop_time", 0L),
                isRunning = false,
                isPaused = false
            )
        } catch (e: Exception) {
            TripData()
        }
    }

    fun saveTripData(data: TripData) {
        try {
            prefs.edit()
                .putFloat("trip_max_speed", data.maxSpeedKmH)
                .putFloat("trip_avg_speed", data.averageSpeedKmH)
                .putFloat("trip_distance", data.distanceKm)
                .putLong("trip_moving_time", data.elapsedMovingTimeSec)
                .putLong("trip_stop_time", data.elapsedStopTimeSec)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving trip data", e)
        }
    }

    // Maps Management
    fun getSavedMaps(): List<MapItem> {
        val raw = prefs.getString("maps_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<MapItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MapItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        filePath = obj.getString("filePath"),
                        fileSizeFormatted = obj.optString("fileSizeFormatted", "--"),
                        isActive = obj.optBoolean("isActive", false),
                        dateAdded = obj.optString("dateAdded", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMaps(maps: List<MapItem>) {
        try {
            val array = JSONArray()
            maps.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("filePath", item.filePath)
                    put("fileSizeFormatted", item.fileSizeFormatted)
                    put("isActive", item.isActive)
                    put("dateAdded", item.dateAdded)
                }
                array.put(obj)
            }
            prefs.edit().putString("maps_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving maps", e)
        }
    }

    companion object {
        private const val TAG = "PreferencesManager"
    }
}
