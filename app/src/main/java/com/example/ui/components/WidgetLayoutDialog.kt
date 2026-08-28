package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.WidgetLayoutPreset
import com.example.model.WidgetLayoutTarget
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun WidgetLayoutDialog(
    viewModel: MainViewModel,
    target: WidgetLayoutTarget,
    onBackgroundFocus: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val savedNames by if (target == WidgetLayoutTarget.HOME) viewModel.savedHomeLayouts.collectAsState() else viewModel.savedScreenSaverLayouts.collectAsState()
    var layoutName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.90f),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = BorderStroke(1.dp, CyanNeon),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ترتيب الودجات", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(if (target == WidgetLayoutTarget.HOME) "اترك الصورة هي العنصر الرئيسي" else "شاشة التوقف", color = TextSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "إغلاق", tint = TextPrimary) }
                }

                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (target == WidgetLayoutTarget.HOME) {
                        item {
                            Surface(
                                onClick = onBackgroundFocus,
                                color = CarbonSurface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AmberRacing.copy(alpha = .75f))
                            ) {
                                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Wallpaper, null, tint = AmberRacing, modifier = Modifier.size(28.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("إبراز الخلفية", color = TextPrimary, fontWeight = FontWeight.Black)
                                        Text("يوزع الودجات على الأطراف ويترك منتصف الشاشة واضحًا لصورة السيارة", color = TextSecondary, fontSize = 10.sp)
                                    }
                                    Icon(Icons.Default.ChevronLeft, null, tint = CyanNeon)
                                }
                            }
                        }
                    }

                    item {
                        Text("ترتيبات عامة", color = AmberRacing, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            WidgetLayoutPreset.values().toList().chunked(3).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { preset ->
                                        OutlinedButton(
                                            onClick = {
                                                if (target == WidgetLayoutTarget.HOME) viewModel.applyWidgetLayoutPreset(preset)
                                                else viewModel.applyScreenSaverLayoutPreset(preset)
                                            },
                                            modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 4.dp),
                                            border = BorderStroke(1.dp, CarbonCardBorder)
                                        ) {
                                            Text(preset.arabicName, color = TextPrimary, fontSize = 9.sp, maxLines = 2)
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = CarbonCardBorder)
                        Spacer(Modifier.height(8.dp))
                        Text("المحاذاة الذكية", color = AmberRacing, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LayoutToolButton("توسيط أفقي", Icons.Default.AlignHorizontalCenter, Modifier.weight(1f)) {
                                if (target == WidgetLayoutTarget.HOME) viewModel.alignHomeWidgetsHorizontalCenter() else viewModel.alignScreenSaverHorizontalCenter()
                            }
                            LayoutToolButton("توسيط رأسي", Icons.Default.AlignVerticalCenter, Modifier.weight(1f)) {
                                if (target == WidgetLayoutTarget.HOME) viewModel.alignHomeWidgetsVerticalCenter() else viewModel.alignScreenSaverVerticalCenter()
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LayoutToolButton("توزيع متساوٍ", Icons.Default.SpaceBar, Modifier.weight(1f)) {
                                if (target == WidgetLayoutTarget.HOME) viewModel.distributeHomeWidgetsEvenly() else viewModel.distributeScreenSaverEvenly()
                            }
                            LayoutToolButton("توحيد المقاس", Icons.Default.AspectRatio, Modifier.weight(1f)) {
                                if (target == WidgetLayoutTarget.HOME) viewModel.equalizeHomeWidgetSizes() else viewModel.equalizeScreenSaverSizes()
                            }
                        }
                        Text("السحب الحر يبقى متاحًا مع تثبيت تلقائي قرب الحواف والمنتصف.", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    }

                    item {
                        HorizontalDivider(color = CarbonCardBorder)
                        Spacer(Modifier.height(8.dp))
                        Text("حفظ ترتيب خاص", color = AmberRacing, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OutlinedTextField(
                                value = layoutName,
                                onValueChange = { layoutName = it.take(30) },
                                singleLine = true,
                                label = { Text("اسم الترتيب") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (target == WidgetLayoutTarget.HOME) viewModel.saveNamedHomeLayout(layoutName) else viewModel.saveNamedScreenSaverLayout(layoutName)
                                    layoutName = ""
                                },
                                enabled = layoutName.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("حفظ")
                            }
                        }
                    }

                    if (savedNames.isNotEmpty()) {
                        item { Text("الترتيبات المحفوظة", color = TextPrimary, fontWeight = FontWeight.Bold) }
                        items(savedNames, key = { it }) { name ->
                            Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder)) {
                                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        if (target == WidgetLayoutTarget.HOME) viewModel.restoreNamedHomeLayout(name) else viewModel.restoreNamedScreenSaverLayout(name)
                                    }) { Text("تطبيق", color = CyanNeon) }
                                    IconButton(onClick = {
                                        if (target == WidgetLayoutTarget.HOME) viewModel.deleteNamedHomeLayout(name) else viewModel.deleteNamedScreenSaverLayout(name)
                                    }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                                }
                            }
                        }
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text("تم")
                }
            }
        }
    }
}

@Composable
private fun LayoutToolButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, action: () -> Unit) {
    FilledTonalButton(
        onClick = action,
        modifier = modifier.heightIn(min = 42.dp),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CarbonSurface)
    ) {
        Icon(icon, null, tint = CyanNeon, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(title, color = TextPrimary, fontSize = 9.sp)
    }
}
