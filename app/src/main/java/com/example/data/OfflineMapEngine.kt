package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.model.MapItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OfflineMapEngine(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val _mapsList = MutableStateFlow<List<MapItem>>(emptyList())
    val mapsList: StateFlow<List<MapItem>> = _mapsList.asStateFlow()

    private val _activeMap = MutableStateFlow<MapItem?>(null)
    val activeMap: StateFlow<MapItem?> = _activeMap.asStateFlow()

    private val _mapError = MutableStateFlow<String?>(null)
    val mapError: StateFlow<String?> = _mapError.asStateFlow()

    fun initialize() {
        try {
            val saved = preferencesManager.getSavedMaps()
                .filterNot { it.id == "built_in_default_map" || it.filePath.startsWith("internal://") }
                .filter { File(it.filePath).exists() }
                .toMutableList()

            var normalized = if (saved.count { it.isActive } > 1) {
                var activeFound = false
                saved.map { item ->
                    if (item.isActive && !activeFound) {
                        activeFound = true
                        item
                    } else item.copy(isActive = false)
                }
            } else saved

            val activeCandidate = normalized.find { it.isActive }
            val activeError = activeCandidate?.let { validateMapFile(File(it.filePath)) }
            if (activeError != null) normalized = normalized.map { it.copy(isActive = false) }

            _mapsList.value = normalized
            _activeMap.value = normalized.find { it.isActive }
            preferencesManager.saveMaps(normalized)
            _mapError.value = activeError
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MapEngine", e)
            _mapError.value = "تعذر تحميل قائمة الخرائط"
        }
    }

    fun importMapFile(file: File, customName: String? = null): Boolean {
        return try {
            if (!file.exists() || file.length() == 0L) {
                _mapError.value = "الملف غير موجود أو فارغ"
                return false
            }

            val validationError = validateMapFile(file)
            if (validationError != null) {
                _mapError.value = validationError
                return false
            }

            val sizeMb = file.length() / (1024 * 1024)
            val formattedSize = if (sizeMb > 0) "$sizeMb ميجابايت" else "${file.length() / 1024} كيلوبايت"
            val mapName = customName?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension

            val newMap = MapItem(
                id = UUID.randomUUID().toString(),
                name = mapName,
                filePath = file.absolutePath,
                fileSizeFormatted = formattedSize,
                isActive = true,
                dateAdded = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            )

            val updated = _mapsList.value.map { it.copy(isActive = false) } + newMap
            _mapsList.value = updated
            _activeMap.value = newMap
            _mapError.value = null
            preferencesManager.saveMaps(updated)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing map", e)
            _mapError.value = "تعذر استيراد ملف الخريطة"
            false
        }
    }

    fun setActiveMap(mapId: String) {
        val requested = _mapsList.value.firstOrNull { it.id == mapId } ?: return
        val validationError = validateMapFile(File(requested.filePath))
        if (validationError != null) {
            _mapError.value = validationError
            return
        }
        val updated = _mapsList.value.map { item -> item.copy(isActive = item.id == mapId) }
        _mapsList.value = updated
        _activeMap.value = updated.find { it.isActive }
        preferencesManager.saveMaps(updated)
        _mapError.value = null
    }

    fun renameMap(mapId: String, newName: String) {
        if (newName.isBlank()) return
        val updated = _mapsList.value.map { item -> if (item.id == mapId) item.copy(name = newName) else item }
        _mapsList.value = updated
        _activeMap.value = updated.find { it.isActive }
        preferencesManager.saveMaps(updated)
    }

    fun deleteMap(mapId: String) {
        val current = _mapsList.value
        val toDelete = current.find { it.id == mapId } ?: return

        if (toDelete.filePath.startsWith(context.filesDir.absolutePath)) {
            try { File(toDelete.filePath).delete() } catch (e: Exception) { Log.w(TAG, "Could not delete physical map file", e) }
        }

        val remaining = current.filter { it.id != mapId }
        val normalized = if (toDelete.isActive && remaining.isNotEmpty()) {
            remaining.mapIndexed { index, item -> item.copy(isActive = index == 0) }
        } else remaining

        _mapsList.value = normalized
        _activeMap.value = normalized.find { it.isActive }
        preferencesManager.saveMaps(normalized)
    }

    fun setMapError(message: String?) {
        _mapError.value = message
    }

    private fun validateMapFile(file: File): String? {
        if (!file.exists() || file.length() <= 0L) return "ملف الخريطة غير موجود أو فارغ"
        return when (file.extension.lowercase(Locale.US)) {
            "mbtiles" -> validateMbTiles(file)
            "map" -> validateMapsforge(file)
            else -> "اختر ملف MBTiles أو Mapsforge (.map)"
        }
    }

    private fun validateMbTiles(file: File): String? {
        var db: SQLiteDatabase? = null
        return try {
            val opened = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            db = opened
            opened.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tiles'", null).use { cursor ->
                if (!cursor.moveToFirst()) throw IllegalArgumentException("tiles table missing")
            }
            val format = opened.rawQuery("SELECT value FROM metadata WHERE name='format' LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.lowercase(Locale.US) else null
            }
            if (format !in SUPPORTED_MBTILES_FORMATS) "اختر MBTiles صوريًا بصيغة PNG أو JPG" else null
        } catch (e: Exception) {
            Log.w(TAG, "Not a valid SQLite MBTiles file", e)
            "ملف MBTiles تالف أو غير صالح"
        } finally {
            try { db?.close() } catch (_: Exception) { }
        }
    }

    private fun validateMapsforge(file: File): String? {
        var mapFile: MapFile? = null
        return try {
            mapFile = MapFile(file, MAP_LANGUAGE_ARABIC)
            mapFile.boundingBox()
            null
        } catch (e: Exception) {
            Log.w(TAG, "Not a valid Mapsforge map file", e)
            "ملف Mapsforge غير صالح"
        } finally {
            try { mapFile?.close() } catch (_: Exception) { }
        }
    }

    companion object {
        private const val TAG = "OfflineMapEngine"
        private const val MAP_LANGUAGE_ARABIC = "ar"
        private val SUPPORTED_MBTILES_FORMATS = setOf("png", "jpg", "jpeg")
    }
}
