package com.example.core.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Lightweight process boundary between Darbak Launcher and the private Darbak apps installed on
 * the head unit. The bridge deliberately uses Android package APIs only, so one missing or broken
 * companion app cannot crash the launcher.
 */
class DarbakAppBridge(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val modules = DarbakModuleCatalog.systemModules(appContext.packageName)
        .sortedBy { it.priority }

    fun refresh(): List<DarbakModuleState> = modules.map(::resolve)

    fun state(id: DarbakModuleId): DarbakModuleState? =
        modules.firstOrNull { it.id == id }?.let(::resolve)

    fun launch(id: DarbakModuleId): Boolean {
        val spec = modules.firstOrNull { it.id == id } ?: return false
        return try {
            val intent = packageManager.getLaunchIntentForPackage(spec.packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            appContext.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun isManagedPackage(packageName: String): Boolean =
        modules.any { it.packageName == packageName }

    private fun resolve(spec: DarbakModuleSpec): DarbakModuleState {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(spec.packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            DarbakModuleState(
                spec = spec,
                installed = true,
                enabled = applicationInfo?.enabled ?: true,
                launchable = packageManager.getLaunchIntentForPackage(spec.packageName) != null,
                versionName = packageInfo.versionName,
                versionCode = versionCode,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            DarbakModuleState(
                spec = spec,
                installed = false,
                enabled = false,
                launchable = false,
            )
        } catch (_: Throwable) {
            DarbakModuleState(
                spec = spec,
                installed = false,
                enabled = false,
                launchable = false,
            )
        }
    }
}
