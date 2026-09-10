package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

internal val LocalSettingsAccent = staticCompositionLocalOf { CyanNeon }

@Composable
internal fun DarbakSettingsDetailHeader(category: SettingsCategory, accent: Color, onBack: () -> Unit,
    onSelect: (SettingsCategory) -> Unit, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.width(180.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("settings_back")) {
                Icon(Icons.Default.ArrowForward, null); Spacer(Modifier.width(10.dp)); Text("الإعدادات", fontSize = 20.sp)
            }
            LazyColumn(Modifier.testTag("settings_rail_list"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(SettingsCategory.values().toList()) { item ->
                    Surface(onClick = { onSelect(item) }, color = if (item == category) CarbonCard else Color.Transparent,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().testTag("settings_rail_"+item.name)) {
                        Row(Modifier.heightIn(min = 48.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(item.icon, null, tint = if (item == category) accent else TextMuted, modifier = Modifier.size(22.dp))
                            Text(item.arabicTitle, fontSize = 14.sp, color = if (item == category) TextPrimary else TextSecondary,
                                fontWeight = if (item == category) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.heightIn(min = 56.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(category.arabicTitle, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(category.subtitle, color = TextSecondary, fontSize = 14.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, icon: ImageVector) {
    Row(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(24.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
internal fun SettingsChoice(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null) {
    FilterChip(selected = selected, onClick = onClick, label = label, leadingIcon = leadingIcon,
        modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), border = null,
        colors = FilterChipDefaults.filterChipColors(containerColor = CarbonSurface,
            selectedContainerColor = CarbonCard, selectedLabelColor = LocalSettingsAccent.current))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = TextSecondary, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        }
    }
}

@Composable
internal fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    ChoiceCard("نظام الساعة", if (is24) "مثال: 18:30" else "مثال: 06:30 م") {
        SettingsChoice(selected = !is24, onClick = { onChange(false) }, label = { Text("12 ساعة", fontSize = 16.sp) })
        SettingsChoice(selected = is24, onClick = { onChange(true) }, label = { Text("24 ساعة", fontSize = 16.sp) })
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 88.dp).padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = TextSecondary, fontSize = 14.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag("setting_"+title))
        }
    }
}

@Composable
internal fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    var editing by remember(value) { mutableStateOf(value.coerceIn(min, max).toFloat()) }
    Surface(color = CarbonSurface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text("${editing.toInt()} $unit", color = LocalSettingsAccent.current, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Slider(value = editing, onValueChange = { editing = it }, onValueChangeFinished = { onChange(editing.toInt()) },
                valueRange = min.toFloat()..max.toFloat(), steps = if (max-min <= 10) (max-min-1).coerceAtLeast(0) else 0)
        }
    }
}

@Composable
internal fun ActionButton(title: String, icon: ImageVector, onClick: () -> Unit) = ActionButton(title, icon, true, onClick)

@Composable
internal fun ActionButton(title: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CarbonCard, contentColor = TextPrimary)) {
        Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
}

@Composable
internal fun InfoCard(text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Info, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Text(text, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
internal fun StatusMetric(label: String, value: String, positive: Boolean) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = TextSecondary, fontSize = 14.sp)
            Text(value, color = if (positive) TextPrimary else TextSecondary, fontSize = 18.sp,
                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
