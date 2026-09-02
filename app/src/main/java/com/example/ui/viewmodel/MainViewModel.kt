package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.LauncherRuntime
import com.example.core.startup.LauncherStartupCoordinator
import com.example.core.startup.StartupStage
import com.example.data.*
import com.example.model.*
import com.example.ui.components.CarScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.min

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = LauncherRuntime(application)
    private val startupCoordinator = LauncherStartupCoordinator()
    private val preferencesManager = runtime.preferences
    private val startupSafeMode = application
        .getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
        .getInt("crash_count", 0) >= SAFE_MODE_CRASH_THRESHOLD
    private val legacyWidgetVisualStore = WidgetVisualStore(application)
    private val appRepository = runtime.apps
    private val musicPlayerService = runtime.music
    private val gpsTelemetryManager = runtime.gps
    private val tripComputer = runtime.trip
    private val offlineMapEngine = runtime.maps
    private val recommendedMapInstaller = runtime.recommendedMap
    private val diagnosticManager = runtime.diagnostics
    private val offroadTrackManager = runtime.offroad
    private val offlineMapSearchEngine = runtime.mapSearch

    private val _currentScreen = MutableStateFlow(CarScreen.HOME)
    val currentScreen: StateFlow<CarScreen> = _currentScreen.asStateFlow()
    private val _safeArea = MutableStateFlow(preferencesManager.getSafeArea())
    val safeArea: StateFlow<SafeAreaConfig> = _safeArea.asStateFlow()
    private val _settings = MutableStateFlow(
        preferencesManager.getSettings().let { saved ->
            if (startupSafeMode) saved.copy(
                backgroundType = BackgroundType.DARK_CARBON,
                customWallpaperPath = null,
                screenSaverEnabled = false
            ) else saved
        }
    )
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

    private val _offlineSearchResults = MutableStateFlow<List<OfflineMapSearchResult>>(emptyList())
    val offlineSearchResults: StateFlow<List<OfflineMapSearchResult>> = _offlineSearchResults.asStateFlow()
    private val _offlineSearchInProgress = MutableStateFlow(false)
    val offlineSearchInProgress: StateFlow<Boolean> = _offlineSearchInProgress.asStateFlow()
    private var offlineSearchJob: Job? = null
    private var offlineSearchGeneration = 0
    private val _offroadTransferMessage = MutableStateFlow<String?>(null)
    val offroadTransferMessage: StateFlow<String?> = _offroadTransferMessage.asStateFlow()
    private val _fileImportStatus = MutableStateFlow<String?>(null)
    val fileImportStatus: StateFlow<String?> = _fileImportStatus.asStateFlow()

    private val layoutPrefs = application.getSharedPreferences("launcher_layout_presets_2026", Context.MODE_PRIVATE)
    private val _savedHomeLayouts = MutableStateFlow(loadNamedLayoutNames(HOME_LAYOUTS_KEY))
    val savedHomeLayouts: StateFlow<List<String>> = _savedHomeLayouts.asStateFlow()
    private val _savedScreenSaverLayouts = MutableStateFlow(loadNamedLayoutNames(SAVER_LAYOUTS_KEY))
    val savedScreenSaverLayouts: StateFlow<List<String>> = _savedScreenSaverLayouts.asStateFlow()

    val playbackState: StateFlow<MusicPlaybackState> = musicPlayerService.playbackState
    val gpsTelemetry: StateFlow<GpsTelemetry> = gpsTelemetryManager.telemetry
    val tripData: StateFlow<TripData> = tripComputer.tripData
    val tripHistory: StateFlow<List<SavedTrip>> = tripComputer.history
    val mapsList: StateFlow<List<MapItem>> = offlineMapEngine.mapsList
    val activeMap: StateFlow<MapItem?> = offlineMapEngine.activeMap
    val mapError: StateFlow<String?> = offlineMapEngine.mapError
    val recommendedMapDownloadState: StateFlow<RecommendedMapDownloadState> = recommendedMapInstaller.state
    val offroadTrackPoints: StateFlow<List<OffroadTrackPoint>> = offroadTrackManager.trackPoints
    val savedOffroadPlaces: StateFlow<List<SavedOffroadPlace>> = offroadTrackManager.savedPlaces
    val offroadNavigationTarget: StateFlow<OffroadNavigationTarget?> = offroadTrackManager.navigationTarget
    val offroadMapState: StateFlow<OffroadMapState> = offroadTrackManager.mapState
    val startupStage: StateFlow<StartupStage> = startupCoordinator.stage
    val startupIssues: StateFlow<List<String>> = startupCoordinator.issues

    init {
        checkSafeMode()
        if (startupSafeMode) {
            _widgets.value = WidgetItem.createDefaultList()
            _screenSaverLayouts.value = emptyList()
        } else {
            runCatching { loadWidgets() }.onFailure { _widgets.value = WidgetItem.createDefaultList() }
            runCatching { loadScreenSaverLayouts() }.onFailure { _screenSaverLayouts.value = emptyList() }
        }
        startupCoordinator.start(
            scope = viewModelScope,
            safeMode = startupSafeMode,
            loadApps = { loadApps() },
            initializeMusic = { musicPlayerService.initialize() },
            initializeMap = { offlineMapEngine.initialize() },
            initializeGps = { gpsTelemetryManager.startGpsUpdates() }
        )
        viewModelScope.launch {
            gpsTelemetry.collect { telemetry ->
                tripComputer.updateTelemetry(telemetry, _settings.value.autoLogTrips)
                if (telemetry.hasGpsFix && telemetry.accuracyMeters <= 45f && telemetry.fixAgeMs <= 8_000L) {
                    offroadTrackManager.record(telemetry)
                }
            }
        }
    }

    private fun checkSafeMode() {
        val p = getApplication<Application>().getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
        _isSafeModeActive.value = p.getInt("crash_count", 0) >= SAFE_MODE_CRASH_THRESHOLD
    }

    fun navigateTo(screen: CarScreen) { if (_currentScreen.value != screen) _currentScreen.value = screen }
    fun restartGps() {
        if (!startupSafeMode) runCatching { gpsTelemetryManager.restartGpsUpdates() }
    }

    private fun loadWidgets() {
        val migrateSpeedWidget = !layoutPrefs.getBoolean(KEY_SPEED_NUMBER_TOP_LEFT, false)
        val migrated = preferencesManager.getWidgets()
            .map(legacyWidgetVisualStore::decorate)
            .map { item ->
                val tone = WidgetTone.fromArgb(item.foregroundColorArgb)
                val modern = item.copy(
                    style = modernWidgetStyle(item.style),
                    foregroundColorArgb = tone.argb,
                    accentColorArgb = tone.argb
                )
                if (migrateSpeedWidget && modern.type == WidgetType.SPEEDOMETER) {
                    modern.copy(
                        style = WidgetStyle.SPEED_DIGITAL_LARGE,
                        xFraction = .025f,
                        yFraction = .025f,
                        widthFraction = .13f,
                        heightFraction = .15f,
                        surfaceStyle = WidgetSurfaceStyle.TRANSPARENT,
                        showBorder = false,
                        foregroundColorArgb = WidgetTone.BLACK.argb,
                        accentColorArgb = WidgetTone.BLACK.argb
                    )
                } else modern
            }
        _widgets.value = migrated
        preferencesManager.saveWidgets(migrated)
        if (migrateSpeedWidget) layoutPrefs.edit().putBoolean(KEY_SPEED_NUMBER_TOP_LEFT, true).apply()
        migrated.forEach { legacyWidgetVisualStore.remove(it.id) }
    }
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
                val rawX = (item.xFraction + dxFraction).coerceIn(0f, (1f - width).coerceAtLeast(0f))
                val rawY = (item.yFraction + dyFraction).coerceIn(0f, (1f - height).coerceAtLeast(0f))
                item.copy(xFraction = snapCoordinate(rawX, width), yFraction = snapCoordinate(rawY, height))
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
    fun setWidgetOpacity(widgetId: String, opacity: Float) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(opacity = opacity.coerceIn(0.20f, 1f)) else it }
    fun setWidgetSurface(widgetId: String, surface: WidgetSurfaceStyle) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(surfaceStyle = surface) else it }
    fun toggleWidgetBorder(widgetId: String) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(showBorder = !it.showBorder) else it }
    fun setWidgetForeground(widgetId: String, argb: Int?) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(foregroundColorArgb = argb) else it }
    fun setWidgetAccent(widgetId: String, argb: Int?) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(accentColorArgb = argb) else it }
    fun setWidgetTone(widgetId: String, tone: WidgetTone) = updateAndSaveWidgets { item ->
        if (item.id == widgetId) item.copy(foregroundColorArgb = tone.argb, accentColorArgb = tone.argb) else item
    }
    fun setWidgetSurfaceOpacity(widgetId: String, opacity: Float) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(surfaceOpacity = opacity.coerceIn(.25f, 1f)) else it }
    fun toggleWidgetLock(widgetId: String) = updateAndSaveWidgets { if (it.id == widgetId) it.copy(isLocked = !it.isLocked) else it }

    fun setWidgetSizePreset(widgetId: String, preset: WidgetSizePreset) = updateAndSaveWidgets { item ->
        if (item.id != widgetId) item else {
            val (width, height) = WidgetItem.recommendedSize(item.type, preset)
            item.copy(
                widthFraction = width,
                heightFraction = height,
                xFraction = item.xFraction.coerceAtMost((1f - width).coerceAtLeast(0f)),
                yFraction = item.yFraction.coerceAtMost((1f - height).coerceAtLeast(0f))
            )
        }
    }

    fun replaceWidgetGeometry(widgetId: String, x: Float, y: Float, width: Float, height: Float) = updateAndSaveWidgets { item ->
        if (item.id != widgetId) item else {
            val w = width.coerceIn(.07f, 1f)
            val h = height.coerceIn(.07f, 1f)
            item.copy(
                xFraction = x.coerceIn(0f, (1f - w).coerceAtLeast(0f)),
                yFraction = y.coerceIn(0f, (1f - h).coerceAtLeast(0f)),
                widthFraction = w,
                heightFraction = h
            )
        }
    }

    fun replaceWidgets(items: List<WidgetItem>) {
        _widgets.value = items
        preferencesManager.saveWidgets(items)
    }

    fun resetWidget(widgetId: String) = updateAndSaveWidgets { item ->
        if (item.id != widgetId) item else {
            val (width, height) = WidgetItem.recommendedSize(item.type, WidgetSizePreset.SMALL)
            item.copy(
                widthFraction = width,
                heightFraction = height,
                xFraction = item.xFraction.coerceAtMost((1f - width).coerceAtLeast(0f)),
                yFraction = item.yFraction.coerceAtMost((1f - height).coerceAtLeast(0f)),
                opacity = 1f,
                isLocked = false,
                surfaceStyle = WidgetItem.defaultSurfaceFor(item.type),
                showBorder = false,
                foregroundColorArgb = WidgetTone.WHITE.argb,
                accentColorArgb = WidgetTone.WHITE.argb,
                surfaceOpacity = 1f
            )
        }
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

    fun applyWidgetLayoutPreset(preset: WidgetLayoutPreset) {
        val visible = _widgets.value.filter { it.isVisible }.sortedBy { it.order }
        if (visible.isEmpty()) return
        val arranged = arrangeHomeWidgets(visible, preset)
        val byId = arranged.associateBy { it.id }
        _widgets.value = _widgets.value.map { byId[it.id] ?: it }
        commitWidgetLayout()
    }

    fun alignHomeWidgetsHorizontalCenter() = transformVisibleHome { list -> list.map { it.copy(xFraction = ((1f - it.widthFraction) / 2f).coerceAtLeast(0f)) } }
    fun alignHomeWidgetsVerticalCenter() = transformVisibleHome { list -> list.map { it.copy(yFraction = ((1f - it.heightFraction) / 2f).coerceAtLeast(0f)) } }
    fun distributeHomeWidgetsEvenly() = transformVisibleHome { list -> arrangeHomeWidgets(list, WidgetLayoutPreset.EVEN_ROW) }
    fun equalizeHomeWidgetSizes() = transformVisibleHome { list ->
        val w = list.map { it.widthFraction }.average().toFloat().coerceIn(.16f, .48f)
        val h = list.map { it.heightFraction }.average().toFloat().coerceIn(.16f, .45f)
        list.map { it.copy(widthFraction = w, heightFraction = h, xFraction = it.xFraction.coerceAtMost(1f - w), yFraction = it.yFraction.coerceAtMost(1f - h)) }
    }

    private fun transformVisibleHome(transform: (List<WidgetItem>) -> List<WidgetItem>) {
        val visible = _widgets.value.filter { it.isVisible }.sortedBy { it.order }
        val changed = transform(visible).associateBy { it.id }
        _widgets.value = _widgets.value.map { changed[it.id] ?: it }
        commitWidgetLayout()
    }

    private fun arrangeHomeWidgets(items: List<WidgetItem>, requested: WidgetLayoutPreset): List<WidgetItem> {
        val n = items.size
        val preset = if (requested == WidgetLayoutPreset.AUTO) when {
            n <= 3 -> WidgetLayoutPreset.CENTER_ROW
            n == 4 -> WidgetLayoutPreset.GRID_2X2
            else -> WidgetLayoutPreset.GRID_3X2
        } else requested
        val gap = .025f
        return when (preset) {
            WidgetLayoutPreset.CENTER_ROW, WidgetLayoutPreset.EVEN_ROW, WidgetLayoutPreset.TOP_ROW, WidgetLayoutPreset.BOTTOM_ROW, WidgetLayoutPreset.LEFT_CENTER_RIGHT -> {
                val margin = .035f
                val width = min(.30f, ((1f - margin * 2 - gap * (n - 1)) / n).coerceAtLeast(.12f))
                val height = .30f
                val total = width * n + gap * (n - 1)
                val startX = if (preset == WidgetLayoutPreset.EVEN_ROW || preset == WidgetLayoutPreset.TOP_ROW || preset == WidgetLayoutPreset.BOTTOM_ROW) margin else ((1f - total) / 2f).coerceAtLeast(margin)
                val y = when (preset) {
                    WidgetLayoutPreset.TOP_ROW -> .04f
                    WidgetLayoutPreset.BOTTOM_ROW -> .66f
                    else -> .34f
                }
                items.mapIndexed { i, item -> item.copy(xFraction = startX + i * (width + gap), yFraction = y, widthFraction = width, heightFraction = height, zIndex = i) }
            }
            WidgetLayoutPreset.CENTER_COLUMN -> {
                val width = .38f
                val height = min(.25f, ((.90f - gap * (n - 1)) / n).coerceAtLeast(.14f))
                val total = height * n + gap * (n - 1)
                val startY = ((1f - total) / 2f).coerceAtLeast(.03f)
                items.mapIndexed { i, item -> item.copy(xFraction = (1f - width) / 2f, yFraction = startY + i * (height + gap), widthFraction = width, heightFraction = height, zIndex = i) }
            }
            WidgetLayoutPreset.GRID_2X2 -> arrangeHomeGrid(items, 2)
            WidgetLayoutPreset.GRID_3X2 -> arrangeHomeGrid(items, 3)
            WidgetLayoutPreset.AUTO -> items
        }
    }

    private fun arrangeHomeGrid(items: List<WidgetItem>, columns: Int): List<WidgetItem> {
        val cols = columns.coerceAtLeast(1)
        val rows = ceil(items.size / cols.toDouble()).toInt().coerceAtLeast(1)
        val gapX = .025f
        val gapY = .035f
        val marginX = .04f
        val marginY = .06f
        val width = ((1f - marginX * 2 - gapX * (cols - 1)) / cols).coerceIn(.12f, .44f)
        val height = ((1f - marginY * 2 - gapY * (rows - 1)) / rows).coerceIn(.16f, .40f)
        val usedWidth = width * cols + gapX * (cols - 1)
        val usedHeight = height * rows + gapY * (rows - 1)
        val startX = (1f - usedWidth) / 2f
        val startY = (1f - usedHeight) / 2f
        return items.mapIndexed { i, item ->
            val col = i % cols
            val row = i / cols
            item.copy(xFraction = startX + col * (width + gapX), yFraction = startY + row * (height + gapY), widthFraction = width, heightFraction = height, zIndex = i)
        }
    }

    fun saveNamedHomeLayout(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val root = readNamedLayouts(HOME_LAYOUTS_KEY)
        root.put(clean, serializeHomeLayout(_widgets.value))
        layoutPrefs.edit().putString(HOME_LAYOUTS_KEY, root.toString()).apply()
        _savedHomeLayouts.value = loadNamedLayoutNames(HOME_LAYOUTS_KEY)
    }

    fun restoreNamedHomeLayout(name: String) {
        try {
            val arr = readNamedLayouts(HOME_LAYOUTS_KEY).optJSONArray(name) ?: return
            val saved = mutableMapOf<String, JSONObject>()
            for (i in 0 until arr.length()) saved[arr.getJSONObject(i).getString("id")] = arr.getJSONObject(i)
            _widgets.value = _widgets.value.map { item -> saved[item.id]?.let { applySavedHome(item, it) } ?: item }
            commitWidgetLayout()
        } catch (_: Exception) { }
    }

    fun deleteNamedHomeLayout(name: String) {
        val root = readNamedLayouts(HOME_LAYOUTS_KEY)
        root.remove(name)
        layoutPrefs.edit().putString(HOME_LAYOUTS_KEY, root.toString()).apply()
        _savedHomeLayouts.value = loadNamedLayoutNames(HOME_LAYOUTS_KEY)
    }

    fun activateChildLock() { _isDesignMode.value = false; _isChildLockActive.value = true }
    fun deactivateChildLock() { _isChildLockActive.value = false }

    fun updateSafeArea(top: Int, bottom: Int, left: Int, right: Int) {
        val config = SafeAreaConfig(top.coerceIn(0, 250), bottom.coerceIn(0, 150), left.coerceIn(0, 150), right.coerceIn(0, 150))
        _safeArea.value = config
        preferencesManager.saveSafeArea(config)
    }
    fun resetSafeArea() { updateSafeArea(0, 0, 0, 0) }
    fun updateSettings(newSettings: LauncherSettings) { _settings.value = newSettings; preferencesManager.saveSettings(newSettings) }

    fun toggleScreenSaverWidget(type: WidgetType) {
        if (type !in SCREEN_SAVER_DISPLAY_WIDGET_TYPES) return
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
                    val style = try {
                        o.optString("style", "")
                            .takeIf { it.isNotBlank() }
                            ?.let { WidgetStyle.valueOf(it) }
                            ?.takeIf { it.type == type }
                            ?.let(::modernWidgetStyle)
                            ?.takeIf { it in screenSaverStylesFor(type) }
                    } catch (_: Exception) { null }
                    saved += ScreenSaverWidgetLayout(
                        type = type,
                        xFraction = o.optDouble("x", 0.05).toFloat(),
                        yFraction = o.optDouble("y", 0.12).toFloat(),
                        widthFraction = o.optDouble("w", 0.42).toFloat(),
                        heightFraction = o.optDouble("h", 0.34).toFloat(),
                        opacity = o.optDouble("opacity", 0.90).toFloat().coerceIn(0.25f, 1f),
                        zIndex = o.optInt("z", i),
                        style = style,
                        surfaceStyle = try { WidgetSurfaceStyle.valueOf(o.optString("surface", WidgetItem.defaultSurfaceFor(type).name)) } catch (_: Exception) { WidgetItem.defaultSurfaceFor(type) },
                        showBorder = o.optBoolean("border", false),
                        foregroundColorArgb = if (o.has("foreground") && !o.isNull("foreground")) o.optInt("foreground") else null,
                        accentColorArgb = if (o.has("accent") && !o.isNull("accent")) o.optInt("accent") else null,
                        surfaceOpacity = o.optDouble("surfaceOpacity", 1.0).toFloat().coerceIn(.25f, 1f)
                    )
                }
            }
        } catch (_: Exception) { }
        val orderedTypes = WidgetType.values().filter { it in _settings.value.screenSaverWidgetTypes && it in SCREEN_SAVER_DISPLAY_WIDGET_TYPES }.take(4)
        _screenSaverLayouts.value = orderedTypes.mapIndexed { index, type ->
            val decorated = legacyWidgetVisualStore.decorate(
                saved.firstOrNull { it.type == type } ?: ScreenSaverWidgetLayout.defaultFor(type, index)
            )
            val tone = WidgetTone.fromArgb(decorated.foregroundColorArgb)
            decorated.copy(
                foregroundColorArgb = tone.argb,
                accentColorArgb = tone.argb
            )
        }
        saveScreenSaverLayouts()
        orderedTypes.forEach { legacyWidgetVisualStore.remove("screensaver_${it.name.lowercase()}") }
    }

    private fun saveScreenSaverLayouts() {
        try {
            val array = JSONArray()
            _screenSaverLayouts.value.forEach { item -> array.put(JSONObject().apply {
                put("type", item.type.name); put("x", item.xFraction.toDouble()); put("y", item.yFraction.toDouble()); put("w", item.widthFraction.toDouble()); put("h", item.heightFraction.toDouble()); put("opacity", item.opacity.toDouble()); put("z", item.zIndex); put("style", item.style?.name ?: "")
                put("surface", item.surfaceStyle.name); put("border", item.showBorder); if (item.foregroundColorArgb != null) put("foreground", item.foregroundColorArgb); if (item.accentColorArgb != null) put("accent", item.accentColorArgb); put("surfaceOpacity", item.surfaceOpacity.toDouble())
            }) }
            screenSaverPrefs().edit().putString("screensaver_layouts_json", array.toString()).apply()
        } catch (_: Exception) { }
    }

    fun previewScreenSaverMove(type: WidgetType, dxFraction: Float, dyFraction: Float) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { item ->
            if (item.type != type) item else {
                val rawX = (item.xFraction + dxFraction).coerceIn(0f, (1f - item.widthFraction).coerceAtLeast(0f))
                val rawY = (item.yFraction + dyFraction).coerceIn(0f, (1f - item.heightFraction).coerceAtLeast(0f))
                item.copy(xFraction = snapCoordinate(rawX, item.widthFraction), yFraction = snapCoordinate(rawY, item.heightFraction))
            }
        }
    }

    fun previewScreenSaverResize(type: WidgetType, dwFraction: Float, dhFraction: Float) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { item ->
            if (item.type != type) item else {
                val maxW = (1f - item.xFraction).coerceAtLeast(0.16f)
                val maxH = (1f - item.yFraction).coerceAtLeast(0.16f)
                item.copy(widthFraction = (item.widthFraction + dwFraction).coerceIn(0.16f, maxW), heightFraction = (item.heightFraction + dhFraction).coerceIn(0.16f, maxH))
            }
        }
    }

    fun setScreenSaverOpacity(type: WidgetType, opacity: Float) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { if (it.type == type) it.copy(opacity = opacity.coerceIn(0.25f, 1f)) else it }
        saveScreenSaverLayouts()
    }

    fun setScreenSaverSurface(type: WidgetType, surface: WidgetSurfaceStyle) = updateScreenSaverAppearance(type) { it.copy(surfaceStyle = surface) }
    fun toggleScreenSaverBorder(type: WidgetType) = updateScreenSaverAppearance(type) { it.copy(showBorder = !it.showBorder) }
    fun setScreenSaverForeground(type: WidgetType, argb: Int?) = updateScreenSaverAppearance(type) { it.copy(foregroundColorArgb = argb) }
    fun setScreenSaverAccent(type: WidgetType, argb: Int?) = updateScreenSaverAppearance(type) { it.copy(accentColorArgb = argb) }
    fun setScreenSaverTone(type: WidgetType, tone: WidgetTone) = updateScreenSaverAppearance(type) {
        it.copy(foregroundColorArgb = tone.argb, accentColorArgb = tone.argb)
    }
    fun setScreenSaverSurfaceOpacity(type: WidgetType, opacity: Float) = updateScreenSaverAppearance(type) { it.copy(surfaceOpacity = opacity.coerceIn(.25f, 1f)) }

    private fun updateScreenSaverAppearance(type: WidgetType, transform: (ScreenSaverWidgetLayout) -> ScreenSaverWidgetLayout) {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { if (it.type == type) transform(it) else it }
        saveScreenSaverLayouts()
    }

    fun cycleScreenSaverStyle(type: WidgetType) {
        val styles = screenSaverStylesFor(type)
        if (styles.isEmpty()) return
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { item ->
            if (item.type != type) item else {
                val currentIndex = styles.indexOf(item.style)
                item.copy(style = styles[(if (currentIndex >= 0) currentIndex + 1 else 0) % styles.size])
            }
        }
        saveScreenSaverLayouts()
    }

    fun bringScreenSaverWidgetToFront(type: WidgetType) {
        val next = (_screenSaverLayouts.value.maxOfOrNull { it.zIndex } ?: 0) + 1
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { if (it.type == type) it.copy(zIndex = next) else it }
    }

    fun commitScreenSaverLayout() = saveScreenSaverLayouts()
    fun resetScreenSaverLayout() {
        val ordered = WidgetType.values().filter { it in _settings.value.screenSaverWidgetTypes && it in SCREEN_SAVER_DISPLAY_WIDGET_TYPES }.take(4)
        _screenSaverLayouts.value = ordered.mapIndexed { index, type -> ScreenSaverWidgetLayout.defaultFor(type, index) }
        saveScreenSaverLayouts()
    }

    fun applyScreenSaverLayoutPreset(preset: WidgetLayoutPreset) {
        val source = _screenSaverLayouts.value
        if (source.isEmpty()) return
        _screenSaverLayouts.value = arrangeScreenSaver(source, preset)
        saveScreenSaverLayouts()
    }

    fun alignScreenSaverHorizontalCenter() {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { it.copy(xFraction = ((1f - it.widthFraction) / 2f).coerceAtLeast(0f)) }
        saveScreenSaverLayouts()
    }
    fun alignScreenSaverVerticalCenter() {
        _screenSaverLayouts.value = _screenSaverLayouts.value.map { it.copy(yFraction = ((1f - it.heightFraction) / 2f).coerceAtLeast(0f)) }
        saveScreenSaverLayouts()
    }
    fun distributeScreenSaverEvenly() { _screenSaverLayouts.value = arrangeScreenSaver(_screenSaverLayouts.value, WidgetLayoutPreset.EVEN_ROW); saveScreenSaverLayouts() }
    fun equalizeScreenSaverSizes() {
        val list = _screenSaverLayouts.value
        if (list.isEmpty()) return
        val w = list.map { it.widthFraction }.average().toFloat().coerceIn(.16f, .48f)
        val h = list.map { it.heightFraction }.average().toFloat().coerceIn(.16f, .45f)
        _screenSaverLayouts.value = list.map { it.copy(widthFraction = w, heightFraction = h, xFraction = it.xFraction.coerceAtMost(1f - w), yFraction = it.yFraction.coerceAtMost(1f - h)) }
        saveScreenSaverLayouts()
    }

    private fun arrangeScreenSaver(items: List<ScreenSaverWidgetLayout>, requested: WidgetLayoutPreset): List<ScreenSaverWidgetLayout> {
        val n = items.size
        val preset = if (requested == WidgetLayoutPreset.AUTO) when {
            n <= 3 -> WidgetLayoutPreset.CENTER_ROW
            else -> WidgetLayoutPreset.GRID_2X2
        } else requested
        val gap = .025f
        return when (preset) {
            WidgetLayoutPreset.CENTER_ROW, WidgetLayoutPreset.EVEN_ROW, WidgetLayoutPreset.TOP_ROW, WidgetLayoutPreset.BOTTOM_ROW, WidgetLayoutPreset.LEFT_CENTER_RIGHT -> {
                val width = min(.30f, ((.93f - gap * (n - 1)) / n).coerceAtLeast(.16f))
                val height = .32f
                val total = width * n + gap * (n - 1)
                val startX = ((1f - total) / 2f).coerceAtLeast(.025f)
                val y = when (preset) { WidgetLayoutPreset.TOP_ROW -> .07f; WidgetLayoutPreset.BOTTOM_ROW -> .60f; else -> .33f }
                items.mapIndexed { i, item -> item.copy(xFraction = startX + i * (width + gap), yFraction = y, widthFraction = width, heightFraction = height, zIndex = i) }
            }
            WidgetLayoutPreset.CENTER_COLUMN -> {
                val width = .40f
                val height = min(.24f, ((.90f - gap * (n - 1)) / n).coerceAtLeast(.16f))
                val total = height * n + gap * (n - 1)
                val startY = (1f - total) / 2f
                items.mapIndexed { i, item -> item.copy(xFraction = .30f, yFraction = startY + i * (height + gap), widthFraction = width, heightFraction = height, zIndex = i) }
            }
            WidgetLayoutPreset.GRID_2X2 -> arrangeSaverGrid(items, 2)
            WidgetLayoutPreset.GRID_3X2 -> arrangeSaverGrid(items, 3)
            WidgetLayoutPreset.AUTO -> items
        }
    }

    private fun arrangeSaverGrid(items: List<ScreenSaverWidgetLayout>, columns: Int): List<ScreenSaverWidgetLayout> {
        val cols = columns.coerceAtLeast(1)
        val rows = ceil(items.size / cols.toDouble()).toInt().coerceAtLeast(1)
        val gapX = .03f
        val gapY = .04f
        val width = ((.92f - gapX * (cols - 1)) / cols).coerceIn(.16f, .44f)
        val height = ((.86f - gapY * (rows - 1)) / rows).coerceIn(.16f, .40f)
        val usedW = width * cols + gapX * (cols - 1)
        val usedH = height * rows + gapY * (rows - 1)
        val startX = (1f - usedW) / 2f
        val startY = (1f - usedH) / 2f
        return items.mapIndexed { i, item -> item.copy(xFraction = startX + (i % cols) * (width + gapX), yFraction = startY + (i / cols) * (height + gapY), widthFraction = width, heightFraction = height, zIndex = i) }
    }

    fun saveNamedScreenSaverLayout(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val root = readNamedLayouts(SAVER_LAYOUTS_KEY)
        root.put(clean, serializeSaverLayout(_screenSaverLayouts.value))
        layoutPrefs.edit().putString(SAVER_LAYOUTS_KEY, root.toString()).apply()
        _savedScreenSaverLayouts.value = loadNamedLayoutNames(SAVER_LAYOUTS_KEY)
    }

    fun restoreNamedScreenSaverLayout(name: String) {
        try {
            val arr = readNamedLayouts(SAVER_LAYOUTS_KEY).optJSONArray(name) ?: return
            val saved = mutableMapOf<WidgetType, JSONObject>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = try { WidgetType.valueOf(o.getString("type")) } catch (_: Exception) { continue }
                saved[type] = o
            }
            _screenSaverLayouts.value = _screenSaverLayouts.value.map { item -> saved[item.type]?.let { applySavedSaver(item, it) } ?: item }
            saveScreenSaverLayouts()
        } catch (_: Exception) { }
    }

    fun deleteNamedScreenSaverLayout(name: String) {
        val root = readNamedLayouts(SAVER_LAYOUTS_KEY)
        root.remove(name)
        layoutPrefs.edit().putString(SAVER_LAYOUTS_KEY, root.toString()).apply()
        _savedScreenSaverLayouts.value = loadNamedLayoutNames(SAVER_LAYOUTS_KEY)
    }

    private fun snapCoordinate(value: Float, size: Float): Float {
        val max = (1f - size).coerceAtLeast(0f)
        val center = max / 2f
        val candidates = floatArrayOf(0f, center, max)
        return candidates.minByOrNull { kotlin.math.abs(it - value) }?.takeIf { kotlin.math.abs(it - value) <= SNAP_TOLERANCE } ?: value
    }

    private fun serializeHomeLayout(items: List<WidgetItem>): JSONArray = JSONArray().apply {
        items.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("style", item.style.name); put("x", item.xFraction); put("y", item.yFraction); put("w", item.widthFraction); put("h", item.heightFraction); put("opacity", item.opacity); put("locked", item.isLocked); put("z", item.zIndex); put("surface", item.surfaceStyle.name); put("border", item.showBorder); if (item.foregroundColorArgb != null) put("foreground", item.foregroundColorArgb); if (item.accentColorArgb != null) put("accent", item.accentColorArgb); put("surfaceOpacity", item.surfaceOpacity)
        }) }
    }

    private fun applySavedHome(item: WidgetItem, o: JSONObject): WidgetItem {
        val style = try { WidgetStyle.valueOf(o.optString("style", item.style.name)).takeIf { it.type == item.type } ?: item.style } catch (_: Exception) { item.style }
        val tone = WidgetTone.fromArgb(if (o.has("foreground") && !o.isNull("foreground")) o.optInt("foreground") else item.foregroundColorArgb)
        return item.copy(style = modernWidgetStyle(style), xFraction = o.optDouble("x", item.xFraction.toDouble()).toFloat(), yFraction = o.optDouble("y", item.yFraction.toDouble()).toFloat(), widthFraction = o.optDouble("w", item.widthFraction.toDouble()).toFloat(), heightFraction = o.optDouble("h", item.heightFraction.toDouble()).toFloat(), opacity = o.optDouble("opacity", item.opacity.toDouble()).toFloat(), isLocked = o.optBoolean("locked", item.isLocked), zIndex = o.optInt("z", item.zIndex), surfaceStyle = try { WidgetSurfaceStyle.valueOf(o.optString("surface", item.surfaceStyle.name)) } catch (_: Exception) { item.surfaceStyle }, showBorder = o.optBoolean("border", item.showBorder), foregroundColorArgb = tone.argb, accentColorArgb = tone.argb, surfaceOpacity = o.optDouble("surfaceOpacity", item.surfaceOpacity.toDouble()).toFloat())
    }

    private fun serializeSaverLayout(items: List<ScreenSaverWidgetLayout>): JSONArray = JSONArray().apply {
        items.forEach { item -> put(JSONObject().apply { put("type", item.type.name); put("style", item.style?.name ?: ""); put("x", item.xFraction); put("y", item.yFraction); put("w", item.widthFraction); put("h", item.heightFraction); put("opacity", item.opacity); put("z", item.zIndex); put("surface", item.surfaceStyle.name); put("border", item.showBorder); if (item.foregroundColorArgb != null) put("foreground", item.foregroundColorArgb); if (item.accentColorArgb != null) put("accent", item.accentColorArgb); put("surfaceOpacity", item.surfaceOpacity) }) }
    }

    private fun applySavedSaver(item: ScreenSaverWidgetLayout, o: JSONObject): ScreenSaverWidgetLayout {
        val style = try { o.optString("style", "").takeIf { it.isNotBlank() }?.let { WidgetStyle.valueOf(it) }?.takeIf { it.type == item.type } ?: item.style } catch (_: Exception) { item.style }
        val modernStyle = style?.let(::modernWidgetStyle)?.takeIf { it in screenSaverStylesFor(item.type) } ?: screenSaverStylesFor(item.type).firstOrNull()
        val tone = WidgetTone.fromArgb(if (o.has("foreground") && !o.isNull("foreground")) o.optInt("foreground") else item.foregroundColorArgb)
        return item.copy(xFraction = o.optDouble("x", item.xFraction.toDouble()).toFloat(), yFraction = o.optDouble("y", item.yFraction.toDouble()).toFloat(), widthFraction = o.optDouble("w", item.widthFraction.toDouble()).toFloat(), heightFraction = o.optDouble("h", item.heightFraction.toDouble()).toFloat(), opacity = o.optDouble("opacity", item.opacity.toDouble()).toFloat(), zIndex = o.optInt("z", item.zIndex), style = modernStyle, surfaceStyle = try { WidgetSurfaceStyle.valueOf(o.optString("surface", item.surfaceStyle.name)) } catch (_: Exception) { item.surfaceStyle }, showBorder = o.optBoolean("border", item.showBorder), foregroundColorArgb = tone.argb, accentColorArgb = tone.argb, surfaceOpacity = o.optDouble("surfaceOpacity", item.surfaceOpacity.toDouble()).toFloat())
    }

    private fun readNamedLayouts(key: String): JSONObject = try { JSONObject(layoutPrefs.getString(key, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
    private fun loadNamedLayoutNames(key: String): List<String> {
        val root = readNamedLayouts(key)
        val names = mutableListOf<String>()
        val keys = root.keys()
        while (keys.hasNext()) names += keys.next()
        return names.sorted()
    }

    fun importWallpaperUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val directory = File(app.filesDir, "wallpapers").apply { mkdirs() }
            val target = File(directory, "launcher_wallpaper.jpg")
            val temporary = File(directory, "launcher_wallpaper.tmp")
            val source = File(directory, "launcher_wallpaper.source")
            try {
                _fileImportStatus.value = "جارٍ تجهيز الخلفية..."
                temporary.delete()
                source.delete()
                val resolver = app.contentResolver
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(source).use { output -> input.copyTo(output, 256 * 1024) }
                } ?: throw IllegalArgumentException("تعذر فتح الصورة")
                if (source.length() <= 0L) throw IllegalArgumentException("ملف الصورة فارغ")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(source.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    throw IllegalArgumentException("صيغة الصورة غير مدعومة؛ استخدم JPG أو PNG")
                }

                var sampleSize = 1
                while (bounds.outWidth / sampleSize > WALLPAPER_WIDTH * 2 ||
                    bounds.outHeight / sampleSize > WALLPAPER_HEIGHT * 2
                ) sampleSize *= 2

                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inDither = true
                }
                val decoded = BitmapFactory.decodeFile(source.absolutePath, options)
                    ?: throw IllegalArgumentException("تعذر قراءة الصورة")
                val prepared = centerCropWallpaper(decoded)
                if (prepared !== decoded) decoded.recycle()

                FileOutputStream(temporary).use { output ->
                    if (!prepared.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                        throw IllegalStateException("تعذر حفظ الخلفية")
                    }
                    output.fd.sync()
                }
                prepared.recycle()
                if (temporary.length() <= 0L) throw IllegalStateException("الخلفية فارغة")

                if (target.exists() && !target.delete()) throw IllegalStateException("تعذر استبدال الخلفية")
                if (!temporary.renameTo(target)) throw IllegalStateException("تعذر تثبيت الخلفية")
                directory.listFiles()?.filter { it != target }?.forEach { old ->
                    if (old.name.startsWith("launcher_wallpaper.")) old.delete()
                }

                val newSettings = _settings.value.copy(
                    backgroundType = BackgroundType.CUSTOM_IMAGE,
                    customWallpaperPath = target.absolutePath
                )
                _settings.value = newSettings
                preferencesManager.saveSettings(newSettings)
                source.delete()
                _fileImportStatus.value = "تمت إضافة الخلفية"
            } catch (_: OutOfMemoryError) {
                temporary.delete()
                source.delete()
                _fileImportStatus.value = "الصورة كبيرة جدًا لهذه الشاشة"
            } catch (e: Exception) {
                temporary.delete()
                source.delete()
                _fileImportStatus.value = e.message ?: "تعذر إضافة الخلفية"
            }
        }
    }

    fun prepareForExternalPicker() {
        (getApplication<Application>() as? com.example.CarLauncherApp)?.prepareForExternalPicker()
    }

    private fun centerCropWallpaper(source: Bitmap): Bitmap {
        val scale = maxOf(
            WALLPAPER_WIDTH.toFloat() / source.width.coerceAtLeast(1),
            WALLPAPER_HEIGHT.toFloat() / source.height.coerceAtLeast(1)
        )
        val scaledWidth = (source.width * scale).toInt().coerceAtLeast(WALLPAPER_WIDTH)
        val scaledHeight = (source.height * scale).toInt().coerceAtLeast(WALLPAPER_HEIGHT)
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val left = ((scaled.width - WALLPAPER_WIDTH) / 2).coerceAtLeast(0)
        val top = ((scaled.height - WALLPAPER_HEIGHT) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, left, top, WALLPAPER_WIDTH, WALLPAPER_HEIGHT)
        if (scaled !== source && scaled !== cropped) scaled.recycle()
        return cropped
    }

    fun loadApps() { _installedApps.value = appRepository.getInstalledApps() }
    fun toggleAppFavorite(packageName: String) { viewModelScope.launch(Dispatchers.IO) { appRepository.toggleFavorite(packageName); loadApps() } }
    fun toggleAppHidden(packageName: String) { viewModelScope.launch(Dispatchers.IO) { appRepository.toggleHidden(packageName); loadApps() } }
    fun launchApp(packageName: String) {
        (getApplication<Application>() as? com.example.CarLauncherApp)?.prepareForExternalActivity()
        appRepository.launchApp(packageName)
    }
    fun launchAndroidSettings() {
        (getApplication<Application>() as? com.example.CarLauncherApp)?.prepareForExternalActivity()
        appRepository.launchAndroidSettings()
    }

    fun togglePlayPause() = musicPlayerService.togglePlayPause()
    fun playNext() = musicPlayerService.playNext()
    fun playPrevious() = musicPlayerService.playPrevious()
    fun seekTo(positionMs: Long) = musicPlayerService.seekTo(positionMs)
    fun skipForward10Sec() = musicPlayerService.skipForward10Sec()
    fun skipBackward10Sec() = musicPlayerService.skipBackward10Sec()
    fun adjustVolume(delta: Float) = musicPlayerService.setVolume(delta)
    fun toggleMute() = musicPlayerService.toggleMute()
    fun playTrack(track: MusicTrack) = musicPlayerService.playTrack(track)
    fun refreshMusicLibrary() = musicPlayerService.initialize()

    fun importMusicUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val name = queryDisplayName(uri) ?: "music_${System.currentTimeMillis()}.mp3"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, name); put(MediaStore.Audio.Media.MIME_TYPE, resolver.getType(uri) ?: "audio/mpeg"); put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Launcher 2026"); put(MediaStore.Audio.Media.IS_PENDING, 1)
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
        getApplication<Application>().contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (_: Exception) { null }

    fun startTrip() = tripComputer.startTrip()
    fun pauseTrip() = tripComputer.pauseTrip()
    fun finishTrip(name: String? = null) = tripComputer.finishTrip(name)
    fun resetTrip() = tripComputer.resetTrip()
    fun renameSavedTrip(id: String, name: String) = tripComputer.renameSavedTrip(id, name)
    fun deleteSavedTrip(id: String) = tripComputer.deleteSavedTrip(id)
    fun hasSavedTripRoute(id: String): Boolean = tripComputer.hasSavedTripRoute(id)
    fun openSavedTripRoute(id: String) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { tripComputer.loadSavedTripWithRoute(id) }
            if (saved != null) {
                SavedTripRouteBridge.show(saved)
                navigateTo(CarScreen.MAP)
            }
        }
    }
    fun noteTripSavedPlace() = tripComputer.noteSavedPlace()

    fun importMapFile(file: File, name: String? = null) = offlineMapEngine.importMapFile(file, name)
    fun importMapUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            var temporary: File? = null
            try {
                _fileImportStatus.value = "جارٍ فحص ملف الخريطة..."
                val resolver = getApplication<Application>().contentResolver
                val name = (queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Saudi-2026.map")
                    .substringAfterLast('/')
                    .replace(Regex("[^A-Za-z0-9._() -]"), "_")
                val extension = name.substringAfterLast('.', "").lowercase()
                if (extension == "zip") throw IllegalArgumentException("فك ضغط ZIP ثم اختر ملف .map أو .mbtiles")
                if (extension !in SUPPORTED_MAP_EXTENSIONS) {
                    throw IllegalArgumentException("اختر خريطة Mapsforge (.map) أو MBTiles صورية")
                }
                val dir = File(getApplication<Application>().filesDir, "maps").apply { mkdirs() }
                val declaredSize = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                val available = StatFs(dir.absolutePath).availableBytes
                if (declaredSize > 0L && available < declaredSize + MAP_IMPORT_FREE_SPACE_MARGIN) {
                    throw IllegalStateException("المساحة غير كافية؛ يلزم ${(declaredSize + MAP_IMPORT_FREE_SPACE_MARGIN) / (1024 * 1024)} ميجابايت تقريبًا")
                }
                // Never overwrite a working map before the replacement has been copied and
                // validated. Re-importing the same filename gets a numbered destination.
                val target = uniqueMapDestination(dir, name)
                val part = File(dir, "${target.name}.part").also { it.delete() }
                temporary = part
                _fileImportStatus.value = "جارٍ نسخ الخريطة؛ لا تغلق التطبيق..."
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(part).use { output -> input.copyTo(output, 1024 * 1024); output.fd.sync() }
                } ?: throw IllegalArgumentException("تعذر فتح ملف الخريطة")
                if (part.length() <= 0L) throw IllegalArgumentException("ملف الخريطة فارغ")
                if (!part.renameTo(target)) throw IllegalStateException("تعذر تثبيت ملف الخريطة")
                if (!offlineMapEngine.importMapFile(target, target.nameWithoutExtension)) {
                    target.delete()
                    throw IllegalArgumentException(offlineMapEngine.mapError.value ?: "ملف الخريطة غير صالح")
                }
                offlineMapSearchEngine.clear()
                _fileImportStatus.value = if (extension == "map") {
                    "تمت إضافة الخريطة المتجهة وتفعيل البحث بالأسماء"
                } else {
                    "تمت إضافة خريطة MBTiles وتفعيل عرضها"
                }
            } catch (e: Exception) {
                temporary?.delete()
                _fileImportStatus.value = e.message ?: "تعذر إضافة الخريطة"
            }
        }
    }

    private fun uniqueMapDestination(directory: File, fileName: String): File {
        val direct = File(directory, fileName)
        if (!direct.exists()) return direct
        val extension = fileName.substringAfterLast('.', "")
        val base = if (extension.isBlank()) fileName else fileName.removeSuffix(".$extension")
        for (index in 2..999) {
            val candidate = File(directory, if (extension.isBlank()) "$base-$index" else "$base-$index.$extension")
            if (!candidate.exists()) return candidate
        }
        return File(directory, if (extension.isBlank()) "$base-${System.currentTimeMillis()}" else "$base-${System.currentTimeMillis()}.$extension")
    }

    fun downloadRecommendedMap() {
        viewModelScope.launch(Dispatchers.IO) {
            if (recommendedMapInstaller.downloadAndInstall()) offlineMapSearchEngine.clear()
        }
    }

    fun clearRecommendedMapDownloadMessage() = recommendedMapInstaller.clearMessage()
    fun clearFileImportStatus() { _fileImportStatus.value = null }
    fun setActiveMap(mapId: String) { offlineMapEngine.setActiveMap(mapId); offlineMapSearchEngine.clear() }
    fun renameMap(mapId: String, newName: String) = offlineMapEngine.renameMap(mapId, newName)
    fun deleteMap(mapId: String) { offlineMapEngine.deleteMap(mapId); offlineMapSearchEngine.clear() }
    fun reportMapError(message: String?) = offlineMapEngine.setMapError(message)

    fun saveCurrentOffroadPlace(name: String? = null): SavedOffroadPlace? {
        val saved = offroadTrackManager.saveCurrentPlace(gpsTelemetry.value, name)
        if (saved != null) tripComputer.noteSavedPlace()
        return saved
    }
    fun renameSavedOffroadPlace(id: String, name: String) = offroadTrackManager.renamePlace(id, name)
    fun deleteSavedOffroadPlace(id: String) = offroadTrackManager.deletePlace(id)
    fun navigateToSavedOffroadPlace(id: String) { savedOffroadPlaces.value.firstOrNull { it.id == id }?.let(offroadTrackManager::navigateTo) }
    fun navigateToTrackStart() = offroadTrackManager.navigateToTrackStart()
    fun stopOffroadNavigation() = offroadTrackManager.stopNavigation()
    fun clearOffroadTrack() = offroadTrackManager.clearTrack()
    fun offroadDistanceToTargetMeters(): Float? = offroadTrackManager.distanceToTargetMeters(gpsTelemetry.value)
    fun offroadBearingToTarget(): Float? = offroadTrackManager.bearingToTarget(gpsTelemetry.value)
    fun offroadDistanceToTrackStartMeters(): Float? = offroadTrackManager.distanceToTrackStartMeters(gpsTelemetry.value)
    fun offroadBearingToTrackStart(): Float? = offroadTrackManager.bearingToTrackStart(gpsTelemetry.value)
    fun offroadTrackDistanceKm(): Double = offroadTrackManager.trackDistanceKm()
    fun updateOffroadMapState(state: OffroadMapState) = offroadTrackManager.saveMapState(state)

    fun searchOfflineMap(query: String) {
        val cleanQuery = query.trim()
        offlineSearchJob?.cancel()
        val generation = ++offlineSearchGeneration
        if (cleanQuery.length < 2) {
            _offlineSearchInProgress.value = false
            _offlineSearchResults.value = emptyList()
            return
        }
        _offlineSearchInProgress.value = true
        offlineSearchJob = viewModelScope.launch(Dispatchers.IO) {
            // Prevent every keystroke from rebuilding a large country map index.
            delay(180L)
            val telemetry = gpsTelemetry.value
            val latitude = telemetry.latitude.takeIf { telemetry.hasGpsFix }
            val longitude = telemetry.longitude.takeIf { telemetry.hasGpsFix }
            val results = offlineMapSearchEngine.search(
                query = cleanQuery,
                activeMap = activeMap.value,
                savedPlaces = savedOffroadPlaces.value,
                currentLatitude = latitude,
                currentLongitude = longitude
            )
            if (generation == offlineSearchGeneration) {
                _offlineSearchResults.value = results
                _offlineSearchInProgress.value = false
            }
        }
    }
    fun clearOfflineMapSearch() {
        offlineSearchJob?.cancel()
        offlineSearchGeneration++
        _offlineSearchInProgress.value = false
        _offlineSearchResults.value = emptyList()
    }
    fun navigateToSearchResult(result: OfflineMapSearchResult) {
        offroadTrackManager.navigateToCoordinates(result.id, result.name, result.latitude, result.longitude)
        // أثناء التوجيه تبقى الخريطة متتبعة للسيارة؛ الهدف يظهر بعلامة وسهم وخط.
        updateOffroadMapState(offroadMapState.value.copy(latitude = result.latitude, longitude = result.longitude, followGps = true, zoomLevel = 15))
    }

    fun importGpxUri(uri: Uri) = readOffroadText(uri, "GPX") { raw -> offroadTrackManager.importGpx(raw) }
    fun importOffroadBackupUri(uri: Uri) = readOffroadText(uri, "النسخة الاحتياطية") { raw -> offroadTrackManager.importBackupJson(raw) }

    private fun readOffroadText(uri: Uri, label: String, importer: (String) -> Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = getApplication<Application>().contentResolver.readUtf8TextLimited(uri)
                val count = importer(raw)
                _offroadTransferMessage.value = if (count > 0) "تم استيراد $label: $count عنصر" else "لم يتم العثور على بيانات صالحة في $label"
            } catch (_: Exception) { _offroadTransferMessage.value = "تعذر استيراد $label" }
        }
    }

    fun exportGpxUri(uri: Uri) = writeOffroadText(uri, offroadTrackManager.exportGpx(), "تم تصدير GPX")
    fun exportOffroadBackupUri(uri: Uri) = writeOffroadText(uri, offroadTrackManager.exportBackupJson(), "تم إنشاء النسخة الاحتياطية")

    private fun writeOffroadText(uri: Uri, content: String, success: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
                _offroadTransferMessage.value = success
            } catch (_: Exception) { _offroadTransferMessage.value = "تعذر حفظ الملف" }
        }
    }
    fun clearOffroadTransferMessage() { _offroadTransferMessage.value = null }

    fun runDiagnostics() { _diagnosticReport.value = diagnosticManager.runFullDiagnostics() }
    fun resetSafeMode() { diagnosticManager.resetCrashCount(); _isSafeModeActive.value = false; runDiagnostics() }

    override fun onCleared() {
        gpsTelemetryManager.stopGpsUpdates()
        musicPlayerService.release()
        tripComputer.release()
        // Application-owned: keep off-road persistence alive across Activity recreation.
        super.onCleared()
    }

    companion object {
        private const val SNAP_TOLERANCE = 0.014f
        private const val HOME_LAYOUTS_KEY = "saved_home_layouts_json"
        private const val SAVER_LAYOUTS_KEY = "saved_saver_layouts_json"
        private const val WALLPAPER_WIDTH = 1024
        private const val WALLPAPER_HEIGHT = 600
        private const val MAP_IMPORT_FREE_SPACE_MARGIN = 64L * 1024L * 1024L
        private const val SAFE_MODE_CRASH_THRESHOLD = 2
        private const val KEY_SPEED_NUMBER_TOP_LEFT = "speed_number_top_left_v203"
        private val SUPPORTED_MAP_EXTENSIONS = setOf("map", "mbtiles")
    }
}
