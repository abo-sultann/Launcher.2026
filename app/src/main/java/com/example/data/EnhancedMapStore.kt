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

data class EnhancedSavedPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: OffroadPlaceKind = OffroadPlaceKind.FLAG,
    val createdAt: Long = System.currentTimeMillis()
)

data class EnhancedMapUiPreferences(
    val trackVisible: Boolean = true,
    val trackWidth: Float = 6f,
    val nightMap: Boolean = false,
    val drivingView: Boolean = true,
    val detailedTheme: Boolean = true
)

class EnhancedMapStore(context: Context) {
    private val prefs = context.getSharedPreferences("enhanced_offroad_map_2026", Context.MODE_PRIVATE)

    private val _extraPlaces = MutableStateFlow(loadExtraPlaces())
    val extraPlaces: StateFlow<List<EnhancedSavedPlace>> = _extraPlaces.asStateFlow()

    private val _placeKinds = MutableStateFlow(loadKinds())
    val placeKinds: StateFlow<Map<String, OffroadPlaceKind>> = _placeKinds.asStateFlow()

    private val _ui = MutableStateFlow(loadUi())
    val ui: StateFlow<EnhancedMapUiPreferences> = _ui.asStateFlow()

    fun saveExtraPlace(name: String, latitude: Double, longitude: Double, kind: OffroadPlaceKind): EnhancedSavedPlace {
        val place = EnhancedSavedPlace(
            id = "map_${UUID.randomUUID()}",
            name = name.trim().ifBlank { "نقطة محفوظة" },
            latitude = latitude,
            longitude = longitude,
            kind = kind
        )
        _extraPlaces.value = _extraPlaces.value + place
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
            .apply()
    }

    private fun loadUi() = EnhancedMapUiPreferences(
        trackVisible = prefs.getBoolean("track_visible", true),
        trackWidth = prefs.getFloat("track_width", 6f).coerceIn(3f, 12f),
        nightMap = prefs.getBoolean("night_map", false),
        drivingView = prefs.getBoolean("driving_view", true),
        detailedTheme = prefs.getBoolean("detailed_theme", true)
    )

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
                put("kind", p.kind.name); put("time", p.createdAt)
            })
        }
        prefs.edit().putString(KEY_EXTRA_PLACES, arr.toString()).apply()
    }

    companion object {
        private const val KEY_KINDS = "place_kinds"
        private const val KEY_EXTRA_PLACES = "extra_places"
    }
}
