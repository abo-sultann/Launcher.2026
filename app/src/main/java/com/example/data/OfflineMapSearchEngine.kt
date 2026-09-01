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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Location-first offline search for Mapsforge .map files.
 *
 * The old whole-country scan started at one edge of the file and could reach its memory limit
 * before indexing the driver's region. This index scans outward from the current GPS tile,
 * reads full nearby tile data (including unnamed classified services), and uses the lighter
 * named-item reader farther away.
 */
class OfflineMapSearchEngine {
    private var indexedKey: String? = null
    private var mapIndex: List<OfflineMapSearchResult> = emptyList()

    @Synchronized
    fun clear() {
        indexedKey = null
        mapIndex = emptyList()
    }

    @Synchronized
    fun search(
        query: String,
        activeMap: MapItem?,
        savedPlaces: List<SavedOffroadPlace>,
        currentLatitude: Double? = null,
        currentLongitude: Double? = null,
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
            ensureIndexed(activeMap, currentLatitude, currentLongitude)
        } else if (activeMap?.filePath != indexedKey) {
            clear()
        }

        return (saved + mapIndex)
            .asSequence()
            .map { result ->
                val searchable = normalize("${result.name} ${result.source}")
                val distance = distanceMetersOrNull(
                    currentLatitude,
                    currentLongitude,
                    result.latitude,
                    result.longitude
                )
                RankedResult(result.copy(distanceMeters = distance), matchScore(searchable, q), distance)
            }
            .filter { it.score > 0 }
            .sortedWith(
                compareByDescending<RankedResult> { it.score }
                    .thenBy { it.distanceMeters ?: Float.MAX_VALUE }
                    .thenBy { it.result.name.length }
            )
            .map { it.result }
            .distinctBy {
                "${normalize(it.name)}:${(it.latitude * 10000).roundToInt()}:${(it.longitude * 10000).roundToInt()}"
            }
            .take(limit.coerceIn(5, 80))
            .toList()
    }

    private fun matchScore(name: String, query: String): Int = when {
        name == query -> 100
        name.startsWith(query) -> 85
        name.contains(query) -> 65
        query.split(' ').all { it.isNotBlank() && name.contains(it) } -> 45
        else -> 0
    }

    private fun ensureIndexed(mapItem: MapItem, latitude: Double?, longitude: Double?) {
        val file = File(mapItem.filePath)
        if (!file.exists()) return
        val focus = focusTile(file, latitude, longitude)
        val key = "${mapItem.filePath}:${file.length()}:${file.lastModified()}:${focus.first}:${focus.second}"
        if (indexedKey == key) return
        mapIndex = buildIndex(mapItem, latitude, longitude)
        indexedKey = key
    }

    private fun focusTile(file: File, latitude: Double?, longitude: Double?): Pair<Int, Int> {
        var mapFile: MapFile? = null
        return try {
            mapFile = MapFile(file, MAP_LANGUAGE_ARABIC)
            val info = mapFile.mapFileInfo
            val zoom = INDEX_ZOOM.coerceIn(info.zoomLevelMin.toInt(), info.zoomLevelMax.toInt()).toByte()
            val bbox = mapFile.boundingBox()
            val lat = latitude?.takeIf { it in bbox.minLatitude..bbox.maxLatitude } ?: bbox.centerPoint.latitude
            val lon = longitude?.takeIf { it in bbox.minLongitude..bbox.maxLongitude } ?: bbox.centerPoint.longitude
            MercatorProjection.longitudeToTileX(lon, zoom) to MercatorProjection.latitudeToTileY(lat, zoom)
        } catch (_: Exception) {
            0 to 0
        } finally {
            try { mapFile?.close() } catch (_: Exception) { }
        }
    }

    private fun buildIndex(
        mapItem: MapItem,
        currentLatitude: Double?,
        currentLongitude: Double?
    ): List<OfflineMapSearchResult> {
        val file = File(mapItem.filePath)
        if (!file.exists()) return emptyList()

        val output = LinkedHashMap<String, OfflineMapSearchResult>()
        var mapFile: MapFile? = null
        try {
            mapFile = MapFile(file, MAP_LANGUAGE_ARABIC)
            val info = mapFile.mapFileInfo
            val bbox = mapFile.boundingBox()
            val zoom = INDEX_ZOOM.coerceIn(info.zoomLevelMin.toInt(), info.zoomLevelMax.toInt()).toByte()
            val tileSize = info.tilePixelSize.coerceAtLeast(128)
            val minX = MercatorProjection.longitudeToTileX(bbox.minLongitude, zoom)
            val maxX = MercatorProjection.longitudeToTileX(bbox.maxLongitude, zoom)
            val minY = MercatorProjection.latitudeToTileY(bbox.maxLatitude, zoom)
            val maxY = MercatorProjection.latitudeToTileY(bbox.minLatitude, zoom)

            val focusLat = currentLatitude?.takeIf { it in bbox.minLatitude..bbox.maxLatitude } ?: bbox.centerPoint.latitude
            val focusLon = currentLongitude?.takeIf { it in bbox.minLongitude..bbox.maxLongitude } ?: bbox.centerPoint.longitude
            val focusX = MercatorProjection.longitudeToTileX(focusLon, zoom).coerceIn(minX, maxX)
            val focusY = MercatorProjection.latitudeToTileY(focusLat, zoom).coerceIn(minY, maxY)

            val orderedTiles = ArrayList<Pair<Int, Int>>(((maxX - minX + 1) * (maxY - minY + 1)).toInt())
            for (x in minX..maxX) for (y in minY..maxY) orderedTiles.add(x to y)
            orderedTiles.sortBy { (x, y) ->
                val dx = x - focusX
                val dy = y - focusY
                dx * dx + dy * dy
            }

            for ((x, y) in orderedTiles) {
                if (output.size >= MAX_INDEX_ITEMS) break
                val tile = Tile(x, y, zoom, tileSize)
                val nearby = abs(x - focusX) <= NEARBY_RADIUS_TILES && abs(y - focusY) <= NEARBY_RADIUS_TILES
                val result = try {
                    if (nearby) mapFile.readMapData(tile) else mapFile.readNamedItems(tile)
                } catch (_: Exception) {
                    null
                } ?: continue

                result.pois.forEach { poi ->
                    if (output.size < MAX_INDEX_ITEMS) addItem(output, poi.tags, poi.position, "معلم")
                }
                result.ways.forEach { way ->
                    if (output.size < MAX_INDEX_ITEMS) {
                        val position = way.labelPosition ?: way.latLongs.firstOrNull()?.firstOrNull()
                        if (position != null) addItem(output, way.tags, position, "اسم على الخريطة")
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

    private fun addItem(
        output: LinkedHashMap<String, OfflineMapSearchResult>,
        tags: List<Tag>,
        position: LatLong,
        fallbackSource: String
    ) {
        val source = classify(tags, fallbackSource)
        val arabic = tags.firstOrNull { it.key.equals("name:ar", true) }?.value
        val plain = tags.firstOrNull { it.key.equals("name", true) }?.value
        val english = tags.firstOrNull { it.key.equals("name:en", true) }?.value
        val explicitName = listOf(arabic, plain, english).firstOrNull { !it.isNullOrBlank() }?.trim()
        val name = explicitName ?: source.takeIf { it in GENERIC_SERVICE_NAMES } ?: return
        if (name.length < 2) return

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

    private fun classify(tags: List<Tag>, fallbackSource: String): String {
        fun value(key: String) = tags.firstOrNull { it.key.equals(key, true) }?.value?.lowercase(Locale.ROOT)
        val amenity = value("amenity")
        val healthcare = value("healthcare")
        val shop = value("shop")
        val tourism = value("tourism")
        val office = value("office")
        val place = value("place")
        val highway = value("highway")
        val waterway = value("waterway")
        val natural = value("natural")
        val leisure = value("leisure")
        val emergency = value("emergency")
        val publicTransport = value("public_transport")

        return when {
            amenity == "fuel" -> "محطة وقود"
            amenity in setOf("restaurant", "fast_food", "food_court") -> "مطعم"
            amenity == "cafe" -> "مقهى"
            amenity in setOf("hospital", "clinic", "doctors") || healthcare in setOf("hospital", "clinic", "doctor") -> "مستشفى/عيادة"
            amenity == "pharmacy" || healthcare == "pharmacy" -> "صيدلية"
            amenity == "place_of_worship" -> "مسجد/دار عبادة"
            amenity in setOf("bank", "atm") -> "بنك/صراف"
            amenity in setOf("school", "college", "university", "kindergarten") -> "تعليم"
            amenity in setOf("police", "fire_station") || emergency != null -> "طوارئ"
            amenity in setOf("parking", "parking_entrance") -> "مواقف"
            amenity in setOf("car_wash", "car_repair") -> "خدمات سيارات"
            publicTransport != null || amenity == "bus_station" -> "نقل"
            shop != null -> when (shop) {
                "supermarket", "convenience", "grocery" -> "تموينات/سوبرماركت"
                "mall", "department_store" -> "سوق/مجمع تجاري"
                else -> "محل تجاري"
            }
            tourism != null -> "سياحة/إقامة"
            leisure != null -> "ترفيه"
            office != null -> "نشاط تجاري"
            place != null -> when (place) {
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
    }

    private fun distanceMetersOrNull(
        fromLat: Double?,
        fromLon: Double?,
        toLat: Double,
        toLon: Double
    ): Float? {
        if (fromLat == null || fromLon == null) return null
        val earthRadius = 6_371_000.0
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val deltaLat = Math.toRadians(toLat - fromLat)
        val deltaLon = Math.toRadians(toLon - fromLon)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        return (earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))).toFloat()
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace('ؤ', 'و')
        .replace('ئ', 'ي')
        .replace(Regex("[ًٌٍَُِّْـ]"), "")
        .replace(Regex("\\s+"), " ")

    private data class RankedResult(
        val result: OfflineMapSearchResult,
        val score: Int,
        val distanceMeters: Float?
    )

    companion object {
        private const val MAX_INDEX_ITEMS = 28_000
        private const val INDEX_ZOOM = 9
        private const val NEARBY_RADIUS_TILES = 10
        private const val MAP_LANGUAGE_ARABIC = "ar"
        private val GENERIC_SERVICE_NAMES = setOf(
            "محطة وقود", "مطعم", "مقهى", "مستشفى/عيادة", "صيدلية", "مسجد/دار عبادة",
            "بنك/صراف", "تعليم", "طوارئ", "مواقف", "خدمات سيارات", "نقل",
            "تموينات/سوبرماركت", "سوق/مجمع تجاري", "محل تجاري", "سياحة/إقامة", "ترفيه"
        )
    }
}
