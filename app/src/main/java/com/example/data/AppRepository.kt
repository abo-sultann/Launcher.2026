package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
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

            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                // Skip launcher itself in the drawer to keep screen clean
                if (pkg == ownPackage) continue

                val activity = resolveInfo.activityInfo.name
                val label = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    pkg
                }
                val icon = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }

                result.add(
                    AppItem(
                        packageName = pkg,
                        activityName = activity,
                        label = label,
                        isFavorite = favorites.contains(pkg),
                        isHidden = hidden.contains(pkg),
                        icon = icon
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying installed applications", e)
        }

        // Clean uninstalled favorites/hidden to avoid memory/data corruption
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

    companion object {
        private const val TAG = "AppRepository"
    }
}
