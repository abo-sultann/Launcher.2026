package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val appRepository = AppRepository(application, preferencesManager)
    private val musicPlayerService = MusicPlayerService(application, preferencesManager)
    private val gpsTelemetryManager = GpsTelemetryManager(application)
    private val tripComputer = TripComputer(preferencesManager)
    private val offlineMapEngine = OfflineMapEngine(application, preferencesManager)
    private val diagnosticManager = DiagnosticManager(application, preferencesManager)

    // Current Screen
    private val _currentScreen = MutableStateFlow(CarScreen.HOME)
    val currentScreen: StateFlow<CarScreen> = _currentScreen.asStateFlow()

    // Safe Area & Settings
    private val _safeArea = MutableStateFlow<SafeAreaConfig>(preferencesManager.getSafeArea())
    val safeArea: StateFlow<SafeAreaConfig> = _safeArea.asStateFlow()

    private val _settings = MutableStateFlow<LauncherSettings>(preferencesManager.getSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    // Widgets & Design Mode
    private val _widgets = MutableStateFlow<List<WidgetItem>>(emptyList())
    val widgets: StateFlow<List<WidgetItem>> = _widgets.asStateFlow()

    private val _isDesignMode = MutableStateFlow(false)
    val isDesignMode: StateFlow<Boolean> = _isDesignMode.asStateFlow()

    // Installed Applications
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps.asStateFlow()

    // Diagnostics & Safe Mode
    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _isSafeModeActive = MutableStateFlow(false)
    val isSafeModeActive: StateFlow<Boolean> = _isSafeModeActive.asStateFlow()

    // Forwarding StateFlows
    val playbackState: StateFlow<MusicPlaybackState> = musicPlayerService.playbackState
    val gpsTelemetry: StateFlow<GpsTelemetry> = gpsTelemetryManager.telemetry
    val tripData: StateFlow<TripData> = tripComputer.tripData
    val mapsList: StateFlow<List<MapItem>> = offlineMapEngine.mapsList
    val activeMap: StateFlow<MapItem?> = offlineMapEngine.activeMap
    val mapError: StateFlow<String?> = offlineMapEngine.mapError

    init {
        checkSafeMode()
        loadWidgets()
        loadApps()
        musicPlayerService.initialize()
        offlineMapEngine.initialize()
        gpsTelemetryManager.startGpsUpdates()

        // Sync GPS speed into Trip Computer
        viewModelScope.launch {
            gpsTelemetry.collect { telemetry ->
                if (telemetry.hasGpsFix) {
                    tripComputer.updateSpeed(telemetry.speedKmH)
                }
            }
        }
    }

    private fun checkSafeMode() {
        val safeModePrefs = getApplication<Application>().getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
        val crashCount = safeModePrefs.getInt("crash_count", 0)
        _isSafeModeActive.value = crashCount >= 2
    }

    fun navigateTo(screen: CarScreen) {
        _currentScreen.value = screen
    }

    // --- Widgets & Layout Management ---
    private fun loadWidgets() {
        _widgets.value = preferencesManager.getWidgets()
    }

    fun toggleDesignMode() {
        _isDesignMode.value = !_isDesignMode.value
    }

    fun addWidget(type: WidgetType, style: WidgetStyle) {
        val newWidget = WidgetItem(
            id = UUID.randomUUID().toString(),
            type = type,
            style = style,
            spanX = if (type == WidgetType.CONTROLS || type == WidgetType.APPS || style == WidgetStyle.MUSIC_LARGE_AUTOMOTIVE) 2 else 1,
            spanY = 1,
            order = _widgets.value.size
        )
        val updated = _widgets.value.toMutableList().apply { add(newWidget) }
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun updateWidgetStyle(widgetId: String, newStyle: WidgetStyle) {
        val updated = _widgets.value.map {
            if (it.id == widgetId) it.copy(style = newStyle) else it
        }
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun toggleWidgetSpan(widgetId: String) {
        val updated = _widgets.value.map {
            if (it.id == widgetId) {
                val newSpan = if (it.spanX == 1) 2 else 1
                it.copy(spanX = newSpan)
            } else it
        }
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun moveWidget(widgetId: String, forward: Boolean) {
        val list = _widgets.value.toMutableList()
        val index = list.indexOfFirst { it.id == widgetId }
        if (index == -1) return

        if (forward && index + 1 < list.size) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
        } else if (!forward && index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
        }

        // Re-index orders
        val reindexed = list.mapIndexed { idx, item -> item.copy(order = idx) }
        _widgets.value = reindexed
        preferencesManager.saveWidgets(reindexed)
    }

    fun removeWidget(widgetId: String) {
        val updated = _widgets.value.filter { it.id != widgetId }
            .mapIndexed { idx, item -> item.copy(order = idx) }
        _widgets.value = updated
        preferencesManager.saveWidgets(updated)
    }

    fun resetWidgetsToDefault() {
        preferencesManager.resetToDefaultWidgets()
        loadWidgets()
    }

    // --- Safe Area Controls ---
    fun updateSafeArea(top: Int, bottom: Int, left: Int, right: Int) {
        val config = SafeAreaConfig(
            topDp = top.coerceIn(0, 120),
            bottomDp = bottom.coerceIn(0, 120),
            leftDp = left.coerceIn(0, 120),
            rightDp = right.coerceIn(0, 120)
        )
        _safeArea.value = config
        preferencesManager.saveSafeArea(config)
    }

    fun resetSafeArea() {
        val defaultArea = SafeAreaConfig(0, 0, 0, 0)
        _safeArea.value = defaultArea
        preferencesManager.saveSafeArea(defaultArea)
    }

    // --- Settings Controls ---
    fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        preferencesManager.saveSettings(newSettings)
    }

    // --- App Drawer Controls ---
    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = appRepository.getInstalledApps()
            _installedApps.value = apps
        }
    }

    fun toggleAppFavorite(packageName: String) {
        appRepository.toggleFavorite(packageName)
        loadApps()
    }

    fun toggleAppHidden(packageName: String) {
        appRepository.toggleHidden(packageName)
        loadApps()
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
    }

    // --- Music Controls ---
    fun togglePlayPause() = musicPlayerService.togglePlayPause()
    fun playNext() = musicPlayerService.playNext()
    fun playPrevious() = musicPlayerService.playPrevious()
    fun seekTo(positionMs: Long) = musicPlayerService.seekTo(positionMs)
    fun skipForward10Sec() = musicPlayerService.skipForward10Sec()
    fun skipBackward10Sec() = musicPlayerService.skipBackward10Sec()
    fun adjustVolume(delta: Float) = musicPlayerService.setVolume(delta)
    fun toggleMute() = musicPlayerService.toggleMute()
    fun playTrack(track: MusicTrack) = musicPlayerService.playTrack(track)

    // --- Trip Computer Controls ---
    fun startTrip() = tripComputer.startTrip()
    fun pauseTrip() = tripComputer.pauseTrip()
    fun resetTrip() = tripComputer.resetTrip()

    // --- Offline Maps Controls ---
    fun importMapFile(file: File, name: String? = null) = offlineMapEngine.importMapFile(file, name)
    fun setActiveMap(mapId: String) = offlineMapEngine.setActiveMap(mapId)
    fun renameMap(mapId: String, newName: String) = offlineMapEngine.renameMap(mapId, newName)
    fun deleteMap(mapId: String) = offlineMapEngine.deleteMap(mapId)

    // --- Diagnostics Controls ---
    fun runDiagnostics() {
        _diagnosticReport.value = diagnosticManager.runFullDiagnostics()
    }

    fun resetSafeMode() {
        diagnosticManager.resetCrashCount()
        _isSafeModeActive.value = false
        runDiagnostics()
    }

    override fun onCleared() {
        super.onCleared()
        gpsTelemetryManager.stopGpsUpdates()
        musicPlayerService.release()
        tripComputer.release()
    }
}
