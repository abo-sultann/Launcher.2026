package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.MusicPlaybackState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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
    modifier: Modifier = Modifier
) {
    var currentTimeStr by remember { mutableStateOf("") }
    var showVolumePopup by remember { mutableStateOf(false) }

    LaunchedEffect(is24Hour) {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val sdf = SimpleDateFormat(pattern, Locale("ar"))
        while (true) {
            currentTimeStr = sdf.format(Date())
            delay(1000L)
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
        // Right Side in RTL: Brand & Time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Automotive Launcher Brand Badge
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "مشغل السيارة",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LAUNCHER 2026",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 12.sp
                        ),
                        color = TextPrimary
                    )
                }
            }

            // Live Clock Badge
            Text(
                text = currentTimeStr,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = CyanNeon
            )
        }

        // Center: Music Pill & Safe Mode Alert (if active)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSafeModeActive) {
                Surface(
                    color = HighContrastRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HighContrastRed),
                    modifier = Modifier.clickable { onOpenDiagnostics() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "الوضع الآمن",
                            tint = HighContrastRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الوضع الآمن مفعّل — اضغط للتشخيص",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighContrastRed
                        )
                    }
                }
            }

            if (musicPlaybackState.currentTrack != null) {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (musicPlaybackState.isPlaying) CyanNeon.copy(alpha = 0.5f) else CarbonCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (musicPlaybackState.isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = "الموسيقى",
                            tint = if (musicPlaybackState.isPlaying) CyanNeon else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = musicPlaybackState.currentTrack.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Left Side in RTL: Status Badges (GPS, Volume, Design Mode, Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // GPS Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CarbonSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    contentDescription = "حالة GPS",
                    tint = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (gpsTelemetry.hasGpsFix) "${gpsTelemetry.speedKmH.toInt()} كم/س" else "GPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (gpsTelemetry.hasGpsFix) TextPrimary else TextMuted
                )
            }

            // Quick Volume Button
            Box {
                IconButton(
                    onClick = { showVolumePopup = !showVolumePopup },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_volume_quick")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "التحكم بالصوت",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showVolumePopup,
                    onDismissRequest = { showVolumePopup = false },
                    modifier = Modifier
                        .background(CarbonSurface)
                        .border(1.dp, CarbonCardBorder, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { onVolumeAdjust(-1f) }) {
                            Icon(Icons.Default.Remove, contentDescription = "خفض الصوت", tint = TextPrimary)
                        }
                        IconButton(onClick = onToggleMute) {
                            Icon(Icons.Default.VolumeOff, contentDescription = "كتم الصوت", tint = AmberRacing)
                        }
                        IconButton(onClick = { onVolumeAdjust(1f) }) {
                            Icon(Icons.Default.Add, contentDescription = "رفع الصوت", tint = TextPrimary)
                        }
                    }
                }
            }

            // Design Mode Button (وضع التصميم)
            Surface(
                color = if (isDesignModeActive) AmberRacing else CarbonSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDesignModeActive) AmberRacing else CarbonCardBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleDesignMode() }
                    .testTag("btn_design_mode")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DashboardCustomize,
                        contentDescription = "وضع التصميم",
                        tint = if (isDesignModeActive) CarbonDark else AmberRacing,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isDesignModeActive) "إنهاء التصميم" else "وضع التصميم",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDesignModeActive) CarbonDark else AmberRacing
                    )
                }
            }

            // Quick Settings Shortcut
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_quick_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "الإعدادات",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
