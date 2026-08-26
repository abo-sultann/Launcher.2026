package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.model.MapItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            val saved = preferencesManager.getSavedMaps().toMutableList()
            if (saved.isEmpty()) {
                // Built-in starter offline vector grid map item
                val defaultMap = MapItem(
                    id = "built_in_default_map",
                    name = "خريطة الملاحة الافتراضية (Vector HUD)",
                    filePath = "internal://vector_map",
                    fileSizeFormatted = "12 ميجابايت",
                    isActive = true,
                    dateAdded = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
                )
                saved.add(defaultMap)
                preferencesManager.saveMaps(saved)
            }

            _mapsList.value = saved
            _activeMap.value = saved.find { it.isActive } ?: saved.firstOrNull()
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

            val isMBTiles = file.name.endsWith(".mbtiles", ignoreCase = true)
            if (isMBTiles) {
                // Verify SQLite header safely
                try {
                    val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                    db.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Not a valid SQLite MBTiles file", e)
                    _mapError.value = "ملف MBTiles تالف أو غير صالح"
                    return false
                }
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

            val updated = _mapsList.value.map { it.copy(isActive = false) }.toMutableList()
            updated.add(newMap)

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
        val updated = _mapsList.value.map { item ->
            item.copy(isActive = item.id == mapId)
        }
        _mapsList.value = updated
        _activeMap.value = updated.find { it.isActive }
        preferencesManager.saveMaps(updated)
    }

    fun renameMap(mapId: String, newName: String) {
        if (newName.isBlank()) return
        val updated = _mapsList.value.map { item ->
            if (item.id == mapId) item.copy(name = newName) else item
        }
        _mapsList.value = updated
        _activeMap.value = updated.find { it.isActive }
        preferencesManager.saveMaps(updated)
    }

    fun deleteMap(mapId: String) {
        val current = _mapsList.value
        val toDelete = current.find { it.id == mapId } ?: return

        // Delete underlying file if stored in app files
        if (toDelete.filePath.startsWith(context.filesDir.absolutePath)) {
            try {
                File(toDelete.filePath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete physical map file", e)
            }
        }

        val updated = current.filter { it.id != mapId }
        val newActive = if (toDelete.isActive) {
            updated.firstOrNull()?.let { first ->
                updated.map { if (it.id == first.id) it.copy(isActive = true) else it }
            } ?: emptyList()
        } else {
            updated
        }

        _mapsList.value = newActive
        _activeMap.value = newActive.find { it.isActive }
        preferencesManager.saveMaps(newActive)
    }

    companion object {
        private const val TAG = "OfflineMapEngine"
    }
}
