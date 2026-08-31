package com.example.core.startup

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StartupStage {
    HOME_READY,
    APPS_READY,
    MUSIC_READY,
    MAP_READY,
    GPS_READY,
    COMPLETE,
    SAFE_MODE
}

/** Runs heavy Android 7 initialization sequentially after the first launcher frame. */
class LauncherStartupCoordinator {
    private val _stage = MutableStateFlow(StartupStage.HOME_READY)
    val stage: StateFlow<StartupStage> = _stage.asStateFlow()
    private val _issues = MutableStateFlow<List<String>>(emptyList())
    val issues: StateFlow<List<String>> = _issues.asStateFlow()
    private var startupJob: Job? = null

    fun start(
        scope: CoroutineScope,
        safeMode: Boolean,
        loadApps: suspend () -> Unit,
        initializeMusic: suspend () -> Unit,
        initializeMap: suspend () -> Unit,
        initializeGps: suspend () -> Unit
    ) {
        if (startupJob?.isActive == true || _stage.value == StartupStage.COMPLETE) return
        if (safeMode) {
            _stage.value = StartupStage.SAFE_MODE
            return
        }
        startupJob = scope.launch(Dispatchers.IO) {
            delay(420L)
            runStep(StartupStage.APPS_READY, loadApps)
            delay(280L)
            runStep(StartupStage.MUSIC_READY, initializeMusic)
            delay(260L)
            runStep(StartupStage.GPS_READY) { withContext(Dispatchers.Main.immediate) { initializeGps() } }
            // Map files can be large. They are deliberately last so loading or rejecting a bad
            // map cannot delay Home, apps, audio or live GPS.
            delay(650L)
            runStep(StartupStage.MAP_READY, initializeMap)
            _stage.value = StartupStage.COMPLETE
        }
    }

    private suspend fun runStep(stage: StartupStage, action: suspend () -> Unit) {
        try {
            action()
            _stage.value = stage
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            // One optional subsystem must never prevent the launcher home from remaining usable.
            _issues.value = (_issues.value + "${stage.name}: ${t.localizedMessage ?: t.javaClass.simpleName}").takeLast(5)
            Log.e(TAG, "Startup step $stage failed", t)
        }
    }

    companion object { private const val TAG = "LauncherStartup" }
}
