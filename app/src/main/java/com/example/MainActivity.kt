package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.Launcher2026Theme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val permissionRequestCode = 2026

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setContent { Launcher2026Theme { CarLauncherMainApp(mainViewModel) } }
        window.decorView.postDelayed({ requestPermissionsIfNeeded() }, 700L)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) mainViewModel.restartGps()
    }

    private fun requestPermissionsIfNeeded() {
        val required = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) required += Manifest.permission.READ_MEDIA_AUDIO
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            required += Manifest.permission.READ_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) required += Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) required += Manifest.permission.ACCESS_FINE_LOCATION
        if (required.isNotEmpty()) ActivityCompat.requestPermissions(this, required.toTypedArray(), permissionRequestCode)
        else mainViewModel.restartGps()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (locationGranted) mainViewModel.restartGps()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mainViewModel.isChildLockActive.value) return
        if (mainViewModel.currentScreen.value != CarScreen.HOME) mainViewModel.navigateTo(CarScreen.HOME)
    }
}

private enum class SubOverlayScreen { NONE, SAFE_AREA_PREVIEW, DIAGNOSTICS }

@Composable
fun CarLauncherMainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val safeArea by viewModel.safeArea.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val screenSaverLayouts by viewModel.screenSaverLayouts.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    val isChildLockActive by viewModel.isChildLockActive.collectAsState()
    val isSafeModeActive by viewModel.isSafeModeActive.collectAsState()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()

    var activeSubOverlay by remember { mutableStateOf(SubOverlayScreen.NONE) }
    var screenSaverVisible by remember { mutableStateOf(false) }
    var interactionToken by remember { mutableStateOf(0L) }
    val rootView = LocalView.current

    SideEffect { rootView.keepScreenOn = settings.keepScreenOn }

    LaunchedEffect(
        settings.screenSaverEnabled,
        settings.screenSaverTimeoutSeconds,
        interactionToken,
        isDesignMode,
        isChildLockActive,
        currentScreen
    ) {
        screenSaverVisible = false
        if (settings.screenSaverEnabled && !isDesignMode && currentScreen != CarScreen.MAP) {
            delay(settings.screenSaverTimeoutSeconds.coerceIn(30, 1800) * 1000L)
            if (currentScreen != CarScreen.MAP) screenSaverVisible = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(screenSaverVisible) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!screenSaverVisible && event.changes.any { it.pressed && !it.previousPressed }) {
                            interactionToken = System.currentTimeMillis()
                        }
                    }
                }
            }
    ) {
        LauncherBackground(settings, Modifier.fillMaxSize())

        val fullCanvas = activeSubOverlay == SubOverlayScreen.NONE && (currentScreen == CarScreen.HOME || currentScreen == CarScreen.MAP)
        if (fullCanvas) {
            if (currentScreen == CarScreen.MAP) {
                OfflineMapScreen(viewModel, Modifier.fillMaxSize())
            } else {
                val topContentInset = safeArea.topDp + if (settings.showTopBar) 48 else 0
                val bottomContentInset = safeArea.bottomDp + if (settings.showBottomBar) 56 else 0
                HomeScreen(
                    viewModel,
                    Modifier
                        .fillMaxSize()
                        .padding(
                            top = topContentInset.dp,
                            bottom = bottomContentInset.dp,
                            start = safeArea.rightDp.dp,
                            end = safeArea.leftDp.dp
                        )
                )
            }

            OverlayLauncherBars(
                viewModel = viewModel,
                currentScreen = currentScreen,
                safeAreaTop = safeArea.topDp,
                safeAreaBottom = safeArea.bottomDp,
                showTop = settings.showTopBar,
                showBottom = settings.showBottomBar,
                gpsTelemetry = gpsTelemetry,
                playbackState = playbackState,
                is24Hour = settings.is24HourFormat,
                isSafeModeActive = isSafeModeActive,
                isDesignMode = isDesignMode,
                onOpenSettings = { activeSubOverlay = SubOverlayScreen.NONE; viewModel.navigateTo(CarScreen.SETTINGS) },
                onOpenDiagnostics = { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS },
                modifier = Modifier.zIndex(500f)
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    if (settings.showTopBar) {
                        Box(Modifier.padding(top = safeArea.topDp.dp)) {
                            TopCarStatusBar(
                                gpsTelemetry,
                                playbackState,
                                settings.is24HourFormat,
                                isSafeModeActive,
                                isDesignMode,
                                onToggleDesignMode = { viewModel.toggleDesignMode() },
                                onOpenSettings = { activeSubOverlay = SubOverlayScreen.NONE; viewModel.navigateTo(CarScreen.SETTINGS) },
                                onOpenDiagnostics = { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS },
                                onToggleMute = { viewModel.toggleMute() },
                                onVolumeAdjust = { viewModel.adjustVolume(it) },
                                onActivateChildLock = { viewModel.activateChildLock() }
                            )
                        }
                    }
                },
                bottomBar = {
                    if (settings.showBottomBar) {
                        Box(Modifier.padding(bottom = safeArea.bottomDp.dp)) {
                            BottomCarNavBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    activeSubOverlay = SubOverlayScreen.NONE
                                    viewModel.navigateTo(screen)
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                val horizontalSafeArea = safeArea.copy(topDp = 0, bottomDp = 0)
                SafeAreaContainer(horizontalSafeArea, Modifier.fillMaxSize().padding(innerPadding)) {
                    when (activeSubOverlay) {
                        SubOverlayScreen.SAFE_AREA_PREVIEW -> SafeAreaPreviewScreen(viewModel, onBack = { activeSubOverlay = SubOverlayScreen.NONE })
                        SubOverlayScreen.DIAGNOSTICS -> DiagnosticsScreen(viewModel, onBack = { activeSubOverlay = SubOverlayScreen.NONE })
                        SubOverlayScreen.NONE -> when (currentScreen) {
                            CarScreen.HOME -> HomeScreen(viewModel)
                            CarScreen.APPS -> AppDrawerScreen(viewModel)
                            CarScreen.MUSIC -> MusicPlayerScreen(viewModel)
                            CarScreen.MAP -> OfflineMapScreen(viewModel)
                            CarScreen.TRIP -> TripComputerScreen(viewModel)
                            CarScreen.SETTINGS -> SettingsScreen(
                                viewModel,
                                { activeSubOverlay = SubOverlayScreen.SAFE_AREA_PREVIEW },
                                { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS }
                            )
                        }
                    }
                }
            }
        }

        if (isChildLockActive) {
            ChildLockOverlay(
                holdSeconds = settings.childUnlockHoldSeconds,
                onUnlock = {
                    viewModel.deactivateChildLock()
                    interactionToken = System.currentTimeMillis()
                },
                modifier = Modifier.zIndex(1000f)
            )
        }

        if (screenSaverVisible && currentScreen != CarScreen.MAP) {
            ScreenSaverOverlay(
                viewModel = viewModel,
                settings = settings,
                widgets = widgets,
                layouts = screenSaverLayouts,
                apps = installedApps,
                playbackState = playbackState,
                gpsTelemetry = gpsTelemetry,
                tripData = tripData,
                activeMap = activeMap,
                onDismiss = {
                    screenSaverVisible = false
                    interactionToken = System.currentTimeMillis()
                },
                modifier = Modifier.zIndex(1100f)
            )
        }
    }
}

@Composable
private fun BoxScope.OverlayLauncherBars(
    viewModel: MainViewModel,
    currentScreen: CarScreen,
    safeAreaTop: Int,
    safeAreaBottom: Int,
    showTop: Boolean,
    showBottom: Boolean,
    gpsTelemetry: com.example.model.GpsTelemetry,
    playbackState: com.example.model.MusicPlaybackState,
    is24Hour: Boolean,
    isSafeModeActive: Boolean,
    isDesignMode: Boolean,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showTop) {
        Box(modifier.align(Alignment.TopCenter).padding(top = safeAreaTop.dp)) {
            TopCarStatusBar(
                gpsTelemetry,
                playbackState,
                is24Hour,
                isSafeModeActive,
                isDesignMode,
                onToggleDesignMode = { viewModel.toggleDesignMode() },
                onOpenSettings = onOpenSettings,
                onOpenDiagnostics = onOpenDiagnostics,
                onToggleMute = { viewModel.toggleMute() },
                onVolumeAdjust = { viewModel.adjustVolume(it) },
                onActivateChildLock = { viewModel.activateChildLock() }
            )
        }
    }

    if (showBottom) {
        Box(modifier.align(Alignment.BottomCenter).padding(bottom = safeAreaBottom.dp)) {
            BottomCarNavBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen -> viewModel.navigateTo(screen) }
            )
        }
    }
}
