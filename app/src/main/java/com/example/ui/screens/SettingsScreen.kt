package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

enum class SettingsCategory(val arabicTitle: String, val icon: ImageVector) {
    GENERAL("عام", Icons.Default.Settings), APPEARANCE("المظهر والخلفية", Icons.Default.Palette),
    SAFE_AREA("الهوامش ومكان الشاشة", Icons.Default.AspectRatio), APPS("التطبيقات", Icons.Default.Apps),
    WIDGETS("الودجات والتخطيط", Icons.Default.Widgets), MUSIC("الموسيقى والصوت", Icons.Default.MusicNote),
    GPS("GPS والسرعة", Icons.Default.Speed), MAPS("الخرائط Offline", Icons.Default.Map),
    TRIP("كمبيوتر الرحلات", Icons.Default.DirectionsCar), SYSTEM("النظام والتشخيص", Icons.Default.HealthAndSafety)
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, onOpenSafeAreaPreview: () -> Unit, onOpenDiagnostics: () -> Unit, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsState()
    val safeArea by viewModel.safeArea.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    val maps by viewModel.mapsList.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    var selected by remember { mutableStateOf(SettingsCategory.GENERAL) }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importMusicUri) }
    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importMapUri) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.updateSettings(settings.copy(backgroundType = BackgroundType.CUSTOM_IMAGE, customWallpaperPath = it.toString())) }
    }

    Row(modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.width(210.dp).fillMaxHeight(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CarbonCard), border = BorderStroke(1.dp, CarbonCardBorder)) {
            LazyColumn(Modifier.fillMaxSize().padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Text("إعدادات Launcher 2026", color = CyanNeon, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
                items(SettingsCategory.values().toList()) { cat ->
                    val active = selected == cat
                    Surface(color = if (active) CyanNeon.copy(alpha = .18f) else Color.Transparent, shape = RoundedCornerShape(8.dp), border = if (active) BorderStroke(1.dp, CyanNeon) else null, modifier = Modifier.fillMaxWidth().clickable { selected = cat }) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(cat.icon, null, tint = if (active) CyanNeon else TextSecondary, modifier = Modifier.size(20.dp))
                            Text(cat.arabicTitle, color = if (active) CyanNeon else TextPrimary, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Card(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CarbonCard), border = BorderStroke(1.dp, CyanNeon.copy(alpha = .3f))) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(selected.icon, null, tint = CyanNeon); Text(selected.arabicTitle, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Divider(color = CarbonCardBorder, modifier = Modifier.padding(vertical = 7.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (selected) {
                        SettingsCategory.GENERAL -> {
                            item { SwitchRow("التشغيل التلقائي", "فتح Launcher بعد تشغيل الشاشة", settings.autoStartOnBoot) { viewModel.updateSettings(settings.copy(autoStartOnBoot = it, autoStartEnabled = it)) } }
                            item { SwitchRow("إبقاء الشاشة مضاءة", "منع إطفاء الشاشة أثناء استخدام اللانشر", settings.keepScreenOn) { viewModel.updateSettings(settings.copy(keepScreenOn = it)) } }
                            item { SwitchRow("الوضع عالي التباين", "رفع وضوح النصوص والعناصر", settings.highContrastMode) { viewModel.updateSettings(settings.copy(highContrastMode = it)) } }
                            item { SwitchRow("إظهار الشريط العلوي", "أوقفه إذا كان شريط الشاشة الأصلي يغطي أعلى الواجهة", settings.showTopBar) { viewModel.updateSettings(settings.copy(showTopBar = it)) } }
                            item { SwitchRow("إظهار الشريط السفلي", "إظهار أو إخفاء شريط التنقل", settings.showBottomBar) { viewModel.updateSettings(settings.copy(showBottomBar = it)) } }
                        }
                        SettingsCategory.APPEARANCE -> {
                            item { Text("الخلفية", color = CyanNeon, fontWeight = FontWeight.Bold) }
                            item { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(listOf(BackgroundType.DARK_CARBON, BackgroundType.CYBER_CYAN, BackgroundType.AMBER_RACING, BackgroundType.DEEP_SPACE, BackgroundType.LUXURY_ONYX)) { bg -> FilterChip(selected = settings.backgroundType == bg, onClick = { viewModel.updateSettings(settings.copy(backgroundType = bg)) }, label = { Text(bg.arabicName, fontSize = 10.sp) }) } } }
                            item { ActionButton("اختيار صورة خلفية من الجهاز", Icons.Default.Image) { imagePicker.launch(arrayOf("image/*")) } }
                            item { NumberSlider("حجم الأيقونات", settings.iconSizeDp, 40, 110, "dp") { viewModel.updateSettings(settings.copy(iconSizeDp = it)) } }
                        }
                        SettingsCategory.SAFE_AREA -> {
                            item { Text("هذا الهامش يعالج شريط شاشة السيارة الأصلي. ارفع Top حتى تختفي تغطية الشريط للعناصر.", color = TextSecondary) }
                            item { NumberSlider("الهامش العلوي Top", safeArea.topDp, 0, 250, "dp") { viewModel.updateSafeArea(it, safeArea.bottomDp, safeArea.leftDp, safeArea.rightDp) } }
                            item { NumberSlider("الهامش السفلي Bottom", safeArea.bottomDp, 0, 150, "dp") { viewModel.updateSafeArea(safeArea.topDp, it, safeArea.leftDp, safeArea.rightDp) } }
                            item { NumberSlider("الهامش الأيمن Right", safeArea.rightDp, 0, 150, "dp") { viewModel.updateSafeArea(safeArea.topDp, safeArea.bottomDp, safeArea.leftDp, it) } }
                            item { NumberSlider("الهامش الأيسر Left", safeArea.leftDp, 0, 150, "dp") { viewModel.updateSafeArea(safeArea.topDp, safeArea.bottomDp, it, safeArea.rightDp) } }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ActionButton("معاينة", Icons.Default.Visibility) { onOpenSafeAreaPreview() }; ActionButton("تصفير", Icons.Default.Refresh) { viewModel.resetSafeArea() } } }
                        }
                        SettingsCategory.APPS -> {
                            item { NumberSlider("أعمدة درج التطبيقات", settings.appDrawerColumns, 2, 8, "أعمدة") { viewModel.updateSettings(settings.copy(appDrawerColumns = it)) } }
                            item { SwitchRow("أسماء التطبيقات", "إظهار أسماء التطبيقات", settings.showAppLabels) { viewModel.updateSettings(settings.copy(showAppLabels = it, showAppNames = it)) } }
                            item { ActionButton("إعادة فحص التطبيقات", Icons.Default.Refresh) { viewModel.loadApps() } }
                        }
                        SettingsCategory.WIDGETS -> {
                            item { SwitchRow("وضع التصميم", "تحريك وتغيير وحذف وإضافة الودجات", isDesignMode) { viewModel.toggleDesignMode() } }
                            item { NumberSlider("أعمدة الشاشة الرئيسية", settings.homeGridColumns, 2, 6, "أعمدة") { viewModel.updateSettings(settings.copy(homeGridColumns = it)) } }
                            item { NumberSlider("ارتفاع الودجت", settings.widgetHeightDp, 90, 220, "dp") { viewModel.updateSettings(settings.copy(widgetHeightDp = it)) } }
                            item { NumberSlider("المسافة الأفقية", settings.gridHorizontalGapDp, 2, 24, "dp") { viewModel.updateSettings(settings.copy(gridHorizontalGapDp = it)) } }
                            item { NumberSlider("المسافة الرأسية", settings.gridVerticalGapDp, 2, 24, "dp") { viewModel.updateSettings(settings.copy(gridVerticalGapDp = it)) } }
                            item { ActionButton("إرجاع الودجات للوضع الافتراضي", Icons.Default.RestartAlt) { viewModel.resetWidgetsToDefault() } }
                        }
                        SettingsCategory.MUSIC -> {
                            item { SwitchRow("استئناف آخر تشغيل", "حفظ الملف وموضع التشغيل واستعادتهما", settings.resumeMusicPlayback) { viewModel.updateSettings(settings.copy(resumeMusicPlayback = it)) } }
                            item { ActionButton("اختيار وإضافة ملف موسيقى", Icons.Default.LibraryMusic) { musicPicker.launch(arrayOf("audio/*")) } }
                            item { Text("الملف الحالي: ${playback.currentTrack?.title ?: "لا يوجد ملف محدد"}", color = if (playback.currentTrack != null) EmeraldSafe else TextSecondary) }
                            item { Text("عدد المقاطع المكتشفة: ${playback.playlist.size}", color = TextSecondary) }
                        }
                        SettingsCategory.GPS -> {
                            item { Text("السرعة الحالية: ${gpsSpeed(viewModel)}", color = CyanNeon, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = settings.speedUnit == "كم/س", onClick = { viewModel.updateSettings(settings.copy(speedUnit = "كم/س")) }, label = { Text("كم/س") }); FilterChip(selected = settings.speedUnit == "MPH", onClick = { viewModel.updateSettings(settings.copy(speedUnit = "MPH")) }, label = { Text("MPH") }) } }
                        }
                        SettingsCategory.MAPS -> {
                            item { Text("التحكم الكامل بالخرائط", color = CyanNeon, fontWeight = FontWeight.Bold) }
                            item { Text("لا توجد خريطة مفروضة عليك. أضف ملفك يدويًا ثم فعّل الخريطة التي تريدها.", color = TextSecondary) }
                            item { ActionButton("إضافة خريطة يدويًا (MBTiles)", Icons.Default.AddLocationAlt) { mapPicker.launch(arrayOf("*/*")) } }
                            item { Text("عدد الخرائط: ${maps.size}", color = TextPrimary, fontWeight = FontWeight.Bold) }
                            items(maps) { map -> Surface(color = if (map.isActive) CyanNeon.copy(alpha = .12f) else CarbonSurface, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (map.isActive) CyanNeon else CarbonCardBorder), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold); Text(map.fileSizeFormatted, color = TextSecondary, fontSize = 11.sp) }; if (!map.isActive) Button(onClick = { viewModel.setActiveMap(map.id) }) { Text("تفعيل") } } } }
                        }
                        SettingsCategory.TRIP -> {
                            item { SwitchRow("التسجيل التلقائي", "تشغيل حساب الرحلة عند الحركة", settings.autoLogTrips) { viewModel.updateSettings(settings.copy(autoLogTrips = it)) } }
                            item { ActionButton("تصفير الرحلة", Icons.Default.Refresh) { viewModel.resetTrip() } }
                        }
                        SettingsCategory.SYSTEM -> {
                            item { ActionButton("فحص شامل للنظام", Icons.Default.HealthAndSafety) { viewModel.runDiagnostics(); onOpenDiagnostics() } }
                            item { ActionButton("إعادة تفعيل الوضع الآمن", Icons.Default.Security) { viewModel.resetSafeMode() } }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f).padding(end = 10.dp)) { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold); Text(subtitle, color = TextSecondary, fontSize = 11.sp) }; Switch(checked = checked, onCheckedChange = onChange) }
    }
}

@Composable private fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = TextPrimary, fontWeight = FontWeight.Bold); Text("$value $unit", color = CyanNeon, fontWeight = FontWeight.Bold) }; Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = min.toFloat()..max.toFloat()) } }
}

@Composable private fun ActionButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface)) { Icon(icon, null, tint = CyanNeon, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) }
}

@Composable private fun gpsSpeed(viewModel: MainViewModel): String { val gps by viewModel.gpsTelemetry.collectAsState(); return "${gps.speedKmH.toInt()} كم/س" }
