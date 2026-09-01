package com.example.ui.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.TripData
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.components.resolvedWidgetSurface
import com.example.ui.theme.*

@Composable
fun SpeedWidget(
    style: WidgetStyle,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    speedUnit: String = "كم/س",
    modifier: Modifier = Modifier
) {
    val widgetColors = resolvedWidgetColors()
    val trusted = gpsTelemetry.hasGpsFix && gpsTelemetry.isSpeedReliable
    val currentSpeed = if (trusted) gpsTelemetry.speedKmH.coerceIn(0f, 180f) else 0f
    val animatedSpeed by animateFloatAsState(targetValue = currentSpeed, animationSpec = tween(300), label = "speed")
    val display = if (trusted) animatedSpeed.toInt().toString() else "--"

    BoxWithConstraints(modifier.fillMaxSize().padding(7.dp), contentAlignment = Alignment.Center) {
        val tiny = maxWidth < 145.dp || maxHeight < 95.dp
        val compact = maxWidth < 210.dp || maxHeight < 140.dp
        val largeNumber = when { tiny -> 30.sp; compact -> 40.sp; else -> 56.sp }
        val mediumNumber = when { tiny -> 26.sp; compact -> 34.sp; else -> 44.sp }
        val labelSize = if (tiny) 8.sp else if (compact) 10.sp else 12.sp

        when (style) {
            WidgetStyle.SPEED_DIGITAL_LARGE -> {
                // رقم فقط: أخف ودجت للقيادة، بلا بطاقة أو وحدة أو عناصر زائدة.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = display,
                        fontSize = largeNumber,
                        fontWeight = FontWeight.Black,
                        color = androidx.compose.ui.graphics.Color.Black,
                        maxLines = 1
                    )
                }
            }

            WidgetStyle.SPEED_GAUGE_CIRCULAR -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(if (tiny) 7.dp else 11.dp)) {
                        val stroke = (if (tiny) 8.dp else 12.dp).toPx()
                        val diameter = minOf(size.width, size.height) - stroke
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)
                        drawArc(CarbonCardBorder, 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        val fraction = (animatedSpeed / 180f).coerceIn(0f, 1f)
                        drawArc(Brush.sweepGradient(listOf(widgetColors.accent.copy(alpha = .45f), widgetColors.accent), Offset(size.width / 2f, size.height / 2f)), 150f, fraction * 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(display, fontSize = mediumNumber, fontWeight = FontWeight.Black, color = widgetColors.primary, maxLines = 1)
                        Text(speedUnit, fontSize = labelSize, color = widgetColors.accent)
                    }
                }
            }

            WidgetStyle.SPEED_GAUGE_SEMI -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp)) {
                        val stroke = (if (tiny) 7.dp else 10.dp).toPx()
                        val arcSize = Size((size.width - stroke).coerceAtLeast(1f), (size.height * 1.55f - stroke).coerceAtLeast(1f))
                        val topLeft = Offset(stroke / 2f, size.height * .12f)
                        drawArc(CarbonCardBorder, 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(Brush.linearGradient(listOf(widgetColors.accent.copy(alpha = .45f), widgetColors.accent)), 180f, (animatedSpeed / 180f).coerceIn(0f, 1f) * 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = if (tiny) 8.dp else 14.dp)) {
                        Text(display, fontSize = mediumNumber, fontWeight = FontWeight.Black, color = widgetColors.primary, maxLines = 1)
                        Text(speedUnit, fontSize = labelSize, color = widgetColors.secondary)
                    }
                }
            }

            WidgetStyle.SPEED_WITH_UNIT -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(display, fontSize = largeNumber, fontWeight = FontWeight.Black, color = widgetColors.primary, maxLines = 1)
                        Text(speedUnit, fontSize = labelSize, color = widgetColors.accent, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    if (!tiny) Text(if (trusted) "GPS ±${gpsTelemetry.accuracyMeters.toInt()}م" else "بانتظار GPS ثابت", fontSize = labelSize, color = if (trusted) EmeraldSafe else TextMuted)
                }
            }

            WidgetStyle.SPEED_WITH_AVG -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(display, fontSize = mediumNumber, fontWeight = FontWeight.Bold, color = widgetColors.accent, maxLines = 1)
                    Text(speedUnit, fontSize = labelSize, color = widgetColors.secondary)
                    if (!tiny) Text("المتوسط ${tripData.averageSpeedKmH.toInt()}", fontSize = labelSize, fontWeight = FontWeight.SemiBold, color = widgetColors.secondary)
                }
            }

            WidgetStyle.SPEED_DASHBOARD -> {
                Surface(color = resolvedWidgetSurface(CarbonSurface), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(if (compact) 7.dp else 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        if (!tiny) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("السرعة", fontSize = labelSize, fontWeight = FontWeight.Bold, color = widgetColors.accent)
                            Icon(Icons.Default.Speed, null, tint = widgetColors.accent, modifier = Modifier.size(17.dp))
                        }
                        Text(display, fontSize = mediumNumber, fontWeight = FontWeight.Black, color = widgetColors.primary, maxLines = 1)
                        if (!tiny) Text("$speedUnit • أعلى ${tripData.maxSpeedKmH.toInt()}", fontSize = labelSize, color = widgetColors.secondary, maxLines = 1)
                    }
                }
            }

            else -> Text("$display $speedUnit", fontSize = mediumNumber, fontWeight = FontWeight.Bold, color = widgetColors.primary)
        }
    }
}
