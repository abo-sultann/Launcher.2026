package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("car_launcher_preferences_2026", Context.MODE_PRIVATE)

    init {
        LauncherDataMigrator(prefs).migrateIfNeeded()
    }

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
        val dockStyle = try {
            DockSurfaceStyle.valueOf(prefs.getString("bottom_dock_style", DockSurfaceStyle.GLASS.name) ?: DockSurfaceStyle.GLASS.name)
        } catch (_: Exception) { DockSurfaceStyle.GLASS }
        val interfaceAccent = try {
            InterfaceAccent.valueOf(prefs.getString("interface_accent", InterfaceAccent.CYAN.name) ?: InterfaceAccent.CYAN.name)
        } catch (_: Exception) { InterfaceAccent.CYAN }
        val screenSaverTypes = (prefs.getStringSet("screensaver_widgets", null)
            ?: setOf(WidgetType.CLOCK.name, WidgetType.SPEEDOMETER.name))
            .mapNotNull { name -> try { WidgetType.valueOf(name) } catch (_: Exception) { null } }
            .filter { it in SCREEN_SAVER_DISPLAY_WIDGET_TYPES }
            .toSet()
            .ifEmpty { setOf(WidgetType.CLOCK) }
        val is24 = prefs.getBoolean("clock_24h", true)

        LauncherSettings(
            safeArea = getSafeArea(),
            backgroundType = bgType,
            customWallpaperPath = prefs.getString("custom_wallpaper_path", null),
            wallpaperDimPercent = prefs.getInt("wallpaper_dim", 10).coerceIn(0, 80),
            iconSizeDp = prefs.getInt("icon_size", 64),
            showAppLabels = if (prefs.contains("show_app_labels")) prefs.getBoolean("show_app_labels", true) else prefs.getBoolean("show_app_names", true),
            appDrawerColumns = prefs.getInt("app_columns", 5),
            homeGridColumns = prefs.getInt("home_columns", 4),
            widgetHeightDp = prefs.getInt("widget_height", 138),
            gridHorizontalGapDp = prefs.getInt("grid_h_gap", 8),
            gridVerticalGapDp = prefs.getInt("grid_v_gap", 8),
            is24HourFormat = is24,
            showSeconds = prefs.getBoolean("clock_seconds", false),
            speedUnit = prefs.getString("speed_unit", "كم/س") ?: "كم/س",
            autoStartOnBoot = prefs.getBoolean("auto_start", true),
            resumeMusicPlayback = prefs.getBoolean("resume_music", true),
            safeModeActive = prefs.getBoolean("safe_mode", false),
            showTopBar = prefs.getBoolean("show_top_bar", true),
            showBottomBar = prefs.getBoolean("show_bottom_bar", true),
            bottomDockStyle = dockStyle,
            bottomDockOpacityPercent = prefs.getInt("bottom_dock_opacity", 76).coerceIn(30, 100),
            interfaceAccent = interfaceAccent,
            highContrastMode = prefs.getBoolean("high_contrast", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            autoLogTrips = prefs.getBoolean("auto_log_trips", true),
            childUnlockHoldSeconds = prefs.getInt("child_unlock_hold", 4).coerceIn(2, 6),
            screenSaverEnabled = prefs.getBoolean("screensaver_enabled", false),
            screenSaverTimeoutSeconds = prefs.getInt("screensaver_timeout", 120).coerceIn(30, 1800),
            screenSaverUseWallpaper = prefs.getBoolean("screensaver_wallpaper", true),
            screenSaverWidgetTypes = screenSaverTypes
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
                .putString("bottom_dock_style", settings.bottomDockStyle.name)
                .putInt("bottom_dock_opacity", settings.bottomDockOpacityPercent.coerceIn(30, 100))
                .putString("interface_accent", settings.interfaceAccent.name)
                .putBoolean("high_contrast", settings.highContrastMode)
                .putBoolean("keep_screen_on", settings.keepScreenOn)
                .putBoolean("auto_log_trips", settings.autoLogTrips)
                .putInt("child_unlock_hold", settings.childUnlockHoldSeconds.coerceIn(2, 6))
                .putBoolean("screensaver_enabled", settings.screenSaverEnabled)
                .putInt("screensaver_timeout", settings.screenSaverTimeoutSeconds.coerceIn(30, 1800))
                .putBoolean("screensaver_wallpaper", settings.screenSaverUseWallpaper)
                .putStringSet("screensaver_widgets", settings.screenSaverWidgetTypes.map { it.name }.toSet())
                .apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving settings", e) }
    }

    fun getWidgets(): List<WidgetItem> {
        val raw = prefs.getString("widgets_json", null) ?: return WidgetItem.createDefaultList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<WidgetItem>()
            for (i in 0 until minOf(array.length(), MAX_WIDGETS)) {
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
                        WidgetType.MAINTENANCE -> WidgetStyle.MAINTENANCE_VERTICAL
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
                    ,surfaceStyle = try { WidgetSurfaceStyle.valueOf(obj.optString("surfaceStyle", WidgetItem.defaultSurfaceFor(type).name)) } catch (_: Exception) { WidgetItem.defaultSurfaceFor(type) }
                    ,showBorder = obj.optBoolean("showBorder", false)
                    ,foregroundColorArgb = if (obj.has("foregroundColorArgb") && !obj.isNull("foregroundColorArgb")) obj.optInt("foregroundColorArgb") else null
                    ,accentColorArgb = if (obj.has("accentColorArgb") && !obj.isNull("accentColorArgb")) obj.optInt("accentColorArgb") else null
                    ,surfaceOpacity = obj.optDouble("surfaceOpacity", 1.0).toFloat().coerceIn(.25f, 1f)
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
                    put("surfaceStyle", item.surfaceStyle.name)
                    put("showBorder", item.showBorder)
                    if (item.foregroundColorArgb != null) put("foregroundColorArgb", item.foregroundColorArgb)
                    if (item.accentColorArgb != null) put("accentColorArgb", item.accentColorArgb)
                    put("surfaceOpacity", item.surfaceOpacity.toDouble())
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
        TripData(
            currentSpeedKmH = 0f,
            maxSpeedKmH = prefs.getFloat("trip_max_speed", 0f),
            averageSpeedKmH = prefs.getFloat("trip_avg_speed", 0f),
            distanceKm = prefs.getFloat("trip_distance", 0f),
            elapsedMovingTimeSec = prefs.getLong("trip_moving_time", 0L),
            elapsedStopTimeSec = prefs.getLong("trip_stop_time", 0L),
            isRunning = prefs.getBoolean("trip_running", false),
            isPaused = prefs.getBoolean("trip_paused", false),
            startTimeStamp = prefs.getLong("trip_start_time", 0L),
            lastUpdateTimestamp = prefs.getLong("trip_last_update", 0L),
            startLatitude = java.lang.Double.longBitsToDouble(prefs.getLong("trip_start_lat", 0L)),
            startLongitude = java.lang.Double.longBitsToDouble(prefs.getLong("trip_start_lon", 0L)),
            lastLatitude = java.lang.Double.longBitsToDouble(prefs.getLong("trip_last_lat", 0L)),
            lastLongitude = java.lang.Double.longBitsToDouble(prefs.getLong("trip_last_lon", 0L)),
            validGpsSamples = prefs.getInt("trip_valid_samples", 0),
            placesSavedCount = prefs.getInt("trip_places_saved", 0)
        )
    } catch (_: Exception) { TripData() }

    fun saveTripData(data: TripData) {
        prefs.edit()
            .putFloat("trip_max_speed", data.maxSpeedKmH)
            .putFloat("trip_avg_speed", data.averageSpeedKmH)
            .putFloat("trip_distance", data.distanceKm)
            .putLong("trip_moving_time", data.elapsedMovingTimeSec)
            .putLong("trip_stop_time", data.elapsedStopTimeSec)
            .putBoolean("trip_running", data.isRunning)
            .putBoolean("trip_paused", data.isPaused)
            .putLong("trip_start_time", data.startTimeStamp)
            .putLong("trip_last_update", data.lastUpdateTimestamp)
            .putLong("trip_start_lat", java.lang.Double.doubleToRawLongBits(data.startLatitude))
            .putLong("trip_start_lon", java.lang.Double.doubleToRawLongBits(data.startLongitude))
            .putLong("trip_last_lat", java.lang.Double.doubleToRawLongBits(data.lastLatitude))
            .putLong("trip_last_lon", java.lang.Double.doubleToRawLongBits(data.lastLongitude))
            .putInt("trip_valid_samples", data.validGpsSamples)
            .putInt("trip_places_saved", data.placesSavedCount)
            .apply()
    }

    fun getSavedTrips(): List<SavedTrip> {
        val raw = prefs.getString("saved_trips_json", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            List(minOf(array.length(), MAX_SAVED_TRIPS)) { i ->
                val o = array.getJSONObject(i)
                SavedTrip(
                    id = o.getString("id"),
                    name = o.optString("name", "رحلة محفوظة"),
                    startTimeStamp = o.optLong("start", 0L),
                    endTimeStamp = o.optLong("end", 0L),
                    distanceKm = o.optDouble("distance", 0.0).toFloat(),
                    movingTimeSec = o.optLong("moving", 0L),
                    stopTimeSec = o.optLong("stopped", 0L),
                    maxSpeedKmH = o.optDouble("max", 0.0).toFloat(),
                    averageSpeedKmH = o.optDouble("avg", 0.0).toFloat(),
                    startLatitude = o.optDouble("startLat", 0.0),
                    startLongitude = o.optDouble("startLon", 0.0),
                    endLatitude = o.optDouble("endLat", 0.0),
                    endLongitude = o.optDouble("endLon", 0.0),
                    placesSavedCount = o.optInt("places", 0)
                )
            }.sortedByDescending { it.startTimeStamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading saved trips", e)
            emptyList()
        }
    }

    fun saveSavedTrips(trips: List<SavedTrip>) {
        try {
            val array = JSONArray()
            trips.take(100).forEach { trip ->
                array.put(JSONObject().apply {
                    put("id", trip.id); put("name", trip.name); put("start", trip.startTimeStamp); put("end", trip.endTimeStamp)
                    put("distance", trip.distanceKm.toDouble()); put("moving", trip.movingTimeSec); put("stopped", trip.stopTimeSec)
                    put("max", trip.maxSpeedKmH.toDouble()); put("avg", trip.averageSpeedKmH.toDouble())
                    put("startLat", trip.startLatitude); put("startLon", trip.startLongitude); put("endLat", trip.endLatitude); put("endLon", trip.endLongitude)
                    put("places", trip.placesSavedCount)
                })
            }
            prefs.edit().putString("saved_trips_json", array.toString()).apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving trip history", e) }
    }

    fun getSavedMaps(): List<MapItem> {
        val raw = prefs.getString("maps_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(minOf(array.length(), MAX_SAVED_MAPS)) { i ->
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

    companion object {
        private const val TAG = "PreferencesManager"
        private const val MAX_WIDGETS = 64
        private const val MAX_SAVED_TRIPS = 100
        private const val MAX_SAVED_MAPS = 32
    }
}
