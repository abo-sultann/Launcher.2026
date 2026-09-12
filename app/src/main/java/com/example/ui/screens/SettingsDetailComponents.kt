package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
 * Darbak Settings detail shell V3.
 * A settings category is treated as a launcher module, not as a phone-style settings page:
 * - fixed identity/hero module
 * - free dashboard workspace for controls
 * - no side rail and no settings-category strip
 */
@Composable
internal fun DarbakSettingsDetailHeader(
    category: SettingsCategory,
    accent: Color,
    onBack: () -> Unit,
    onSelect: (SettingsCategory) -> Unit,
    content: @Composable () -> Unit,
) {
    val categories = SettingsCategory.values()
    val index = categories.indexOf(category).coerceAtLeast(0)
    val previous = categories[(index - 1 + categories.size) % categories.size]
    val next = categories[(index + 1) % categories.size]

    Row(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DarbakSettingsHero(
            category = category,
            accent = accent,
            previous = previous,
            next = next,
            onBack = onBack,
            onSelect = onSelect,
            modifier = Modifier.width(232.dp).fillMaxHeight(),
        )

        Surface(
            color = Color.Black.copy(alpha = .15f),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = .055f)),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            Box(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) { content() }
        }
    }
}

@Composable
private fun DarbakSettingsHero(
    category: SettingsCategory,
    accent: Color,
    previous: SettingsCategory,
    next: SettingsCategory,
    onBack: () -> Unit,
    onSelect: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = CarbonSurface.copy(alpha = .38f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, DarbakGold.copy(alpha = .26f)),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().height(145.dp).align(Alignment.TopCenter)
            ) {
                Surface(
                    color = accent.copy(alpha = .07f),
                    shape = RoundedCornerShape(bottomStart = 80.dp, bottomEnd = 80.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {}
            }

            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = onBack,
                            color = Color.Black.copy(alpha = .24f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
                            modifier = Modifier.size(44.dp).testTag("settings_back"),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowForward, "رجوع", tint = TextPrimary, modifier = Modifier.size(21.dp))
                            }
                        }
                        Text("DARBAK", color = DarbakGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    }

                    Surface(
                        color = accent.copy(alpha = .14f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .34f)),
                        modifier = Modifier.size(74.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(category.icon, null, tint = accent, modifier = Modifier.size(40.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(category.arabicTitle, color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text(category.subtitle, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = DarbakGold, shape = RoundedCornerShape(4.dp), modifier = Modifier.width(44.dp).height(3.dp)) {}
                        Text("لوحة تحكم", color = DarbakGold.copy(alpha = .88f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("تنقّل بين الوحدات", color = TextMuted, fontSize = 10.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleStepButton(
                            title = previous.arabicTitle,
                            icon = Icons.Default.KeyboardArrowRight,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        ) { onSelect(previous) }
                        ModuleStepButton(
                            title = next.arabicTitle,
                            icon = Icons.Default.KeyboardArrowLeft,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        ) { onSelect(next) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleStepButton(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = .20f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
        modifier = modifier.height(62.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        Modifier.padding(top = 5.dp, bottom = 2.dp, start = 3.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = LocalSettingsAccent.current.copy(alpha = .12f),
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.size(34.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(19.dp))
            }
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        Surface(color = DarbakGold.copy(alpha = .78f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(26.dp).height(2.dp)) {}
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
        modifier = Modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = .48f) else Color.White.copy(alpha = .055f)),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Black.copy(alpha = .12f),
            selectedContainerColor = accent.copy(alpha = .15f),
            labelColor = TextSecondary,
            selectedLabelColor = TextPrimary,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    DarbakControlTile {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { content() }
        }
    }
}

@Composable
internal fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    ChoiceCard("نظام الساعة", if (is24) "18:30" else "06:30 م") {
        SettingsChoice(selected = !is24, onClick = { onChange(false) }, label = { Text("12 ساعة", fontSize = 13.sp) })
        SettingsChoice(selected = is24, onClick = { onChange(true) }, label = { Text("24 ساعة", fontSize = 13.sp) })
    }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DarbakControlTile {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 2)
                    Text(subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            Surface(
                color = if (checked) LocalSettingsAccent.current.copy(alpha = .55f) else Color.White.copy(alpha = .08f),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.fillMaxWidth(if (checked) .72f else .20f).height(3.dp),
            ) {}
        }
    }
}

@Composable
internal fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    var editing by remember(value) { mutableStateOf(value.coerceIn(min, max).toFloat()) }
    DarbakControlTile {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 2)
                Surface(
                    color = LocalSettingsAccent.current.copy(alpha = .13f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LocalSettingsAccent.current.copy(alpha = .20f)),
                ) {
                    Text("${editing.toInt()} $unit", color = LocalSettingsAccent.current, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CarbonSurface.copy(alpha = .34f),
            contentColor = TextPrimary,
            disabledContainerColor = Color.Black.copy(alpha = .10f),
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
    ) {
        Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
internal fun InfoCard(text: String) {
    DarbakControlTile {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Info, null, tint = DarbakGold, modifier = Modifier.size(18.dp))
            Text(text, color = TextSecondary, fontSize = 11.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
internal fun StatusMetric(label: String, value: String, positive: Boolean) {
    DarbakControlTile {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(label, color = TextMuted, fontSize = 10.sp)
                    Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Surface(color = (if (positive) EmeraldSafe else AmberRacing).copy(alpha = .20f), shape = RoundedCornerShape(50), modifier = Modifier.size(12.dp)) {}
            }
            Surface(
                color = (if (positive) EmeraldSafe else AmberRacing).copy(alpha = .52f),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.fillMaxWidth(if (positive) .82f else .34f).height(3.dp),
            ) {}
        }
    }
}

@Composable
private fun DarbakControlTile(content: @Composable () -> Unit) {
    Surface(
        color = CarbonSurface.copy(alpha = .26f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .055f)),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}
