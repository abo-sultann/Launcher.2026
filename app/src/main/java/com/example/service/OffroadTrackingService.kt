package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.CarLauncherApp
import com.example.MainActivity
import com.example.R
import com.example.model.GpsTelemetry

class OffroadTrackingService : Service(), LocationListener {
    private var locationManager: LocationManager? = null
    private var listening = false
    private var previousLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("تسجيل مسار البر جاهز"))
        startTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!listening) startTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        stopTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun startTracking() {
        if (!hasLocationPermission()) return
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            locationManager = lm
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 2.5f, this)
            }
            listening = true
        } catch (_: Exception) {
            listening = false
        }
    }

    private fun stopTracking() {
        try { if (listening) locationManager?.removeUpdates(this) } catch (_: Exception) { }
        listening = false
    }

    override fun onLocationChanged(location: Location) {
        val directSpeed = if (location.hasSpeed()) location.speed * 3.6f else -1f
        val fallbackSpeed = previousLocation?.let { previous ->
            val dt = (location.time - previous.time) / 1000f
            if (dt in 0.5f..15f) (previous.distanceTo(location) / dt) * 3.6f else 0f
        } ?: 0f
        val speed = (if (directSpeed >= 0f) directSpeed else fallbackSpeed).coerceIn(0f, 260f).let { if (it < 1.2f) 0f else it }
        previousLocation = Location(location)

        val telemetry = GpsTelemetry(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
            speedKmH = speed,
            bearingDegrees = if (location.hasBearing()) location.bearing else 0f,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
            hasGpsFix = true,
            satellitesCount = location.extras?.getInt("satellites", 0) ?: 0,
            statusArabic = "تسجيل المسار بالخلفية"
        )
        (application as? CarLauncherApp)?.offroadTrackManager?.record(telemetry)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
    override fun onProviderEnabled(provider: String) { if (!listening) startTracking() }
    override fun onProviderDisabled(provider: String) { }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "تسجيل مسار البر", NotificationManager.IMPORTANCE_LOW).apply {
                description = "يحافظ على تسجيل أثر المسار عند خروج Launcher أو إطفاء الشاشة"
                setShowBadge(false)
            })
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Launcher 2026")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "offroad_tracking"
        private const val NOTIFICATION_ID = 20261

        fun start(context: Context) {
            val intent = Intent(context, OffroadTrackingService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
            } catch (_: Exception) { }
        }
    }
}
