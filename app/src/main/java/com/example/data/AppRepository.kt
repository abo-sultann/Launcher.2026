package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppItem

class AppRepository(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val packageManager: PackageManager = context.packageManager

    fun getInstalledApps(): List<AppItem> {
        val result = mutableListOf<AppItem>()
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            val favorites = preferencesManager.getFavorites()
            val hidden = preferencesManager.getHiddenApps()
            val ownPackage = context.packageName
            val seenPackages = HashSet<String>()

            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == ownPackage || !seenPackages.add(pkg)) continue

                val activity = resolveInfo.activityInfo.name
                val label = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (_: Exception) {
                    pkg
                }

                // Decode/scale icons here. MainViewModel calls this method on Dispatchers.IO,
                // so opening the Apps page never performs Drawable -> Bitmap conversion.
                val iconBitmap = try {
                    resolveInfo.loadIcon(packageManager)?.toBitmap(width = 48, height = 48)
                } catch (_: Exception) {
                    null
                }

                result.add(
                    AppItem(
                        packageName = pkg,
                        activityName = activity,
                        label = label,
                        isFavorite = favorites.contains(pkg),
                        isHidden = hidden.contains(pkg),
                        iconBitmap = iconBitmap
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying installed applications", e)
        }

        cleanRemovedPackages(result.map { it.packageName }.toSet())

        return result.sortedWith(
            compareByDescending<AppItem> { it.isFavorite }
                .thenBy { it.label.lowercase() }
        )
    }

    private fun cleanRemovedPackages(installedPackages: Set<String>) {
        try {
            val favorites = preferencesManager.getFavorites().filter { installedPackages.contains(it) }.toSet()
            preferencesManager.saveFavorites(favorites)

            val hidden = preferencesManager.getHiddenApps().filter { installedPackages.contains(it) }.toSet()
            preferencesManager.saveHiddenApps(hidden)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning removed packages", e)
        }
    }

    fun toggleFavorite(packageName: String): Boolean {
        return try {
            val favorites = preferencesManager.getFavorites().toMutableSet()
            val isFav: Boolean
            if (favorites.contains(packageName)) {
                favorites.remove(packageName)
                isFav = false
            } else {
                favorites.add(packageName)
                isFav = true
            }
            preferencesManager.saveFavorites(favorites)
            isFav
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite", e)
            false
        }
    }

    fun toggleHidden(packageName: String): Boolean {
        return try {
            val hidden = preferencesManager.getHiddenApps().toMutableSet()
            val isHidden: Boolean
            if (hidden.contains(packageName)) {
                hidden.remove(packageName)
                isHidden = false
            } else {
                hidden.add(packageName)
                isHidden = true
            }
            preferencesManager.saveHiddenApps(hidden)
            isHidden
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling hidden app", e)
            false
        }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                Log.w(TAG, "No launch intent found for $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app $packageName", e)
            false
        }
    }

    fun launchAndroidSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Android settings", e)
            false
        }
    }

    companion object {
        private const val TAG = "AppRepository"
    }
}
