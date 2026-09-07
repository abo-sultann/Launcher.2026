package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Converts trusted Launcher GPS speed samples into whole odometer kilometres and
 * forwards them to Darbak Maintenance. Fractional distance is retained locally,
 * so the maintenance app does not need to run a second GPS listener or stay open.
 */
class MaintenanceMileageBridge(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var pendingMillimeters = prefs.getLong(KEY_PENDING_MM, 0L).coerceAtLeast(0L)
    private var lastPersistedMillimeters = pendingMillimeters
    private var nextFlushAttemptElapsedMs = 0L

    fun recordSpeedSample(speedKmH: Float, dtSec: Float, accuracyMeters: Float) {
        if (!shouldCountSample(speedKmH, dtSec, accuracyMeters)) return

        val distanceMeters = (speedKmH / 3.6f) * dtSec
        val addMm = (distanceMeters * 1000.0).roundToLong().coerceAtLeast(0L)
        if (addMm == 0L) return

        pendingMillimeters = safeAdd(pendingMillimeters, addMm)
        flushWholeKilometersIfPossible()
        persistIfNeeded()
    }

    private fun flushWholeKilometersIfPossible() {
        val wholeKm = floor(pendingMillimeters / MM_PER_KM.toDouble()).toLong()
        if (wholeKm <= 0L) return

        val now = SystemClock.elapsedRealtime()
        if (now < nextFlushAttemptElapsedMs) return

        val delta = wholeKm.coerceAtMost(MAX_KM_PER_SYNC)
        val values = ContentValues().apply { put(COL_DELTA_KM, delta) }
        val applied = try {
            val mileageUri = Uri.parse(MILEAGE_URI_STRING)
            appContext.contentResolver.update(mileageUri, values, null, null) == 1
        } catch (e: Exception) {
            Log.d(TAG, "Maintenance mileage endpoint not ready", e)
            false
        }

        if (applied) {
            pendingMillimeters = (pendingMillimeters - delta * MM_PER_KM).coerceAtLeast(0L)
            nextFlushAttemptElapsedMs = 0L
            persist(force = true)
        } else {
            nextFlushAttemptElapsedMs = now + RETRY_AFTER_MS
            persist(force = true)
        }
    }

    private fun persistIfNeeded() {
        val changed = kotlin.math.abs(pendingMillimeters - lastPersistedMillimeters)
        if (changed >= PERSIST_EVERY_MM) persist(force = false)
    }

    private fun persist(force: Boolean) {
        if (!force && pendingMillimeters == lastPersistedMillimeters) return
        prefs.edit().putLong(KEY_PENDING_MM, pendingMillimeters).apply()
        lastPersistedMillimeters = pendingMillimeters
    }

    private fun safeAdd(a: Long, b: Long): Long =
        if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b

    companion object {
        private const val TAG = "MaintenanceMileage"
        private const val PREFS_NAME = "launcher_maintenance_mileage_2026"
        private const val KEY_PENDING_MM = "pending_mm"
        private const val COL_DELTA_KM = "delta_km"
        private const val MM_PER_KM = 1_000_000L
        private const val PERSIST_EVERY_MM = 250_000L
        private const val MAX_KM_PER_SYNC = 10_000L
        private const val RETRY_AFTER_MS = 30_000L
        private const val MILEAGE_URI_STRING = "content://com.abosultan.darbakmaintenance.status/mileage"

        internal fun shouldCountSample(speedKmH: Float, dtSec: Float, accuracyMeters: Float): Boolean =
            speedKmH in 2.2f..180f &&
                dtSec in 0.5f..5.5f &&
                accuracyMeters in 0f..45f
    }
}
