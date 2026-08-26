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
                val prefs = context.getSharedPreferences("car_launcher_prefs", Context.MODE_PRIVATE)
                val autoStart = prefs.getBoolean("auto_start_enabled", true)
                if (autoStart) {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to auto-start launcher", e)
            }
        }
    }
}
