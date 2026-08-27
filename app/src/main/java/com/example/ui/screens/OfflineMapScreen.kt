package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun OfflineMapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    val mapsList by viewModel.mapsList.collectAsState()
    val activeMap by viewModel.activeMap.collectAsState()
    val mapError by viewModel.mapError.collectAsState()

    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importMapUri)
    }

    Row(
        modifier = modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1.15f).fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الخرائط Offline", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("لا توجد خريطة افتراضية — أنت تختار ملف MBTiles", color = TextSecondary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { mapPicker.launch(arrayOf("application/*", "*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, null, tint = CarbonDark)
                        Spacer(Modifier.width(6.dp))
                        Text("إضافة خريطة", color = CarbonDark, fontWeight = FontWeight.Bold)
                    }
                }

                if (mapError != null) {
                    Surface(color = HighContrastRed.copy(alpha = .12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, HighContrastRed)) {
                        Text(mapError!!, color = HighContrastRed, modifier = Modifier.padding(10.dp))
                    }
                }

                if (mapsList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Map, null, tint = TextMuted, modifier = Modifier.size(58.dp))
                            Text("لا توجد خريطة مضافة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("أضف ملف MBTiles من الذاكرة أو USB/SD", color = TextSecondary)
                            OutlinedButton(onClick = { mapPicker.launch(arrayOf("application/*", "*/*")) }) {
                                Text("اختيار ملف خريطة")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mapsList, key = { it.id }) { mapItem ->
                            Surface(
                                color = if (mapItem.isActive) CyanNeon.copy(alpha = .14f) else CarbonSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (mapItem.isActive) CyanNeon else CarbonCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Map, null, tint = if (mapItem.isActive) CyanNeon else TextSecondary)
                                    Column(Modifier.weight(1f)) {
                                        Text(mapItem.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${mapItem.fileSizeFormatted} • ${mapItem.dateAdded}", color = TextSecondary, fontSize = 11.sp)
                                        if (mapItem.isActive) Text("الخريطة النشطة", color = EmeraldSafe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (!mapItem.isActive) {
                                        Button(onClick = { viewModel.setActiveMap(mapItem.id) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                            Text("تفعيل")
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteMap(mapItem.id) }) {
                                        Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(.85f).fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = .35f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("الحالة المباشرة", color = AmberRacing, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                StatusCard("الخريطة المختارة", activeMap?.name ?: "لا توجد خريطة", Icons.Default.Layers)
                StatusCard(
                    "GPS",
                    if (gpsTelemetry.hasGpsFix) gpsTelemetry.statusArabic else gpsTelemetry.statusArabic.ifBlank { "بانتظار إشارة GPS" },
                    if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed
                )
                StatusCard("السرعة", "${gpsTelemetry.speedKmH.toInt()} كم/س", Icons.Default.Speed)
                StatusCard("مسافة الرحلة", "${String.format(Locale.US, "%.2f", tripData.distanceKm)} كم", Icons.Default.DirectionsCar)

                if (gpsTelemetry.hasGpsFix) {
                    Text(
                        "${String.format(Locale.US, "%.5f", gpsTelemetry.latitude)}, ${String.format(Locale.US, "%.5f", gpsTelemetry.longitude)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, CarbonCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = CyanNeon)
            Column {
                Text(title, color = TextSecondary, fontSize = 11.sp)
                Text(value, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
