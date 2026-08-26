package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetStyle
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockWidget(
    style: WidgetStyle,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val timeFormat = if (is24Hour) "HH:mm" else "hh:mm"
    val timeWithSecFormat = if (is24Hour) "HH:mm:ss" else "hh:mm:ss"
    val amPmFormat = "a"
    val dateFormat = "yyyy/MM/dd"
    val dayFormat = "EEEE"

    val timeStr = SimpleDateFormat(timeFormat, Locale("ar")).format(currentTime)
    val timeWithSecStr = SimpleDateFormat(timeWithSecFormat, Locale("ar")).format(currentTime)
    val amPmStr = SimpleDateFormat(amPmFormat, Locale("ar")).format(currentTime)
    val dateStr = SimpleDateFormat(dateFormat, Locale("ar")).format(currentTime)
    val dayStr = SimpleDateFormat(dayFormat, Locale("ar")).format(currentTime)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.CLOCK_DIGITAL_LARGE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = CyanNeon
                    )
                    if (!is24Hour) {
                        Text(
                            text = amPmStr,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }
            }

            WidgetStyle.CLOCK_WITH_DATE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = CarbonSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
                    ) {
                        Text(
                            text = "$dayStr • $dateStr",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberRacing
                        )
                    }
                }
            }

            WidgetStyle.CLOCK_WITH_SECONDS -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeWithSecStr,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = CyanNeon
                    )
                    Text(
                        text = "توقيت محلي دقيق",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            WidgetStyle.CLOCK_MINIMAL -> {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = TextPrimary
                )
            }

            WidgetStyle.CLOCK_CARD -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "$dayStr, $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberRacing
                            )
                        }
                    }
                }
            }

            WidgetStyle.CLOCK_AUTOMOTIVE_LARGE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(CarbonCard, CarbonSurface)),
                            RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, CarbonCardBorder, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = CyanNeon
                            )
                            if (!is24Hour) {
                                Text(
                                    text = amPmStr,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AmberRacing,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        Text(
                            text = "نظام الوقت المباشر",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            WidgetStyle.CLOCK_DAY_DATE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CyanNeon
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayStr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            else -> {
                Text(text = timeStr, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
            }
        }
    }
}
