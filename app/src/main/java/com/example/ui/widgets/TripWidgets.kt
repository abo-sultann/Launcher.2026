package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TripData
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.components.resolvedWidgetSurface
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TripWidget(
    style: WidgetStyle,
    tripData: TripData,
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val widgetColors = resolvedWidgetColors()
    val distanceStr = String.format(Locale.US, "%.1f", tripData.distanceKm)
    val movingMinutes = tripData.elapsedMovingTimeSec / 60
    val movingSeconds = tripData.elapsedMovingTimeSec % 60
    val durationStr = String.format(Locale.US, "%02d:%02d", movingMinutes, movingSeconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.TRIP_SPEED_DISTANCE -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "المسافة", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                        Text(text = "$distanceStr كم", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = widgetColors.accent)
                    }
                    Divider(modifier = Modifier.height(36.dp).width(1.dp), color = CarbonCardBorder)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "السرعة", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                        Text(text = "${tripData.currentSpeedKmH.toInt()} كم/س", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary)
                    }
                }
            }

            WidgetStyle.TRIP_SPEED_DURATION -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "مدة القيادة", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                        Text(text = durationStr, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = widgetColors.accent)
                    }
                    Divider(modifier = Modifier.height(36.dp).width(1.dp), color = CarbonCardBorder)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "السرعة الحالية", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                        Text(text = "${tripData.currentSpeedKmH.toInt()} كم/س", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary)
                    }
                }
            }

            WidgetStyle.TRIP_DASHBOARD -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "كمبيوتر الرحلة", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = widgetColors.accent)
                            Text(text = if (tripData.isRunning) "الرحلة قيد التسجيل" else "متوقف", style = MaterialTheme.typography.labelSmall, color = if (tripData.isRunning) EmeraldSafe else TextMuted)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "المسافة", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                                Text(text = "$distanceStr كم", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.accent)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "المتوسط", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                                Text(text = "${tripData.averageSpeedKmH.toInt()} كم/س", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.secondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "الزمن", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                                Text(text = durationStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary)
                            }
                        }
                    }
                }
            }

            WidgetStyle.TRIP_CARD -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "بيانات الرحلة", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                            Text(text = "$distanceStr كم", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary)
                            Text(text = "الزمن: $durationStr", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = if (tripData.isRunning && !tripData.isPaused) onPauseTrip else onStartTrip,
                                modifier = Modifier.size(40.dp).testTag("btn_trip_toggle")
                            ) {
                                Icon(
                                    imageVector = if (tripData.isRunning && !tripData.isPaused) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "بدء / إيقاف الرحلة",
                                    tint = widgetColors.accent,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            IconButton(
                                onClick = onResetTrip,
                                modifier = Modifier.size(40.dp).testTag("btn_trip_reset")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "إعادة ضبط",
                                    tint = widgetColors.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            WidgetStyle.TRIP_FULL_METRICS -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المسافة: $distanceStr كم", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.accent)
                        Text(text = "الزمن: $durationStr", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.secondary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "أعلى سرعة: ${tripData.maxSpeedKmH.toInt()} كم/س", style = MaterialTheme.typography.labelSmall, color = widgetColors.primary)
                        Text(text = "المتوسط: ${tripData.averageSpeedKmH.toInt()} كم/س", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                    }
                }
            }

            else -> {
                Text(text = "المسافة: $distanceStr كم • $durationStr", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
