package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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

/**
 * Darbak Settings internal shell.
 * Keeps the same visual language as the launcher home: dark canvas, transparent
 * automotive cards, restrained cyan/gold accents and no heavy blur/animation.
 */
@Composable
internal fun DarbakSettingsDetailHeader(
    category: SettingsCategory,
    accent: Color,
    onBack: () -> Unit,
    onSelect: (SettingsCategory) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            color = CarbonSurface.copy(alpha = .34f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
            modifier = Modifier.width(190.dp).fillMaxHeight(),
        ) {
            Column(
                Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Surface(
                    onClick = onBack,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("settings_back"),
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(Icons.Default.ArrowForward, null, tint = accent, modifier = Modifier.size(21.dp))
                        Text("الإعدادات", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = .08f))

                LazyColumn(
                    Modifier.testTag("settings_rail_list"),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    items(SettingsCategory.values().toList()) { item ->
                        val selected = item == category
                        Surface(
                            onClick = { onSelect(item) },
                            color = if (selected) accent.copy(alpha = .13f) else Color.Transparent,
                            shape = RoundedCornerShape(15.dp),
                            border = if (selected) BorderStroke(1.dp, accent.copy(alpha = .34f)) else null,
                            modifier = Modifier.fillMaxWidth().testTag("settings_rail_" + item.name),
                        ) {
                            Row(
                                Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Icon(
                                    item.icon,
                                    null,
                                    tint = if (selected) accent else TextMuted,
                                    modifier = Modifier.size(21.dp),
                                )
                                Text(
                                    item.arabicTitle,
                                    fontSize = 13.sp,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = CarbonSurface.copy(alpha = .28f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Surface(
                        color = accent.copy(alpha = .13f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(category.icon, null, tint = accent, modifier = Modifier.size(25.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(category.arabicTitle, color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text(category.subtitle, color = TextSecondary, fontSize = 13.sp)
                    }
                    Box(
                        Modifier.width(48.dp).height(3.dp)
                    ) {
                        Surface(color = DarbakGold, shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxSize()) {}
                    }
                }
            }

            Surface(
                color = CarbonDark.copy(alpha = .24f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .05f)),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Box(Modifier.fillMaxSize().padding(4.dp)) { content() }
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = LocalSettingsAccent.current.copy(alpha = .12f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(34.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(20.dp))
            }
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
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
        modifier = Modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = .42f)) else BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CarbonSurface.copy(alpha = .42f),
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
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) { content() }
        }
    }
}

@Composable
internal fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    ChoiceCard("نظام الساعة", if (is24) "مثال: 18:30" else "مثال: 06:30 م") {
        SettingsChoice(selected = !is24, onClick = { onChange(false) }, label = { Text("12 ساعة", fontSize = 15.sp) })
        SettingsChoice(selected = is24, onClick = { onChange(true) }, label = { Text("24 ساعة", fontSize = 15.sp) })
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DarbakGlassPanel {
        Row(
            Modifier.heightIn(min = 82.dp).padding(horizontal = 17.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.testTag("setting_" + title),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CarbonDark,
                    checkedTrackColor = LocalSettingsAccent.current,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CarbonSurface,
                ),
            )
        }
    }
}

@Composable
internal fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    var editing by remember(value) { mutableStateOf(value.coerceIn(min, max).toFloat()) }
    DarbakGlassPanel {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Surface(
                    color = LocalSettingsAccent.current.copy(alpha = .12f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "${editing.toInt()} $unit",
                        color = LocalSettingsAccent.current,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
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
        modifier = Modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 11.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CarbonSurface.copy(alpha = .56f),
            contentColor = TextPrimary,
            disabledContainerColor = CarbonSurface.copy(alpha = .25f),
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
    ) {
        Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(9.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
internal fun InfoCard(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Info, null, tint = DarbakGold, modifier = Modifier.size(19.dp))
        Text(text, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
internal fun StatusMetric(label: String, value: String, positive: Boolean) {
    DarbakGlassPanel {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, color = TextMuted, fontSize = 12.sp)
                    Text(
                        value,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                color = (if (positive) EmeraldSafe else AmberRacing).copy(alpha = .14f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(12.dp),
            ) {}
        }
    }
}

@Composable
private fun DarbakGlassPanel(content: @Composable () -> Unit) {
    Surface(
        color = CarbonSurface.copy(alpha = .40f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}
