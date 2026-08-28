package com.example.data

import android.content.Context
import com.example.model.WidgetItem
import com.example.model.WidgetSurfaceStyle

/**
 * V2 appearance settings live separately from the legacy widget JSON so upgrades do not
 * invalidate an existing home layout. Geometry/style continue to use PreferencesManager.
 */
class WidgetVisualStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("launcher_widget_v2_visuals", Context.MODE_PRIVATE)

    fun decorate(item: WidgetItem): WidgetItem {
        val surface = try {
            prefs.getString("surface_${item.id}", null)?.let(WidgetSurfaceStyle::valueOf)
        } catch (_: Exception) { null } ?: WidgetItem.defaultSurfaceFor(item.type)
        val border = prefs.getBoolean("border_${item.id}", false)
        return item.copy(surfaceStyle = surface, showBorder = border)
    }

    fun saveAppearance(item: WidgetItem) {
        prefs.edit()
            .putString("surface_${item.id}", item.surfaceStyle.name)
            .putBoolean("border_${item.id}", item.showBorder)
            .apply()
    }

    fun setSurface(widgetId: String, surface: WidgetSurfaceStyle) {
        prefs.edit().putString("surface_$widgetId", surface.name).apply()
    }

    fun setBorder(widgetId: String, enabled: Boolean) {
        prefs.edit().putBoolean("border_$widgetId", enabled).apply()
    }

    fun remove(widgetId: String) {
        prefs.edit().remove("surface_$widgetId").remove("border_$widgetId").apply()
    }

    fun reset() = prefs.edit().clear().apply()
}
