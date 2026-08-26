package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MapItem
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

    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showMapManagerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4.0f)
                    panOffset += pan
                }
            }
    ) {
        // High-contrast Offline Vector Map Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2 + panOffset.x, height / 2 + panOffset.y)

            // Background Night Palette
            drawRect(color = Color(0xFF0D121B))

            // Navigation Grid Lines
            val gridStep = 60f * zoomScale
            var x = panOffset.x % gridStep
            while (x < width) {
                drawLine(
                    color = Color(0xFF162130),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.5f
                )
                x += gridStep
            }
            var y = panOffset.y % gridStep
            while (y < height) {
                drawLine(
                    color = Color(0xFF162130),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.5f
                )
                y += gridStep
            }

            // Highway Arterial Road
            val highway = Path().apply {
                moveTo(center.x - 400f * zoomScale, center.y + 250f * zoomScale)
                cubicTo(
                    center.x - 100f * zoomScale, center.y + 100f * zoomScale,
                    center.x + 150f * zoomScale, center.y - 120f * zoomScale,
                    center.x + 450f * zoomScale, center.y - 300f * zoomScale
                )
            }
            drawPath(path = highway, color = Color(0xFF283D58), style = Stroke(width = 16f * zoomScale))
            drawPath(path = highway, color = CyanNeon.copy(alpha = 0.8f), style = Stroke(width = 5f * zoomScale))

            // Ring Road
            drawCircle(
                color = AmberRacing.copy(alpha = 0.5f),
                radius = 180f * zoomScale,
                center = center,
                style = Stroke(width = 4f * zoomScale)
            )

            // GPS Vehicle Pointer (Cyan Arrow / Pulse)
            drawCircle(
                color = CyanNeon.copy(alpha = 0.25f),
                radius = 36f,
                center = center
            )
            drawCircle(
                color = CyanNeon,
                radius = 12f,
                center = center
            )
            drawCircle(
                color = CarbonDark,
                radius = 5f,
                center = center
            )
        }

        // Top Navigation Header: Active Map & Coordinates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = CarbonDark.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showMapManagerDialog = true }
                    .testTag("btn_open_map_manager")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = CyanNeon)
                    Column {
                        Text(
                            text = activeMap?.name ?: "الخريطة الافتراضية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "اضغط لتبديل أو استيراد ملفات الخرائط (MBTiles)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberRacing
                        )
                    }
                }
            }

            // GPS Status Pill
            Surface(
                color = CarbonDark.copy(alpha = 0.9f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                        contentDescription = null,
                        tint = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted
                    )
                    Text(
                        text = if (gpsTelemetry.hasGpsFix) "N ${String.format(Locale.US, "%.4f", gpsTelemetry.latitude)}°, E ${String.format(Locale.US, "%.4f", gpsTelemetry.longitude)}°" else "خريطة بدون اتصال",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }

        // Bottom Left: Speedometer HUD Overlay
        Surface(
            color = CarbonDark.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        text = "${gpsTelemetry.speedKmH.toInt()} كم/س",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = CyanNeon
                    )
                    Text(
                        text = "المسافة: ${String.format(Locale.US, "%.1f", tripData.distanceKm)} كم",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Bottom Right: Map Zoom & Recenter Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Recenter GPS
            Surface(
                color = CarbonDark.copy(alpha = 0.9f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = {
                    panOffset = Offset.Zero
                    zoomScale = 1.0f
                }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "إعادة تمركز", tint = CyanNeon)
                }
            }

            // Zoom In (+)
            Surface(
                color = CarbonDark.copy(alpha = 0.9f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f) }) {
                    Icon(Icons.Default.Add, contentDescription = "تكبير", tint = TextPrimary)
                }
            }

            // Zoom Out (-)
            Surface(
                color = CarbonDark.copy(alpha = 0.9f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.Remove, contentDescription = "تصغير", tint = TextPrimary)
                }
            }
        }

        // Map Manager Modal Dialog
        if (showMapManagerDialog) {
            Dialog(onDismissRequest = { showMapManagerDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إدارة الخرائط Offline (MBTiles)",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = CyanNeon
                            )
                            IconButton(onClick = { showMapManagerDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                            }
                        }

                        if (mapError != null) {
                            Text(text = mapError!!, color = HighContrastRed, style = MaterialTheme.typography.bodySmall)
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mapsList) { mapItem ->
                                Surface(
                                    color = if (mapItem.isActive) CyanNeon.copy(alpha = 0.15f) else CarbonSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (mapItem.isActive) CyanNeon else CarbonCardBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mapItem.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (mapItem.isActive) CyanNeon else TextPrimary
                                            )
                                            Text(
                                                text = "الحجم: ${mapItem.fileSizeFormatted} • التاريخ: ${mapItem.dateAdded}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (!mapItem.isActive) {
                                                Button(
                                                    onClick = { viewModel.setActiveMap(mapItem.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("تفعيل", color = CarbonDark, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }

                                            if (mapItem.id != "built_in_default_map") {
                                                IconButton(onClick = { viewModel.deleteMap(mapItem.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = HighContrastRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showMapManagerDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تم", color = CarbonDark, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}
