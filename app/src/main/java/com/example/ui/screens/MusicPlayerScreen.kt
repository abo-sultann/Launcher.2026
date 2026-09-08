package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun MusicPlayerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val track = playbackState.currentTrack
    val isPlaying = playbackState.isPlaying
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pendingSeek by remember { mutableStateOf<Float?>(null) }
    BackHandler(expanded) { expanded = false }
    LaunchedEffect(track?.dataPath) { pendingSeek = null }
    val visibleTracks = remember(playbackState.playlist, query) {
        val text = query.trim()
        playbackState.playlist.filter { text.isEmpty() || it.title.contains(text, true) || it.artist.contains(text, true) }
    }
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::importMusicUri)
    }
    val addMusic = {
        viewModel.prepareForExternalPicker()
        musicPicker.launch("audio/*")
    }

    val currentMin = (playbackState.currentPositionMs / 1000) / 60
    val currentSec = (playbackState.currentPositionMs / 1000) % 60
    val durationMin = (playbackState.durationMs / 1000) / 60
    val durationSec = (playbackState.durationMs / 1000) % 60

    val timeCurrentStr = String.format(Locale.US, "%02d:%02d", currentMin, currentSec)
    val timeDurationStr = String.format(Locale.US, "%02d:%02d", durationMin, durationSec)

    val progressFraction = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = modifier.fillMaxSize().background(CarbonDark).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!expanded) Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة التشغيل (${playbackState.playlist.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = addMusic, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة ملف صوتي", tint = AmberRacing)
                        }
                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = CyanNeon)
                    }
                }

                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("بحث عن مقطع", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = CyanNeon) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح البحث") } },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (playbackState.playlist.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillParentMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.LibraryMusic, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("لا توجد ملفات صوتية", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("أضف ملفًا حقيقيًا من ذاكرة الجهاز", color = TextSecondary, fontSize = 10.sp)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = addMusic) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("إضافة ملف") }
                            }
                        }
                    }
                    if (visibleTracks.isEmpty() && playbackState.playlist.isNotEmpty()) item {
                        Text("لا توجد نتائج", color = TextSecondary, modifier = Modifier.padding(16.dp))
                    }
                    items(visibleTracks, key = { it.id }) { item ->
                        val isCurrent = item.dataPath == track?.dataPath
                        Surface(
                            color = if (isCurrent) CyanNeon.copy(alpha = 0.18f) else CarbonSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) CyanNeon else CarbonCardBorder),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.playTrack(item) }
                                .testTag("playlist_item_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isCurrent) CyanNeon else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                                            color = if (isCurrent) CyanNeon else TextPrimary,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                        Text(text = item.artist, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(1.3f).fillMaxHeight(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "الموسيقى", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(52.dp)) {
                        Icon(if (expanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            if (expanded) "إظهار القائمة" else "توسيع المشغل", tint = CyanNeon)
                    }
                }

                DarbakMusicArtwork(track?.dataPath, Modifier.size(124.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = track?.title ?: "لا يوجد ملف صوتي",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = track?.artist ?: "مشغل الوسائط", style = MaterialTheme.typography.bodyMedium, color = AmberRacing, maxLines = 1)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = pendingSeek ?: progressFraction,
                        onValueChange = { pendingSeek = it },
                        onValueChangeFinished = {
                            pendingSeek?.let { viewModel.seekTo((it * playbackState.durationMs).toLong()) }
                            pendingSeek = null
                        },
                        enabled = track != null && playbackState.durationMs > 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon, inactiveTrackColor = CarbonCardBorder)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = timeCurrentStr, style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                        Text(text = timeDurationStr, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.skipBackward10Sec() }, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.Replay10, contentDescription = "تراجع 10 ثوانٍ", tint = TextPrimary, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "المقطع السابق", tint = TextPrimary, modifier = Modifier.size(32.dp))
                    }
                    Button(
                        onClick = { viewModel.togglePlayPause() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp).testTag("btn_full_player_play"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل / إيقاف",
                            tint = CarbonDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "المقطع التالي", tint = TextPrimary, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = { viewModel.skipForward10Sec() }, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.Forward10, contentDescription = "تقديم 10 ثوانٍ", tint = TextPrimary, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}
