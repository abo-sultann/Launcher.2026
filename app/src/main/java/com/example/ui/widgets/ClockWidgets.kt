package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetStyle
import com.example.ui.components.resolvedWidgetColors
import com.example.ui.components.resolvedWidgetSurface
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockWidget(style: WidgetStyle, is24Hour: Boolean, modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val widgetColors = resolvedWidgetColors()
    val timeFormat = if (is24Hour) "HH:mm" else "hh:mm"
    val timeWithSecFormat = if (is24Hour) "HH:mm:ss" else "hh:mm:ss"
    val timeStr = SimpleDateFormat(timeFormat, Locale("ar")).format(currentTime)
    val timeWithSecStr = SimpleDateFormat(timeWithSecFormat, Locale("ar")).format(currentTime)
    val amPmStr = SimpleDateFormat("a", Locale("ar")).format(currentTime)
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(currentTime)
    val dayStr = SimpleDateFormat("EEEE", Locale("ar")).format(currentTime)

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val micro = maxWidth < 115.dp || maxHeight < 58.dp
        val tiny = maxWidth < 155.dp || maxHeight < 82.dp
        val compact = maxWidth < 225.dp || maxHeight < 125.dp
        val bigTime = when { micro -> 22.sp; tiny -> 30.sp; compact -> 39.sp; else -> 52.sp }
        val mediumTime = when { micro -> 20.sp; tiny -> 27.sp; compact -> 34.sp; else -> 44.sp }
        val smallText = when { micro -> 7.sp; tiny -> 8.sp; compact -> 10.sp; else -> 12.sp }

        when (style) {
            WidgetStyle.CLOCK_DIGITAL_LARGE -> {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(timeStr, fontSize = bigTime, fontWeight = FontWeight.Black, color = widgetColors.accent, maxLines = 1)
                    if (!is24Hour && !micro) Text(amPmStr, fontSize = smallText, color = widgetColors.secondary, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            WidgetStyle.CLOCK_WITH_DATE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(timeStr, fontSize = mediumTime, fontWeight = FontWeight.Bold, color = widgetColors.primary, maxLines = 1)
                    if (!micro) Text("$dayStr • $dateStr", fontSize = smallText, color = widgetColors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            WidgetStyle.CLOCK_WITH_SECONDS -> Text(timeWithSecStr, fontSize = if (micro) 18.sp else if (tiny) 23.sp else if (compact) 29.sp else 39.sp, fontWeight = FontWeight.Bold, color = widgetColors.accent, maxLines = 1)
            WidgetStyle.CLOCK_MINIMAL -> Text(timeStr, fontSize = bigTime, fontWeight = FontWeight.Light, color = widgetColors.primary, maxLines = 1)
            WidgetStyle.CLOCK_CARD -> {
                Surface(color = resolvedWidgetSurface(CarbonSurface.copy(alpha = .90f)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, widgetColors.accent.copy(alpha = .30f)), modifier = Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize().padding(if (compact) 6.dp else 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                        if (!tiny) Icon(Icons.Default.AccessTime, null, tint = widgetColors.accent, modifier = Modifier.size(if (compact) 23.dp else 33.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(timeStr, fontSize = if (micro) 20.sp else if (tiny) 25.sp else if (compact) 30.sp else 35.sp, fontWeight = FontWeight.Bold, color = widgetColors.primary, maxLines = 1)
                            if (!tiny) Text("$dayStr • $dateStr", fontSize = smallText, color = widgetColors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            WidgetStyle.CLOCK_AUTOMOTIVE_LARGE -> {
                Box(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CarbonCard.copy(alpha = .92f), CarbonSurface.copy(alpha = .88f))), RoundedCornerShape(10.dp)).border(1.dp, CarbonCardBorder, RoundedCornerShape(10.dp)).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(timeStr, fontSize = bigTime, fontWeight = FontWeight.Black, color = widgetColors.accent, maxLines = 1)
                        if (!is24Hour && !micro) Text(amPmStr, fontSize = smallText, fontWeight = FontWeight.Bold, color = widgetColors.secondary, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
            }
            WidgetStyle.CLOCK_DAY_DATE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(timeStr, fontSize = mediumTime, fontWeight = FontWeight.Bold, color = widgetColors.accent, maxLines = 1)
                    if (!tiny) {
                        Text(dayStr, fontSize = if (compact) 10.sp else 13.sp, fontWeight = FontWeight.Bold, color = widgetColors.primary, maxLines = 1)
                        Text(dateStr, fontSize = smallText, color = widgetColors.secondary, maxLines = 1)
                    }
                }
            }
            else -> Text(timeStr, style = MaterialTheme.typography.displayMedium, color = widgetColors.primary)
        }
    }
}
