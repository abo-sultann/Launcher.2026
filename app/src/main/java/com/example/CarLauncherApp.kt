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
import android.os.SystemClock
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
    private val processStartedAtElapsed = SystemClock.elapsedRealtime()
    @Volatile private var expectedExternalHandoffUntil = 0L

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)

        normalizeCrashWindow()

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            recordCrash(throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }

        appScope.launch {
            delay(STABLE_SESSION_MS)
            clearCrashStreakAfterStableSession()
        }

        appScope.launch {
            delay(20_000L)
            updateManager.checkAndAutoDownload()
            // Some old Android 7 head-unit ROMs are unstable when posting an install-ready
            // notification immediately after a large APK download. On API 25 and below the
            // update simply remains READY inside Launcher settings; no process hand-off happens.
            if (updateManager.state.value.status == UpdateStatus.READY_TO_INSTALL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                showUpdateReadyNotification()
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        if (startedActivities == 1 && !changingConfiguration) OffroadTrackingService.stop(this)
    }

    override fun onActivityStopped(activity: Activity) {
        changingConfiguration = activity.isChangingConfigurations
        startedActivities = (startedActivities - 1).coerceAtLeast(0)

        // Do not start a foreground GPS service merely because Launcher hands the screen to
        // another app. Several Android 7 head-unit ROMs terminate the process while creating
        // that service/notification. The in-app map continues to record from its own GPS flow;
        // background off-road recording must be started only by an explicit user action.
        if (startedActivities == 0 && !changingConfiguration) {
            OffroadTrackingService.stop(this)
        }
    }

    /** Marks an intentional hand-off so it cannot be mistaken for a startup crash loop. */
    fun prepareForExternalActivity() {
        expectedExternalHandoffUntil = SystemClock.elapsedRealtime() + EXTERNAL_HANDOFF_WINDOW_MS
        OffroadTrackingService.stop(this)
    }

    /** Kept as a compatibility alias for wallpaper/map/file pickers. */
    fun prepareForExternalPicker() = prepareForExternalActivity()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {
        expectedExternalHandoffUntil = 0L
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private fun showUpdateReadyNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(NotificationChannel(UPDATE_CHANNEL, "تحديث Launcher 2026", NotificationManager.IMPORTANCE_HIGH))
            }
            // Notification opens Launcher only. The package installer is started explicitly from
            // the update panel so OEM Android builds cannot unexpectedly kill the launcher.
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= 23) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pending = PendingIntent.getActivity(this, 0, intent, pendingFlags)
            val info = updateManager.state.value.info
            manager.notify(
                UPDATE_NOTIFICATION_ID,
                NotificationCompat.Builder(this, UPDATE_CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle("تحديث Launcher 2026 جاهز")
                    .setContentText("الإصدار ${info?.versionName ?: "الجديد"} تم تنزيله — افتح Launcher للتثبيت")
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to show update notification", t)
        }
    }

    private fun safeModePrefs() = getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)

    private fun normalizeCrashWindow() {
        try {
            val prefs = safeModePrefs()
            // A new build must not inherit a stale crash streak from the previous version.
            val installedVersion = BuildConfig.VERSION_CODE
            if (prefs.getInt("last_version_code", -1) != installedVersion) {
                prefs.edit()
                    .remove("crash_count")
                    .remove("last_crash_time")
                    .remove("last_crash_msg")
                    .putInt("last_version_code", installedVersion)
                    .apply()
                return
            }
            val lastCrash = prefs.getLong("last_crash_time", 0L)
            if (lastCrash > 0L && System.currentTimeMillis() - lastCrash > CRASH_WINDOW_MS) {
                prefs.edit().remove("crash_count").apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to normalize crash window", e)
        }
    }

    private fun clearCrashStreakAfterStableSession() {
        try {
            val prefs = safeModePrefs()
            val lastCrash = prefs.getLong("last_crash_time", 0L)
            if (lastCrash == 0L || System.currentTimeMillis() - lastCrash >= STABLE_SESSION_MS) {
                prefs.edit().remove("crash_count").apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear stable-session crash streak", e)
        }
    }

    private fun recordCrash(throwable: Throwable) {
        try {
            val prefs = safeModePrefs()
            val elapsed = SystemClock.elapsedRealtime()
            val message = throwable.localizedMessage ?: throwable.javaClass.simpleName

            // Safe mode is for a repeated crash while Launcher itself is starting. A runtime
            // failure during an intentional app/file hand-off must remain diagnostic only.
            val isStartupWindow = elapsed - processStartedAtElapsed <= STARTUP_CRASH_WINDOW_MS
            val isExpectedHandoff = elapsed <= expectedExternalHandoffUntil
            if (!isStartupWindow || isExpectedHandoff) {
                prefs.edit()
                    .putLong("last_runtime_crash_time", System.currentTimeMillis())
                    .putString("last_runtime_crash_msg", message)
                    .apply()
                return
            }

            val now = System.currentTimeMillis()
            val lastCrash = prefs.getLong("last_crash_time", 0L)
            val previousCount = if (lastCrash > 0L && now - lastCrash <= CRASH_WINDOW_MS) prefs.getInt("crash_count", 0) else 0
            prefs.edit()
                .putInt("crash_count", previousCount + 1)
                .putLong("last_crash_time", now)
                .putString("last_crash_msg", message)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record crash", e)
        }
    }

    companion object {
        private const val TAG = "CarLauncherApp"
        private const val UPDATE_CHANNEL = "launcher_updates"
        private const val UPDATE_NOTIFICATION_ID = 20262
        private const val CRASH_WINDOW_MS = 10 * 60 * 1000L
        private const val STARTUP_CRASH_WINDOW_MS = 45 * 1000L
        private const val EXTERNAL_HANDOFF_WINDOW_MS = 5 * 60 * 1000L
        private const val STABLE_SESSION_MS = 90 * 1000L
        lateinit var instance: CarLauncherApp
            private set
    }
}
