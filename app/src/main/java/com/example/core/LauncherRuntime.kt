package com.example.core

import android.app.Application
import com.example.CarLauncherApp
import com.example.data.AppRepository
import com.example.data.DiagnosticManager
import com.example.data.GpsTelemetryManager
import com.example.data.MusicPlayerService
import com.example.data.OfflineMapEngine
import com.example.data.RecommendedMapInstaller
import com.example.data.OfflineMapSearchEngine
import com.example.data.PreferencesManager
import com.example.data.TripComputer

/**
 * Single dependency owner for Launcher 2026.
 *
 * The old implementation constructed every manager inside MainViewModel. That made the UI
 * responsible for hardware, storage and service lifetimes. Keeping those dependencies here
 * lets each feature be split out without recreating GPS, audio or map engines.
 */
class LauncherRuntime(application: Application) {
    val preferences = PreferencesManager(application)
    val apps = AppRepository(application, preferences)
    val music = MusicPlayerService(application, preferences)
    val gps = GpsTelemetryManager(application)
    val trip = TripComputer(preferences)
    val maps = OfflineMapEngine(application, preferences)
    val recommendedMap = RecommendedMapInstaller(application, maps)
    val diagnostics = DiagnosticManager(application, preferences)
    val offroad = (application as CarLauncherApp).offroadTrackManager
    val mapSearch = OfflineMapSearchEngine()
}
