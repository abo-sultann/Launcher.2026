package com.example.data

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class StableBackupManager(private val context: Context) {

    fun exportBackup(): String {
        val root = JSONObject()
        root.put("format", "Launcher2026-StableBackup")
        root.put("version", 1)
        root.put("appVersion", BuildConfig.VERSION_NAME)
        root.put("createdAt", System.currentTimeMillis())

        val prefsRoot = JSONObject()
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        prefsDir.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
            val name = file.nameWithoutExtension
            val values = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
            val json = JSONObject()
            values.forEach { (key, value) -> json.put(key, encodeValue(value)) }
            prefsRoot.put(name, json)
        }
        root.put("preferences", prefsRoot)

        val track = File(context.filesDir, "offroad_track_rolling.json")
        if (track.exists()) root.put("offroadTrack", track.readText())

        val mapsDir = File(context.filesDir, "maps")
        root.put("mapFiles", JSONArray(mapsDir.listFiles()?.map { it.name } ?: emptyList<String>()))
        return root.toString(2)
    }

    fun importBackup(raw: String): Int {
        val root = JSONObject(raw)
        if (root.optString("format") != "Launcher2026-StableBackup") return 0
        var restored = 0
        val prefsRoot = root.optJSONObject("preferences") ?: JSONObject()
        val names = prefsRoot.keys()
        while (names.hasNext()) {
            val name = names.next()
            val objectValues = prefsRoot.optJSONObject(name) ?: continue
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            val keys = objectValues.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val encoded = objectValues.optJSONObject(key) ?: continue
                decodeInto(editor, key, encoded)
                restored++
            }
            editor.commit()
        }

        if (root.has("offroadTrack")) {
            File(context.filesDir, "offroad_track_rolling.json").writeText(root.optString("offroadTrack", "[]"))
            restored++
        }
        return restored
    }

    fun exportDiagnostics(): String {
        val report = DiagnosticManager(context, PreferencesManager(context)).runFullDiagnostics()
        val runtime = Runtime.getRuntime()
        val freeStorage = context.filesDir.usableSpace / (1024L * 1024L)
        val totalStorage = context.filesDir.totalSpace / (1024L * 1024L)
        val components = listOf(
            report.homeStatus,
            report.gpsStatus,
            report.musicStatus,
            report.mapsStatus,
            report.databaseStatus,
            report.widgetsStatus,
            report.storageStatus
        )
        return buildString {
            appendLine("Launcher 2026 — تقرير التشخيص")
            appendLine("الإصدار: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            appendLine("الجهاز: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("RAM JVM: ${(runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)} MB مستخدم من ${runtime.maxMemory() / (1024 * 1024)} MB")
            appendLine("التخزين: $freeStorage MB متاح من $totalStorage MB")
            appendLine("عدد الأعطال المسجلة: ${report.crashCount}")
            report.lastCrashMessage?.let { appendLine("آخر عطل: $it") }
            appendLine()
            components.forEach { appendLine("${it.nameArabic}: ${it.status.arabicLabel} — ${it.details}") }
        }
    }

    private fun encodeValue(value: Any?): JSONObject = JSONObject().apply {
        when (value) {
            is String -> { put("t", "s"); put("v", value) }
            is Int -> { put("t", "i"); put("v", value) }
            is Long -> { put("t", "l"); put("v", value) }
            is Float -> { put("t", "f"); put("v", value.toDouble()) }
            is Boolean -> { put("t", "b"); put("v", value) }
            is Set<*> -> { put("t", "ss"); put("v", JSONArray(value.filterIsInstance<String>())) }
            else -> { put("t", "s"); put("v", value?.toString() ?: "") }
        }
    }

    private fun decodeInto(editor: android.content.SharedPreferences.Editor, key: String, encoded: JSONObject) {
        when (encoded.optString("t")) {
            "s" -> editor.putString(key, encoded.optString("v"))
            "i" -> editor.putInt(key, encoded.optInt("v"))
            "l" -> editor.putLong(key, encoded.optLong("v"))
            "f" -> editor.putFloat(key, encoded.optDouble("v").toFloat())
            "b" -> editor.putBoolean(key, encoded.optBoolean("v"))
            "ss" -> {
                val arr = encoded.optJSONArray("v") ?: JSONArray()
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) set += arr.optString(i)
                editor.putStringSet(key, set)
            }
        }
    }
}
