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
    val track = playbackState.currentTrack
    val title = track?.title ?: "لا توجد موسيقى مشغلة"
    val artist = track?.artist ?: "مشغل السيارة 2026"
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                        Text(text = artist, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(40.dp).testTag("btn_music_mini_play")) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = CyanNeon,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "التالي في RTL", tint = TextPrimary)
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_COMPACT -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1)
                                Text(text = artist, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = CyanNeon,
                            trackColor = CarbonCardBorder
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = TextPrimary)
                            }
                            IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(42.dp)) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "تشغيل/إيقاف",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = TextPrimary)
                            }
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_COVER -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Vinyl Disc
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF2A2E3D), Color(0xFF0F1218))))
                                .border(2.dp, CyanNeon.copy(alpha = 0.5f), CircleShape)
                                .rotate(if (isPlaying) rotationAngle else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(AmberRacing),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CarbonDark)
                                )
                            }
                        }

                        // Info & Controls
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1)
                                Text(text = artist, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = TextPrimary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(36.dp).testTag("btn_music_cover_play")) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "تشغيل/إيقاف",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = TextPrimary, modifier = Modifier.size(20.dp))
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
                    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1)
                    Text(text = artist, style = MaterialTheme.typography.labelSmall, color = CyanNeon, maxLines = 1)

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            onSeek((frac * playbackState.durationMs).toLong())
                        },
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = CarbonCardBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = TextPrimary)
                        }
                        IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = "تشغيل/إيقاف",
                                tint = CyanNeon,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = TextPrimary)
                        }
                    }
                }
            }

            WidgetStyle.MUSIC_LARGE_AUTOMOTIVE -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "مشغل صوت لوحة القيادة", style = MaterialTheme.typography.labelSmall, color = AmberRacing)
                            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1)
                            Text(text = artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
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
                                Icon(Icons.Default.SkipNext, contentDescription = "السابق", tint = TextPrimary)
                            }

                            Button(
                                onClick = onTogglePlayPause,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
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
                                Icon(Icons.Default.SkipPrevious, contentDescription = "التالي", tint = TextPrimary)
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
                    Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل/إيقاف",
                            tint = CyanNeon,
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
