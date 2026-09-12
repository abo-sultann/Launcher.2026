package com.example.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Calendar

/**
 * Lightweight, offline display automation for the Android 7 head unit.
 *
 * The controller owns only brightness/night-mode preferences so it can work even before the
 * Compose settings model is created. System brightness is changed only after the user grants
 * WRITE_SETTINGS through Android's special-access screen.
 */
class DisplayAutomationController(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Config(
        val enabled: Boolean = true,
        val startHour: Int = 18,
        val endHour: Int = 6,
        val nightBrightnessPercent: Int = 28,
        val extraDimPercent: Int = 14,
        val safeDrivingEnabled: Boolean = true,
        val safeDrivingThresholdKmH: Int = 15,
    )

    enum class ManualOverride { AUTO, FORCE_NIGHT, FORCE_DAY }

    fun readConfig(): Config = Config(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        startHour = prefs.getInt(KEY_START_HOUR, 18).coerceIn(0, 23),
        endHour = prefs.getInt(KEY_END_HOUR, 6).coerceIn(0, 23),
        nightBrightnessPercent = prefs.getInt(KEY_NIGHT_BRIGHTNESS, 28).coerceIn(10, 70),
        extraDimPercent = prefs.getInt(KEY_EXTRA_DIM, 14).coerceIn(0, 45),
        safeDrivingEnabled = prefs.getBoolean(KEY_SAFE_DRIVING, true),
        safeDrivingThresholdKmH = prefs.getInt(KEY_SAFE_DRIVING_THRESHOLD, 15).coerceIn(5, 40),
    )

    fun saveConfig(config: Config) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_START_HOUR, config.startHour.coerceIn(0, 23))
            .putInt(KEY_END_HOUR, config.endHour.coerceIn(0, 23))
            .putInt(KEY_NIGHT_BRIGHTNESS, config.nightBrightnessPercent.coerceIn(10, 70))
            .putInt(KEY_EXTRA_DIM, config.extraDimPercent.coerceIn(0, 45))
            .putBoolean(KEY_SAFE_DRIVING, config.safeDrivingEnabled)
            .putInt(KEY_SAFE_DRIVING_THRESHOLD, config.safeDrivingThresholdKmH.coerceIn(5, 40))
            .apply()
        applyNow(config)
    }

    fun hasWriteSettingsPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(appContext)

    fun manualOverride(): ManualOverride = when (prefs.getInt(KEY_OVERRIDE, 0)) {
        1 -> ManualOverride.FORCE_NIGHT
        2 -> ManualOverride.FORCE_DAY
        else -> ManualOverride.AUTO
    }

    fun toggleQuickNight(): ManualOverride {
        val next = if (manualOverride() == ManualOverride.FORCE_NIGHT) ManualOverride.AUTO else ManualOverride.FORCE_NIGHT
        prefs.edit().putInt(KEY_OVERRIDE, if (next == ManualOverride.FORCE_NIGHT) 1 else 0).apply()
        applyNow()
        return next
    }

    fun useAutomaticSchedule() {
        prefs.edit().putInt(KEY_OVERRIDE, 0).apply()
        applyNow()
    }

    fun isNightActive(config: Config = readConfig(), calendar: Calendar = Calendar.getInstance()): Boolean {
        return when (manualOverride()) {
            ManualOverride.FORCE_NIGHT -> true
            ManualOverride.FORCE_DAY -> false
            ManualOverride.AUTO -> config.enabled && isScheduledNight(config, calendar)
        }
    }

    fun isScheduledNight(config: Config = readConfig(), calendar: Calendar = Calendar.getInstance()): Boolean {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val start = config.startHour
        val end = config.endHour
        return if (start == end) true else if (start > end) hour >= start || hour < end else hour in start until end
    }

    /** Apply night brightness or restore the last day brightness. Safe to call repeatedly. */
    fun applyNow(config: Config = readConfig()): Boolean {
        if (!hasWriteSettingsPermission()) return false
        return try {
            val resolver = appContext.contentResolver
            val night = isNightActive(config)
            val nightWasApplied = prefs.getBoolean(KEY_NIGHT_APPLIED, false)
            val current = runCatching {
                Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            }.getOrDefault(128).coerceIn(1, 255)

            if (night) {
                if (!nightWasApplied) prefs.edit().putInt(KEY_SAVED_DAY_BRIGHTNESS, current).apply()
                runCatching {
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                }
                val target = ((config.nightBrightnessPercent.coerceIn(10, 70) / 100f) * 255f).toInt().coerceIn(12, 255)
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, target)
                prefs.edit().putBoolean(KEY_NIGHT_APPLIED, true).apply()
            } else if (nightWasApplied) {
                val restore = prefs.getInt(KEY_SAVED_DAY_BRIGHTNESS, 204).coerceIn(20, 255)
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, restore)
                prefs.edit().putBoolean(KEY_NIGHT_APPLIED, false).apply()
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun automaticBackupNeededMarker(versionCode: Int): Boolean {
        val last = prefs.getInt(KEY_LAST_PREUPDATE_BACKUP_VERSION, -1)
        return last != versionCode
    }

    fun markAutomaticBackup(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_PREUPDATE_BACKUP_VERSION, versionCode).apply()
    }

    companion object {
        const val PREFS_NAME = "darbak_display_automation"
        private const val KEY_ENABLED = "night_enabled"
        private const val KEY_START_HOUR = "night_start_hour"
        private const val KEY_END_HOUR = "night_end_hour"
        private const val KEY_NIGHT_BRIGHTNESS = "night_brightness_percent"
        private const val KEY_EXTRA_DIM = "night_extra_dim_percent"
        private const val KEY_SAFE_DRIVING = "safe_driving_enabled"
        private const val KEY_SAFE_DRIVING_THRESHOLD = "safe_driving_threshold_kmh"
        private const val KEY_OVERRIDE = "night_manual_override"
        private const val KEY_SAVED_DAY_BRIGHTNESS = "saved_day_brightness"
        private const val KEY_NIGHT_APPLIED = "night_applied"
        private const val KEY_LAST_PREUPDATE_BACKUP_VERSION = "preupdate_backup_version"
    }
}
