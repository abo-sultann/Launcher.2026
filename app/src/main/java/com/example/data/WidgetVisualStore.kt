package com.example.data

import android.content.Context
import com.example.model.WidgetItem
import com.example.model.ScreenSaverWidgetLayout
import com.example.model.WidgetSurfaceStyle

/**
 * One-time migration bridge for 1.0.8. New versions store geometry and appearance in the
 * canonical widget/screen-saver JSON, so values cannot drift between two preference files.
 */
class WidgetVisualStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("launcher_widget_v2_visuals", Context.MODE_PRIVATE)
    private val previews = mutableMapOf<String, FloatArray>()

    fun decorate(item: WidgetItem): WidgetItem {
        val surface = try {
            prefs.getString("surface_${item.id}", null)?.let(WidgetSurfaceStyle::valueOf)
        } catch (_: Exception) { null } ?: item.surfaceStyle
        val border = if (prefs.contains("border_${item.id}")) prefs.getBoolean("border_${item.id}", false) else item.showBorder
        val preview = previews[item.id]
        val hasStoredGeometry = prefs.contains("x_${item.id}") && prefs.contains("y_${item.id}") && prefs.contains("w_${item.id}") && prefs.contains("h_${item.id}")
        val geometry = preview ?: if (hasStoredGeometry) {
            floatArrayOf(
                prefs.getFloat("x_${item.id}", item.xFraction),
                prefs.getFloat("y_${item.id}", item.yFraction),
                prefs.getFloat("w_${item.id}", item.widthFraction),
                prefs.getFloat("h_${item.id}", item.heightFraction)
            )
        } else null
        return if (geometry == null) {
            item.copy(
                surfaceStyle = surface,
                showBorder = border,
                foregroundColorArgb = getForegroundColorArgb(item.id) ?: item.foregroundColorArgb,
                accentColorArgb = getAccentColorArgb(item.id) ?: item.accentColorArgb,
                surfaceOpacity = if (prefs.contains("surface_opacity_${item.id}")) getSurfaceOpacity(item.id) else item.surfaceOpacity
            )
        } else {
            item.copy(
                xFraction = geometry[0],
                yFraction = geometry[1],
                widthFraction = geometry[2],
                heightFraction = geometry[3],
                surfaceStyle = surface,
                showBorder = border,
                foregroundColorArgb = getForegroundColorArgb(item.id) ?: item.foregroundColorArgb,
                accentColorArgb = getAccentColorArgb(item.id) ?: item.accentColorArgb,
                surfaceOpacity = if (prefs.contains("surface_opacity_${item.id}")) getSurfaceOpacity(item.id) else item.surfaceOpacity
            )
        }
    }

    fun decorate(layout: ScreenSaverWidgetLayout): ScreenSaverWidgetLayout {
        val id = "screensaver_${layout.type.name.lowercase()}"
        val surface = try { prefs.getString("surface_$id", null)?.let(WidgetSurfaceStyle::valueOf) } catch (_: Exception) { null }
        return layout.copy(
            surfaceStyle = surface ?: layout.surfaceStyle,
            showBorder = if (prefs.contains("border_$id")) prefs.getBoolean("border_$id", false) else layout.showBorder,
            foregroundColorArgb = getForegroundColorArgb(id) ?: layout.foregroundColorArgb,
            accentColorArgb = getAccentColorArgb(id) ?: layout.accentColorArgb,
            surfaceOpacity = if (prefs.contains("surface_opacity_$id")) getSurfaceOpacity(id) else layout.surfaceOpacity
        )
    }

    fun previewGeometry(widgetId: String, x: Float, y: Float, width: Float, height: Float) {
        previews[widgetId] = normalize(x, y, width, height)
    }

    fun commitGeometry(widgetId: String) {
        val g = previews.remove(widgetId) ?: return
        prefs.edit()
            .putFloat("x_$widgetId", g[0])
            .putFloat("y_$widgetId", g[1])
            .putFloat("w_$widgetId", g[2])
            .putFloat("h_$widgetId", g[3])
            .apply()
    }

    fun setGeometry(widgetId: String, x: Float, y: Float, width: Float, height: Float) {
        val g = normalize(x, y, width, height)
        previews.remove(widgetId)
        prefs.edit()
            .putFloat("x_$widgetId", g[0])
            .putFloat("y_$widgetId", g[1])
            .putFloat("w_$widgetId", g[2])
            .putFloat("h_$widgetId", g[3])
            .apply()
    }

    fun setSurface(widgetId: String, surface: WidgetSurfaceStyle) {
        prefs.edit().putString("surface_$widgetId", surface.name).apply()
    }

    fun getSurface(widgetId: String, fallback: WidgetSurfaceStyle): WidgetSurfaceStyle = try {
        prefs.getString("surface_$widgetId", null)?.let(WidgetSurfaceStyle::valueOf) ?: fallback
    } catch (_: Exception) { fallback }

    fun setBorder(widgetId: String, enabled: Boolean) {
        prefs.edit().putBoolean("border_$widgetId", enabled).apply()
    }

    fun getBorder(widgetId: String): Boolean = prefs.getBoolean("border_$widgetId", false)

    fun getSurfaceOpacity(widgetId: String): Float =
        prefs.getFloat("surface_opacity_$widgetId", 1f).coerceIn(.25f, 1f)

    fun setSurfaceOpacity(widgetId: String, opacity: Float) {
        prefs.edit().putFloat("surface_opacity_$widgetId", opacity.coerceIn(.25f, 1f)).apply()
    }

    fun getForegroundColorArgb(widgetId: String): Int? =
        if (prefs.contains("foreground_$widgetId")) prefs.getInt("foreground_$widgetId", 0) else null

    fun setForegroundColorArgb(widgetId: String, argb: Int?) {
        val edit = prefs.edit()
        if (argb == null) edit.remove("foreground_$widgetId") else edit.putInt("foreground_$widgetId", argb)
        edit.apply()
    }

    fun getAccentColorArgb(widgetId: String): Int? =
        if (prefs.contains("accent_$widgetId")) prefs.getInt("accent_$widgetId", 0) else null

    fun setAccentColorArgb(widgetId: String, argb: Int?) {
        val edit = prefs.edit()
        if (argb == null) edit.remove("accent_$widgetId") else edit.putInt("accent_$widgetId", argb)
        edit.apply()
    }

    fun remove(widgetId: String) {
        previews.remove(widgetId)
        prefs.edit()
            .remove("surface_$widgetId")
            .remove("border_$widgetId")
            .remove("foreground_$widgetId")
            .remove("accent_$widgetId")
            .remove("surface_opacity_$widgetId")
            .remove("x_$widgetId")
            .remove("y_$widgetId")
            .remove("w_$widgetId")
            .remove("h_$widgetId")
            .apply()
    }

    fun reset() {
        previews.clear()
        prefs.edit().clear().apply()
    }

    private fun normalize(x: Float, y: Float, width: Float, height: Float): FloatArray {
        val w = width.coerceIn(MIN_WIDTH, 1f)
        val h = height.coerceIn(MIN_HEIGHT, 1f)
        val nx = x.coerceIn(0f, (1f - w).coerceAtLeast(0f))
        val ny = y.coerceIn(0f, (1f - h).coerceAtLeast(0f))
        return floatArrayOf(nx, ny, w, h)
    }

    companion object {
        private const val MIN_WIDTH = .07f
        private const val MIN_HEIGHT = .07f
    }
}
