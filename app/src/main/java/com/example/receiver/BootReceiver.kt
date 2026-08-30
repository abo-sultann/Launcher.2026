package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            try {
                val prefs = context.getSharedPreferences("car_launcher_preferences_2026", Context.MODE_PRIVATE)
                val autoStart = prefs.getBoolean("auto_start", true)
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val defaultHome = context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName

                // Android starts the selected HOME activity itself. Starting it again here while
                // also starting the tracking service caused a boot-time race on the Android 7 unit.
                if (autoStart && defaultHome != context.packageName) {
                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to auto-start launcher", e)
            }
        }
    }
}
