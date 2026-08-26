package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@Composable
fun TripComputerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tripData by viewModel.tripData.collectAsState()
    val gpsTelemetry by viewModel.gpsTelemetry.collectAsState()

    val movingMin = (tripData.elapsedMovingTimeSec / 60)
    val movingSec = (tripData.elapsedMovingTimeSec % 60)
    val movingTimeStr = String.format(Locale.US, "%02d:%02d", movingMin, movingSec)

    val stopMin = (tripData.elapsedStopTimeSec / 60)
    val stopSec = (tripData.elapsedStopTimeSec % 60)
    val stopTimeStr = String.format(Locale.US, "%02d:%02d", stopMin, stopSec)

    val currentSpeed = if (gpsTelemetry.hasGpsFix) gpsTelemetry.speedKmH else tripData.currentSpeedKmH
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = tween(durationMillis = 350),
        label = "trip_speed"
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Big Cockpit Gauge (in RTL: center/right side)
        Card(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular Speedometer Gauge
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 18.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2)
                    val topLeft = Offset(strokeWidth, strokeWidth)

                    // Track Arc
                    drawArc(
                        color = CarbonCardBorder,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Arc
                    val speedFrac = (animatedSpeed / 240f).coerceIn(0f, 1f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(CyanNeon, AmberRacing, CrimsonSport),
                            center = Offset(size.width / 2, size.height / 2)
                        ),
                        startAngle = 140f,
                        sweepAngle = speedFrac * 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center Numerical Speed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${animatedSpeed.toInt()}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "كم / ساعة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (tripData.isRunning) EmeraldSafe.copy(alpha = 0.2f) else CarbonSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (tripData.isRunning) EmeraldSafe else CarbonCardBorder)
                    ) {
                        Text(
                            text = if (tripData.isRunning && !tripData.isPaused) "الرحلة قيد التسجيل" else if (tripData.isPaused) "الرحلة متوقفة مؤقتاً" else "جاهز لبدء الرحلة",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tripData.isRunning) EmeraldSafe else TextSecondary
                        )
                    }
                }
            }
        }

        // Right Column: Trip Computer Telemetry & Action Buttons
        Card(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سجل وبيانات الرحلة المباشرة",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AmberRacing
                )

                // 2x2 Telemetry Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Distance
                    TelemetryCard(
                        title = "المسافة المقطوعة",
                        value = "${String.format(Locale.US, "%.1f", tripData.distanceKm)} كم",
                        icon = Icons.Default.DirectionsCar,
                        tint = CyanNeon,
                        modifier = Modifier.weight(1f)
                    )
                    // Moving Time
                    TelemetryCard(
                        title = "زمن الحركة",
                        value = movingTimeStr,
                        icon = Icons.Default.Timer,
                        tint = EmeraldSafe,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Max Speed
                    TelemetryCard(
                        title = "أعلى سرعة",
                        value = "${tripData.maxSpeedKmH.toInt()} كم/س",
                        icon = Icons.Default.FlashOn,
                        tint = CrimsonSport,
                        modifier = Modifier.weight(1f)
                    )
                    // Avg Speed
                    TelemetryCard(
                        title = "متوسط السرعة",
                        value = "${tripData.averageSpeedKmH.toInt()} كم/س",
                        icon = Icons.Default.Speed,
                        tint = AmberRacing,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Trip Actions (Start/Pause, Reset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (tripData.isRunning && !tripData.isPaused) {
                                viewModel.pauseTrip()
                            } else {
                                viewModel.startTrip()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tripData.isRunning && !tripData.isPaused) AmberRacing else CyanNeon
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("btn_trip_screen_toggle")
                    ) {
                        Icon(
                            imageVector = if (tripData.isRunning && !tripData.isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CarbonDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tripData.isRunning && !tripData.isPaused) "إيقاف مؤقت" else "بدء تسجيل الرحلة",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CarbonDark
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetTrip() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_trip_screen_reset")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "إعادة ضبط", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
        modifier = modifier.height(68.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            }
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}
