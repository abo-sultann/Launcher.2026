package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetStyle
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DateWidget(
    style: WidgetStyle,
    modifier: Modifier = Modifier
) {
    val now = remember { Date() }
    val dayName = remember(now) { SimpleDateFormat("EEEE", Locale("ar")).format(now) }
    val dateGregorian = remember(now) { SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(now) }
    val monthName = remember(now) { SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(now) }

    // Offline Hijri calendar calculation
    val hijriStr = remember(now) {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        // Approximate offline hijri calculation for pure offline robustness
        val hijriYear = ((year - 622) * 1.030684).toInt()
        val hijriMonths = arrayOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
        val monthIdx = ((dayOfYear / 30.5).toInt()).coerceIn(0, 11)
        val day = ((dayOfYear % 30) + 1).coerceIn(1, 30)
        "$day ${hijriMonths[monthIdx]} $hijriYear هـ"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            WidgetStyle.DATE_ONLY -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = dateGregorian,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon
                    )
                }
            }

            WidgetStyle.DATE_DAY_DATE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = CyanNeon
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
            }

            WidgetStyle.DATE_HIJRI_GREGORIAN -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = hijriStr,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AmberRacing
                    )
                    Text(
                        text = dateGregorian,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            WidgetStyle.DATE_CARD -> {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = AmberRacing,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon
                            )
                        }
                    }
                }
            }

            WidgetStyle.DATE_MINIMAL -> {
                Text(
                    text = "$dayName, $dateGregorian",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
                    color = TextPrimary
                )
            }

            else -> {
                Text(text = "$dayName • $dateGregorian", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
