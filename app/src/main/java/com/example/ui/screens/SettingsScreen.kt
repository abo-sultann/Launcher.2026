package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.model.WidgetType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Suppress("DEPRECATION")
enum class SettingsCategory(val arabicTitle: String, val icon: ImageVector) {
    GENERAL("عام", Icons.Default.Settings),
    APPEARANCE("المظهر والخلفية", Icons.Default.Palette),
    SAFE_AREA("الهوامش ومكان الشاشة", Icons.Default.AspectRatio),
    APPS("التطبيقات", Icons.Default.Apps),
    WIDGETS("الودجات والتخطيط", Icons.Default.Widgets),
    MUSIC("الموسيقى والصوت", Icons.Default.MusicNote),
    GPS("GPS والسرعة", Icons.Default.Speed),
    MAPS("الخرائط", Icons.Default.Map),
    TRIP("رحلتي", Icons.Default.DirectionsCar),
    CHILD_LOCK("قفل الأطفال", Icons.Default.Lock),
    SCREENSAVER("شاشة التوقف", Icons.Default.Schedule),
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
    val maps by viewModel.mapsList.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    var selected by remember { mutableStateOf(SettingsCategory.GENERAL) }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importMusicUri) }
    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importMapUri) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importWallpaperUri) }

    Row(modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            Modifier.width(210.dp).fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = BorderStroke(1.dp, CarbonCardBorder)
        ) {
            LazyColumn(Modifier.fillMaxSize().padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { Text("إعدادات Launcher 2026", color = CyanNeon, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp)) }
                items(SettingsCategory.values().toList()) { cat ->
                    val active = selected == cat
                    Surface(
                        color = if (active) CyanNeon.copy(alpha = .18f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = if (active) BorderStroke(1.dp, CyanNeon) else null,
                        modifier = Modifier.fillMaxWidth().clickable { selected = cat }
                    ) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(cat.icon, null, tint = if (active) CyanNeon else TextSecondary, modifier = Modifier.size(20.dp))
                            Text(cat.arabicTitle, color = if (active) CyanNeon else TextPrimary, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        Card(
            Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = BorderStroke(1.dp, CyanNeon.copy(alpha = .3f))
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(selected.icon, null, tint = CyanNeon)
                    Text(selected.arabicTitle, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = CarbonCardBorder, modifier = Modifier.padding(vertical = 7.dp))

                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (selected) {
                        SettingsCategory.GENERAL -> {
                            item { SwitchRow("التشغيل التلقائي", "فتح Launcher بعد تشغيل الشاشة", settings.autoStartOnBoot) { viewModel.updateSettings(settings.copy(autoStartOnBoot = it, autoStartEnabled = it)) } }
                            item { ClockFormatRow(settings.is24HourFormat) { is24 -> viewModel.updateSettings(settings.copy(is24HourFormat = is24, is24HourClock = is24)) } }
                            item { SwitchRow("إبقاء الشاشة مضاءة", "منع إطفاء الشاشة أثناء استخدام Launcher", settings.keepScreenOn) { viewModel.updateSettings(settings.copy(keepScreenOn = it)) } }
                            item { SwitchRow("الوضع عالي التباين", "رفع وضوح النصوص والعناصر", settings.highContrastMode) { viewModel.updateSettings(settings.copy(highContrastMode = it)) } }
                            item { SwitchRow("إظهار الشريط العلوي", "إظهار معلومات السيارة والاختصارات", settings.showTopBar) { viewModel.updateSettings(settings.copy(showTopBar = it)) } }
                            item { SwitchRow("إظهار الشريط السفلي", "إظهار شريط التنقل الرئيسي", settings.showBottomBar) { viewModel.updateSettings(settings.copy(showBottomBar = it)) } }
                        }

                        SettingsCategory.APPEARANCE -> {
                            item { Text("الخلفية", color = CyanNeon, fontWeight = FontWeight.Bold) }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf(BackgroundType.DARK_CARBON, BackgroundType.CYBER_CYAN, BackgroundType.AMBER_RACING, BackgroundType.DEEP_SPACE, BackgroundType.LUXURY_ONYX)) { bg ->
                                        FilterChip(selected = settings.backgroundType == bg, onClick = { viewModel.updateSettings(settings.copy(backgroundType = bg)) }, label = { Text(bg.arabicName, fontSize = 10.sp) })
                                    }
                                }
                            }
                            item { ActionButton("اختيار صورة من الجهاز", Icons.Default.Image) { imagePicker.launch(arrayOf("image/*")) } }
                            if (settings.backgroundType == BackgroundType.CUSTOM_IMAGE) {
                                item { NumberSlider("تعتيم الخلفية", settings.wallpaperDimPercent, 0, 80, "%") { viewModel.updateSettings(settings.copy(wallpaperDimPercent = it)) } }
                            }
                            item { NumberSlider("حجم أيقونات التطبيقات", settings.iconSizeDp, 40, 110, "dp") { viewModel.updateSettings(settings.copy(iconSizeDp = it)) } }
                        }

                        SettingsCategory.SAFE_AREA -> {
                            item { InfoCard("اضبط الهوامش فقط إذا كانت واجهة الشاشة الأصلية تغطي جزءًا من Launcher.") }
                            item { NumberSlider("الهامش العلوي", safeArea.topDp, 0, 250, "dp") { viewModel.updateSafeArea(it, safeArea.bottomDp, safeArea.leftDp, safeArea.rightDp) } }
                            item { NumberSlider("الهامش السفلي", safeArea.bottomDp, 0, 150, "dp") { viewModel.updateSafeArea(safeArea.topDp, it, safeArea.leftDp, safeArea.rightDp) } }
                            item { NumberSlider("الهامش الأيمن", safeArea.rightDp, 0, 150, "dp") { viewModel.updateSafeArea(safeArea.topDp, safeArea.bottomDp, safeArea.leftDp, it) } }
                            item { NumberSlider("الهامش الأيسر", safeArea.leftDp, 0, 150, "dp") { viewModel.updateSafeArea(safeArea.topDp, safeArea.bottomDp, it, safeArea.rightDp) } }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ActionButton("معاينة", Icons.Default.Visibility) { onOpenSafeAreaPreview() }; ActionButton("تصفير", Icons.Default.Refresh) { viewModel.resetSafeArea() } } }
                        }

                        SettingsCategory.APPS -> {
                            item { NumberSlider("أعمدة درج التطبيقات", settings.appDrawerColumns, 2, 8, "أعمدة") { viewModel.updateSettings(settings.copy(appDrawerColumns = it)) } }
                            item { SwitchRow("أسماء التطبيقات", "إظهار الاسم أسفل الأيقونة", settings.showAppLabels) { viewModel.updateSettings(settings.copy(showAppLabels = it, showAppNames = it)) } }
                            item { ActionButton("إعادة فحص التطبيقات", Icons.Default.Refresh) { viewModel.loadApps() } }
                        }

                        SettingsCategory.WIDGETS -> {
                            item { SwitchRow("وضع التصميم", "اختر ودجت ثم اسحبه من أي مكان", isDesignMode) { viewModel.toggleDesignMode() } }
                            item { InfoCard("عند اختيار الودجت تظهر أدوات كبيرة: أصغر، أكبر، أعرض، أطول، ومقاسات جاهزة. لا تحتاج سحب مقبض صغير إلا إذا أردت تحجيمًا دقيقًا.") }
                            item { ActionButton("إرجاع الترتيب الافتراضي", Icons.Default.RestartAlt) { viewModel.resetWidgetsToDefault() } }
                        }

                        SettingsCategory.MUSIC -> {
                            item { SwitchRow("حفظ آخر موضع", "عند الضغط على تشغيل لاحقًا يبدأ من نفس المقطع والموضع، بدون تشغيل تلقائي", settings.resumeMusicPlayback) { viewModel.updateSettings(settings.copy(resumeMusicPlayback = it)) } }
                            item { ActionButton("إضافة ملف صوت", Icons.Default.LibraryMusic) { musicPicker.launch(arrayOf("audio/*")) } }
                            item { Text("المقطع: ${playback.currentTrack?.title ?: "لا يوجد"}", color = if (playback.currentTrack != null) EmeraldSafe else TextSecondary) }
                            item { Text("المقاطع المكتشفة: ${playback.playlist.size}", color = TextSecondary) }
                        }

                        SettingsCategory.GPS -> {
                            item { Text("${gps.speedKmH.toInt()} كم/س", color = CyanNeon, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                            item { InfoCard(if (gps.hasGpsFix) "دقة GPS ±${gps.accuracyMeters.toInt()}م • ${if (gps.isSpeedReliable) "السرعة مؤكدة" else "السرعة غير معتمدة الآن"}" else gps.statusArabic) }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = settings.speedUnit == "كم/س", onClick = { viewModel.updateSettings(settings.copy(speedUnit = "كم/س")) }, label = { Text("كم/س") })
                                    FilterChip(selected = settings.speedUnit == "MPH", onClick = { viewModel.updateSettings(settings.copy(speedUnit = "MPH")) }, label = { Text("MPH") })
                                }
                            }
                        }

                        SettingsCategory.MAPS -> {
                            item { InfoCard("للعرض المباشر استخدم ملف Mapsforge بامتداد .map.") }
                            item { ActionButton("إضافة خريطة", Icons.Default.AddLocationAlt) { mapPicker.launch(arrayOf("*/*")) } }
                            item { Text("الخرائط: ${maps.size}", color = TextPrimary, fontWeight = FontWeight.Bold) }
                            items(maps) { map ->
                                Surface(color = if (map.isActive) CyanNeon.copy(alpha = .12f) else CarbonSurface, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, if (map.isActive) CyanNeon else CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Column(Modifier.weight(1f)) { Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold); Text(map.fileSizeFormatted, color = TextSecondary, fontSize = 11.sp) }
                                        if (!map.isActive) Button(onClick = { viewModel.setActiveMap(map.id) }) { Text("تفعيل") }
                                        IconButton(onClick = { viewModel.deleteMap(map.id) }) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed) }
                                    }
                                }
                            }
                        }

                        SettingsCategory.TRIP -> {
                            item { SwitchRow("بدء الرحلة تلقائيًا", "لا تبدأ من قراءة واحدة؛ يتأكد Launcher من حركة فعلية أولًا", settings.autoLogTrips) { viewModel.updateSettings(settings.copy(autoLogTrips = it)) } }
                            item { InfoCard("الرحلة تعتمد فقط على نقاط GPS المقبولة، لذلك قفزات الموقع لا تضيف كيلومترات وهمية.") }
                            item { ActionButton("تصفير الرحلة الحالية", Icons.Default.Refresh) { viewModel.resetTrip() } }
                        }

                        SettingsCategory.CHILD_LOCK -> {
                            item { InfoCard("القفل غير مرئي: تبقى الشاشة كما هي وتُمنع اللمسات. للتفعيل اضغط مطولًا على شعار LAUNCHER 2026 في الشريط العلوي. لفك القفل اضغط مطولًا في نفس منطقة الشعار.") }
                            item { NumberSlider("مدة الضغط", settings.childUnlockHoldSeconds, 2, 6, "ث") { viewModel.updateSettings(settings.copy(childUnlockHoldSeconds = it)) } }
                            item { ActionButton("تفعيل القفل الآن", Icons.Default.Lock) { viewModel.activateChildLock() } }
                        }

                        SettingsCategory.SCREENSAVER -> {
                            item { SwitchRow("تفعيل شاشة التوقف", "تظهر بعد عدم لمس الشاشة، ولا تظهر أثناء الخريطة", settings.screenSaverEnabled) { viewModel.updateSettings(settings.copy(screenSaverEnabled = it)) } }
                            item { NumberSlider("وقت الانتظار", settings.screenSaverTimeoutSeconds, 30, 1800, "ث") { viewModel.updateSettings(settings.copy(screenSaverTimeoutSeconds = it)) } }
                            item { SwitchRow("استخدام الخلفية الحالية", "إظهار الخلفية خلف ودجات شاشة التوقف", settings.screenSaverUseWallpaper) { viewModel.updateSettings(settings.copy(screenSaverUseWallpaper = it)) } }
                            item { Text("ودجات شاشة التوقف — حتى 4", color = CyanNeon, fontWeight = FontWeight.Bold) }
                            item {
                                val candidates = listOf(WidgetType.CLOCK, WidgetType.SPEEDOMETER, WidgetType.DATE, WidgetType.GPS, WidgetType.MUSIC, WidgetType.TRIP)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    candidates.chunked(3).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            row.forEach { type -> FilterChip(selected = type in settings.screenSaverWidgetTypes, onClick = { viewModel.toggleScreenSaverWidget(type) }, label = { Text(type.arabicTitle, fontSize = 10.sp) }) }
                                        }
                                    }
                                }
                            }
                        }

                        SettingsCategory.SYSTEM -> {
                            item { StableSystemPanel() }
                            item { ActionButton("فحص النظام", Icons.Default.HealthAndSafety) { viewModel.runDiagnostics(); onOpenDiagnostics() } }
                            item { ActionButton("تصفير سجل الوضع الآمن", Icons.Default.Security) { viewModel.resetSafeMode() } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("نظام الساعة", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(if (is24) "مثال: 18:30" else "مثال: 06:30 م", color = TextSecondary, fontSize = 11.sp)
            }
            FilterChip(selected = !is24, onClick = { onChange(false) }, label = { Text("12 ساعة") })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = is24, onClick = { onChange(true) }, label = { Text("24 ساعة") })
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold); Text(subtitle, color = TextSecondary, fontSize = 11.sp) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = TextPrimary, fontWeight = FontWeight.Bold); Text("$value $unit", color = CyanNeon, fontWeight = FontWeight.Bold) }
            Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = min.toFloat()..max.toFloat())
        }
    }
}

@Composable
private fun ActionButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface)) {
        Icon(icon, null, tint = CyanNeon, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(color = CyanNeon.copy(alpha = .08f), shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, CyanNeon.copy(alpha = .25f)), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
    }
}
