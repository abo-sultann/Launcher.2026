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
    val timeStr = SimpleDateFormat(timeFormat, Locale("ar")).format(currentTime)
    val timeWithSecStr = SimpleDateFormat(timeWithSecFormat, Locale("ar")).format(currentTime)
    val amPmStr = SimpleDateFormat("a", Locale("ar")).format(currentTime)
    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(currentTime)
    val dayStr = SimpleDateFormat("EEEE", Locale("ar")).format(currentTime)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val tiny = maxWidth < 150.dp || maxHeight < 95.dp
        val compact = maxWidth < 220.dp || maxHeight < 135.dp
        val bigTime = when { tiny -> 27.sp; compact -> 36.sp; else -> 50.sp }
        val mediumTime = when { tiny -> 23.sp; compact -> 31.sp; else -> 42.sp }
        val smallText = when { tiny -> 8.sp; compact -> 10.sp; else -> 12.sp }

        when (style) {
            WidgetStyle.CLOCK_DIGITAL_LARGE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(timeStr, fontSize = bigTime, fontWeight = FontWeight.Black, color = CyanNeon, maxLines = 1)
                    if (!is24Hour) Text(amPmStr, fontSize = smallText, color = TextSecondary)
                }
            }

            WidgetStyle.CLOCK_WITH_DATE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(timeStr, fontSize = mediumTime, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    if (!tiny) {
                        Spacer(Modifier.height(3.dp))
                        Surface(color = CarbonSurface, shape = RoundedCornerShape(6.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)) {
                            Text("$dayStr • $dateStr", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = smallText, color = AmberRacing, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            WidgetStyle.CLOCK_WITH_SECONDS -> {
                Text(timeWithSecStr, fontSize = if (tiny) 21.sp else if (compact) 28.sp else 39.sp, fontWeight = FontWeight.Bold, color = CyanNeon, maxLines = 1)
            }

            WidgetStyle.CLOCK_MINIMAL -> {
                Text(timeStr, fontSize = bigTime, fontWeight = FontWeight.Light, color = TextPrimary, maxLines = 1)
            }

            WidgetStyle.CLOCK_CARD -> {
                Surface(color = CarbonSurface, shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)), modifier = Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize().padding(if (compact) 7.dp else 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                        if (!tiny) Icon(Icons.Default.AccessTime, null, tint = CyanNeon, modifier = Modifier.size(if (compact) 24.dp else 34.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(timeStr, fontSize = if (tiny) 24.sp else if (compact) 29.sp else 34.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                            if (!tiny) Text("$dayStr • $dateStr", fontSize = smallText, color = AmberRacing, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            WidgetStyle.CLOCK_AUTOMOTIVE_LARGE -> {
                Box(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CarbonCard, CarbonSurface)), RoundedCornerShape(10.dp)).border(1.dp, CarbonCardBorder, RoundedCornerShape(10.dp)).padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(timeStr, fontSize = bigTime, fontWeight = FontWeight.Black, color = CyanNeon, maxLines = 1)
                        if (!is24Hour) Text(amPmStr, fontSize = smallText, fontWeight = FontWeight.Bold, color = AmberRacing, modifier = Modifier.padding(bottom = if (tiny) 3.dp else 6.dp))
                    }
                }
            }

            WidgetStyle.CLOCK_DAY_DATE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(timeStr, fontSize = mediumTime, fontWeight = FontWeight.Bold, color = CyanNeon, maxLines = 1)
                    if (!tiny) {
                        Text(dayStr, fontSize = if (compact) 11.sp else 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        Text(dateStr, fontSize = smallText, color = TextSecondary, maxLines = 1)
                    }
                }
            }

            else -> Text(timeStr, style = MaterialTheme.typography.displayMedium, color = TextPrimary)
        }
    }
}
