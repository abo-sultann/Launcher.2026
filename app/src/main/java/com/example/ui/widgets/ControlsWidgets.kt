package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MusicPlaybackState
import com.example.model.WidgetStyle
import com.example.ui.theme.*

@Composable
fun ControlsWidget(
    style: WidgetStyle,
    playbackState: MusicPlaybackState,
    onVolumeAdjust: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.CONTROLS_CIRCULAR -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleControlButton(icon = Icons.Default.VolumeDown, label = "خفض", onClick = { onVolumeAdjust(-1f) })
                    CircleControlButton(icon = Icons.Default.VolumeOff, label = "كتم", tint = AmberRacing, onClick = onToggleMute)
                    CircleControlButton(
                        icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        label = "تشغيل",
                        tint = CyanNeon,
                        isPrimary = true,
                        onClick = onTogglePlayPause
                    )
                    CircleControlButton(icon = Icons.Default.VolumeUp, label = "رفع", onClick = { onVolumeAdjust(1f) })
                }
            }

            WidgetStyle.CONTROLS_SQUARE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SquareControlButton(icon = Icons.Default.VolumeDown, label = "خفض الصوت", onClick = { onVolumeAdjust(-1f) })
                    SquareControlButton(
                        icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        label = "الوسائط",
                        tint = CyanNeon,
                        onClick = onTogglePlayPause
                    )
                    SquareControlButton(icon = Icons.Default.VolumeUp, label = "رفع الصوت", onClick = { onVolumeAdjust(1f) })
                }
            }

            WidgetStyle.CONTROLS_HORIZONTAL_BAR -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onVolumeAdjust(-1f) }) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "خفض", tint = TextPrimary)
                        }
                        IconButton(onClick = onToggleMute) {
                            Icon(Icons.Default.VolumeMute, contentDescription = "كتم", tint = AmberRacing)
                        }
                        IconButton(onClick = onTogglePlayPause) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "تشغيل",
                                tint = CyanNeon
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = TextPrimary)
                        }
                        IconButton(onClick = { onVolumeAdjust(1f) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "رفع", tint = TextPrimary)
                        }
                    }
                }
            }

            WidgetStyle.CONTROLS_CARD -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "لوحة التحكم السريع", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onVolumeAdjust(-1f) }) {
                                Icon(Icons.Default.VolumeDown, contentDescription = null, tint = TextPrimary)
                            }
                            IconButton(onClick = onToggleMute) {
                                Icon(Icons.Default.VolumeOff, contentDescription = null, tint = AmberRacing)
                            }
                            IconButton(onClick = onTogglePlayPause) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            IconButton(onClick = { onVolumeAdjust(1f) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextPrimary)
                            }
                        }
                    }
                }
            }

            WidgetStyle.CONTROLS_LARGE_AUTOMOTIVE -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onVolumeAdjust(-1f) },
                        colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "خفض", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                    Button(
                        onClick = onTogglePlayPause,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل/إيقاف",
                            tint = CarbonDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Button(
                        onClick = { onVolumeAdjust(1f) },
                        colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "رفع", tint = TextPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            else -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { onVolumeAdjust(-1f) }) { Icon(Icons.Default.VolumeDown, null, tint = TextPrimary) }
                    IconButton(onClick = onTogglePlayPause) { Icon(Icons.Default.PlayArrow, null, tint = CyanNeon) }
                    IconButton(onClick = { onVolumeAdjust(1f) }) { Icon(Icons.Default.VolumeUp, null, tint = TextPrimary) }
                }
            }
        }
    }
}

@Composable
private fun CircleControlButton(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (isPrimary) CyanNeon.copy(alpha = 0.25f) else CarbonSurface,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPrimary) CyanNeon else CarbonCardBorder),
        modifier = Modifier.size(46.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SquareControlButton(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
        modifier = Modifier.size(54.dp)
    ) {
        IconButton(onClick = onClick) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
            }
        }
    }
}
