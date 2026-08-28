package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.MusicPlaybackState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private enum class WifiVisualState { CONNECTED, ENABLED, OFF }

@Composable
fun TopCarStatusBar(
    gpsTelemetry: GpsTelemetry,
    musicPlaybackState: MusicPlaybackState,
    is24Hour: Boolean,
    isSafeModeActive: Boolean,
    isDesignModeActive: Boolean,
    onToggleDesignMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onToggleMute: () -> Unit,
    onVolumeAdjust: (Float) -> Unit,
    onActivateChildLock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeStr by remember { mutableStateOf("") }
    var showVolumePopup by remember { mutableStateOf(false) }
    var wifiState by remember { mutableStateOf(readWifiState(context)) }

    LaunchedEffect(is24Hour) {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale("ar"))
        while (true) {
            currentTimeStr = sdf.format(Date())
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            wifiState = readWifiState(context)
            delay(2500L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(CarbonDark.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onActivateChildLock() })
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.DirectionsCar, "Launcher", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    Text("LAUNCHER 2026", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp), color = TextPrimary)
                }
            }
            Text(currentTimeStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = CyanNeon)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            if (isSafeModeActive) {
                Surface(
                    color = HighContrastRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HighContrastRed),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).testTag("safe_mode_status")
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Warning, "الوضع الآمن", tint = HighContrastRed, modifier = Modifier.size(15.dp))
                        Text("الوضع الآمن", style = MaterialTheme.typography.labelSmall, color = HighContrastRed)
                    }
                }
            }

            if (musicPlaybackState.isPlaying && musicPlaybackState.currentTrack != null) {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f)),
                    modifier = Modifier.widthIn(max = 245.dp)
                ) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.GraphicEq, "الموسيقى", tint = CyanNeon, modifier = Modifier.size(15.dp))
                        Text(
                            cleanTrackTitle(musicPlaybackState.currentTrack.title),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            WifiStatusIcon(wifiState)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CarbonSurface).padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Icon(if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, "GPS", tint = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted, modifier = Modifier.size(16.dp))
                Text(if (gpsTelemetry.hasGpsFix) "${gpsTelemetry.speedKmH.toInt()} كم/س" else "GPS", style = MaterialTheme.typography.labelSmall, color = if (gpsTelemetry.hasGpsFix) TextPrimary else TextMuted)
            }

            Box {
                IconButton(onClick = { showVolumePopup = !showVolumePopup }, modifier = Modifier.size(34.dp).testTag("btn_volume_quick")) {
                    Icon(Icons.Default.VolumeUp, "الصوت", tint = TextSecondary, modifier = Modifier.size(19.dp))
                }
                DropdownMenu(
                    expanded = showVolumePopup,
                    onDismissRequest = { showVolumePopup = false },
                    modifier = Modifier.background(CarbonSurface).border(1.dp, CarbonCardBorder, RoundedCornerShape(8.dp))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { onVolumeAdjust(-1f) }) { Icon(Icons.Default.Remove, "خفض الصوت", tint = TextPrimary) }
                        IconButton(onClick = onToggleMute) { Icon(Icons.Default.VolumeOff, "كتم", tint = AmberRacing) }
                        IconButton(onClick = { onVolumeAdjust(1f) }) { Icon(Icons.Default.Add, "رفع الصوت", tint = TextPrimary) }
                    }
                }
            }

            Surface(
                color = if (isDesignModeActive) AmberRacing else CarbonSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDesignModeActive) AmberRacing else CarbonCardBorder),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).testTag("btn_design_mode")
            ) {
                Row(
                    Modifier
                        .pointerInput(Unit) { detectTapGestures(onTap = { onToggleDesignMode() }) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.DashboardCustomize, "التصميم", tint = if (isDesignModeActive) CarbonDark else AmberRacing, modifier = Modifier.size(15.dp))
                    Text(if (isDesignModeActive) "إنهاء" else "تصميم", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isDesignModeActive) CarbonDark else AmberRacing)
                }
            }

            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp).testTag("btn_quick_settings")) {
                Icon(Icons.Default.Settings, "الإعدادات", tint = CyanNeon, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun WifiStatusIcon(state: WifiVisualState) {
    val tint = when (state) {
        WifiVisualState.CONNECTED -> EmeraldSafe
        WifiVisualState.ENABLED -> TextSecondary
        WifiVisualState.OFF -> TextMuted
    }
    val icon = if (state == WifiVisualState.OFF) Icons.Default.WifiOff else Icons.Default.Wifi
    Surface(color = CarbonSurface, shape = RoundedCornerShape(7.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)) {
        Box(Modifier.size(31.dp), contentAlignment = Alignment.Center) {
            Icon(icon, if (state == WifiVisualState.CONNECTED) "Wi-Fi متصل" else if (state == WifiVisualState.ENABLED) "Wi-Fi مفعّل" else "Wi-Fi متوقف", tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Suppress("DEPRECATION")
private fun readWifiState(context: Context): WifiVisualState {
    return try {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi?.isWifiEnabled != true) return WifiVisualState.OFF
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val info = cm?.activeNetworkInfo
        if (info?.isConnected == true && info.type == ConnectivityManager.TYPE_WIFI) WifiVisualState.CONNECTED else WifiVisualState.ENABLED
    } catch (_: Exception) { WifiVisualState.OFF }
}

private fun cleanTrackTitle(raw: String): String {
    var value = raw.substringBeforeLast('.').trim()
    value = value.replace(Regex("(?i)^tiktok[_-]?"), "")
    value = value.replace(Regex("(?i)[_-]?audio[_-]?original$"), "")
    value = value.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
    return value.ifBlank { "تشغيل صوت" }.take(55)
}
