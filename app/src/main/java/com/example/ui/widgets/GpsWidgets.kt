package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GpsTelemetry
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.components.resolvedWidgetSurface
import com.example.ui.theme.*
import com.example.util.bearingToArabicDirection
import java.util.Locale

@Composable
fun GpsWidget(
    style: WidgetStyle,
    gpsTelemetry: GpsTelemetry,
    modifier: Modifier = Modifier
) {
    val widgetColors = resolvedWidgetColors()
    val latStr = String.format(Locale.US, "%.4f", gpsTelemetry.latitude)
    val lngStr = String.format(Locale.US, "%.4f", gpsTelemetry.longitude)
    val altStr = "${gpsTelemetry.altitudeMeters.toInt()} م"
    val directionArabic = bearingToArabicDirection(gpsTelemetry.bearingDegrees)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.GPS_INDICATOR_MINI -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarbonSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted)
                    )
                    Text(
                        text = if (gpsTelemetry.hasGpsFix) "GPS نشط (${gpsTelemetry.satellitesCount})" else "GPS بانتظار إشارة",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (gpsTelemetry.hasGpsFix) EmeraldSafe else widgetColors.secondary
                    )
                }
            }

            WidgetStyle.GPS_CARD -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مستشعر GPS والاتجاه",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = widgetColors.accent
                            )
                            Icon(
                                imageVector = if (gpsTelemetry.hasGpsFix) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                contentDescription = null,
                                tint = if (gpsTelemetry.hasGpsFix) EmeraldSafe else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "الارتفاع", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                                Text(text = altStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "الاتجاه", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                                Text(text = directionArabic, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = widgetColors.accent)
                            }
                        }

                        Text(
                            text = gpsTelemetry.statusArabic,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            WidgetStyle.GPS_WITH_SPEED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${gpsTelemetry.speedKmH.toInt()} كم/س",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = widgetColors.accent
                    )
                    Text(
                        text = directionArabic,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = widgetColors.primary
                    )
                    Text(
                        text = "خط العرض: $latStr • خط الطول: $lngStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = widgetColors.secondary
                    )
                }
            }

            WidgetStyle.GPS_COORDINATES -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "الإحداثيات الجغرافية",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = widgetColors.accent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "N $latStr°",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = widgetColors.primary
                    )
                    Text(
                        text = "E $lngStr°",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = widgetColors.primary
                    )
                }
            }

            WidgetStyle.GPS_ACCURACY -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "دقة تثبيت الموقع", style = MaterialTheme.typography.labelSmall, color = widgetColors.secondary)
                        Text(
                            text = if (gpsTelemetry.hasGpsFix) "±${gpsTelemetry.accuracyMeters.toInt()} متر" else "--",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSafe
                        )
                        Text(
                            text = "الأقمار المتصلة: ${gpsTelemetry.satellitesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            else -> {
                Text(text = gpsTelemetry.statusArabic, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
