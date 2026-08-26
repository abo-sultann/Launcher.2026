package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.Launcher2026Theme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on for automotive dashboard use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            Launcher2026Theme {
                CarLauncherMainApp(viewModel = mainViewModel)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // In car launcher, pressing back should return to HOME screen safely rather than exiting
        if (mainViewModel.currentScreen.value != CarScreen.HOME) {
            mainViewModel.navigateTo(CarScreen.HOME)
        } else {
            // Stay on home screen
        }
    }
}

private enum class SubOverlayScreen {
    NONE,
    SAFE_AREA_PREVIEW,
    DIAGNOSTICS
}

@Composable
fun CarLauncherMainApp(
    viewModel: MainViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val safeArea by viewModel.safeArea.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    val isSafeModeActive by viewModel.isSafeModeActive.collectAsState()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var activeSubOverlay by remember { mutableStateOf(SubOverlayScreen.NONE) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark),
        topBar = {
            TopCarStatusBar(
                gpsTelemetry = gpsTelemetry,
                musicPlaybackState = playbackState,
                is24Hour = settings.is24HourFormat,
                isSafeModeActive = isSafeModeActive,
                isDesignModeActive = isDesignMode,
                onToggleDesignMode = { viewModel.toggleDesignMode() },
                onOpenSettings = {
                    activeSubOverlay = SubOverlayScreen.NONE
                    viewModel.navigateTo(CarScreen.SETTINGS)
                },
                onOpenDiagnostics = {
                    activeSubOverlay = SubOverlayScreen.DIAGNOSTICS
                },
                onToggleMute = { viewModel.toggleMute() },
                onVolumeAdjust = { delta -> viewModel.adjustVolume(delta) }
            )
        },
        bottomBar = {
            BottomCarNavBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    activeSubOverlay = SubOverlayScreen.NONE
                    viewModel.navigateTo(screen)
                }
            )
        }
    ) { innerPadding ->
        SafeAreaContainer(
            safeArea = safeArea,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CarbonDark)
        ) {
            when (activeSubOverlay) {
                SubOverlayScreen.SAFE_AREA_PREVIEW -> {
                    SafeAreaPreviewScreen(
                        viewModel = viewModel,
                        onBack = { activeSubOverlay = SubOverlayScreen.NONE }
                    )
                }

                SubOverlayScreen.DIAGNOSTICS -> {
                    DiagnosticsScreen(
                        viewModel = viewModel,
                        onBack = { activeSubOverlay = SubOverlayScreen.NONE }
                    )
                }

                SubOverlayScreen.NONE -> {
                    when (currentScreen) {
                        CarScreen.HOME -> HomeScreen(viewModel = viewModel)
                        CarScreen.APPS -> AppDrawerScreen(viewModel = viewModel)
                        CarScreen.MUSIC -> MusicPlayerScreen(viewModel = viewModel)
                        CarScreen.MAP -> OfflineMapScreen(viewModel = viewModel)
                        CarScreen.TRIP -> TripComputerScreen(viewModel = viewModel)
                        CarScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenSafeAreaPreview = { activeSubOverlay = SubOverlayScreen.SAFE_AREA_PREVIEW },
                                onOpenDiagnostics = { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS }
                            )
                        }
                    }
                }
            }
        }
    }
}
