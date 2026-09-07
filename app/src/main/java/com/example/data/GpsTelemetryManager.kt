package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.GpsTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import kotlin.math.max

class GpsTelemetryManager(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _telemetry = MutableStateFlow(GpsTelemetry())
    val telemetry: StateFlow<GpsTelemetry> = _telemetry.asStateFlow()

    private var isListening = false
    private var previousAcceptedGps: Location? = null
    private val recentSpeeds = ArrayDeque<Float>()
    private var movingConfirmations = 0
    private var stationaryConfirmations = 0
    private val maintenanceMileageBridge = MaintenanceMileageBridge(context)

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        if (!hasLocationPermission()) {
            publishNoFix("في انتظار منح إذن الموقع")
            return
        }
        if (isListening) return

        try {
            val lm = locationManager ?: return
            val isGpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
            val isNetworkEnabled = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

            if (!isGpsEnabled && !isNetworkEnabled) {
                publishNoFix("خدمة تحديد المواقع غير مفعلة")
                return
            }

            // GPS is the authoritative source for vehicle speed. Network location is only
            // retained as a coarse position fallback and is never allowed to create speed.
            if (isGpsEnabled) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, this)
            if (isNetworkEnabled) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000L, 10f, this)
            isListening = true

            val lastGps = if (isGpsEnabled) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            if (lastGps != null && locationAgeMs(lastGps) <= LAST_KNOWN_MAX_AGE_MS && accuracyOf(lastGps) <= 60f) {
                processLocation(lastGps, fromLastKnown = true)
            } else {
                _telemetry.value = _telemetry.value.copy(hasGpsFix = false, speedKmH = 0f, isSpeedReliable = false, statusArabic = "جارٍ البحث عن الأقمار الصناعية...")
            }
        } catch (e: SecurityException) {
            isListening = false
            Log.w(TAG, "GPS permission not granted", e)
            publishNoFix("في انتظار منح إذن الموقع")
        } catch (e: Exception) {
            isListening = false
            Log.e(TAG, "Error starting GPS listener", e)
            publishNoFix("تعذر تشغيل GPS")
        }
    }

    fun restartGpsUpdates() { stopGpsUpdates(); startGpsUpdates() }

    fun stopGpsUpdates() {
        try { if (isListening) locationManager?.removeUpdates(this) } catch (e: Exception) { Log.e(TAG, "Error stopping GPS updates", e) }
        finally { isListening = false }
    }

    override fun onLocationChanged(location: Location) = processLocation(location, fromLastKnown = false)

    private fun processLocation(location: Location, fromLastKnown: Boolean) {
        try {
            val provider = location.provider ?: ""
            val ageMs = locationAgeMs(location)
            val accuracy = accuracyOf(location)

            if (ageMs > MAX_FIX_AGE_MS && !fromLastKnown) {
                publishRejected(location, ageMs, "قراءة GPS قديمة")
                return
            }

            if (provider != LocationManager.GPS_PROVIDER) {
                // Keep a coarse network position only when no fresh GPS fix exists.
                val current = _telemetry.value
                if (!current.hasGpsFix || current.fixAgeMs > 12_000L) {
                    _telemetry.value = GpsTelemetry(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
                        speedKmH = 0f,
                        bearingDegrees = current.bearingDegrees,
                        accuracyMeters = accuracy,
                        hasGpsFix = false,
                        satellitesCount = 0,
                        statusArabic = "موقع تقريبي — بانتظار GPS",
                        isSpeedReliable = false,
                        fixAgeMs = ageMs,
                        providerName = provider,
                        rejectedReason = "Network location لا يستخدم لحساب السرعة"
                    )
                }
                return
            }

            if (accuracy > MAX_ACCEPTED_ACCURACY_M) {
                publishRejected(location, ageMs, "دقة GPS ضعيفة ±${accuracy.toInt()}م")
                return
            }

            val previous = previousAcceptedGps
            val dtSec = previous?.let { (location.time - it.time) / 1000f } ?: 0f
            val distanceM = previous?.distanceTo(location) ?: 0f

            // Reject impossible point jumps before they contaminate speed/trip distance.
            if (previous != null && dtSec in 0.4f..10f) {
                val jumpSpeed = (distanceM / dtSec) * 3.6f
                val tolerance = max(12f, accuracy + accuracyOf(previous))
                if (distanceM > tolerance && jumpSpeed > MAX_PLAUSIBLE_SPEED_KMH) {
                    publishRejected(location, ageMs, "قفزة موقع غير منطقية")
                    return
                }
            }

            val sensorSpeed = if (location.hasSpeed() && location.speed >= 0f) location.speed * 3.6f else -1f
            val derivedSpeed = if (previous != null && dtSec in 0.8f..5f && accuracy <= 25f && accuracyOf(previous) <= 25f) {
                (distanceM / dtSec) * 3.6f
            } else -1f

            var rawSpeed = when {
                sensorSpeed in 0f..MAX_PLAUSIBLE_SPEED_KMH -> sensorSpeed
                derivedSpeed in 0f..MAX_PLAUSIBLE_SPEED_KMH -> derivedSpeed
                else -> 0f
            }

            // A GPS point can drift several metres while parked. Treat low displacement inside
            // the accuracy envelope as stationary regardless of the provider's instantaneous speed.
            val stationaryByDisplacement = previous != null && dtSec in 0.5f..6f && distanceM <= max(3.5f, (accuracy + accuracyOf(previous)) * 0.28f)
            if (rawSpeed < STOP_SPEED_KMH || stationaryByDisplacement) rawSpeed = 0f

            if (rawSpeed == 0f) {
                stationaryConfirmations++
                movingConfirmations = 0
            } else {
                movingConfirmations++
                stationaryConfirmations = 0
            }

            pushSpeed(rawSpeed)
            val median = medianSpeed()
            val confirmedSpeed = when {
                stationaryConfirmations >= 2 -> 0f
                movingConfirmations >= 2 -> median
                else -> 0f
            }.coerceIn(0f, MAX_PLAUSIBLE_SPEED_KMH)

            // Maintenance odometer tracking is independent from the Trip Computer. We integrate
            // only trusted GPS speed samples and keep sub-kilometre remainder inside the launcher,
            // so Darbak Maintenance keeps counting even while its activity is closed.
            if (previous != null && !fromLastKnown) {
                maintenanceMileageBridge.recordSpeedSample(confirmedSpeed, dtSec, accuracy)
            }

            previousAcceptedGps = Location(location)
            _telemetry.value = GpsTelemetry(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
                speedKmH = confirmedSpeed,
                bearingDegrees = if (location.hasBearing() && confirmedSpeed >= 2f) location.bearing else _telemetry.value.bearingDegrees,
                accuracyMeters = accuracy,
                hasGpsFix = true,
                satellitesCount = location.extras?.getInt("satellites", 0) ?: _telemetry.value.satellitesCount,
                statusArabic = if (confirmedSpeed > 0f) "GPS ثابت" else "GPS ثابت — متوقف",
                isSpeedReliable = movingConfirmations >= 2 || stationaryConfirmations >= 2,
                fixAgeMs = ageMs,
                providerName = provider,
                rejectedReason = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing location update", e)
        }
    }

    private fun publishRejected(location: Location, ageMs: Long, reason: String) {
        val old = _telemetry.value
        _telemetry.value = old.copy(
            latitude = if (old.latitude != 0.0) old.latitude else location.latitude,
            longitude = if (old.longitude != 0.0) old.longitude else location.longitude,
            speedKmH = 0f,
            hasGpsFix = old.hasGpsFix,
            isSpeedReliable = false,
            fixAgeMs = ageMs,
            statusArabic = reason,
            rejectedReason = reason
        )
    }

    private fun publishNoFix(status: String) {
        recentSpeeds.clear()
        movingConfirmations = 0
        stationaryConfirmations = 0
        _telemetry.value = _telemetry.value.copy(hasGpsFix = false, speedKmH = 0f, isSpeedReliable = false, statusArabic = status, rejectedReason = status)
    }

    private fun pushSpeed(value: Float) {
        recentSpeeds.addLast(value)
        while (recentSpeeds.size > 5) recentSpeeds.removeFirst()
    }

    private fun medianSpeed(): Float {
        if (recentSpeeds.isEmpty()) return 0f
        val sorted = recentSpeeds.toList().sorted()
        return sorted[sorted.size / 2]
    }

    private fun accuracyOf(location: Location): Float = if (location.hasAccuracy()) location.accuracy.coerceAtLeast(0f) else 999f

    private fun locationAgeMs(location: Location): Long {
        return if (BuildCompat.hasElapsedRealtimeNanos(location)) {
            ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L).coerceAtLeast(0L)
        } else {
            (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) { if (!isListening) startGpsUpdates() }
    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) publishNoFix("GPS متوقف")
    }

    private object BuildCompat {
        fun hasElapsedRealtimeNanos(location: Location): Boolean = try { location.elapsedRealtimeNanos > 0L } catch (_: Throwable) { false }
    }

    companion object {
        private const val TAG = "GpsTelemetryManager"
        private const val MAX_FIX_AGE_MS = 8_000L
        private const val LAST_KNOWN_MAX_AGE_MS = 20_000L
        private const val MAX_ACCEPTED_ACCURACY_M = 45f
        private const val MAX_PLAUSIBLE_SPEED_KMH = 180f
        private const val STOP_SPEED_KMH = 2.2f
    }
}
