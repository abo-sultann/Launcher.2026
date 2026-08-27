package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import com.example.ui.components.CarScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)
    private val appRepository = AppRepository(application, preferencesManager)
    private val musicPlayerService = MusicPlayerService(application, preferencesManager)
    private val gpsTelemetryManager = GpsTelemetryManager(application)
    private val tripComputer = TripComputer(preferencesManager)
    private val offlineMapEngine = OfflineMapEngine(application, preferencesManager)
    private val diagnosticManager = DiagnosticManager(application, preferencesManager)

    private val _currentScreen = MutableStateFlow(CarScreen.HOME)
    val currentScreen: StateFlow<CarScreen> = _currentScreen.asStateFlow()
    private val _safeArea = MutableStateFlow(preferencesManager.getSafeArea())
    val safeArea: StateFlow<SafeAreaConfig> = _safeArea.asStateFlow()
    private val _settings = MutableStateFlow(preferencesManager.getSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()
    private val _widgets = MutableStateFlow<List<WidgetItem>>(emptyList())
    val widgets: StateFlow<List<WidgetItem>> = _widgets.asStateFlow()
    private val _screenSaverLayouts = MutableStateFlow<List<ScreenSaverWidgetLayout>>(emptyList())
    val screenSaverLayouts: StateFlow<List<ScreenSaverWidgetLayout>> = _screenSaverLayouts.asStateFlow()
    private val _isDesignMode = MutableStateFlow(false)
    val isDesignMode: StateFlow<Boolean> = _isDesignMode.asStateFlow()
    private val _isChildLockActive = MutableStateFlow(false)
    val isChildLockActive: StateFlow<Boolean> = _isChildLockActive.asStateFlow()
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()
    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()
    private val _isSafeModeActive = MutableStateFlow(false)
    val isSafeModeActive: StateFlow<Boolean> = _isSafeModeActive.asStateFlow()

    val playbackState: StateFlow<MusicPlaybackState> = musicPlayerService.playbackState
    val gpsTelemetry: StateFlow<GpsTelemetry> = gpsTelemetryManager.telemetry
    val tripData: StateFlow<TripData> = tripComputer.tripData
    val mapsList: StateFlow<List<MapItem>> = offlineMapEngine.mapsList
    val activeMap: StateFlow<MapItem?> = offlineMapEngine.activeMap
    val mapError: StateFlow<String?> = offlineMapEngine.mapError

    init {
        checkSafeMode()
        loadWidgets()
        loadScreenSaverLayouts()
        viewModelScope.launch(Dispatchers.IO) { loadApps() }
        viewModelScope.launch(Dispatchers.IO) { musicPlayerService.initialize() }
        viewModelScope.launch(Dispatchers.IO) { offlineMapEngine.initialize() }
        viewModelScope.launch(Dispatchers.Main) { gpsTelemetryManager.startGpsUpdates() }
        viewModelScope.launch {
            gpsTelemetry.collect { telemetry ->
                val speed = if (telemetry.hasGpsFix) telemetry.speedKmH else 0f
                tripComputer.updateSpeed(speed)
                if (_settings.value.autoLogTrips && telemetry.hasGpsFix && speed >= 3f && !tripComputer.tripData.value.isRunning) {
                    tripComputer.startTrip()
                }
            }
        }
    }

    private fun checkSafeMode() {
        val p = getApplication<Application>().getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
        _isSafeModeActive.value = p.getInt("crash_count", 0) >= 2
    }

    fun navigateTo(screen: CarScreen) {
        if (_currentScreen.value != screen) _currentScreen.value = screen
    }

    fun restartGps() = gpsTelemetryManager.restartGpsUpdates()

    private fun loadWidgets() { _widgets.value = preferencesManager.getWidgets() }
    fun toggleDesignMode() { _isDesignMode.value = !_isDesignMode.value }

    fun addWidget(type: WidgetType, style: WidgetStyle) {
        val spanX = if (type == WidgetType.CONTROLS || type == WidgetType.APPS || style == WidgetStyle.MUSIC_LARGE_AUTOMOTIVE) 2 else 1
        val order = _widgets.value.size
        val item = WidgetItem.createForOrder(UUID.randomUUID().toString(), type, style, order, spanX)
        val updated = _widgets.value + item
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun updateWidgetStyle(widgetId: String, newStyle: WidgetStyle) = updateAndSaveWidgets { item ->
        if (item.id == widgetId) item.copy(style = newStyle) else item
    }

    fun toggleWidgetSpan(widgetId: String) = updateAndSaveWidgets { item ->
        if (item.id == widgetId) {
            val wider = item.widthFraction < 0.45f
            item.copy(
                spanX = if (wider) 2 else 1,
                widthFraction = if (wider) 0.48f else 0.235f,
                xFraction = item.xFraction.coerceIn(0f, if (wider) 0.52f else 0.765f)
            )
        } else item
    }

    fun moveWidget(widgetId: String, forward: Boolean) {
        val list = _widgets.value.toMutableList()
        val i = list.indexOfFirst { it.id == widgetId }
        if (i < 0) return
        val target = if (forward) i + 1 else i - 1
        if (target !in list.indices) return
        val item = list.removeAt(i)
        list.add(target, item)
        val updated = list.mapIndexed { idx, w -> w.copy(order = idx, zIndex = idx) }
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun previewWidgetMove(widgetId: String, dxFraction: Float, dyFraction: Float) {
        _widgets.value = _widgets.value.map { item ->
            if (item.id != widgetId || item.isLocked) item else {
                val width = item.widthFraction.coerceIn(0.12f, 1f)
                val height = item.heightFraction.coerceIn(0.14f, 1f)
                item.copy(
                    xFraction = (item.xFraction + dxFraction).coerceIn(0f, (1f - width).coerceAtLeast(0f)),
                    yFraction = (item.yFraction + dyFraction).coerceIn(0f, (1f - height).coerceAtLeast(0f))
                )
            }
        }
    }

    fun previewWidgetResize(widgetId: String, dwFraction: Float, dhFraction: Float) {
        _widgets.value = _widgets.value.map { item ->
            if (item.id != widgetId || item.isLocked) item else {
                val maxWidth = (1f - item.xFraction).coerceAtLeast(0.12f)
                val maxHeight = (1f - item.yFraction).coerceAtLeast(0.14f)
                item.copy(
                    widthFraction = (item.widthFraction + dwFraction).coerceIn(0.12f, maxWidth),
                    heightFraction = (item.heightFraction + dhFraction).coerceIn(0.14f, maxHeight)
                )
            }
        }
    }

    fun commitWidgetLayout() = preferencesManager.saveWidgets(_widgets.value)

    fun setWidgetOpacity(widgetId: String, opacity: Float) = updateAndSaveWidgets { item ->
        if (item.id == widgetId) item.copy(opacity = opacity.coerceIn(0.20f, 1f)) else item
    }

    fun toggleWidgetLock(widgetId: String) = updateAndSaveWidgets { item ->
        if (item.id == widgetId) item.copy(isLocked = !item.isLocked) else item
    }

    fun bringWidgetToFront(widgetId: String) {
        val next = (_widgets.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        _widgets.value = _widgets.value.map { if (it.id == widgetId) it.copy(zIndex = next) else it }
    }

    private inline fun updateAndSaveWidgets(transform: (WidgetItem) -> WidgetItem) {
        val updated = _widgets.value.map(transform)
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun removeWidget(widgetId: String) {
        val updated = _widgets.value.filterNot { it.id == widgetId }.mapIndexed { i, w -> w.copy(order = i) }
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun resetWidgetsToDefault() { preferencesManager.resetToDefaultWidgets(); loadWidgets() }

    fun activateChildLock() {
        _isDesignMode.value = false
        _isChildLockActive.value = true
    }
    fun deactivateChildLock() { _isChildLockActive.value = false }

    fun updateSafeArea(top: Int, bottom: Int, left: Int, right: Int) {
        val config = SafeAreaConfig(top.coerceIn(0, 250), bottom.coerceIn(0, 150), left.coerceIn(0, 150), right.coerceIn(0, 150))
        _safeArea.value = config
        preferencesManager.saveSafeArea(config)
    }
    fun resetSafeArea() { updateSafeArea(0, 0, 0, 0) }
    fun updateSettings(newSettings: LauncherSettings) { _settings.value = newSettings; preferencesManager.saveSettings(newSettings) }

    fun toggleScreenSaverWidget(type: WidgetType) {
        val current = _settings.value.screenSaverWidgetTypes.toMutableSet()
        if (type in current) current.remove(type) else if (current.size < 4) current.add(type)
        val selected = current.ifEmpty { mutableSetOf(WidgetType.CLOCK) }.toSet()
        updateSettings(_settings.value.copy(screenSaverWidgetTypes = selected))
        loadScreenSaverLayouts()
    }

    private fun screenSaverPrefs() = getApplication<Application>().getSharedPreferences("car_launcher_preferences_2026", Context.MODE_PRIVATE)

    private fun loadScreenSaverLayouts() {
        val saved = mutableListOf<ScreenSaverWidgetLayout>()
        try {
            val raw = screenSaverPrefs().getString("screensaver_layouts_json", null)
            if (!raw.isNullOrBlank()) {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val type = try { WidgetType.valueOf(o.getString("type")) } catch (_: Exception) { continue }
                    saved += ScreenSaverWidgetLayout(
                        type = type,
                        xFraction = o.optDouble("x", 0.05).toFloat(),
                        yFraction = o.optDouble("y", 0.12).toFloat(),
                        widthFraction = o.optDouble("w", 0.42).toFloat(),
                        heightFraction = o.optDouble("h", 0.34).toFloat(),
                        opacity = o.optDouble("opacity", 0.90).toFloat().coerceIn(0.25f, 1f),
                        zIndex = o.optInt("z", i)
                    )
                }
            }
        } catch (_: Exception) { }

        val orderedTypes = WidgetType.values().filter { it in _settings.value.screenSaverWidgetTypes }.take(4)
        val merged = orderedTypes.mapIndexed { index, type ->
            val existing = saved.firstOrNull { it.type == type }
            existing ?: ScreenSaverWidgetLayout.defaultFor(type, index)
        }
        _screenSaverLayouts.value = merged
        saveScreenSaverLayouts()
    }

    private fun saveScreenSaverLayouts() {
        try {
            val array = JSONArray()
            _screenSaverLayouts.value.forEach { item ->
                array.put(JSONObject().apply {
                    put("type", item.type.name)
                    put("x", item.xFraction.toDouble())
                    put("y", item.yFraction.toDouble())
                    put("w", item.widthFraction.toDouble())
                    put("h", item.heightFraction.toDouble())
                    put("opacity", item.opacity.toDouble())
                    put("z", item.zIndex)
                })
            }
            screenSaverPrefs().edit().putString("screensaver_layouts_json", array.toString()).apply()
        } catch (_: Exception) { }
    }

    fun previewScreenSaverMove(type: WidgetType, dxFraction: Float, dyFraction: Float) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { item ->
            if (item.type != type) item else item.copy(
                xFraction = (item.xFraction + dxFraction).coerceIn(0f, (1f - item.widthFraction).coerceAtLeast(0f)),
                yFraction = (item.yFraction + dyFraction).coerceIn(0f, (1f - item.heightFraction).coerceAtLeast(0f))
            )
        }
    }

    fun previewScreenSaverResize(type: WidgetType, dwFraction: Float, dhFraction: Float) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { item ->
            if (item.type != type) item else {
                val maxW = (1f - item.xFraction).coerceAtLeast(0.16f)
                val maxH = (1f - item.yFraction).coerceAtLeast(0.16f)
                item.copy(
                    widthFraction = (item.widthFraction + dwFraction).coerceIn(0.16f, maxW),
                    heightFraction = (item.heightFraction + dhFraction).coerceIn(0.16f, maxH)
                )
            }
        }
    }

    fun setScreenSaverOpacity(type: WidgetType, opacity: Float) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map {
            if (it.type == type) it.copy(opacity = opacity.coerceIn(0.25f, 1f)) else it
        }
        saveScreenSaverLayouts()
    }

    fun bringScreenSaverWidgetToFront(type: WidgetType) {
        val next = (_screenSaverLayouts.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { if (it.type == type) it.copy(zIndex = next) else it }
    }

    fun commitScreenSaverLayout() = saveScreenSaverLayouts()

    fun resetScreenSaverLayout() {
        val ordered = WidgetType.values().filter { it in _settings.value.screenSaverWidgetTypes }.take(4)
        _screenSaverLayouts.value = ordered.mapIndexed { index, type -> ScreenSaverWidgetLayout.defaultFor(type, index) }
        saveScreenSaverLayouts()
    }

    fun importWallpaperUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val originalName = queryDisplayName(uri) ?: "wallpaper.jpg"
                val extension = originalName.substringAfterLast('.', "jpg").take(5)
                val dir = File(getApplication<Application>().filesDir, "wallpapers").apply { mkdirs() }
                val target = File(dir, "launcher_wallpaper.$extension")
                resolver.openInputStream(uri)?.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                if (target.exists() && target.length() > 0) {
                    val newSettings = _settings.value.copy(
                        backgroundType = BackgroundType.CUSTOM_IMAGE,
                        customWallpaperPath = target.absolutePath
                    )
                    _settings.value = newSettings
                    preferencesManager.saveSettings(newSettings)
                }
            } catch (_: Exception) { }
        }
    }

    fun loadApps() {
        val apps = appRepository.getInstalledApps()
        _installedApps.value = apps
    }
    fun toggleAppFavorite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) { appRepository.toggleFavorite(packageName); loadApps() }
    }
    fun toggleAppHidden(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) { appRepository.toggleHidden(packageName); loadApps() }
    }
    fun launchApp(packageName: String) { appRepository.launchApp(packageName) }
    fun launchAndroidSettings() { appRepository.launchAndroidSettings() }

    fun togglePlayPause() = musicPlayerService.togglePlayPause()
    fun playNext() = musicPlayerService.playNext()
    fun playPrevious() = musicPlayerService.playPrevious()
    fun seekTo(positionMs: Long) = musicPlayerService.seekTo(positionMs)
    fun skipForward10Sec() = musicPlayerService.skipForward10Sec()
    fun skipBackward10Sec() = musicPlayerService.skipBackward10Sec()
    fun adjustVolume(delta: Float) = musicPlayerService.setVolume(delta)
    fun toggleMute() = musicPlayerService.toggleMute()
    fun playTrack(track: MusicTrack) = musicPlayerService.playTrack(track)

    fun importMusicUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val name = queryDisplayName(uri) ?: "music_${System.currentTimeMillis()}.mp3"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                        put(MediaStore.Audio.Media.MIME_TYPE, resolver.getType(uri) ?: "audio/mpeg")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Launcher 2026")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val outUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    if (outUri != null) {
                        resolver.openInputStream(uri)?.use { input -> resolver.openOutputStream(outUri)?.use { output -> input.copyTo(output) } }
                        values.clear()
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(outUri, values, null, null)
                    }
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).resolve("Launcher 2026")
                    if (!dir.exists()) dir.mkdirs()
                    val target = File(dir, name)
                    resolver.openInputStream(uri)?.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                    MediaScannerConnection.scanFile(getApplication(), arrayOf(target.absolutePath), arrayOf(resolver.getType(uri) ?: "audio/mpeg"), null)
                }
                musicPlayerService.initialize()
            } catch (_: Exception) { }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) { null }

    fun startTrip() = tripComputer.startTrip()
    fun pauseTrip() = tripComputer.pauseTrip()
    fun resetTrip() = tripComputer.resetTrip()

    fun importMapFile(file: File, name: String? = null) = offlineMapEngine.importMapFile(file, name)
    fun importMapUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val name = queryDisplayName(uri) ?: "map_${System.currentTimeMillis()}.mbtiles"
                val dir = File(getApplication<Application>().filesDir, "maps").apply { mkdirs() }
                val target = File(dir, name)
                resolver.openInputStream(uri)?.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
                if (target.exists() && target.length() > 0) offlineMapEngine.importMapFile(target, target.nameWithoutExtension)
            } catch (_: Exception) { }
        }
    }
    fun setActiveMap(mapId: String) = offlineMapEngine.setActiveMap(mapId)
    fun renameMap(mapId: String, newName: String) = offlineMapEngine.renameMap(mapId, newName)
    fun deleteMap(mapId: String) = offlineMapEngine.deleteMap(mapId)

    fun runDiagnostics() { _diagnosticReport.value = diagnosticManager.runFullDiagnostics() }
    fun resetSafeMode() { diagnosticManager.resetCrashCount(); _isSafeModeActive.value = false; runDiagnostics() }

    override fun onCleared() {
        gpsTelemetryManager.stopGpsUpdates()
        musicPlayerService.release()
        tripComputer.release()
        super.onCleared()
    }
}
