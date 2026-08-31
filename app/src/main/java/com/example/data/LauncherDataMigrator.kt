package com.example.data

import android.content.SharedPreferences
import android.util.Log
import com.example.model.WidgetTone
import org.json.JSONArray

/**
 * One-way, idempotent migration from Launcher 1.x preferences to the rebuilt data contract.
 * Geometry, selected widget type/style, wallpaper, maps, trips and music resume state are kept.
 */
internal class LauncherDataMigrator(private val prefs: SharedPreferences) {

    fun migrateIfNeeded() {
        val current = prefs.getInt(KEY_SCHEMA_VERSION, 0)
        if (current >= CURRENT_SCHEMA_VERSION) return

        try {
            val editor = prefs.edit()

            if (current < 1) {
                if (!prefs.contains("show_app_labels") && prefs.contains("show_app_names")) {
                    editor.putBoolean("show_app_labels", prefs.getBoolean("show_app_names", true))
                }
                prefs.getString("widgets_json", null)?.takeIf { it.isNotBlank() }?.let {
                    if (!prefs.contains("widgets_json_1x_backup")) editor.putString("widgets_json_1x_backup", it)
                }
                prefs.getString("screensaver_layouts_json", null)?.takeIf { it.isNotBlank() }?.let {
                    if (!prefs.contains("screensaver_layouts_1x_backup")) editor.putString("screensaver_layouts_1x_backup", it)
                }
            }

            if (current < 2) {
                prefs.getString("widgets_json", null)?.let(::normalizeWidgetColours)?.let {
                    editor.putString("widgets_json", it)
                }
                prefs.getString("screensaver_layouts_json", null)?.let(::normalizeScreenSaverColours)?.let {
                    editor.putString("screensaver_layouts_json", it)
                }
            }

            if (current < 3) {
                // Removed concepts. Leaving these aliases around caused later settings saves to
                // resurrect UI that no longer exists.
                editor.remove("show_app_names")
                editor.remove("screensaver_night_mode")
                editor.remove("screensaver_night_brightness")
            }

            editor.putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION).commit()
        } catch (t: Throwable) {
            // Reading remains backwards compatible, so a migration error must never block Home.
            Log.e(TAG, "Preference migration failed", t)
        }
    }

    private fun normalizeWidgetColours(raw: String): String = normalizeColours(raw, "foregroundColorArgb", "accentColorArgb")

    private fun normalizeScreenSaverColours(raw: String): String = normalizeColours(raw, "foreground", "accent")

    private fun normalizeColours(raw: String, foregroundKey: String, accentKey: String): String {
        val array = JSONArray(raw)
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val old = if (item.has(foregroundKey) && !item.isNull(foregroundKey)) item.optInt(foregroundKey) else null
            val tone = if (old != null && isDark(old)) WidgetTone.BLACK else WidgetTone.WHITE
            item.put(foregroundKey, tone.argb)
            item.put(accentKey, tone.argb)
        }
        return array.toString()
    }

    private fun isDark(argb: Int): Boolean {
        val red = (argb shr 16) and 0xff
        val green = (argb shr 8) and 0xff
        val blue = argb and 0xff
        return red * 299 + green * 587 + blue * 114 < 128_000
    }

    companion object {
        private const val TAG = "LauncherDataMigration"
        private const val KEY_SCHEMA_VERSION = "launcher_data_schema"
        private const val CURRENT_SCHEMA_VERSION = 3
    }
}
