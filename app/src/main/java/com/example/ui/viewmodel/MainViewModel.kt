package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
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
    private val _isDesignMode = MutableStateFlow(false)
    val isDesignMode: StateFlow<Boolean> = _isDesignMode.asStateFlow()
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
        val item = WidgetItem(UUID.randomUUID().toString(), type, style,
            if (type == WidgetType.CONTROLS || type == WidgetType.APPS || style == WidgetStyle.MUSIC_LARGE_AUTOMOTIVE) 2 else 1,
            1, true, _widgets.value.size)
        val updated = _widgets.value + item
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }
    fun updateWidgetStyle(widgetId: String, newStyle: WidgetStyle) {
        val updated = _widgets.value.map { if (it.id == widgetId) it.copy(style = newStyle) else it }
        _widgets.value = updated; preferencesManager.saveWidgets(updated)
    }
    fun toggleWidgetSpan(widgetId: String) {
        val updated = _widgets.value.map { if (it.id == widgetId) it.copy(spanX = if (it.spanX == 1) 2 else 1) else it }
        _widgets.value = updated; preferencesManager.saveWidgets(updated)
    }
    fun moveWidget(widgetId: String, forward: Boolean) {
        val list = _widgets.value.toMutableList(); val i = list.indexOfFirst { it.id == widgetId }; if (i < 0) return
        val target = if (forward) i + 1 else i - 1
        if (target !in list.indices) return
        val item = list.removeAt(i); list.add(target, item)
        val updated = list.mapIndexed { idx, w -> w.copy(order = idx) }
        _widgets.value = updated; preferencesManager.saveWidgets(updated)
    }
    fun removeWidget(widgetId: String) {
        val updated = _widgets.value.filterNot { it.id == widgetId }.mapIndexed { i, w -> w.copy(order = i) }
        _widgets.value = updated; preferencesManager.saveWidgets(updated)
    }
    fun resetWidgetsToDefault() { preferencesManager.resetToDefaultWidgets(); loadWidgets() }

    fun updateSafeArea(top: Int, bottom: Int, left: Int, right: Int) {
        val config = SafeAreaConfig(top.coerceIn(0, 250), bottom.coerceIn(0, 150), left.coerceIn(0, 150), right.coerceIn(0, 150))
        _safeArea.value = config; preferencesManager.saveSafeArea(config)
    }
    fun resetSafeArea() { updateSafeArea(0, 0, 0, 0) }
    fun updateSettings(newSettings: LauncherSettings) { _settings.value = newSettings; preferencesManager.saveSettings(newSettings) }

    fun loadApps() {
        val apps = appRepository.getInstalledApps(); _installedApps.value = apps
    }
    fun toggleAppFavorite(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) { appRepository.toggleFavorite(packageName); loadApps() }
    }
    fun toggleAppHidden(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) { appRepository.toggleHidden(packageName); loadApps() }
    }
    fun launchApp(packageName: String) { appRepository.launchApp(packageName) }

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
                        values.clear(); values.put(MediaStore.Audio.Media.IS_PENDING, 0); resolver.update(outUri, values, null, null)
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
        gpsTelemetryManager.stopGpsUpdates(); musicPlayerService.release(); tripComputer.release(); super.onCleared()
    }
}
