package com.example

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.OffroadTrackManager
import com.example.data.UpdateManager
import com.example.data.UpdateStatus
import com.example.service.OffroadTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CarLauncherApp : Application(), Application.ActivityLifecycleCallbacks {

    val offroadTrackManager: OffroadTrackManager by lazy { OffroadTrackManager(this) }
    val updateManager: UpdateManager by lazy { UpdateManager(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startedActivities = 0
    private var changingConfiguration = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            recordCrash(throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }

        appScope.launch {
            delay(20_000L)
            updateManager.checkAndAutoDownload()
            if (updateManager.state.value.status == UpdateStatus.READY_TO_INSTALL) showUpdateReadyNotification()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        if (startedActivities == 1 && !changingConfiguration) OffroadTrackingService.stop(this)
    }

    override fun onActivityStopped(activity: Activity) {
        changingConfiguration = activity.isChangingConfigurations
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0 && !changingConfiguration) OffroadTrackingService.start(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private fun showUpdateReadyNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(NotificationChannel(UPDATE_CHANNEL, "تحديث Launcher 2026", NotificationManager.IMPORTANCE_HIGH))
            }
            val intent = Intent(this, UpdateInstallActivity::class.java)
            val pending = PendingIntent.getActivity(this, 0, intent, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT)
            val info = updateManager.state.value.info
            manager.notify(
                UPDATE_NOTIFICATION_ID,
                NotificationCompat.Builder(this, UPDATE_CHANNEL)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("تحديث Launcher 2026 جاهز")
                    .setContentText("الإصدار ${info?.versionName ?: "الجديد"} تم تنزيله — اضغط للتثبيت")
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            )
        } catch (e: Exception) { Log.e(TAG, "Failed to show update notification", e) }
    }

    private fun recordCrash(throwable: Throwable) {
        try {
            val prefs = getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
            val crashCount = prefs.getInt("crash_count", 0) + 1
            prefs.edit()
                .putInt("crash_count", crashCount)
                .putLong("last_crash_time", System.currentTimeMillis())
                .putString("last_crash_msg", throwable.localizedMessage ?: "Unknown error")
                .apply()
        } catch (e: Exception) { Log.e(TAG, "Failed to record crash", e) }
    }

    companion object {
        private const val TAG = "CarLauncherApp"
        private const val UPDATE_CHANNEL = "launcher_updates"
        private const val UPDATE_NOTIFICATION_ID = 20262
        lateinit var instance: CarLauncherApp
            private set
    }
}
