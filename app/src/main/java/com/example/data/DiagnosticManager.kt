package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import com.example.model.ComponentHealth
import com.example.model.ComponentStatus
import com.example.model.DiagnosticReport

class DiagnosticManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    fun runFullDiagnostics(): DiagnosticReport {
        val safeModePrefs = context.getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
        val crashCount = safeModePrefs.getInt("crash_count", 0)
        val lastCrash = safeModePrefs.getString("last_crash_msg", null)

        val homeHealth = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val availableMb = info.availMem / (1024L * 1024L)
            val totalMb = info.totalMem / (1024L * 1024L)
            ComponentHealth(
                nameArabic = "الشاشة الرئيسية والذاكرة",
                status = ComponentStatus.RUNNING,
                details = "RAM متاح $availableMb MB من $totalMb MB • حد الذاكرة المنخفضة ${info.threshold / (1024L * 1024L)} MB"
            )
        } catch (e: Exception) {
            ComponentHealth("الشاشة الرئيسية والذاكرة", ComponentStatus.ERROR, "تعذر قراءة حالة RAM: ${e.message}")
        }

        val gpsHealth = try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsOn = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val last = if (lm != null) readLastGpsSafely(lm) else null
            val ageText = last?.let { location ->
                val ageMs = if (android.os.Build.VERSION.SDK_INT >= 17) {
                    (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos).coerceAtLeast(0L) / 1_000_000L
                } else {
                    (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
                }
                "آخر قراءة قبل ${ageMs / 1000L}ث • دقة ±${last.accuracy.toInt()}م"
            } ?: "لا توجد قراءة GPS محفوظة"
            ComponentHealth(
                "نظام تحديد المواقع GPS",
                ComponentStatus.RUNNING,
                if (isGpsOn) "GPS مفعل • $ageText" else "GPS غير مفعل من النظام • $ageText"
            )
        } catch (e: Exception) {
            ComponentHealth("نظام تحديد المواقع GPS", ComponentStatus.ERROR, "خطأ في الاتصال بمزود الموقع: ${e.message}")
        }

        val musicHealth = try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                val volume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                ComponentHealth("مشغل الموسيقى والصوت", ComponentStatus.RUNNING, "AudioManager جاهز • مستوى الوسائط $volume/$max")
            } else {
                ComponentHealth("مشغل الموسيقى والصوت", ComponentStatus.ERROR, "خدمة AudioManager غير متاحة")
            }
        } catch (e: Exception) {
            ComponentHealth("مشغل الموسيقى والصوت", ComponentStatus.ERROR, "خطأ في خدمة الصوت: ${e.message}")
        }

        val mapsHealth = try {
            val maps = preferencesManager.getSavedMaps()
            val active = maps.firstOrNull { it.isActive }?.name ?: "لا توجد خريطة نشطة"
            ComponentHealth("نظام الخرائط Offline", ComponentStatus.RUNNING, "خرائط محفوظة: ${maps.size} • النشطة: $active")
        } catch (e: Exception) {
            ComponentHealth("نظام الخرائط Offline", ComponentStatus.ERROR, "تعذر قراءة الخرائط: ${e.message}")
        }

        val dbHealth = try {
            preferencesManager.getSettings()
            ComponentHealth("التخزين المحلي والإعدادات", ComponentStatus.RUNNING, "تمت قراءة إعدادات Darbak Launcher بنجاح")
        } catch (e: Exception) {
            ComponentHealth("التخزين المحلي والإعدادات", ComponentStatus.ERROR, "فشل في التخزين: ${e.message}")
        }

        val widgetsHealth = try {
            val widgets = preferencesManager.getWidgets()
            ComponentHealth("محرك الودجات (Widgets)", ComponentStatus.RUNNING, "ودجات محفوظة: ${widgets.size} • الظاهرة: ${widgets.count { it.isVisible }}")
        } catch (e: Exception) {
            ComponentHealth("محرك الودجات (Widgets)", ComponentStatus.ERROR, "تعذر تحميل الودجات: ${e.message}")
        }

        val storageHealth = try {
            val filesDir = context.filesDir
            val freeMb = filesDir.usableSpace / (1024L * 1024L)
            val totalMb = filesDir.totalSpace / (1024L * 1024L)
            if (filesDir.canWrite()) {
                ComponentHealth("الذاكرة والملفات", ComponentStatus.RUNNING, "متاح $freeMb MB من $totalMb MB • الكتابة متاحة")
            } else {
                ComponentHealth("الذاكرة والملفات", ComponentStatus.ERROR, "المساحة $freeMb MB لكن لا يمكن الكتابة على مجلد التطبيق")
            }
        } catch (e: Exception) {
            ComponentHealth("الذاكرة والملفات", ComponentStatus.ERROR, "خطأ في فحص الذاكرة: ${e.message}")
        }

        return DiagnosticReport(
            homeStatus = homeHealth,
            gpsStatus = gpsHealth,
            musicStatus = musicHealth,
            mapsStatus = mapsHealth,
            databaseStatus = dbHealth,
            widgetsStatus = widgetsHealth,
            storageStatus = storageHealth,
            crashCount = crashCount,
            lastCrashMessage = lastCrash
        )
    }

    @Suppress("MissingPermission")
    private fun readLastGpsSafely(manager: LocationManager): Location? = try {
        manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } catch (_: SecurityException) {
        null
    }

    fun resetCrashCount() {
        try {
            val safeModePrefs = context.getSharedPreferences("car_launcher_safe_mode", Context.MODE_PRIVATE)
            safeModePrefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("DiagnosticManager", "Error clearing crash count", e)
        }
    }
}
