package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OffroadPlaceKind(val arabicName: String) {
    CAMP("مخيم"),
    BIRD("طير / سمان"),
    CAR("السيارة"),
    WATER("ماء"),
    WELL("بئر"),
    HOME("منزل"),
    IMPORTANT("مهم"),
    FLAG("نقطة")
}

enum class MapOverlaySlot(val arabicName: String) {
    TOP_START("أعلى اليسار"),
    TOP_CENTER("أعلى المنتصف"),
    TOP_END("أعلى اليمين"),
    CENTER_START("منتصف اليسار"),
    CENTER_END("منتصف اليمين"),
    BOTTOM_START("أسفل اليسار"),
    BOTTOM_CENTER("أسفل المنتصف"),
    BOTTOM_END("أسفل اليمين")
}

data class EnhancedSavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: OffroadPlaceKind = OffroadPlaceKind.FLAG,
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class EnhancedMapUiPreferences(
    val trackVisible: Boolean = true,
    val trackWidth: Float = 6f,
    val nightMap: Boolean = false,
    val drivingView: Boolean = true,
    val detailedTheme: Boolean = true,
    val showTelemetry: Boolean = true,
    val showPrimaryActions: Boolean = true,
    val showMapScale: Boolean = true,
    val telemetrySlot: MapOverlaySlot = MapOverlaySlot.TOP_START,
    val primaryActionsSlot: MapOverlaySlot = MapOverlaySlot.TOP_START,
    val dockSlot: MapOverlaySlot = MapOverlaySlot.CENTER_END,
    val scaleSlot: MapOverlaySlot = MapOverlaySlot.BOTTOM_START
)

class EnhancedMapStore(context: Context) {
    private val prefs = context.getSharedPreferences("enhanced_offroad_map_2026", Context.MODE_PRIVATE)

    private val _extraPlaces: MutableStateFlow<List<EnhancedSavedPlace>> = synchronized(SHARED_LOCK) {
        sharedExtraPlaces ?: MutableStateFlow(loadExtraPlaces()).also { sharedExtraPlaces = it }
    }
    val extraPlaces: StateFlow<List<EnhancedSavedPlace>> = _extraPlaces.asStateFlow()

    private val _placeKinds: MutableStateFlow<Map<String, OffroadPlaceKind>> = synchronized(SHARED_LOCK) {
        sharedPlaceKinds ?: MutableStateFlow(loadKinds()).also { sharedPlaceKinds = it }
    }
    val placeKinds: StateFlow<Map<String, OffroadPlaceKind>> = _placeKinds.asStateFlow()

    private val _ui: MutableStateFlow<EnhancedMapUiPreferences> = synchronized(SHARED_LOCK) {
        sharedUi ?: MutableStateFlow(loadUi()).also { sharedUi = it }
    }
    val ui: StateFlow<EnhancedMapUiPreferences> = _ui.asStateFlow()

    fun saveExtraPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        kind: OffroadPlaceKind,
        notes: String = "",
        favorite: Boolean = false
    ): EnhancedSavedPlace {
        val place = EnhancedSavedPlace(
            id = "map_${UUID.randomUUID()}",
            name = name.trim().ifBlank { "نقطة محفوظة" },
            latitude = latitude,
            longitude = longitude,
            kind = kind,
            notes = notes.trim().take(120),
            favorite = favorite
        )
        _extraPlaces.value = (_extraPlaces.value + place).sortedWith(compareByDescending<EnhancedSavedPlace> { it.favorite }.thenByDescending { it.createdAt })
        persistExtraPlaces()
        return place
    }

    fun renameExtraPlace(id: String, name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        _extraPlaces.value = _extraPlaces.value.map { if (it.id == id) it.copy(name = clean) else it }
        persistExtraPlaces()
    }

    fun updateExtraPlaceKind(id: String, kind: OffroadPlaceKind) {
        _extraPlaces.value = _extraPlaces.value.map { if (it.id == id) it.copy(kind = kind) else it }
        persistExtraPlaces()
    }

    fun updateExtraPlace(id: String, name: String, kind: OffroadPlaceKind, notes: String, favorite: Boolean) {
        val clean = name.trim()
        if (clean.isBlank()) return
        _extraPlaces.value = _extraPlaces.value.map {
            if (it.id == id) it.copy(name = clean, kind = kind, notes = notes.trim().take(120), favorite = favorite) else it
        }.sortedWith(compareByDescending<EnhancedSavedPlace> { it.favorite }.thenByDescending { it.createdAt })
        persistExtraPlaces()
    }

    fun deleteExtraPlace(id: String) {
        _extraPlaces.value = _extraPlaces.value.filterNot { it.id == id }
        persistExtraPlaces()
    }

    fun setPlaceKind(id: String, kind: OffroadPlaceKind) {
        val updated = _placeKinds.value.toMutableMap().apply { put(id, kind) }
        _placeKinds.value = updated
        val json = JSONObject()
        updated.forEach { (key, value) -> json.put(key, value.name) }
        prefs.edit().putString(KEY_KINDS, json.toString()).apply()
    }

    fun updateUi(transform: (EnhancedMapUiPreferences) -> EnhancedMapUiPreferences) {
        val transformed = transform(_ui.value)
        val next = transformed.copy(trackWidth = transformed.trackWidth.coerceIn(3f, 12f))
        _ui.value = next
        prefs.edit()
            .putBoolean("track_visible", next.trackVisible)
            .putFloat("track_width", next.trackWidth)
            .putBoolean("night_map", next.nightMap)
            .putBoolean("driving_view", next.drivingView)
            .putBoolean("detailed_theme", next.detailedTheme)
            .putBoolean("show_telemetry", next.showTelemetry)
            .putBoolean("show_primary_actions", next.showPrimaryActions)
            .putBoolean("show_map_scale", next.showMapScale)
            .putString("telemetry_slot", next.telemetrySlot.name)
            .putString("primary_actions_slot", next.primaryActionsSlot.name)
            .putString("dock_slot", next.dockSlot.name)
            .putString("scale_slot", next.scaleSlot.name)
            .apply()
    }

    private fun loadUi(): EnhancedMapUiPreferences {
        val migrateAbsoluteSlots = !prefs.getBoolean(KEY_ABSOLUTE_OVERLAY_SLOTS, false)
        val telemetrySlot = if (migrateAbsoluteSlots) MapOverlaySlot.TOP_START
        else loadSlot("telemetry_slot", MapOverlaySlot.TOP_START)
        if (migrateAbsoluteSlots) {
            prefs.edit()
                .putString("telemetry_slot", telemetrySlot.name)
                .putBoolean(KEY_ABSOLUTE_OVERLAY_SLOTS, true)
                .apply()
        }
        return EnhancedMapUiPreferences(
            trackVisible = prefs.getBoolean("track_visible", true),
            trackWidth = prefs.getFloat("track_width", 6f).coerceIn(3f, 12f),
            nightMap = prefs.getBoolean("night_map", false),
            drivingView = prefs.getBoolean("driving_view", true),
            detailedTheme = prefs.getBoolean("detailed_theme", true),
            showTelemetry = prefs.getBoolean("show_telemetry", true),
            showPrimaryActions = prefs.getBoolean("show_primary_actions", true),
            showMapScale = prefs.getBoolean("show_map_scale", true),
            telemetrySlot = telemetrySlot,
            primaryActionsSlot = loadSlot("primary_actions_slot", MapOverlaySlot.TOP_START),
            dockSlot = loadSlot("dock_slot", MapOverlaySlot.CENTER_END),
            scaleSlot = loadSlot("scale_slot", MapOverlaySlot.BOTTOM_START)
        )
    }

    private fun loadSlot(key: String, fallback: MapOverlaySlot): MapOverlaySlot =
        try { MapOverlaySlot.valueOf(prefs.getString(key, fallback.name) ?: fallback.name) } catch (_: Exception) { fallback }

    private fun loadKinds(): Map<String, OffroadPlaceKind> {
        return try {
            val root = JSONObject(prefs.getString(KEY_KINDS, "{}") ?: "{}")
            val out = mutableMapOf<String, OffroadPlaceKind>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val kind = try { OffroadPlaceKind.valueOf(root.optString(id, OffroadPlaceKind.FLAG.name)) } catch (_: Exception) { OffroadPlaceKind.FLAG }
                out[id] = kind
            }
            out
        } catch (_: Exception) { emptyMap() }
    }

    private fun loadExtraPlaces(): List<EnhancedSavedPlace> {
        return try {
            val arr = JSONArray(prefs.getString(KEY_EXTRA_PLACES, "[]") ?: "[]")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val kind = try { OffroadPlaceKind.valueOf(o.optString("kind", OffroadPlaceKind.FLAG.name)) } catch (_: Exception) { OffroadPlaceKind.FLAG }
                    add(EnhancedSavedPlace(
                        id = o.getString("id"),
                        name = o.optString("name", "نقطة محفوظة"),
                        latitude = o.getDouble("lat"),
                        longitude = o.getDouble("lon"),
                        kind = kind,
                        notes = o.optString("notes", ""),
                        favorite = o.optBoolean("favorite", false),
                        createdAt = o.optLong("time", 0L)
                    ))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun persistExtraPlaces() {
        val arr = JSONArray()
        _extraPlaces.value.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("lat", p.latitude); put("lon", p.longitude)
                put("kind", p.kind.name); put("notes", p.notes); put("favorite", p.favorite); put("time", p.createdAt)
            })
        }
        prefs.edit().putString(KEY_EXTRA_PLACES, arr.toString()).apply()
    }

    companion object {
        private val SHARED_LOCK = Any()
        @Volatile private var sharedExtraPlaces: MutableStateFlow<List<EnhancedSavedPlace>>? = null
        @Volatile private var sharedPlaceKinds: MutableStateFlow<Map<String, OffroadPlaceKind>>? = null
        @Volatile private var sharedUi: MutableStateFlow<EnhancedMapUiPreferences>? = null

        private const val KEY_KINDS = "place_kinds"
        private const val KEY_EXTRA_PLACES = "extra_places"
        private const val KEY_ABSOLUTE_OVERLAY_SLOTS = "absolute_overlay_slots_v203"
    }
}
