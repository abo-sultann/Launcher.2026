package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.data.OffroadTrackManager

class CarLauncherApp : Application() {

    val offroadTrackManager: OffroadTrackManager by lazy { OffroadTrackManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            recordCrash(throwable)
        }
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record crash", e)
        }
    }

    companion object {
        private const val TAG = "CarLauncherApp"
        lateinit var instance: CarLauncherApp
            private set
    }
}
