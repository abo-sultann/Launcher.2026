package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.example.model.GpsTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GpsTelemetryManager(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _telemetry = MutableStateFlow(GpsTelemetry())
    val telemetry: StateFlow<GpsTelemetry> = _telemetry.asStateFlow()

    private var isListening = false

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        if (isListening) return
        try {
            val lm = locationManager ?: return

            val isGpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
            val isNetworkEnabled = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }

            if (!isGpsEnabled && !isNetworkEnabled) {
                _telemetry.value = _telemetry.value.copy(
                    hasGpsFix = false,
                    statusArabic = "خدمة تحديد المواقع (GPS) غير مفعلة"
                )
                return
            }

            if (isGpsEnabled) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 1 sec interval
                    1.0f,  // 1 meter min distance
                    this
                )
            }

            if (isNetworkEnabled) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    5.0f,
                    this
                )
            }

            // Get last known location if available
            val lastGps = if (isGpsEnabled) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNet = if (isNetworkEnabled) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            val bestLocation = lastGps ?: lastNet
            if (bestLocation != null) {
                onLocationChanged(bestLocation)
            }

            isListening = true
            _telemetry.value = _telemetry.value.copy(
                statusArabic = "جارٍ البحث عن الأقمار الصناعية..."
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "GPS permission not granted yet", e)
            _telemetry.value = _telemetry.value.copy(
                hasGpsFix = false,
                statusArabic = "في انتظار منح إذن الموقع"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting GPS listener", e)
            _telemetry.value = _telemetry.value.copy(
                hasGpsFix = false,
                statusArabic = "تعذر تشغيل GPS"
            )
        }
    }

    fun stopGpsUpdates() {
        try {
            if (isListening) {
                locationManager?.removeUpdates(this)
                isListening = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GPS updates", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        try {
            // Speed in m/s converted to km/h (speed * 3.6f)
            val speedKmH = if (location.hasSpeed()) {
                (location.speed * 3.6f).coerceAtLeast(0f)
            } else {
                0f
            }

            val bearing = if (location.hasBearing()) location.bearing else 0f
            val altitude = if (location.hasAltitude()) location.altitude else 0.0
            val accuracy = if (location.hasAccuracy()) location.accuracy else 0f

            _telemetry.value = GpsTelemetry(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = altitude,
                speedKmH = speedKmH,
                bearingDegrees = bearing,
                accuracyMeters = accuracy,
                hasGpsFix = true,
                satellitesCount = if (location.extras?.containsKey("satellites") == true) {
                    location.extras?.getInt("satellites") ?: 8
                } else {
                    8
                },
                statusArabic = "متصل بالأقمار الصناعية"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing location update", e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {
        _telemetry.value = _telemetry.value.copy(statusArabic = "تم تفعيل $provider")
    }

    override fun onProviderDisabled(provider: String) {
        _telemetry.value = _telemetry.value.copy(
            hasGpsFix = false,
            statusArabic = "تم إيقاف $provider"
        )
    }

    companion object {
        private const val TAG = "GpsTelemetryManager"
    }
}
