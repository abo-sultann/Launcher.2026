package com.example.ui.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.TripData
import com.example.model.WidgetStyle
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedWidget(
    style: WidgetStyle,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    speedUnit: String = "كم/س",
    modifier: Modifier = Modifier
) {
    val currentSpeed = if (gpsTelemetry.hasGpsFix) gpsTelemetry.speedKmH else tripData.currentSpeedKmH
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = tween(durationMillis = 400),
        label = "speed_animation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.SPEED_DIGITAL_LARGE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (gpsTelemetry.hasGpsFix || currentSpeed > 0) "${animatedSpeed.toInt()}" else "--",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = CyanNeon
                    )
                    Text(
                        text = speedUnit,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                }
            }

            WidgetStyle.SPEED_GAUGE_CIRCULAR -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Background Arc (240 degrees from 150 to 390)
                        drawArc(
                            color = CarbonCardBorder,
                            startAngle = 150f,
                            sweepAngle = 240f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Active Speed Arc (0 to 220 km/h mapped to 240 degrees)
                        val speedFraction = (animatedSpeed / 220f).coerceIn(0f, 1f)
                        val sweep = speedFraction * 240f
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(CyanNeon, AmberRacing, CrimsonSport),
                                center = Offset(size.width / 2, size.height / 2)
                            ),
                            startAngle = 150f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (gpsTelemetry.hasGpsFix || currentSpeed > 0) "${animatedSpeed.toInt()}" else "--",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = speedUnit,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyanNeon
                        )
                    }
                }
            }

            WidgetStyle.SPEED_GAUGE_SEMI -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, (size.height * 1.8f) - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, size.height * 0.1f)

                        drawArc(
                            color = CarbonCardBorder,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        val speedFraction = (animatedSpeed / 220f).coerceIn(0f, 1f)
                        drawArc(
                            brush = Brush.linearGradient(listOf(CyanNeon, AmberRacing)),
                            startAngle = 180f,
                            sweepAngle = speedFraction * 180f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = if (gpsTelemetry.hasGpsFix || currentSpeed > 0) "${animatedSpeed.toInt()}" else "--",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AmberRacing
                        )
                        Text(
                            text = speedUnit,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            WidgetStyle.SPEED_WITH_UNIT -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (gpsTelemetry.hasGpsFix || currentSpeed > 0) "${animatedSpeed.toInt()}" else "--",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = speedUnit,
                            style = MaterialTheme.typography.titleSmall,
                            color = CyanNeon,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Surface(
                        color = if (gpsTelemetry.hasGpsFix) EmeraldSafe.copy(alpha = 0.2f) else CarbonSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (gpsTelemetry.hasGpsFix) EmeraldSafe else CarbonCardBorder
                        )
                    ) {
                        Text(
                            text = if (gpsTelemetry.hasGpsFix) "سرعة GPS فضائية" else "بانتظار حركة المركبة",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted
                        )
                    }
                }
            }

            WidgetStyle.SPEED_WITH_AVG -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (gpsTelemetry.hasGpsFix || currentSpeed > 0) "${animatedSpeed.toInt()}" else "--",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CyanNeon
                    )
                    Text(
                        text = speedUnit,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المتوسط: ${tripData.averageSpeedKmH.toInt()} $speedUnit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AmberRacing
                    )
                }
            }

            WidgetStyle.SPEED_DASHBOARD -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "عداد السرعة",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyanNeon
                            )
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = if (gpsTelemetry.hasGpsFix || currentSpeed > 0) "${animatedSpeed.toInt()}" else "--",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = TextPrimary
                        )

                        Text(
                            text = "$speedUnit • السرعة القصوى: ${tripData.maxSpeedKmH.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            else -> {
                Text(text = "${animatedSpeed.toInt()} $speedUnit", style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}
