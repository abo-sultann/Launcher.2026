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

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(
            color = CarbonSurface.copy(alpha = .30f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("الرحلة", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        when {
                            trip.isRunning && trip.isPaused -> "متوقفة مؤقتًا"
                            trip.isRunning -> "التسجيل جارٍ"
                            settings.autoLogTrips -> "جاهز للبدء التلقائي"
                            else -> "جاهز للبدء اليدوي"
                        },
                        color = if (trip.isRunning) EmeraldSafe else TextSecondary,
                        fontSize = 11.sp,
                    )
                }
                Surface(
                    color = if (gps.hasGpsFix && gps.isSpeedReliable) EmeraldSafe.copy(alpha = .12f) else AmberRacing.copy(alpha = .10f),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, if (gps.hasGpsFix && gps.isSpeedReliable) EmeraldSafe.copy(alpha = .45f) else AmberRacing.copy(alpha = .42f)),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (gps.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, null, tint = if (gps.hasGpsFix) EmeraldSafe else AmberRacing, modifier = Modifier.size(17.dp))
                        Text(if (gps.hasGpsFix) "GPS ±${gps.accuracyMeters.toInt()}م" else "بانتظار GPS", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = DarbakGold, shape = RoundedCornerShape(2.dp), modifier = Modifier.width(34.dp).height(3.dp)) {}
            }
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = CarbonSurface.copy(alpha = .27f),
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(1.dp, if (trip.isRunning) EmeraldSafe.copy(alpha = .36f) else Color.White.copy(alpha = .055f)),
                modifier = Modifier.width(380.dp).fillMaxHeight(),
            ) {
                Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color.Black.copy(alpha = .14f), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().height(88.dp)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (gps.hasGpsFix && gps.isSpeedReliable) gps.speedKmH.toInt().toString() else "--", color = CyanNeon, fontSize = 39.sp, fontWeight = FontWeight.Black)
                                Text("كم/س", color = TextSecondary, fontSize = 11.sp)
                            }
                            VerticalDivider(color = Color.White.copy(alpha = .08f), modifier = Modifier.height(50.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(String.format(Locale.US, "%.2f", trip.distanceKm), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                Text("المسافة كم", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TripMetric("الحركة", formatDuration(trip.elapsedMovingTimeSec), Icons.Default.Timer, EmeraldSafe, Modifier.weight(1f))
                        TripMetric("الوقوف", formatDuration(trip.elapsedStopTimeSec), Icons.Default.PauseCircle, TextSecondary, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TripMetric("أعلى", "${trip.maxSpeedKmH.toInt()} كم/س", Icons.Default.FlashOn, CrimsonSport, Modifier.weight(1f))
                        TripMetric("المتوسط", "${trip.averageSpeedKmH.toInt()} كم/س", Icons.Default.Speed, AmberRacing, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TripMetric("المواقع", trip.placesSavedCount.toString(), Icons.Default.Bookmark, CyanNeon, Modifier.weight(1f))
                        TripMetric("GPS صالح", trip.validGpsSamples.toString(), Icons.Default.SatelliteAlt, EmeraldSafe, Modifier.weight(1f))
                    }
                    Spacer(Modifier.weight(1f))
                    if (!trip.isRunning) {
                        Button(
                            onClick = { viewModel.startTrip() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = CarbonDark)
                            Spacer(Modifier.width(5.dp))
                            Text("بدء رحلة الآن", color = CarbonDark, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            FilledTonalButton(
                                onClick = { if (trip.isPaused) viewModel.startTrip() else viewModel.pauseTrip() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(15.dp),
                            ) {
                                Icon(if (trip.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (trip.isPaused) "متابعة" else "إيقاف مؤقت", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { finishName = ""; showFinishDialog = true },
                                modifier = Modifier.weight(1.1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSafe),
                                shape = RoundedCornerShape(15.dp),
                            ) {
                                Icon(Icons.Default.Save, null, tint = CarbonDark)
                                Spacer(Modifier.width(4.dp))
                                Text("إنهاء وحفظ", color = CarbonDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Surface(
                color = CarbonSurface.copy(alpha = .24f),
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .055f)),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, tint = DarbakGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("الرحلات السابقة", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Text(history.size.toString(), color = CyanNeon, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(7.dp))
                    if (history.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Route, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("لم تُحفظ رحلة بعد", color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(history, key = { it.id }) { saved ->
                                SavedTripCard(
                                    trip = saved,
                                    hasRoute = viewModel.hasSavedTripRoute(saved.id),
                                    onMap = { viewModel.openSavedTripRoute(saved.id) },
                                    onRename = { renameTrip = saved; renameText = saved.name },
                                    onDelete = { deleteTrip = saved },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) AlertDialog(
        onDismissRequest = { showFinishDialog = false },
        title = { Text("حفظ الرحلة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("اترك الاسم فارغًا لإنشاء اسم بالتاريخ والوقت.", color = TextSecondary, fontSize = 13.sp)
                OutlinedTextField(value = finishName, onValueChange = { finishName = it.take(50) }, label = { Text("اسم الرحلة") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { viewModel.finishTrip(finishName.ifBlank { null }); showFinishDialog = false }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("إلغاء") } },
    )

    renameTrip?.let { saved ->
        AlertDialog(
            onDismissRequest = { renameTrip = null },
            title = { Text("تعديل اسم الرحلة") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it.take(50) }, singleLine = true) },
            confirmButton = { Button(onClick = { viewModel.renameSavedTrip(saved.id, renameText); renameTrip = null }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { renameTrip = null }) { Text("إلغاء") } },
        )
    }

    deleteTrip?.let { saved ->
        AlertDialog(
            onDismissRequest = { deleteTrip = null },
            title = { Text("حذف الرحلة؟") },
            text = { Text("سيُحذف سجل «${saved.name}» ومساره نهائيًا.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteSavedTrip(saved.id); deleteTrip = null }, colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTrip = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun TripMetric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = .13f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .045f)),
        modifier = modifier.height(52.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(title, color = TextSecondary, fontSize = 10.sp); Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SavedTripCard(trip: SavedTrip, hasRoute: Boolean, onMap: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = .12f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .045f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text(trip.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatTripDate(trip.startTimeStamp)} • ${String.format(Locale.US, "%.1f", trip.distanceKm)} كم • ${formatDuration(trip.movingTimeSec)}", color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("أعلى ${trip.maxSpeedKmH.toInt()} • متوسط ${trip.averageSpeedKmH.toInt()} • مواقع ${trip.placesSavedCount}", color = TextMuted, fontSize = 9.sp, maxLines = 1)
            }
            FilledTonalIconButton(onClick = onMap, enabled = hasRoute, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Map, "عرض المسار", tint = if (hasRoute) CyanNeon else TextMuted, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = onRename, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Edit, "تعديل الاسم", tint = TextSecondary, modifier = Modifier.size(16.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed, modifier = Modifier.size(16.dp)) }
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
