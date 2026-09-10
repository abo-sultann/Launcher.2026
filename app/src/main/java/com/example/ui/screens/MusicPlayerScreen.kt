package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun MusicPlayerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.playbackState.collectAsState()
    val track = state.currentTrack
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var volumeOpen by rememberSaveable { mutableStateOf(false) }
    var pendingSeek by remember { mutableStateOf<Float?>(null) }
    BackHandler(expanded || volumeOpen) { expanded = false; volumeOpen = false }
    LaunchedEffect(track?.dataPath) { pendingSeek = null }
    val filtered = remember(state.playlist, query) {
        state.playlist.filter { query.isBlank() || it.title.contains(query.trim(), true) || it.artist.contains(query.trim(), true) }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::importMusicUri) }
    val addMusic = { viewModel.prepareForExternalPicker(); picker.launch("audio/*") }
    val duration = state.durationMs.coerceAtLeast(0L)
    val fraction = if (duration > 0) (state.currentPositionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Column(modifier.fillMaxSize().background(CarbonDark).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DarbakPageHeading("الموسيقى", "${state.playlist.size} مقطع") {
            TextButton(onClick = addMusic, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("إضافة مقطع", fontSize = 16.sp)
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(52.dp).testTag("music_expand")) {
                Icon(if (expanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                    if (expanded) "إظهار القائمة" else "توسيع المشغل", tint = TextPrimary)
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (!expanded) Column(Modifier.width(306.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DarbakSearch(query, { query = it }, "بحث في المقاطع", "music_search", Modifier.fillMaxWidth())
                if (filtered.isEmpty()) DarbakEmptyState(Icons.Default.LibraryMusic,
                    if (state.playlist.isEmpty()) "أضف أول مقطع" else "لا توجد نتائج", Modifier.fillMaxSize())
                else LazyColumn(Modifier.fillMaxSize().testTag("music_queue"), contentPadding = PaddingValues(bottom = 8.dp)) {
                    itemsIndexed(filtered, key = { _, item -> item.id }) { index, item ->
                        val active = item.dataPath == track?.dataPath
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(if (active) CarbonCard else Color.Transparent)
                            .clickable { viewModel.playTrack(item) }.testTag("playlist_item_${item.id}")
                            .heightIn(min = 76.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                if (active) Icon(if (state.isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote, null, tint = CyanNeon)
                                else Text(String.format(Locale.US, "%02d", index + 1), color = TextMuted, fontSize = 14.sp)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(displayTrackTitle(item.title), color = if (active) CyanNeon else TextPrimary,
                                    fontSize = 16.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(displayArtist(item.artist) ?: musicTime(item.durationMs), color = TextSecondary, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Surface(Modifier.weight(1f).fillMaxHeight(), color = CarbonSurface, shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        DarbakMusicArtwork(track?.dataPath, Modifier.size(if (expanded) 240.dp else 184.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (state.isPlaying) "يُشغّل الآن" else "المقطع الحالي", color = CyanNeon, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(track?.let { displayTrackTitle(it.title) } ?: "مكتبتك الصوتية", color = TextPrimary,
                                fontSize = if (expanded) 30.sp else 24.sp, lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("music_title"))
                            displayArtist(track?.artist)?.let { Text(it, color = TextSecondary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                    state.errorMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = HighContrastRed, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    // Media timelines are left-to-right independently of the surrounding Arabic UI.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column {
                            Slider(value = pendingSeek ?: fraction, onValueChange = { pendingSeek = it },
                                onValueChangeFinished = { pendingSeek?.let { viewModel.seekTo((it * duration).toLong()) }; pendingSeek = null },
                                enabled = track != null && duration > 0, modifier = Modifier.fillMaxWidth().testTag("music_seek"),
                                colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon, inactiveTrackColor = CarbonCardBorder))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(musicTime(pendingSeek?.let { (it * duration).toLong() } ?: state.currentPositionMs), color = TextPrimary, fontSize = 14.sp)
                                Text(musicTime(duration), color = TextSecondary, fontSize = 14.sp)
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            MusicControl(Icons.Default.Replay10, "تراجع 10 ثوانٍ", track != null, viewModel::skipBackward10Sec)
                            MusicControl(Icons.Default.SkipPrevious, "المقطع السابق", state.playlist.isNotEmpty(), viewModel::playPrevious)
                            FilledIconButton(onClick = { viewModel.togglePlayPause() }, enabled = state.playlist.isNotEmpty(),
                                shape = CircleShape, modifier = Modifier.size(68.dp).testTag("btn_full_player_play"),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyanNeon, contentColor = CarbonDark)) {
                                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (state.isPlaying) "إيقاف مؤقت" else "تشغيل", Modifier.size(38.dp))
                            }
                            MusicControl(Icons.Default.SkipNext, "المقطع التالي", state.playlist.isNotEmpty(), viewModel::playNext)
                            MusicControl(Icons.Default.Forward10, "تقديم 10 ثوانٍ", track != null, viewModel::skipForward10Sec)
                            MusicControl(Icons.Default.VolumeUp, "مستوى الصوت", true, { volumeOpen = true })
                        }
                    }
                }
            }
        }
    }
    if (volumeOpen) AlertDialog(onDismissRequest = { volumeOpen = false }, title = { Text("مستوى الصوت") },
        text = { Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledTonalButton(onClick = { viewModel.adjustVolume(-1f) }, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.Default.VolumeDown, null); Spacer(Modifier.width(8.dp)); Text("خفض")
            }
            FilledTonalButton(onClick = { viewModel.adjustVolume(1f) }, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(8.dp)); Text("رفع")
            }
        } }, confirmButton = { TextButton(onClick = { volumeOpen = false }) { Text("تم") } })
}

@Composable
private fun MusicControl(icon: ImageVector, label: String, enabled: Boolean, action: () -> Unit) {
    IconButton(onClick = action, enabled = enabled, modifier = Modifier.size(52.dp)) {
        Icon(icon, label, Modifier.size(28.dp), tint = if (enabled) TextPrimary else TextMuted)
    }
}

internal fun displayArtist(value: String?): String? = value?.trim()?.takeUnless {
    it.isEmpty() || it.equals("<unknown>", true) || it.equals("unknown", true)
}

internal fun displayTrackTitle(value: String): String = value.trim().replace('_', ' ').ifBlank { "مقطع صوتي" }
private fun musicTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}
