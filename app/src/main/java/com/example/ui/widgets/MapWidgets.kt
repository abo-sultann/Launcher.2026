package com.example.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.MapItem
import com.example.model.OffroadNavigationTarget
import com.example.model.TripData
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.theme.*
import com.example.util.bearingToArabicDirection
import java.util.Locale

@Composable
fun MapWidget(
    style: WidgetStyle,
    gpsTelemetry: GpsTelemetry,
    tripData: TripData,
    activeMap: MapItem?,
    navigationTarget: OffroadNavigationTarget? = null,
    targetDistanceMeters: Float? = null,
    targetBearing: Float? = null,
    onOpenFullMap: () -> Unit,
    interactionEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val widgetColors = resolvedWidgetColors()
    val hasTarget = navigationTarget != null && targetDistanceMeters != null
    val direction = if (hasTarget && targetBearing != null) bearingToArabicDirection(targetBearing) else if (gpsTelemetry.hasGpsFix) bearingToArabicDirection(gpsTelemetry.bearingDegrees) else "--"
    val distance = targetDistanceMeters?.let(::formatWidgetDistance)
    val title = navigationTarget?.name ?: activeMap?.name ?: "الخريطة"

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().clickable(enabled = interactionEnabled, onClick = onOpenFullMap).padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        val tiny = maxWidth < 155.dp || maxHeight < 100.dp
        val compact = maxWidth < 230.dp || maxHeight < 145.dp

        when (style) {
            WidgetStyle.MAP_MINI -> {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = widgetColors.accent.copy(alpha = .16f), shape = CircleShape, modifier = Modifier.size(if (tiny) 40.dp else 50.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Navigation,
                                null,
                                tint = widgetColors.accent,
                                modifier = Modifier.size(if (tiny) 24.dp else 30.dp).rotate(if (hasTarget) targetBearing ?: 0f else gpsTelemetry.bearingDegrees)
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(if (hasTarget) distance ?: "--" else direction, color = widgetColors.primary, fontSize = if (tiny) 16.sp else 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(if (hasTarget) direction else if (gpsTelemetry.hasGpsFix) "اضغط لفتح الخريطة" else "بانتظار GPS", color = if (hasTarget) widgetColors.accent else widgetColors.secondary, fontSize = if (tiny) 8.sp else 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            WidgetStyle.MAP_WITH_SPEED -> {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (gpsTelemetry.hasGpsFix && gpsTelemetry.isSpeedReliable) gpsTelemetry.speedKmH.toInt().toString() else "--", color = widgetColors.accent, fontSize = if (tiny) 25.sp else 34.sp, fontWeight = FontWeight.Black)
                        Text("كم/س", color = widgetColors.secondary, fontSize = 8.sp)
                    }
                    VerticalDivider(Modifier.height(if (tiny) 36.dp else 50.dp), color = CarbonCardBorder)
                    NavigationSummary(title, distance, direction, hasTarget, tiny)
                }
            }

            WidgetStyle.MAP_WITH_GPS -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed, null, tint = if (gpsTelemetry.hasGpsFix) widgetColors.accent else widgetColors.secondary, modifier = Modifier.size(20.dp))
                        Text(if (gpsTelemetry.hasGpsFix) "GPS ±${gpsTelemetry.accuracyMeters.toInt()}م" else "بانتظار GPS", color = widgetColors.primary, fontWeight = FontWeight.Bold, fontSize = if (tiny) 9.sp else 11.sp)
                    }
                    if (!tiny && gpsTelemetry.hasGpsFix) {
                        Spacer(Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.5f , %.5f", gpsTelemetry.latitude, gpsTelemetry.longitude), color = widgetColors.secondary, fontSize = 9.sp, maxLines = 1)
                    }
                    if (hasTarget) Text("${distance ?: "--"} • $direction", color = widgetColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            WidgetStyle.MAP_WITH_TRIP -> {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    NavigationSummary(title, distance, direction, hasTarget, tiny)
                    if (!tiny) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Route, null, tint = widgetColors.secondary, modifier = Modifier.size(19.dp))
                            Text(String.format(Locale.US, "%.1f كم", tripData.distanceKm), color = widgetColors.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("رحلتي", color = widgetColors.secondary, fontSize = 8.sp)
                        }
                    }
                }
            }

            WidgetStyle.MAP_LARGE, WidgetStyle.MAP_MEDIUM -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = widgetColors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(title, color = widgetColors.primary, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("فتح", color = widgetColors.accent, fontSize = 9.sp)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                        Icon(Icons.Default.Navigation, null, tint = widgetColors.accent, modifier = Modifier.size(if (compact) 27.dp else 36.dp).rotate(if (hasTarget) targetBearing ?: 0f else gpsTelemetry.bearingDegrees))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (hasTarget) distance ?: "--" else direction, color = widgetColors.primary, fontSize = if (compact) 19.sp else 25.sp, fontWeight = FontWeight.Black)
                            Text(if (hasTarget) direction else if (gpsTelemetry.hasGpsFix) "اتجاه السيارة" else "لا توجد إشارة", color = widgetColors.secondary, fontSize = 9.sp)
                        }
                    }
                    if (!tiny) Text(if (activeMap != null) "الخريطة: ${activeMap.name}" else "أضف خريطة Mapsforge للاستخدام دون إنترنت", color = widgetColors.secondary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            else -> NavigationSummary(title, distance, direction, hasTarget, tiny)
        }
    }
}

@Composable
private fun NavigationSummary(title: String, distance: String?, direction: String, hasTarget: Boolean, tiny: Boolean) {
    val widgetColors = resolvedWidgetColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (hasTarget) distance ?: "--" else direction, color = widgetColors.accent, fontSize = if (tiny) 17.sp else 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(if (hasTarget) title else "الخريطة", color = widgetColors.primary, fontSize = if (tiny) 8.sp else 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (hasTarget) Text(direction, color = widgetColors.secondary, fontSize = 8.sp, maxLines = 1)
    }
}

private fun formatWidgetDistance(meters: Float): String = if (meters < 1000f) "${meters.toInt()} م" else String.format(Locale.US, "%.1f كم", meters / 1000f)
