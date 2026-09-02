package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SavedTrip
import com.example.ui.components.CarScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripComputerScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val trip by viewModel.tripData.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    val history by viewModel.tripHistory.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showFinishDialog by remember { mutableStateOf(false) }
    var finishName by remember { mutableStateOf("") }
    var renameTrip by remember { mutableStateOf<SavedTrip?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTrip by remember { mutableStateOf<SavedTrip?>(null) }

    Row(modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.width(390.dp).fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = BorderStroke(1.dp, if (trip.isRunning) EmeraldSafe.copy(alpha = .65f) else CarbonCardBorder)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("رحلتي", color = CyanNeon, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(
                            when {
                                trip.isRunning && trip.isPaused -> "متوقفة مؤقتًا"
                                trip.isRunning -> "الرحلة قيد التسجيل"
                                settings.autoLogTrips -> "جاهز — يبدأ عند حركة مؤكدة"
                                else -> "جاهز للبدء يدويًا"
                            },
                            color = if (trip.isRunning) EmeraldSafe else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Surface(
                        color = if (gps.hasGpsFix && gps.isSpeedReliable) EmeraldSafe.copy(alpha = .15f) else AmberRacing.copy(alpha = .12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (gps.hasGpsFix && gps.isSpeedReliable) EmeraldSafe else AmberRacing)
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(if (gps.hasGpsFix && gps.isSpeedReliable) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, null, tint = if (gps.hasGpsFix && gps.isSpeedReliable) EmeraldSafe else AmberRacing, modifier = Modifier.size(16.dp))
                            Text(if (gps.hasGpsFix) "±${gps.accuracyMeters.toInt()}م" else "GPS", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(color = CarbonSurface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth().height(96.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (gps.hasGpsFix && gps.isSpeedReliable) gps.speedKmH.toInt().toString() else "--", color = CyanNeon, fontSize = 42.sp, fontWeight = FontWeight.Black)
                            Text("كم/س", color = TextSecondary, fontSize = 10.sp)
                        }
                        VerticalDivider(color = CarbonCardBorder, modifier = Modifier.height(58.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format(Locale.US, "%.2f", trip.distanceKm), color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Text("كم", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TripMetric("الحركة", formatDuration(trip.elapsedMovingTimeSec), Icons.Default.Timer, EmeraldSafe, Modifier.weight(1f))
                    TripMetric("الوقوف", formatDuration(trip.elapsedStopTimeSec), Icons.Default.PauseCircle, TextSecondary, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TripMetric("أعلى", "${trip.maxSpeedKmH.toInt()} كم/س", Icons.Default.FlashOn, CrimsonSport, Modifier.weight(1f))
                    TripMetric("المتوسط", "${trip.averageSpeedKmH.toInt()} كم/س", Icons.Default.Speed, AmberRacing, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TripMetric("المواقع", trip.placesSavedCount.toString(), Icons.Default.Bookmark, CyanNeon, Modifier.weight(1f))
                    TripMetric("GPS صالح", trip.validGpsSamples.toString(), Icons.Default.SatelliteAlt, EmeraldSafe, Modifier.weight(1f))
                }

                Spacer(Modifier.weight(1f))

                if (!trip.isRunning) {
                    Button(
                        onClick = { viewModel.startTrip() },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = CarbonDark)
                        Spacer(Modifier.width(5.dp))
                        Text("بدء رحلة الآن", color = CarbonDark, fontWeight = FontWeight.Black)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilledTonalButton(
                            onClick = { if (trip.isPaused) viewModel.startTrip() else viewModel.pauseTrip() },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(if (trip.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (trip.isPaused) "متابعة" else "إيقاف مؤقت")
                        }
                        Button(
                            onClick = { finishName = ""; showFinishDialog = true },
                            modifier = Modifier.weight(1.15f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe)
                        ) {
                            Icon(Icons.Default.Save, null, tint = CarbonDark)
                            Spacer(Modifier.width(4.dp))
                            Text("إنهاء وحفظ", color = CarbonDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = AmberRacing)
                    Spacer(Modifier.width(7.dp))
                    Text("الرحلات السابقة", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Text("${history.size}", color = CyanNeon, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Route, null, tint = TextMuted, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(7.dp))
                            Text("لم تُحفظ رحلة بعد", color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(history, key = { it.id }) { saved ->
                            SavedTripCard(
                                trip = saved,
                                hasRoute = viewModel.hasSavedTripRoute(saved.id),
                                onMap = { viewModel.openSavedTripRoute(saved.id) },
                                onRename = { renameTrip = saved; renameText = saved.name },
                                onDelete = { deleteTrip = saved }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("حفظ الرحلة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يمكنك ترك الاسم فارغًا ليُنشئ Launcher اسمًا بالتاريخ والوقت.", color = TextSecondary, fontSize = 11.sp)
                    OutlinedTextField(value = finishName, onValueChange = { finishName = it.take(50) }, label = { Text("اسم الرحلة") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.finishTrip(finishName.ifBlank { null }); showFinishDialog = false }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("إلغاء") } }
        )
    }

    renameTrip?.let { saved ->
        AlertDialog(
            onDismissRequest = { renameTrip = null },
            title = { Text("تعديل اسم الرحلة") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it.take(50) }, singleLine = true) },
            confirmButton = { Button(onClick = { viewModel.renameSavedTrip(saved.id, renameText); renameTrip = null }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { renameTrip = null }) { Text("إلغاء") } }
        )
    }

    deleteTrip?.let { saved ->
        AlertDialog(
            onDismissRequest = { deleteTrip = null },
            title = { Text("حذف الرحلة؟") },
            text = { Text("سيُحذف سجل «${saved.name}» ومساره نهائيًا.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteSavedTrip(saved.id); deleteTrip = null },
                    colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)
                ) { Text("حذف", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { deleteTrip = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun TripMetric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = modifier.height(55.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(title, color = TextSecondary, fontSize = 9.sp); Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SavedTripCard(trip: SavedTrip, hasRoute: Boolean, onMap: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Column(Modifier.weight(1f)) {
                Text(trip.name, color = TextPrimary, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatTripDate(trip.startTimeStamp)} • ${String.format(Locale.US, "%.1f", trip.distanceKm)} كم • حركة ${formatDuration(trip.movingTimeSec)}", color = TextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("أعلى ${trip.maxSpeedKmH.toInt()} • متوسط ${trip.averageSpeedKmH.toInt()} • مواقع ${trip.placesSavedCount} • ${if (hasRoute) "مسار محفوظ" else "بلا مسار"}", color = TextMuted, fontSize = 8.sp, maxLines = 1)
            }
            FilledTonalIconButton(onClick = onMap, enabled = hasRoute, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Map, "عرض المسار", tint = if (hasRoute) CyanNeon else TextMuted, modifier = Modifier.size(17.dp)) }
            IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "تعديل الاسم", tint = TextSecondary, modifier = Modifier.size(17.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed, modifier = Modifier.size(17.dp)) }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d", hours, minutes) else String.format(Locale.US, "%02d:%02d", minutes, seconds % 60)
}

private fun formatTripDate(timestamp: Long): String = try {
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar")).format(Date(timestamp))
} catch (_: Exception) { "--" }
