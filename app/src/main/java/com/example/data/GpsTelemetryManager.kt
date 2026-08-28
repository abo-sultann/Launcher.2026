package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.GpsTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GpsTelemetryManager(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _telemetry = MutableStateFlow(GpsTelemetry())
    val telemetry: StateFlow<GpsTelemetry> = _telemetry.asStateFlow()

    private var isListening = false
    private var previousLocation: Location? = null

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        if (!hasLocationPermission()) {
            _telemetry.value = _telemetry.value.copy(hasGpsFix = false, statusArabic = "في انتظار منح إذن الموقع")
            return
        }
        if (isListening) return

        try {
            val lm = locationManager ?: return
            val isGpsEnabled = try { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
            val isNetworkEnabled = try { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

            if (!isGpsEnabled && !isNetworkEnabled) {
                _telemetry.value = _telemetry.value.copy(hasGpsFix = false, statusArabic = "خدمة تحديد المواقع (GPS) غير مفعلة")
                return
            }

            if (isGpsEnabled) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, this)
            if (isNetworkEnabled) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2500L, 5.0f, this)
            isListening = true

            val lastGps = if (isGpsEnabled) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNet = if (isNetworkEnabled) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            val bestLocation = listOfNotNull(lastGps, lastNet).maxByOrNull { it.time }
            if (bestLocation != null) onLocationChanged(bestLocation)
            else _telemetry.value = _telemetry.value.copy(statusArabic = "جارٍ البحث عن الأقمار الصناعية...")
        } catch (e: SecurityException) {
            isListening = false
            Log.w(TAG, "GPS permission not granted", e)
            _telemetry.value = _telemetry.value.copy(hasGpsFix = false, statusArabic = "في انتظار منح إذن الموقع")
        } catch (e: Exception) {
            isListening = false
            Log.e(TAG, "Error starting GPS listener", e)
            _telemetry.value = _telemetry.value.copy(hasGpsFix = false, statusArabic = "تعذر تشغيل GPS")
        }
    }

    fun restartGpsUpdates() { stopGpsUpdates(); startGpsUpdates() }

    fun stopGpsUpdates() {
        try { if (isListening) locationManager?.removeUpdates(this) } catch (e: Exception) { Log.e(TAG, "Error stopping GPS updates", e) }
        finally { isListening = false }
    }

    override fun onLocationChanged(location: Location) {
        try {
            val directSpeed = if (location.hasSpeed()) location.speed * 3.6f else -1f
            val fallbackSpeed = previousLocation?.let { previous ->
                val dtSec = (location.time - previous.time) / 1000f
                if (dtSec in 0.4f..10f) (previous.distanceTo(location) / dtSec) * 3.6f else 0f
            } ?: 0f
            var speedKmH = if (directSpeed >= 0f) directSpeed else fallbackSpeed
            if (speedKmH < 1.2f) speedKmH = 0f
            speedKmH = speedKmH.coerceIn(0f, 260f)
            previousLocation = Location(location)
            _telemetry.value = GpsTelemetry(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
                speedKmH = speedKmH,
                bearingDegrees = if (location.hasBearing()) location.bearing else 0f,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
                hasGpsFix = true,
                satellitesCount = location.extras?.getInt("satellites", 0) ?: 0,
                statusArabic = "GPS يعمل"
            )
        } catch (e: Exception) { Log.e(TAG, "Error processing location update", e) }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) { _telemetry.value = _telemetry.value.copy(statusArabic = "تم تفعيل GPS"); if (!isListening) startGpsUpdates() }
    override fun onProviderDisabled(provider: String) { _telemetry.value = _telemetry.value.copy(hasGpsFix = false, speedKmH = 0f, statusArabic = "GPS متوقف") }

    companion object { private const val TAG = "GpsTelemetryManager" }
}
