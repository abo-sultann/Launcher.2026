package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LauncherSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

enum class SettingsCategory(val arabicTitle: String, val icon: ImageVector) {
    GENERAL("عام", Icons.Default.Settings),
    APPEARANCE("المظهر", Icons.Default.Palette),
    SAFE_AREA("تخطيط الشاشة والهوامش", Icons.Default.AspectRatio),
    APPS("التطبيقات", Icons.Default.Apps),
    WIDGETS("Widgets والودجات", Icons.Default.Widgets),
    MUSIC("الموسيقى والصوت", Icons.Default.MusicNote),
    GPS("GPS والسرعة", Icons.Default.Speed),
    MAPS("الخرائط Offline", Icons.Default.Map),
    TRIP("كمبيوتر الرحلات", Icons.Default.DirectionsCar),
    SYSTEM("النظام والتشخيص", Icons.Default.HealthAndSafety)
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenSafeAreaPreview: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val safeArea by viewModel.safeArea.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: 10 Settings Category Tabs (in RTL: Sidebar)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Text(
                    text = "إعدادات المشغل (Settings)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CyanNeon,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(SettingsCategory.values()) { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            color = if (isSelected) CyanNeon.copy(alpha = 0.18f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanNeon else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = category }
                                .testTag("settings_cat_${category.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) CyanNeon else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = category.arabicTitle,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) CyanNeon else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Settings Content for Selected Category
        Card(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = selectedCategory.icon,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = selectedCategory.arabicTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Divider(color = CarbonCardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Body based on category
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedCategory) {
                        SettingsCategory.GENERAL -> {
                            item {
                                SettingSwitchRow(
                                    title = "التشغيل التلقائي عند إقلاع السيارة (Auto-Start)",
                                    subtitle = "فتح المشغل تلقائياً عند تشغيل شاشة الأندرويد بعد الإقلاع",
                                    checked = settings.autoStartOnBoot,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoStartOnBoot = it)) }
                                )
                            }
                            item {
                                SettingSwitchRow(
                                    title = "نظام الوقت 24 ساعة",
                                    subtitle = "التبديل بين صيغة 12 ساعة و 24 ساعة في جميع الودجات",
                                    checked = settings.is24HourFormat,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(is24HourFormat = it)) }
                                )
                            }
                            item {
                                SettingSwitchRow(
                                    title = "حفظ وإعادة الشاشة عند استئناف التطبيق",
                                    subtitle = "العودة التلقائية لآخر شاشة كانت مفتوحة",
                                    checked = settings.keepScreenOn,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(keepScreenOn = it)) }
                                )
                            }
                        }

                        SettingsCategory.APPEARANCE -> {
                            item {
                                SettingSwitchRow(
                                    title = "الوضع عالي التباين (High Contrast HUD)",
                                    subtitle = "زيادة وضوح الخطوط والحدود المضيئة للقراءة المريحة أثناء القيادة",
                                    checked = settings.highContrastMode,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(highContrastMode = it)) }
                                )
                            }
                            item {
                                Surface(
                                    color = CarbonSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "السمة المعتمدة للسيارة", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(text = "الكربون الفاخر الداكن مع إضاءة نيون سماوية وبرتقالية (Automotive Dark Glow)", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                                    }
                                }
                            }
                        }

                        SettingsCategory.SAFE_AREA -> {
                            item {
                                Surface(
                                    color = CarbonSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "إعدادات الهوامش الحالية", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "أعلى: ${safeArea.topDp}dp | أسفل: ${safeArea.bottomDp}dp | يمين: ${safeArea.rightDp}dp | يسار: ${safeArea.leftDp}dp",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CyanNeon
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = onOpenSafeAreaPreview,
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("btn_goto_safe_area_editor")
                                        ) {
                                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = CarbonDark)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فتح محرر الهوامش التفاعلي (Safe Area Editor)", color = CarbonDark, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.APPS -> {
                            item {
                                SettingSwitchRow(
                                    title = "عرض أسماء التطبيقات في الودجات",
                                    subtitle = "إظهار النص التوضيحي أسفل أيقونة التطبيق",
                                    checked = settings.showAppLabels,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(showAppLabels = it)) }
                                )
                            }
                            item {
                                Button(
                                    onClick = { viewModel.loadApps() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanNeon)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إعادة مسح وتحديث قائمة التطبيقات المثبتة", color = TextPrimary)
                                }
                            }
                        }

                        SettingsCategory.WIDGETS -> {
                            item {
                                SettingSwitchRow(
                                    title = "وضع تصميم الودجات (Design Mode)",
                                    subtitle = "تفعيل أزرار التحرير وتغيير النماذج وتكبير المقاسات على الشاشة الرئيسية",
                                    checked = isDesignMode,
                                    onCheckedChange = { viewModel.toggleDesignMode() }
                                )
                            }
                            item {
                                Button(
                                    onClick = { viewModel.resetWidgetsToDefault() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AmberRacing)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("استعادة ترتيب الودجات الافتراضي للسيارة", color = TextPrimary)
                                }
                            }
                        }

                        SettingsCategory.MUSIC -> {
                            item {
                                SettingSwitchRow(
                                    title = "استئناف التشغيل الذكي (Resume Playback)",
                                    subtitle = "الميزة الإلزامية: حفظ آخر ملف صوتي وموضع الثواني التلقائي عند الخروج واستئنافه بدقة",
                                    checked = settings.resumeMusicPlayback,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(resumeMusicPlayback = it)) }
                                )
                            }
                        }

                        SettingsCategory.GPS -> {
                            item {
                                Surface(
                                    color = CarbonSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "وحدة قياس السرعة", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(text = "كيلومتر في الساعة (كم/س) - الافتراضي المعتمد للسيارات", style = MaterialTheme.typography.labelSmall, color = CyanNeon)
                                    }
                                }
                            }
                        }

                        SettingsCategory.MAPS -> {
                            item {
                                Surface(
                                    color = CarbonSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "نظام الخرائط بدون إنترنت Offline", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(text = "يدعم ملفات Vector HUD و MBTiles المباشرة بدون انقطاع وبأمان تام", style = MaterialTheme.typography.labelSmall, color = EmeraldSafe)
                                    }
                                }
                            }
                        }

                        SettingsCategory.TRIP -> {
                            item {
                                SettingSwitchRow(
                                    title = "تسجيل الرحلة التلقائي",
                                    subtitle = "حساب المسافات والسرعة المتوسطة فور استشعار حركة السيارة",
                                    checked = settings.autoLogTrips,
                                    onCheckedChange = { viewModel.updateSettings(settings.copy(autoLogTrips = it)) }
                                )
                            }
                            item {
                                Button(
                                    onClick = { viewModel.resetTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصفير عدادات الرحلة الحالية", color = TextPrimary)
                                }
                            }
                        }

                        SettingsCategory.SYSTEM -> {
                            item {
                                Surface(
                                    color = CarbonSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = "فحص سلامة النظام والوضع الآمن", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                        Text(text = "فحص متكامل لجميع المكونات (GPS, Music, Maps, Widgets, DB) مع عزل الأعطال", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = onOpenDiagnostics,
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("btn_open_diagnostics_from_settings")
                                        ) {
                                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = CarbonDark)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فتح تقرير التشخيص وحالة الأمان", color = CarbonDark, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CarbonDark,
                    checkedTrackColor = CyanNeon,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = CarbonDark
                )
            )
        }
    }
}
