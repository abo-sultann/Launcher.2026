package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarbakEmptyState
import com.example.ui.components.DarbakSearch
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

private const val DARB_AL_SOUT_2_PACKAGE = "com.abosultan.darbalsoot.gdrive"

@Composable
fun MusicPlayerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.playbackState.collectAsState()
    val track = state.currentTrack
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var volumeOpen by rememberSaveable { mutableStateOf(false) }
    var pendingSeek by remember { mutableStateOf<Float?>(null) }

    BackHandler(expanded || volumeOpen) {
        expanded = false
        volumeOpen = false
    }
    LaunchedEffect(track?.dataPath) { pendingSeek = null }

    val filtered = remember(state.playlist, query) {
        state.playlist.filter {
            query.isBlank() || it.title.contains(query.trim(), true) || it.artist.contains(query.trim(), true)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::importMusicUri)
    }
    val duration = state.durationMs.coerceAtLeast(0L)
    val fraction = if (duration > 0L) (state.currentPositionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Column(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(
            color = CarbonSurface.copy(alpha = .30f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("الموسيقى", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("${state.playlist.size} مقطع • مشغل دربك", color = TextSecondary, fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.prepareForExternalPicker()
                        picker.launch("audio/*")
                    },
                    modifier = Modifier.height(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black.copy(alpha = .16f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("إضافة", fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = { viewModel.launchApp(DARB_AL_SOUT_2_PACKAGE) },
                    modifier = Modifier.height(42.dp).testTag("darbak_audio_sync"),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanNeon.copy(alpha = .14f), contentColor = TextPrimary),
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = .35f)),
                ) {
                    Icon(Icons.Default.CloudSync, null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("درب الصوت 2", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(42.dp).testTag("music_expand")) {
                    Icon(if (expanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull, null, tint = TextPrimary)
                }
                Surface(color = DarbakGold, shape = RoundedCornerShape(2.dp), modifier = Modifier.width(30.dp).height(3.dp)) {}
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!expanded) {
                Surface(
                    color = CarbonSurface.copy(alpha = .24f),
                    shape = RoundedCornerShape(21.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .055f)),
                    modifier = Modifier.width(285.dp).fillMaxHeight(),
                ) {
                    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarbakSearch(query, { query = it }, "بحث في المقاطع", "music_search", Modifier.fillMaxWidth())
                        if (filtered.isEmpty()) {
                            DarbakEmptyState(
                                Icons.Default.LibraryMusic,
                                if (state.playlist.isEmpty()) "أضف أول مقطع" else "لا توجد نتائج",
                                Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(Modifier.fillMaxSize().testTag("music_queue"), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                itemsIndexed(filtered, key = { _, item -> item.id }) { index, item ->
                                    val active = item.dataPath == track?.dataPath
                                    Surface(
                                        color = if (active) CyanNeon.copy(alpha = .11f) else Color.Black.copy(alpha = .10f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, if (active) CyanNeon.copy(alpha = .30f) else Color.White.copy(alpha = .04f)),
                                        modifier = Modifier.fillMaxWidth().height(59.dp).clickable { viewModel.playTrack(item) }.testTag("playlist_item_${item.id}"),
                                    ) {
                                        Row(
                                            Modifier.fillMaxSize().padding(horizontal = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                                                if (active) Icon(if (state.isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote, null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                                else Text(String.format(Locale.US, "%02d", index + 1), color = TextMuted, fontSize = 11.sp)
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text(displayTrackTitle(item.title), color = if (active) CyanNeon else TextPrimary, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(displayArtist(item.artist) ?: musicTime(item.durationMs), color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = CarbonSurface.copy(alpha = .28f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        DarbakMusicArtwork(track?.dataPath, Modifier.size(if (expanded) 230.dp else 170.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(if (state.isPlaying) "يُشغّل الآن" else "المقطع الحالي", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(
                                track?.let { displayTrackTitle(it.title) } ?: "مكتبتك الصوتية",
                                color = TextPrimary,
                                fontSize = if (expanded) 29.sp else 23.sp,
                                lineHeight = 31.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("music_title"),
                            )
                            displayArtist(track?.artist)?.let { artist ->
                                Text(artist, color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Surface(color = DarbakGold, shape = RoundedCornerShape(2.dp), modifier = Modifier.width(42.dp).height(3.dp)) {}
                                Text("DARBAK MEDIA", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    state.errorMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = HighContrastRed, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column {
                            Slider(
                                value = pendingSeek ?: fraction,
                                onValueChange = { pendingSeek = it },
                                onValueChangeFinished = {
                                    pendingSeek?.let { viewModel.seekTo((it * duration).toLong()) }
                                    pendingSeek = null
                                },
                                enabled = track != null && duration > 0,
                                modifier = Modifier.fillMaxWidth().testTag("music_seek"),
                                colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon, inactiveTrackColor = CarbonCardBorder),
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(musicTime(pendingSeek?.let { (it * duration).toLong() } ?: state.currentPositionMs), color = TextPrimary, fontSize = 11.sp)
                                Text(musicTime(duration), color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            MusicControl(Icons.Default.Replay10, "تراجع 10 ثوانٍ", track != null, viewModel::skipBackward10Sec)
                            MusicControl(Icons.Default.SkipPrevious, "المقطع السابق", state.playlist.isNotEmpty(), viewModel::playPrevious)
                            FilledIconButton(
                                onClick = { viewModel.togglePlayPause() },
                                enabled = state.playlist.isNotEmpty(),
                                shape = CircleShape,
                                modifier = Modifier.size(62.dp).testTag("btn_full_player_play"),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyanNeon, contentColor = CarbonDark),
                            ) {
                                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(34.dp))
                            }
                            MusicControl(Icons.Default.SkipNext, "المقطع التالي", state.playlist.isNotEmpty(), viewModel::playNext)
                            MusicControl(Icons.Default.Forward10, "تقديم 10 ثوانٍ", track != null, viewModel::skipForward10Sec)
                            MusicControl(Icons.Default.VolumeUp, "مستوى الصوت", true) { volumeOpen = true }
                        }
                    }
                }
            }
        }
    }

    if (volumeOpen) AlertDialog(
        onDismissRequest = { volumeOpen = false },
        title = { Text("مستوى الصوت") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { viewModel.adjustVolume(-1f) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.VolumeDown, null); Spacer(Modifier.width(7.dp)); Text("خفض")
                }
                FilledTonalButton(onClick = { viewModel.adjustVolume(1f) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Default.VolumeUp, null); Spacer(Modifier.width(7.dp)); Text("رفع")
                }
            }
        },
        confirmButton = { TextButton(onClick = { volumeOpen = false }) { Text("تم") } },
    )
}

@Composable
private fun MusicControl(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Icon(icon, description, tint = if (enabled) TextPrimary else TextMuted, modifier = Modifier.size(25.dp))
    }
}

private fun musicTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

private fun displayTrackTitle(raw: String): String = raw
    .substringBeforeLast('.', raw)
    .replace('_', ' ')
    .trim()
    .ifBlank { "مقطع صوتي" }

private fun displayArtist(raw: String?): String? = raw
    ?.trim()
    ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) && !it.equals("unknown", true) }
