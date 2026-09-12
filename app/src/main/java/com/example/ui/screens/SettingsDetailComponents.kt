package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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

/**
 * Darbak Settings detail shell V2.
 * No side rail: internal pages now use the same wide, low-profile automotive composition
 * as the launcher home with a compact identity header and horizontal category strip.
 */
@Composable
internal fun DarbakSettingsDetailHeader(
    category: SettingsCategory,
    accent: Color,
    onBack: () -> Unit,
    onSelect: (SettingsCategory) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(
            color = CarbonSurface.copy(alpha = .30f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        onClick = onBack,
                        color = accent.copy(alpha = .12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .30f)),
                        modifier = Modifier.size(48.dp).testTag("settings_back"),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowForward, "رجوع", tint = accent, modifier = Modifier.size(22.dp))
                        }
                    }
                    Surface(
                        color = accent.copy(alpha = .10f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(category.icon, null, tint = accent, modifier = Modifier.size(25.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(category.arabicTitle, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(category.subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DARBAK", color = DarbakGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Surface(color = DarbakGold, shape = RoundedCornerShape(3.dp), modifier = Modifier.width(48.dp).height(3.dp)) {}
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().testTag("settings_rail_list"),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(SettingsCategory.values().toList()) { item ->
                        val selected = item == category
                        Surface(
                            onClick = { onSelect(item) },
                            color = if (selected) accent.copy(alpha = .15f) else Color.Black.copy(alpha = .12f),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, if (selected) accent.copy(alpha = .42f) else Color.White.copy(alpha = .06f)),
                            modifier = Modifier.height(42.dp).testTag("settings_rail_" + item.name),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(item.icon, null, tint = if (selected) accent else TextMuted, modifier = Modifier.size(17.dp))
                                Text(
                                    item.arabicTitle,
                                    fontSize = 12.sp,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = Color.Black.copy(alpha = .16f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .055f)),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp)) { content() }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        Modifier.padding(top = 8.dp, bottom = 3.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(color = LocalSettingsAccent.current.copy(alpha = .10f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(32.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(18.dp))
            }
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Surface(color = DarbakGold.copy(alpha = .75f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(28.dp).height(2.dp)) {}
    }
}

@Composable
internal fun SettingsChoice(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val accent = LocalSettingsAccent.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        leadingIcon = leadingIcon,
        modifier = Modifier.heightIn(min = 42.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = .40f) else Color.White.copy(alpha = .06f)),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Black.copy(alpha = .13f),
            selectedContainerColor = accent.copy(alpha = .13f),
            labelColor = TextSecondary,
            selectedLabelColor = TextPrimary,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    DarbakGlassPanel {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        }
    }
}

@Composable
internal fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    ChoiceCard("نظام الساعة", if (is24) "مثال: 18:30" else "مثال: 06:30 م") {
        SettingsChoice(selected = !is24, onClick = { onChange(false) }, label = { Text("12 ساعة", fontSize = 14.sp) })
        SettingsChoice(selected = is24, onClick = { onChange(true) }, label = { Text("24 ساعة", fontSize = 14.sp) })
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DarbakGlassPanel {
        Row(
            Modifier.heightIn(min = 72.dp).padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.testTag("setting_" + title),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CarbonDark,
                    checkedTrackColor = LocalSettingsAccent.current,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Color.Black.copy(alpha = .35f),
                ),
            )
        }
    }
}

@Composable
internal fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    var editing by remember(value) { mutableStateOf(value.coerceIn(min, max).toFloat()) }
    DarbakGlassPanel {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Surface(color = LocalSettingsAccent.current.copy(alpha = .11f), shape = RoundedCornerShape(11.dp)) {
                    Text("${editing.toInt()} $unit", color = LocalSettingsAccent.current, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                }
            }
            Slider(
                value = editing,
                onValueChange = { editing = it },
                onValueChangeFinished = { onChange(editing.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                steps = if (max - min <= 10) (max - min - 1).coerceAtLeast(0) else 0,
                colors = SliderDefaults.colors(activeTrackColor = LocalSettingsAccent.current, thumbColor = LocalSettingsAccent.current),
            )
        }
    }
}

@Composable
internal fun ActionButton(title: String, icon: ImageVector, onClick: () -> Unit) = ActionButton(title, icon, true, onClick)

@Composable
internal fun ActionButton(title: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 9.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Black.copy(alpha = .18f),
            contentColor = TextPrimary,
            disabledContainerColor = Color.Black.copy(alpha = .10f),
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .065f)),
    ) {
        Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
internal fun InfoCard(text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Icon(Icons.Default.Info, null, tint = DarbakGold, modifier = Modifier.size(18.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
internal fun StatusMetric(label: String, value: String, positive: Boolean) {
    DarbakGlassPanel {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = TextMuted, fontSize = 11.sp)
                Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Surface(color = (if (positive) EmeraldSafe else AmberRacing).copy(alpha = .18f), shape = RoundedCornerShape(50), modifier = Modifier.size(11.dp)) {}
        }
    }
}

@Composable
private fun DarbakGlassPanel(content: @Composable () -> Unit) {
    Surface(
        color = CarbonSurface.copy(alpha = .28f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}
