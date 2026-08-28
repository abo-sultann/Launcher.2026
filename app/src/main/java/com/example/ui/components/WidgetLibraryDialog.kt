package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.WidgetStyle
import com.example.model.WidgetType
import com.example.ui.theme.*

@Composable
fun WidgetLibraryDialog(
    initialType: WidgetType? = null,
    isStyleChangerMode: Boolean = false,
    onDismiss: () -> Unit,
    onSelectStyle: (WidgetStyle) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType ?: WidgetType.CLOCK) }
    val stylesForType = remember(selectedType) { WidgetStyle.values().filter { it.type == selectedType } }
    var selectedStyle by remember(selectedType) { mutableStateOf(stylesForType.firstOrNull() ?: WidgetStyle.CLOCK_MINIMAL) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(.95f).fillMaxHeight(.92f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = BorderStroke(1.5.dp, CyanNeon.copy(alpha = .55f))
        ) {
            Column(Modifier.fillMaxSize().padding(13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (isStyleChangerMode) "اختر تصميم الودجت" else "مكتبة الودجات", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("المعاينة هي الشكل الذي سيظهر تقريبًا على الرئيسية", color = TextSecondary, fontSize = 10.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "إغلاق", tint = TextSecondary) }
                }

                if (!isStyleChangerMode) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WidgetType.values()) { type ->
                            val active = selectedType == type
                            Surface(
                                color = if (active) CyanNeon else CarbonSurface,
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, if (active) CyanNeon else CarbonCardBorder),
                                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { selectedType = type }.testTag("tab_type_${type.name}")
                            ) {
                                Text(type.arabicTitle, color = if (active) CarbonDark else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(9.dp))
                Text("${selectedType.arabicTitle} — اختر الشكل", color = AmberRacing, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))

                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stylesForType.chunked(2)) { rowStyles ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowStyles.forEach { style ->
                                val active = selectedStyle == style
                                PreviewStyleCard(
                                    style = style,
                                    selected = active,
                                    onClick = { selectedStyle = style },
                                    modifier = Modifier.weight(1f).testTag("style_item_${style.name}")
                                )
                            }
                            if (rowStyles.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(9.dp)) { Text("إلغاء", color = TextPrimary) }
                    Button(
                        onClick = { onSelectStyle(selectedStyle) },
                        modifier = Modifier.weight(1f).testTag("btn_confirm_widget_style"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(9.dp)
                    ) {
                        Icon(if (isStyleChangerMode) Icons.Default.Check else Icons.Default.Add, null, tint = CarbonDark, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isStyleChangerMode) "تطبيق" else "إضافة", color = CarbonDark, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewStyleCard(style: WidgetStyle, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (selected) CyanNeon.copy(alpha = .10f) else CarbonSurface.copy(alpha = .88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) CyanNeon else CarbonCardBorder),
        modifier = modifier.height(132.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxSize().padding(7.dp)) {
            Box(
                Modifier.fillMaxWidth().weight(1f).background(Color.Black.copy(alpha = .18f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                WidgetStylePreview(style)
                if (selected) {
                    Surface(color = CyanNeon, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(21.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = CarbonDark, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(style.arabicName, color = if (selected) CyanNeon else TextPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(style.description, color = TextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WidgetStylePreview(style: WidgetStyle) {
    when (style.type) {
        WidgetType.CLOCK -> ClockStylePreview(style)
        WidgetType.SPEEDOMETER -> SpeedStylePreview(style)
        WidgetType.DATE -> DateStylePreview(style)
        WidgetType.GPS -> GpsStylePreview(style)
        WidgetType.MUSIC -> MusicStylePreview(style)
        WidgetType.MAP -> MapStylePreview(style)
        WidgetType.TRIP -> TripStylePreview(style)
        WidgetType.APPS -> AppsStylePreview(style)
        WidgetType.CONTROLS -> ControlsStylePreview(style)
    }
}

@Composable
private fun ClockStylePreview(style: WidgetStyle) {
    when (style) {
        WidgetStyle.CLOCK_MINIMAL -> Text("9:23", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Light)
        WidgetStyle.CLOCK_WITH_SECONDS -> Text("9:23:45", color = CyanNeon, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        WidgetStyle.CLOCK_WITH_DATE, WidgetStyle.CLOCK_DAY_DATE -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("9:23", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("الخميس • 28 أغسطس", color = AmberRacing, fontSize = 8.sp) }
        WidgetStyle.CLOCK_CARD -> Surface(color = CarbonCard, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CyanNeon.copy(alpha = .35f))) { Text("9:23", color = TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) }
        WidgetStyle.CLOCK_AUTOMOTIVE_LARGE -> Row(verticalAlignment = Alignment.Bottom) { Text("9:23", color = CyanNeon, fontSize = 32.sp, fontWeight = FontWeight.Black); Text(" PM", color = AmberRacing, fontSize = 8.sp, modifier = Modifier.padding(bottom = 5.dp)) }
        else -> Text("9:23", color = CyanNeon, fontSize = 34.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SpeedStylePreview(style: WidgetStyle) {
    when (style) {
        WidgetStyle.SPEED_GAUGE_CIRCULAR -> Surface(color = Color.Transparent, shape = CircleShape, border = BorderStroke(6.dp, CyanNeon.copy(alpha = .65f)), modifier = Modifier.size(66.dp)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("82", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black) } }
        WidgetStyle.SPEED_GAUGE_SEMI -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("⌒", color = CyanNeon, fontSize = 32.sp, fontWeight = FontWeight.Black); Text("82", color = TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black) }
        WidgetStyle.SPEED_WITH_AVG -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("82", color = CyanNeon, fontSize = 28.sp, fontWeight = FontWeight.Black); Text("متوسط 67", color = AmberRacing, fontSize = 8.sp) }
        WidgetStyle.SPEED_DASHBOARD -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Speed, null, tint = CyanNeon); Column { Text("82", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("كم/س", color = TextSecondary, fontSize = 8.sp) } }
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("82", color = CyanNeon, fontSize = 32.sp, fontWeight = FontWeight.Black); Text("كم/س", color = TextSecondary, fontSize = 8.sp) }
    }
}

@Composable private fun DateStylePreview(style: WidgetStyle) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if (style == WidgetStyle.DATE_ONLY || style == WidgetStyle.DATE_MINIMAL) "28 أغسطس" else "الخميس", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold); if (style != WidgetStyle.DATE_ONLY && style != WidgetStyle.DATE_MINIMAL) Text("28 أغسطس 2026", color = AmberRacing, fontSize = 9.sp) } }
@Composable private fun GpsStylePreview(style: WidgetStyle) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) { Icon(Icons.Default.GpsFixed, null, tint = EmeraldSafe, modifier = Modifier.size(if (style == WidgetStyle.GPS_INDICATOR_MINI) 28.dp else 22.dp)); Column { Text(if (style == WidgetStyle.GPS_COORDINATES) "26.31 • 43.97" else "GPS جيد", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp); if (style != WidgetStyle.GPS_INDICATOR_MINI) Text("±6 م • 9 أقمار", color = TextSecondary, fontSize = 8.sp) } } }
@Composable private fun MusicStylePreview(style: WidgetStyle) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) { if (style == WidgetStyle.MUSIC_COVER || style == WidgetStyle.MUSIC_LARGE_AUTOMOTIVE) Surface(color = AmberRacing, shape = CircleShape, modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = CarbonDark) } }; Icon(Icons.Default.SkipPrevious, null, tint = TextPrimary, modifier = Modifier.size(17.dp)); Surface(color = CyanNeon, shape = CircleShape, modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = CarbonDark) } }; Icon(Icons.Default.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(17.dp)); if (style != WidgetStyle.MUSIC_MINI) Text("المقطع الحالي", color = TextSecondary, fontSize = 8.sp) } }
@Composable private fun MapStylePreview(style: WidgetStyle) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.TurnRight, null, tint = CyanNeon, modifier = Modifier.size(34.dp)); Column { Text("1.6 كم", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(if (style == WidgetStyle.MAP_WITH_TRIP) "رحلتي 42 كم" else "اتجه يمين", color = TextSecondary, fontSize = 8.sp) } } }
@Composable private fun TripStylePreview(style: WidgetStyle) { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("124", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black); Text("كم", color = TextSecondary, fontSize = 7.sp) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("1:46", color = CyanNeon, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("مدة", color = TextSecondary, fontSize = 7.sp) }; if (style == WidgetStyle.TRIP_DASHBOARD || style == WidgetStyle.TRIP_FULL_METRICS) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("72", color = AmberRacing, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("متوسط", color = TextSecondary, fontSize = 7.sp) } } }
@Composable private fun AppsStylePreview(style: WidgetStyle) { Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { val count = when (style) { WidgetStyle.APPS_GRID_2X2 -> 4; WidgetStyle.APPS_GRID_3X2 -> 6; WidgetStyle.APPS_GRID_4X2 -> 8; else -> 5 }; repeat(count.coerceAtMost(6)) { i -> Surface(color = listOf(CyanNeon, AmberRacing, EmeraldSafe, CrimsonSport)[i % 4].copy(alpha = .85f), shape = RoundedCornerShape(7.dp), modifier = Modifier.size(28.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Apps, null, tint = CarbonDark, modifier = Modifier.size(15.dp)) } } } } }
@Composable private fun ControlsStylePreview(style: WidgetStyle) { Row(horizontalArrangement = Arrangement.spacedBy(if (style == WidgetStyle.CONTROLS_LARGE_AUTOMOTIVE) 12.dp else 7.dp), verticalAlignment = Alignment.CenterVertically) { listOf(Icons.Default.VolumeDown, Icons.Default.SkipPrevious, Icons.Default.PlayArrow, Icons.Default.SkipNext, Icons.Default.VolumeUp).forEach { icon -> Surface(color = if (icon == Icons.Default.PlayArrow) CyanNeon else CarbonCard, shape = if (style == WidgetStyle.CONTROLS_CIRCULAR) CircleShape else RoundedCornerShape(8.dp), modifier = Modifier.size(if (style == WidgetStyle.CONTROLS_LARGE_AUTOMOTIVE) 34.dp else 27.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (icon == Icons.Default.PlayArrow) CarbonDark else TextPrimary, modifier = Modifier.size(15.dp)) } } } } }
