package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
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
 * Darbak Settings detail shell V4.
 * Full-width automotive control center: no side rail, no duplicated navigation,
 * and no permanent hero panel consuming the 1024x600 workspace.
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

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = .22f)),
            modifier = Modifier.fillMaxWidth().height(84.dp),
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = .12f),
                            CarbonSurface.copy(alpha = .52f),
                            Color.Black.copy(alpha = .18f),
                        )
                    )
                )
            ) {
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.fillMaxWidth(.22f).height(3.dp).align(Alignment.TopEnd),
                ) {}

                Row(
                    Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        onClick = onBack,
                        color = Color.Black.copy(alpha = .25f),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .09f)),
                        modifier = Modifier.size(46.dp).testTag("settings_back"),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowForward, "رجوع", tint = TextPrimary, modifier = Modifier.size(22.dp))
                        }
                    }

                    Surface(
                        color = accent.copy(alpha = .15f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = .38f)),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(category.icon, null, tint = accent, modifier = Modifier.size(28.dp))
                        }
                    }

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(category.arabicTitle, color = TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text(category.subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("DARBAK", color = DarbakGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                        Text("CONTROL CENTER", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                        Surface(
                            color = Color.Black.copy(alpha = .24f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .06f)),
                        ) {
                            Text(
                                "${index + 1} / ${categories.size}",
                                color = accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            CarbonSurface.copy(alpha = .24f),
                            Color.Black.copy(alpha = .10f),
                        )
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                .border(1.dp, Color.White.copy(alpha = .055f), RoundedCornerShape(24.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = LocalSettingsAccent.current.copy(alpha = .13f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LocalSettingsAccent.current.copy(alpha = .18f)),
            modifier = Modifier.size(32.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(18.dp))
            }
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Surface(color = LocalSettingsAccent.current.copy(alpha = .72f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(34.dp).height(2.dp)) {}
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
        modifier = Modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = .62f) else Color.White.copy(alpha = .06f)),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Black.copy(alpha = .15f),
            selectedContainerColor = accent.copy(alpha = .18f),
            labelColor = TextSecondary,
            selectedLabelColor = TextPrimary,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    DarbakControlTile {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(
                    color = LocalSettingsAccent.current.copy(alpha = .85f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.width(28.dp).height(3.dp),
                ) {}
            }
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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = if (checked) LocalSettingsAccent.current.copy(alpha = .15f) else Color.Black.copy(alpha = .16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (checked) LocalSettingsAccent.current.copy(alpha = .28f) else Color.White.copy(alpha = .05f)),
                modifier = Modifier.size(38.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = if (checked) LocalSettingsAccent.current else TextMuted.copy(alpha = .45f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(if (checked) 12.dp else 8.dp),
                    ) {}
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.testTag("setting_" + title),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CarbonDark,
                    checkedTrackColor = LocalSettingsAccent.current,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = Color.Black.copy(alpha = .34f),
                ),
            )
        }
    }
}

@Composable
internal fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    var editing by remember(value) { mutableStateOf(value.coerceIn(min, max).toFloat()) }
    DarbakControlTile {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(
                    color = LocalSettingsAccent.current.copy(alpha = .14f),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, LocalSettingsAccent.current.copy(alpha = .24f)),
                ) {
                    Text("${editing.toInt()} $unit", color = LocalSettingsAccent.current, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
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
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(15.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = LocalSettingsAccent.current.copy(alpha = .11f),
            contentColor = TextPrimary,
            disabledContainerColor = Color.Black.copy(alpha = .10f),
        ),
        border = BorderStroke(1.dp, LocalSettingsAccent.current.copy(alpha = .18f)),
    ) {
        Icon(icon, null, tint = LocalSettingsAccent.current, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
internal fun InfoCard(text: String) {
    DarbakControlTile {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(Icons.Default.Info, null, tint = DarbakGold, modifier = Modifier.size(17.dp))
            Text(text, color = TextSecondary, fontSize = 10.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun StatusMetric(label: String, value: String, positive: Boolean) {
    val tint = if (positive) EmeraldSafe else AmberRacing
    DarbakControlTile {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(color = tint.copy(alpha = .16f), shape = RoundedCornerShape(50), modifier = Modifier.size(12.dp)) {}
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = TextMuted, fontSize = 9.sp)
                Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(color = tint.copy(alpha = .75f), shape = RoundedCornerShape(3.dp), modifier = Modifier.width(34.dp).height(3.dp)) {}
        }
    }
}

@Composable
private fun DarbakControlTile(content: @Composable () -> Unit) {
    val accent = LocalSettingsAccent.current
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CarbonSurface.copy(alpha = .34f),
                        accent.copy(alpha = .035f),
                        Color.Black.copy(alpha = .10f),
                    )
                ),
                RoundedCornerShape(17.dp),
            )
            .border(1.dp, Color.White.copy(alpha = .055f), RoundedCornerShape(17.dp)),
    ) {
        content()
    }
}