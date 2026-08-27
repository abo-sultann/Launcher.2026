package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.CarbonDark
import com.example.ui.theme.Launcher2026Theme
import com.example.ui.viewmodel.MainViewModel

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
        ) {
            mainViewModel.restartGps()
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
        if (mainViewModel.currentScreen.value != CarScreen.HOME) mainViewModel.navigateTo(CarScreen.HOME)
    }
}

private enum class SubOverlayScreen { NONE, SAFE_AREA_PREVIEW, DIAGNOSTICS }

@Composable
fun CarLauncherMainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val safeArea by viewModel.safeArea.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    val isSafeModeActive by viewModel.isSafeModeActive.collectAsState()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    var activeSubOverlay by remember { mutableStateOf(SubOverlayScreen.NONE) }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(CarbonDark),
        topBar = {
            if (settings.showTopBar) Box(Modifier.padding(top = safeArea.topDp.dp)) {
                TopCarStatusBar(gpsTelemetry, playbackState, settings.is24HourFormat, isSafeModeActive, isDesignMode,
                    onToggleDesignMode = { viewModel.toggleDesignMode() },
                    onOpenSettings = { activeSubOverlay = SubOverlayScreen.NONE; viewModel.navigateTo(CarScreen.SETTINGS) },
                    onOpenDiagnostics = { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS },
                    onToggleMute = { viewModel.toggleMute() }, onVolumeAdjust = { viewModel.adjustVolume(it) })
            }
        },
        bottomBar = {
            if (settings.showBottomBar) Box(Modifier.padding(bottom = safeArea.bottomDp.dp)) {
                BottomCarNavBar(currentScreen, onScreenSelected = { screen -> activeSubOverlay = SubOverlayScreen.NONE; viewModel.navigateTo(screen) })
            }
        }
    ) { innerPadding ->
        val horizontalSafeArea = safeArea.copy(topDp = 0, bottomDp = 0)
        SafeAreaContainer(horizontalSafeArea, Modifier.fillMaxSize().padding(innerPadding).background(CarbonDark)) {
            when (activeSubOverlay) {
                SubOverlayScreen.SAFE_AREA_PREVIEW -> SafeAreaPreviewScreen(viewModel, onBack = { activeSubOverlay = SubOverlayScreen.NONE })
                SubOverlayScreen.DIAGNOSTICS -> DiagnosticsScreen(viewModel, onBack = { activeSubOverlay = SubOverlayScreen.NONE })
                SubOverlayScreen.NONE -> when (currentScreen) {
                    CarScreen.HOME -> HomeScreen(viewModel)
                    CarScreen.APPS -> AppDrawerScreen(viewModel)
                    CarScreen.MUSIC -> MusicPlayerScreen(viewModel)
                    CarScreen.MAP -> OfflineMapScreen(viewModel)
                    CarScreen.TRIP -> TripComputerScreen(viewModel)
                    CarScreen.SETTINGS -> SettingsScreen(viewModel, { activeSubOverlay = SubOverlayScreen.SAFE_AREA_PREVIEW }, { activeSubOverlay = SubOverlayScreen.DIAGNOSTICS })
                }
            }
        }
    }
}
