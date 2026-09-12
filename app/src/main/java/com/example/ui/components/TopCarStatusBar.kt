package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DisplayAutomationController
import com.example.ui.theme.*

private enum class WifiVisualState { CONNECTED, ENABLED, OFF }

/** Minimal Darbak status chrome: Wi‑Fi + quick night brightness only. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopCarStatusBar(
    isSafeModeActive: Boolean,
    onOpenDiagnostics: () -> Unit,
    onActivateChildLock: () -> Unit = {},
    accentColor: Color = CyanNeon,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wifiState by rememberWifiVisualState(context)
    val displayController = remember(context) { DisplayAutomationController(context) }
    var nightOverride by remember { mutableStateOf(displayController.manualOverride()) }

    Box(
        modifier = modifier.fillMaxWidth().height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NightBrightnessQuickButton(
                forcedNight = nightOverride == DisplayAutomationController.ManualOverride.FORCE_NIGHT,
                accentColor = accentColor,
                onClick = { nightOverride = displayController.toggleQuickNight() },
                onLongClick = { openDisplaySettings(context) }
            )
            WifiStatusButton(
                state = wifiState,
                accentColor = accentColor,
                onClick = { openWifiSettings(context) },
                onLongClick = onActivateChildLock
            )
        }

        if (isSafeModeActive) {
            Surface(
                onClick = onOpenDiagnostics,
                color = CarbonDark.copy(alpha = .84f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HighContrastRed.copy(alpha = .80f))
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Warning, "تنبيه النظام", tint = HighContrastRed, modifier = Modifier.size(16.dp))
                    Text("الوضع الآمن", color = HighContrastRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NightBrightnessQuickButton(
    forcedNight: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val tint = if (forcedNight) DarbakGold else accentColor
    Surface(
        color = if (forcedNight) DarbakGold.copy(alpha = .10f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (forcedNight) .86f else .44f)),
        modifier = Modifier
            .size(40.dp, 34.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DarkMode, "الوضع الليلي", tint = tint, modifier = Modifier.size(20.dp))
            if (!forcedNight) {
                Text("A", color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WifiStatusButton(
    state: WifiVisualState,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (state) {
        WifiVisualState.CONNECTED -> EmeraldSafe
        WifiVisualState.ENABLED -> AmberRacing
        WifiVisualState.OFF -> TextMuted
    }
    val description = when (state) {
        WifiVisualState.CONNECTED -> "Wi-Fi متصل"
        WifiVisualState.ENABLED -> "Wi-Fi يعمل لكن غير متصل"
        WifiVisualState.OFF -> "Wi-Fi متوقف"
    }
    val badge = when (state) {
        WifiVisualState.CONNECTED -> "✓"
        WifiVisualState.ENABLED -> "!"
        WifiVisualState.OFF -> "×"
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = .72f)),
        modifier = modifier
            .size(44.dp, 34.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (state == WifiVisualState.OFF) Icons.Default.WifiOff else Icons.Default.Wifi,
                contentDescription = description,
                tint = statusColor,
                modifier = Modifier.size(21.dp)
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 2.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, color = CarbonDark, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun rememberWifiVisualState(context: Context): State<WifiVisualState> {
    val state = remember(context) { mutableStateOf(readWifiState(context)) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = readWifiState(context) }
            override fun onLost(network: Network) { state.value = readWifiState(context) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                state.value = readWifiState(context)
            }
        }

        var registered = false
        try {
            manager?.registerDefaultNetworkCallback(callback)
            registered = manager != null
        } catch (_: Exception) {
            try {
                val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
                manager?.registerNetworkCallback(request, callback)
                registered = manager != null
            } catch (_: Exception) { registered = false }
        }

        state.value = readWifiState(context)
        onDispose {
            if (registered) try { manager?.unregisterNetworkCallback(callback) } catch (_: Exception) { }
        }
    }
    return state
}

private fun readWifiState(context: Context): WifiVisualState {
    return try {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi?.isWifiEnabled != true) return WifiVisualState.OFF
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = manager?.activeNetwork
        val capabilities = if (manager != null && activeNetwork != null) manager.getNetworkCapabilities(activeNetwork) else null
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) WifiVisualState.CONNECTED else WifiVisualState.ENABLED
    } catch (_: Exception) { WifiVisualState.OFF }
}

private fun openWifiSettings(context: Context) {
    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    }
}

private fun openDisplaySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        try { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    }
}
