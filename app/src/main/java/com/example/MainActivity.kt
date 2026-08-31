package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
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
import com.example.model.LauncherSettings
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val permissionRequestCode = 2026

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLauncherFullscreen()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setContent { Launcher2026Theme { CarLauncherMainApp(mainViewModel) } }
        window.decorView.postDelayed({ if (!isFinishing) requestPermissionsIfNeeded() }, 2_500L)
    }

    override fun onResume() {
        super.onResume()
        applyLauncherFullscreen()
        window.decorView.postDelayed({
            if (!isFinishing && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            ) mainViewModel.restartGps()
        }, 1_800L)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyLauncherFullscreen()
    }

    @Suppress("DEPRECATION")
    private fun applyLauncherFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        } catch (_: Exception) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
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
        // GPS is started from onResume after the first frame is stable.
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (locationGranted) mainViewModel.restartGps()
            val audioGranted = if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            if (audioGranted) mainViewModel.refreshMusicLibrary()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> { mainViewModel.playNext(); return true }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { mainViewModel.playPrevious(); return true }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> { mainViewModel.togglePlayPause(); return true }
                KeyEvent.KEYCODE_MEDIA_PLAY -> { if (!mainViewModel.playbackState.value.isPlaying) mainViewModel.togglePlayPause(); return true }
                KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> { if (mainViewModel.playbackState.value.isPlaying) mainViewModel.togglePlayPause(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mainViewModel.isChildLockActive.value) return
        if (mainViewModel.currentScreen.value != CarScreen.HOME) mainViewModel.navigateTo(CarScreen.HOME)
    }
}

private enum class SubOverlayScreen { NONE, SAFE_AREA_PREVIEW, DIAGNOSTICS, SCREEN_SAVER_EDITOR }

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
    val offroadMapState by viewModel.offroadMapState.collectAsState()

    var activeSubOverlay by remember { mutableStateOf(SubOverlayScreen.NONE) }
    var screenSaverVisible by remember { mutableStateOf(false) }
    var interactionToken by remember { mutableStateOf(0L) }
    var mapChromeVisible by remember { mutableStateOf(true) }
    var mapChromeToken by remember { mutableStateOf(0L) }
    val rootView = LocalView.current

    SideEffect { rootView.keepScreenOn = settings.keepScreenOn }

    LaunchedEffect(settings.screenSaverEnabled, settings.screenSaverTimeoutSeconds, interactionToken, isDesignMode, isChildLockActive, currentScreen) {
        screenSaverVisible = false
        if (settings.screenSaverEnabled && !isDesignMode && currentScreen != CarScreen.MAP) {
            delay(settings.screenSaverTimeoutSeconds.coerceIn(30, 1800) * 1000L)
            if (currentScreen != CarScreen.MAP) screenSaverVisible = true
        }
    }

    LaunchedEffect(currentScreen, mapChromeToken) {
        if (currentScreen == CarScreen.MAP) {
            mapChromeVisible = true
            delay(4500L)
            if (currentScreen == CarScreen.MAP) mapChromeVisible = false
        } else mapChromeVisible = true
    }

    Box(
        Modifier.fillMaxSize().pointerInput(screenSaverVisible, currentScreen) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed && !it.previousPressed }) {
                        if (!screenSaverVisible) interactionToken = System.currentTimeMillis()
                        if (currentScreen == CarScreen.MAP) {
                            mapChromeVisible = true
                            mapChromeToken = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    ) {
        LauncherBackground(settings, Modifier.fillMaxSize())

        val fullCanvas = activeSubOverlay == SubOverlayScreen.NONE && (currentScreen == CarScreen.HOME || currentScreen == CarScreen.MAP)
        if (activeSubOverlay == SubOverlayScreen.SCREEN_SAVER_EDITOR) {
            ScreenSaverEditorScreen(
                viewModel = viewModel,
                onDone = { activeSubOverlay = SubOverlayScreen.NONE },
                modifier = Modifier.fillMaxSize()
            )
        } else if (fullCanvas) {
            if (currentScreen == CarScreen.MAP) {
                // Recreate only when external actions (such as opening a saved trip) switch follow mode.
                key(offroadMapState.followGps) {
                    EnhancedOfflineMapScreen(viewModel, Modifier.fillMaxSize())
                }
                PersistentOffroadMapOverlay(viewModel, Modifier.fillMaxSize().zIndex(220f))
            } else {
                // Wi-Fi is a floating status control, not a bar that should consume wallpaper.
                val topContentInset = safeArea.topDp
                val bottomContentInset = safeArea.bottomDp + if (settings.showBottomBar) 54 else 0
                HomeScreen(
                    viewModel,
                    Modifier.fillMaxSize().padding(
                        top = topContentInset.dp,
                        bottom = bottomContentInset.dp,
                        start = safeArea.rightDp.dp,
                        end = safeArea.leftDp.dp
                    )
                )
            }

            val showMapChrome = currentScreen != CarScreen.MAP || mapChromeVisible
            OverlayLauncherBars(
                viewModel = viewModel,
                currentScreen = currentScreen,
                safeAreaTop = safeArea.topDp,
                safeAreaBottom = safeArea.bottomDp,
                // The map owns its full top edge. Keeping the launcher status strip here
                // covered map labels and appeared as a permanent dark rectangle.
                showTop = currentScreen != CarScreen.MAP && settings.showTopBar && showMapChrome,
                showBottom = settings.showBottomBar && showMapChrome,
                settings = settings,
                isSafeModeActive = isSafeModeActive,
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
                                isSafeModeActive = isSafeModeActive,
                                onOpenDiagnostics = { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS },
                                onActivateChildLock = { viewModel.activateChildLock() },
                                accentColor = Color(settings.interfaceAccent.argb)
                            )
                        }
                    }
                },
                bottomBar = {
                    if (settings.showBottomBar) {
                        Box(Modifier.padding(bottom = safeArea.bottomDp.dp)) {
                            BottomCarNavBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen -> activeSubOverlay = SubOverlayScreen.NONE; viewModel.navigateTo(screen) },
                                surfaceStyle = settings.bottomDockStyle,
                                opacityPercent = settings.bottomDockOpacityPercent,
                                accentColor = Color(settings.interfaceAccent.argb),
                                highContrast = settings.highContrastMode
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
                        SubOverlayScreen.SCREEN_SAVER_EDITOR -> Unit
                        SubOverlayScreen.NONE -> when (currentScreen) {
                            CarScreen.HOME -> HomeScreen(viewModel)
                            CarScreen.APPS -> AppDrawerScreen(viewModel)
                            CarScreen.MUSIC -> MusicPlayerScreen(viewModel)
                            CarScreen.MAP -> EnhancedOfflineMapScreen(viewModel)
                            CarScreen.TRIP -> TripComputerScreen(viewModel)
                            CarScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onOpenSafeAreaPreview = { activeSubOverlay = SubOverlayScreen.SAFE_AREA_PREVIEW },
                                onOpenDiagnostics = { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS },
                                onOpenScreenSaverEditor = { activeSubOverlay = SubOverlayScreen.SCREEN_SAVER_EDITOR }
                            )
                        }
                    }
                }
            }
        }

        if (isChildLockActive) {
            ChildLockOverlay(
                holdSeconds = settings.childUnlockHoldSeconds,
                onUnlock = { viewModel.deactivateChildLock(); interactionToken = System.currentTimeMillis() },
                modifier = Modifier.zIndex(1000f)
            )
        }

        if (screenSaverVisible && currentScreen != CarScreen.MAP) {
            ScreenSaverOverlay(
                viewModel = viewModel,
                settings = settings,
                widgets = widgets,
                layouts = screenSaverLayouts,
                playbackState = playbackState,
                gpsTelemetry = gpsTelemetry,
                tripData = tripData,
                activeMap = activeMap,
                onDismiss = { screenSaverVisible = false; interactionToken = System.currentTimeMillis() },
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
    settings: LauncherSettings,
    isSafeModeActive: Boolean,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showTop) {
        Box(modifier.align(Alignment.TopCenter).padding(top = safeAreaTop.dp)) {
            TopCarStatusBar(
                isSafeModeActive = isSafeModeActive,
                onOpenDiagnostics = onOpenDiagnostics,
                onActivateChildLock = { viewModel.activateChildLock() },
                accentColor = Color(settings.interfaceAccent.argb)
            )
        }
    }
    if (showBottom) {
        Box(modifier.align(Alignment.BottomCenter).padding(bottom = safeAreaBottom.dp)) {
            BottomCarNavBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen -> viewModel.navigateTo(screen) },
                surfaceStyle = settings.bottomDockStyle,
                opacityPercent = settings.bottomDockOpacityPercent,
                accentColor = Color(settings.interfaceAccent.argb),
                highContrast = settings.highContrastMode
            )
        }
    }
}
