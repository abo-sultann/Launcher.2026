package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.MapItem
import com.example.model.TripData
import com.example.model.WidgetStyle
import com.example.ui.theme.*

@Composable
fun MapWidget(
    style: WidgetStyle,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    onOpenFullMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonSurface)
            .clickable { onOpenFullMap() }
    ) {
        // Offline Vector Map Canvas (Roads, grid, GPS marker)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Background Grid (simulating navigation tiles)
            val gridStep = 40f * zoomLevel
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0xFF1E2838),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0xFF1E2838),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // Stylized Offline Highway & Primary Roads
            val highwayPath = Path().apply {
                moveTo(width * 0.1f, height * 0.9f)
                cubicTo(
                    width * 0.3f, height * 0.7f,
                    width * 0.6f, height * 0.3f,
                    width * 0.9f, height * 0.1f
                )
            }
            drawPath(
                path = highwayPath,
                color = Color(0xFF334A68),
                style = Stroke(width = 10f * zoomLevel)
            )
            drawPath(
                path = highwayPath,
                color = CyanNeon.copy(alpha = 0.6f),
                style = Stroke(width = 4f * zoomLevel)
            )

            // Secondary Ring Road
            drawCircle(
                color = Color(0xFF26354A),
                radius = (height * 0.35f) * zoomLevel,
                center = Offset(width * 0.5f, height * 0.5f),
                style = Stroke(width = 5f)
            )

            // Live GPS Pinpoint Marker
            val markerCenter = Offset(width * 0.5f, height * 0.5f)
            drawCircle(
                color = CyanNeon.copy(alpha = 0.25f),
                radius = 24f,
                center = markerCenter
            )
            drawCircle(
                color = CyanNeon,
                radius = 8f,
                center = markerCenter
            )
        }

        // Overlay based on Widget Style
        when (style) {
            WidgetStyle.MAP_MINI -> {
                Surface(
                    color = CarbonDark.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                ) {
                    Text(
                        text = "خريطة مصغرة",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon
                    )
                }
            }

            WidgetStyle.MAP_WITH_SPEED -> {
                Surface(
                    color = CarbonDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${gpsTelemetry.speedKmH.toInt()} كم/س",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }
            }

            WidgetStyle.MAP_WITH_GPS -> {
                Surface(
                    color = CarbonDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text(
                        text = if (gpsTelemetry.hasGpsFix) "GPS متصل • ${gpsTelemetry.satellitesCount} أقمار" else "خريطة بدون اتصال",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextSecondary
                    )
                }
            }

            WidgetStyle.MAP_WITH_TRIP -> {
                Surface(
                    color = CarbonDark.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                ) {
                    Text(
                        text = "المسافة: ${String.format(java.util.Locale.US, "%.1f", tripData.distanceKm)} كم",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberRacing
                    )
                }
            }

            else -> {
                // Standard medium/large HUD overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(CarbonDark.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeMap?.name ?: "الخريطة الملاحية Offline",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "اضغط للتكبير والتحكم",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon
                    )
                }
            }
        }
    }
}
