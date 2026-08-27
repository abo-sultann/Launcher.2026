package com.example.data

import com.example.model.MapItem
import com.example.model.OfflineMapSearchResult
import com.example.model.SavedOffroadPlace
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Lightweight, lazy offline index for Mapsforge .map files.
 * It reads only named items and keeps a compact in-memory index so the old head unit
 * does not need a database or an Internet connection.
 */
class OfflineMapSearchEngine {
    private var indexedPath: String? = null
    private var mapIndex: List<OfflineMapSearchResult> = emptyList()

    @Synchronized
    fun clear() {
        indexedPath = null
        mapIndex = emptyList()
    }

    @Synchronized
    fun search(
        query: String,
        activeMap: MapItem?,
        savedPlaces: List<SavedOffroadPlace>,
        limit: Int = 40
    ): List<OfflineMapSearchResult> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()

        val saved = savedPlaces.map {
            OfflineMapSearchResult(
                id = "saved:${it.id}",
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                source = "موقع محفوظ"
            )
        }

        if (activeMap != null && activeMap.filePath.endsWith(".map", true) && File(activeMap.filePath).exists()) {
            ensureIndexed(activeMap)
        } else if (activeMap?.filePath != indexedPath) {
            clear()
        }

        return (saved + mapIndex)
            .asSequence()
            .map { result -> result to matchScore(normalize(result.name), q) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<OfflineMapSearchResult, Int>> { it.second }.thenBy { it.first.name.length })
            .map { it.first }
            .distinctBy { "${normalize(it.name)}:${(it.latitude * 10000).roundToInt()}:${(it.longitude * 10000).roundToInt()}" }
            .take(limit.coerceIn(5, 80))
            .toList()
    }

    private fun matchScore(name: String, query: String): Int = when {
        name == query -> 100
        name.startsWith(query) -> 80
        name.contains(query) -> 60
        query.split(' ').all { it.isNotBlank() && name.contains(it) } -> 40
        else -> 0
    }

    private fun ensureIndexed(mapItem: MapItem) {
        if (indexedPath == mapItem.filePath) return
        mapIndex = buildIndex(mapItem)
        indexedPath = mapItem.filePath
    }

    private fun buildIndex(mapItem: MapItem): List<OfflineMapSearchResult> {
        val file = File(mapItem.filePath)
        if (!file.exists()) return emptyList()

        val output = LinkedHashMap<String, OfflineMapSearchResult>()
        var mapFile: MapFile? = null
        try {
            mapFile = MapFile(file)
            val info = mapFile.mapFileInfo
            val bbox = mapFile.boundingBox()
            val preferredZoom = 9
            val zoom = preferredZoom.coerceIn(info.zoomLevelMin.toInt(), info.zoomLevelMax.toInt()).toByte()
            val tileSize = info.tilePixelSize.coerceAtLeast(128)
            val minX = MercatorProjection.longitudeToTileX(bbox.minLongitude, zoom)
            val maxX = MercatorProjection.longitudeToTileX(bbox.maxLongitude, zoom)
            val minY = MercatorProjection.latitudeToTileY(bbox.maxLatitude, zoom)
            val maxY = MercatorProjection.latitudeToTileY(bbox.minLatitude, zoom)

            outer@ for (x in minX..maxX) {
                for (y in minY..maxY) {
                    if (output.size >= MAX_INDEX_ITEMS) break@outer
                    val result = try { mapFile.readNamedItems(Tile(x, y, zoom, tileSize)) } catch (_: Exception) { null } ?: continue

                    result.pois.forEach { poi ->
                        if (output.size < MAX_INDEX_ITEMS) {
                            addNamed(output, poi.tags, poi.position, "معلم")
                        }
                    }
                    result.ways.forEach { way ->
                        if (output.size < MAX_INDEX_ITEMS) {
                            val position = way.labelPosition ?: way.latLongs.firstOrNull()?.firstOrNull()
                            if (position != null) addNamed(output, way.tags, position, "اسم على الخريطة")
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return emptyList()
        } finally {
            try { mapFile?.close() } catch (_: Exception) { }
        }
        return output.values.toList()
    }

    private fun addNamed(
        output: LinkedHashMap<String, OfflineMapSearchResult>,
        tags: List<Tag>,
        position: LatLong,
        fallbackSource: String
    ) {
        val arabic = tags.firstOrNull { it.key.equals("name:ar", true) }?.value
        val plain = tags.firstOrNull { it.key.equals("name", true) }?.value
        val english = tags.firstOrNull { it.key.equals("name:en", true) }?.value
        val name = listOf(arabic, plain, english).firstOrNull { !it.isNullOrBlank() }?.trim() ?: return
        if (name.length < 2) return

        val placeType = tags.firstOrNull { it.key == "place" }?.value
        val waterway = tags.firstOrNull { it.key == "waterway" }?.value
        val highway = tags.firstOrNull { it.key == "highway" }?.value
        val natural = tags.firstOrNull { it.key == "natural" }?.value
        val source = when {
            placeType != null -> when (placeType) {
                "city" -> "مدينة"
                "town" -> "بلدة"
                "village" -> "قرية"
                "hamlet" -> "هجرة/تجمع"
                else -> "مكان"
            }
            waterway != null -> "وادي/مجرى"
            highway in setOf("track", "path", "unclassified") -> "طريق بري"
            natural != null -> "معلم طبيعي"
            else -> fallbackSource
        }
        val key = "${normalize(name)}:${(position.latitude * 10000).roundToInt()}:${(position.longitude * 10000).roundToInt()}"
        output.putIfAbsent(
            key,
            OfflineMapSearchResult(
                id = "map:$key",
                name = name,
                latitude = position.latitude,
                longitude = position.longitude,
                source = source
            )
        )
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace(Regex("\\s+"), " ")

    companion object {
        private const val MAX_INDEX_ITEMS = 8_000
    }
}
