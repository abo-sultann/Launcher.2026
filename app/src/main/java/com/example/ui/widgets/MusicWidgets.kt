package com.example.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MusicPlaybackState
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.components.resolvedWidgetSurface
import com.example.ui.theme.*

@Composable
fun MusicWidget(
    style: WidgetStyle,
    playbackState: MusicPlaybackState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val widgetColors = resolvedWidgetColors()
    val track = playbackState.currentTrack
    val title = track?.title ?: "لا توجد موسيقى مشغلة"
    val artist = track?.artist ?: "أضف ملفًا صوتيًا من الإعدادات"
    val isPlaying = playbackState.isPlaying

    val progressFraction = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Vinyl rotation animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rot")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.MUSIC_MINI -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = widgetColors.primary, modifier = Modifier.size(21.dp))
                    }
                    Surface(
                        onClick = onTogglePlayPause,
                        color = widgetColors.accent.copy(alpha = .16f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "تشغيل/إيقاف",
                                tint = widgetColors.accent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = widgetColors.primary, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 5.dp)) {
                        Text(title, color = widgetColors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(if (isPlaying) "يعمل الآن" else "متوقف مؤقتًا", color = widgetColors.secondary, fontSize = 8.sp, maxLines = 1)
                    }
                }
            }

            WidgetStyle.MUSIC_COMPACT -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.primary.copy(alpha = .20f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, color = widgetColors.primary, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(artist, color = widgetColors.secondary, fontSize = 8.sp, maxLines = 1)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = widgetColors.accent,
                                trackColor = widgetColors.primary.copy(alpha = .14f)
                            )
                        }
                        IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.SkipNext, "السابق", tint = widgetColors.primary, modifier = Modifier.size(21.dp))
                        }
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(46.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = widgetColors.accent)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل/إيقاف", tint = CarbonDark, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.SkipPrevious, "التالي", tint = widgetColors.primary, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_COVER -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.primary.copy(alpha = .22f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(widgetColors.primary.copy(alpha = .10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GraphicEq, null, tint = widgetColors.accent, modifier = Modifier.size(34.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(title, color = widgetColors.primary, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(artist, color = widgetColors.secondary, fontSize = 9.sp, maxLines = 1)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                IconButton(onClick = onPrevious, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.SkipNext, "السابق", tint = widgetColors.primary, modifier = Modifier.size(20.dp))
                                }
                                FilledIconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier.size(40.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = widgetColors.accent)
                                ) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل/إيقاف", tint = CarbonDark, modifier = Modifier.size(25.dp))
                                }
                                IconButton(onClick = onNext, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.SkipPrevious, "التالي", tint = widgetColors.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_CONTROLS -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary, maxLines = 1)
                    Text(text = artist, style = MaterialTheme.typography.labelSmall, color = widgetColors.accent, maxLines = 1)

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            onSeek((frac * playbackState.durationMs).toLong())
                        },
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = widgetColors.accent,
                            activeTrackColor = widgetColors.accent,
                            inactiveTrackColor = CarbonCardBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = widgetColors.primary)
                        }
                        IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = "تشغيل/إيقاف",
                                tint = widgetColors.accent,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = widgetColors.primary)
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_LARGE_AUTOMOTIVE -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.accent.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "مشغل صوت لوحة القيادة", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary, maxLines = 1)
                            Text(text = artist, style = MaterialTheme.typography.bodySmall, color = widgetColors.secondary, maxLines = 1)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onPrevious,
                                colors = ButtonDefaults.buttonColors(containerColor = CarbonCard),
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = widgetColors.primary)
                            }

                            Button(
                                onClick = onTogglePlayPause,
                                colors = ButtonDefaults.buttonColors(containerColor = widgetColors.accent),
                                modifier = Modifier.size(54.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل/إيقاف",
                                    tint = CarbonDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Button(
                                onClick = onNext,
                                colors = ButtonDefaults.buttonColors(containerColor = CarbonCard),
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = widgetColors.primary)
                            }
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_MINIMAL -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = title, style = MaterialTheme.typography.labelLarge, color = widgetColors.primary, maxLines = 1)
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل/إيقاف",
                            tint = widgetColors.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            else -> {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
