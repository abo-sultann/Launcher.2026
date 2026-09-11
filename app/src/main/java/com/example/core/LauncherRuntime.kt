package com.example.core

import android.app.Application
import com.example.CarLauncherApp
import com.example.core.bridge.DarbakAppBridge
import com.example.core.bridge.DarbakSystemStateStore
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
 * Single dependency owner for Darbak Launcher.
 *
 * Hardware, storage and service lifetimes stay outside the UI. The Darbak app bridge and its
 * observable system state are also owned here so every screen reads one consistent module view.
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
    val darbakBridge = DarbakAppBridge(application)
    val darbakSystem = DarbakSystemStateStore(darbakBridge)
}
