package com.example.ui.widgets

import android.icu.util.IslamicCalendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.Date
import java.util.Locale

@Composable
fun DateWidget(style: WidgetStyle, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(60_000L)
        }
    }

    val widgetColors = resolvedWidgetColors()
    val dayName = remember(now) { SimpleDateFormat("EEEE", Locale("ar")).format(now) }
    val dayNumber = remember(now) { SimpleDateFormat("d", Locale("ar")).format(now) }
    val monthYear = remember(now) { SimpleDateFormat("MMMM yyyy", Locale("ar")).format(now) }
    val gregorian = remember(now) { SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(now) }
    val hijri = remember(now) { formatHijri(now) }

    BoxWithConstraints(modifier.fillMaxSize().padding(7.dp), contentAlignment = Alignment.Center) {
        val tiny = maxWidth < 150.dp || maxHeight < 90.dp
        val compact = maxWidth < 230.dp || maxHeight < 135.dp
        val big = when { tiny -> 24.sp; compact -> 31.sp; else -> 40.sp }
        val medium = when { tiny -> 13.sp; compact -> 17.sp; else -> 21.sp }
        val small = if (tiny) 8.sp else if (compact) 10.sp else 12.sp

        when (style) {
            WidgetStyle.DATE_ONLY -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dayNumber, color = widgetColors.accent, fontSize = big, fontWeight = FontWeight.Black)
                    Column {
                        Text(monthYear, color = widgetColors.primary, fontSize = medium, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (!tiny) Text(dayName, color = widgetColors.secondary, fontSize = small)
                    }
                }
            }

            WidgetStyle.DATE_DAY_DATE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(dayName, color = widgetColors.accent, fontSize = big, fontWeight = FontWeight.Black, maxLines = 1)
                    Text("$dayNumber $monthYear", color = widgetColors.primary, fontSize = medium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            WidgetStyle.DATE_HIJRI_GREGORIAN -> {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    CalendarColumn("هجري", hijri, widgetColors.accent, widgetColors.primary, small, medium)
                    if (!tiny) Box(Modifier.width(1.dp).height(42.dp)) { Surface(color = CarbonCardBorder, modifier = Modifier.fillMaxSize()) {} }
                    CalendarColumn("ميلادي", gregorian, widgetColors.secondary, widgetColors.primary, small, medium)
                }
            }

            WidgetStyle.DATE_CARD -> {
                Surface(
                    color = resolvedWidgetSurface(CarbonSurface.copy(alpha = .58f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, widgetColors.accent.copy(alpha = .30f)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = widgetColors.accent.copy(alpha = .16f), shape = RoundedCornerShape(11.dp), modifier = Modifier.size(if (compact) 43.dp else 54.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, null, tint = widgetColors.accent, modifier = Modifier.size(if (compact) 25.dp else 31.dp))
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                            Text(dayName, color = widgetColors.primary, fontSize = medium, fontWeight = FontWeight.Black, maxLines = 1)
                            Text("$dayNumber $monthYear", color = widgetColors.accent, fontSize = small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!tiny) Text(hijri, color = widgetColors.secondary, fontSize = small, maxLines = 1)
                        }
                    }
                }
            }

            WidgetStyle.DATE_MINIMAL -> Text("$dayName، $gregorian", color = widgetColors.primary, fontSize = medium, fontWeight = FontWeight.Light, maxLines = 1)
            else -> Text("$dayName • $gregorian", color = widgetColors.primary, fontSize = medium, maxLines = 1)
        }
    }
}

@Composable
private fun CalendarColumn(
    label: String,
    value: String,
    labelColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
    labelSize: androidx.compose.ui.unit.TextUnit,
    valueSize: androidx.compose.ui.unit.TextUnit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = labelColor, fontSize = labelSize, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = valueSize, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun formatHijri(date: Date): String {
    return try {
        val calendar = IslamicCalendar(Locale("ar", "SA")).apply { time = date }
        val months = arrayOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        val day = calendar.get(IslamicCalendar.DAY_OF_MONTH)
        val month = months[calendar.get(IslamicCalendar.MONTH).coerceIn(0, 11)]
        val year = calendar.get(IslamicCalendar.YEAR)
        "$day $month $year هـ"
    } catch (_: Exception) { "--" }
}
