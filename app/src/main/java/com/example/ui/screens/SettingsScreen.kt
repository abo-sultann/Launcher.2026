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
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/** Seven task-oriented groups replace the previous twelve fragmented pages. */
enum class SettingsCategory(val arabicTitle: String, val icon: ImageVector) {
    INTERFACE("الواجهة", Icons.Default.DashboardCustomize),
    WIDGETS("الودجت", Icons.Default.Widgets),
    SCREENSAVER("شاشة التوقف", Icons.Default.NightsStay),
    MEDIA("الوسائط", Icons.Default.MusicNote),
    DRIVING("القيادة والخريطة", Icons.Default.Navigation),
    SECURITY("الأمان", Icons.Default.Lock),
    SYSTEM("النظام والتحديث", Icons.Default.SettingsSuggest)
}

private val LocalSettingsAccent = staticCompositionLocalOf { CyanNeon }

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenSafeAreaPreview: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenScreenSaverEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val safeArea by viewModel.safeArea.collectAsState()
    val isDesignMode by viewModel.isDesignMode.collectAsState()
    val maps by viewModel.mapsList.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    val gps by viewModel.gpsTelemetry.collectAsState()
    var selected by remember { mutableStateOf(SettingsCategory.INTERFACE) }
    var interfaceBackgroundOpen by remember { mutableStateOf(true) }
    var interfaceBarsOpen by remember { mutableStateOf(false) }
    var interfaceAppsOpen by remember { mutableStateOf(false) }
    var interfaceSafeAreaOpen by remember { mutableStateOf(false) }
    val accent = Color(settings.interfaceAccent.argb)

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importMusicUri) }
    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importMapUri) }
    // More reliable than ACTION_OPEN_DOCUMENT on Android 7 car-unit file managers.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::importWallpaperUri) }

    CompositionLocalProvider(LocalSettingsAccent provides accent) {
        Row(modifier.fillMaxSize().padding(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Card(
                Modifier.width(178.dp).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = .94f)),
                border = BorderStroke(1.dp, CarbonCardBorder)
            ) {
                Column(Modifier.fillMaxSize().padding(7.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.size(20.dp))
                        Text("الإعدادات", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(SettingsCategory.values().toList()) { category ->
                            val active = selected == category
                            Surface(
                                color = if (active) accent.copy(alpha = .16f) else Color.Transparent,
                                shape = RoundedCornerShape(11.dp),
                                border = if (active) BorderStroke(1.dp, accent.copy(alpha = .85f)) else null,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { selected = category }
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(category.icon, null, tint = if (active) accent else TextSecondary, modifier = Modifier.size(20.dp))
                                    Text(category.arabicTitle, color = if (active) accent else TextPrimary, fontSize = 12.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Medium, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Card(
                Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = .94f)),
                border = BorderStroke(1.dp, accent.copy(alpha = .32f))
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 11.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = accent.copy(alpha = .14f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(selected.icon, null, tint = accent, modifier = Modifier.size(21.dp)) }
                        }
                        Column {
                            Text(selected.arabicTitle, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Text("إعدادات مرتبة حسب المهمة", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = CarbonCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                        when (selected) {
                            SettingsCategory.INTERFACE -> {
                                item { ExpandableSectionHeader("الخلفية والألوان", Icons.Default.Palette, interfaceBackgroundOpen) { interfaceBackgroundOpen = !interfaceBackgroundOpen } }
                                if (interfaceBackgroundOpen) {
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(BackgroundType.values().toList()) { bg ->
                                            FilterChip(selected = settings.backgroundType == bg, onClick = { viewModel.updateSettings(settings.copy(backgroundType = bg)) }, label = { Text(bg.arabicName, fontSize = 9.sp) })
                                        }
                                    }
                                }
                                item { ActionButton("اختيار صورة من الجهاز", Icons.Default.Image) { viewModel.prepareForExternalPicker(); imagePicker.launch("image/*") } }
                                item { NumberSlider("تعتيم الخلفية", settings.wallpaperDimPercent, 0, 80, "%") { viewModel.updateSettings(settings.copy(wallpaperDimPercent = it)) } }
                                item {
                                    ChoiceCard("لون الواجهة", "يُطبّق على شريطي الحالة والتنقل وعناصر الإعدادات") {
                                        InterfaceAccent.values().forEach { option ->
                                            FilterChip(
                                                selected = settings.interfaceAccent == option,
                                                onClick = { viewModel.updateSettings(settings.copy(interfaceAccent = option)) },
                                                label = { Text(option.arabicName, fontSize = 10.sp) },
                                                leadingIcon = { Surface(color = Color(option.argb), shape = RoundedCornerShape(4.dp), modifier = Modifier.size(13.dp)) {} }
                                            )
                                        }
                                    }
                                }
                                }

                                item { ExpandableSectionHeader("شريطا الشاشة", Icons.Default.ViewDay, interfaceBarsOpen) { interfaceBarsOpen = !interfaceBarsOpen } }
                                if (interfaceBarsOpen) {
                                item { SwitchRow("مؤشر Wi‑Fi العلوي", "عنصر واحد فقط؛ المسه لفتح إعدادات الشبكة", settings.showTopBar) { viewModel.updateSettings(settings.copy(showTopBar = it)) } }
                                item { SwitchRow("شريط التنقل السفلي", "الرئيسية والتطبيقات والموسيقى والخريطة والرحلة والإعدادات", settings.showBottomBar) { viewModel.updateSettings(settings.copy(showBottomBar = it)) } }
                                if (settings.showBottomBar) {
                                    item {
                                        ChoiceCard("مظهر الشريط السفلي", "اختر شفافية كاملة أو زجاجًا أو خلفية داكنة") {
                                            DockSurfaceStyle.values().forEach { option ->
                                                FilterChip(selected = settings.bottomDockStyle == option, onClick = { viewModel.updateSettings(settings.copy(bottomDockStyle = option)) }, label = { Text(option.arabicName, fontSize = 10.sp) })
                                            }
                                        }
                                    }
                                    item { NumberSlider("شفافية الشريط السفلي", settings.bottomDockOpacityPercent, 35, 100, "%") { viewModel.updateSettings(settings.copy(bottomDockOpacityPercent = it)) } }
                                }
                                }

                                item { ExpandableSectionHeader("التطبيقات", Icons.Default.Apps, interfaceAppsOpen) { interfaceAppsOpen = !interfaceAppsOpen } }
                                if (interfaceAppsOpen) {
                                item { NumberSlider("حجم أيقونات التطبيقات", settings.iconSizeDp, 40, 110, "dp") { viewModel.updateSettings(settings.copy(iconSizeDp = it)) } }
                                item { NumberSlider("أعمدة درج التطبيقات", settings.appDrawerColumns, 2, 8, "أعمدة") { viewModel.updateSettings(settings.copy(appDrawerColumns = it)) } }
                                item { SwitchRow("أسماء التطبيقات", "إظهار الاسم أسفل الأيقونة", settings.showAppLabels) { viewModel.updateSettings(settings.copy(showAppLabels = it)) } }
                                }

                                item { ExpandableSectionHeader("المساحة الآمنة", Icons.Default.AspectRatio, interfaceSafeAreaOpen) { interfaceSafeAreaOpen = !interfaceSafeAreaOpen } }
                                if (interfaceSafeAreaOpen) {
                                item { InfoCard("الهوامش الحالية: أعلى ${safeArea.topDp} • أسفل ${safeArea.bottomDp} • يمين ${safeArea.rightDp} • يسار ${safeArea.leftDp} dp") }
                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ActionButton("ضبط الهوامش", Icons.Default.AspectRatio, onOpenSafeAreaPreview)
                                        ActionButton("تصفير", Icons.Default.Refresh) { viewModel.resetSafeArea() }
                                    }
                                }
                                }
                            }

                            SettingsCategory.WIDGETS -> {
                                item { SwitchRow("وضع تصميم الشاشة", "اختر أي ودجت ثم حرّكه أو غيّر حجمه ومظهره", isDesignMode) { viewModel.toggleDesignMode() } }
                                item { InfoCard("كل الودجت الآن تستخدم نفس محرر المظهر: لون النص، اللون المميّز، خلفية شفافة أو زجاجية أو بطاقة، شفافية الخلفية، الإطار، المقاس والقفل.") }
                                item { InfoCard("اختلاف الشكل ليس لونًا فقط: اختر «الشكل» داخل محرر الودجت للتبديل بين بناء رقمي، عدّاد، بطاقة، شريط، شبكة أو تخطيط مختصر بحسب نوع الودجت.") }
                                item { ActionButton("إعادة فحص التطبيقات", Icons.Default.Refresh) { viewModel.loadApps() } }
                                item { ActionButton("إرجاع ودجت الرئيسية للوضع الافتراضي", Icons.Default.RestartAlt) { viewModel.resetWidgetsToDefault() } }
                            }

                            SettingsCategory.SCREENSAVER -> {
                                item { SwitchRow("تفعيل شاشة التوقف", "تظهر عند السكون ولا تقاطع شاشة الخريطة", settings.screenSaverEnabled) { viewModel.updateSettings(settings.copy(screenSaverEnabled = it)) } }
                                item { NumberSlider("وقت الانتظار", settings.screenSaverTimeoutSeconds, 30, 1800, "ث") { viewModel.updateSettings(settings.copy(screenSaverTimeoutSeconds = it)) } }
                                item { SwitchRow("استخدام الخلفية الحالية", "إظهار الخلفية خلف ودجت شاشة التوقف", settings.screenSaverUseWallpaper) { viewModel.updateSettings(settings.copy(screenSaverUseWallpaper = it)) } }
                                item { SwitchRow("الوضع الليلي", "يخفض سطوع النافذة ويعتّم الخلفية أثناء القيادة ليلًا", settings.screenSaverNightMode) { viewModel.updateSettings(settings.copy(screenSaverNightMode = it)) } }
                                if (settings.screenSaverNightMode) {
                                    item { NumberSlider("سطوع الوضع الليلي", settings.screenSaverNightBrightnessPercent, 5, 40, "%") { viewModel.updateSettings(settings.copy(screenSaverNightBrightnessPercent = it)) } }
                                }
                                item { SectionTitle("ودجت شاشة التوقف — حتى 4", Icons.Default.Widgets) }
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        SCREEN_SAVER_DISPLAY_WIDGET_TYPES.toList().chunked(3).forEach { row ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                row.forEach { type ->
                                                    FilterChip(selected = type in settings.screenSaverWidgetTypes, onClick = { viewModel.toggleScreenSaverWidget(type) }, label = { Text(type.arabicTitle, fontSize = 9.sp) })
                                                }
                                            }
                                        }
                                    }
                                }
                                item { ActionButton("فتح محرر شاشة التوقف", Icons.Default.Edit, onOpenScreenSaverEditor) }
                                item { InfoCard("داخل المحرر: اضغط الودجت المطلوب، ثم استخدم الشريط السفلي لتغيير الشكل والألوان والخلفية والإطار والشفافية. التحريك والتحجيم يظهران للودجت المختار فقط.") }
                            }

                            SettingsCategory.MEDIA -> {
                                item { SwitchRow("حفظ آخر موضع", "عند التشغيل لاحقًا يبدأ من نفس المقطع والموضع دون تشغيل تلقائي", settings.resumeMusicPlayback) { viewModel.updateSettings(settings.copy(resumeMusicPlayback = it)) } }
                                item { ActionButton("إضافة ملف صوت", Icons.Default.LibraryMusic) { musicPicker.launch(arrayOf("audio/*")) } }
                                item { StatusMetric("المقطع الحالي", playback.currentTrack?.title ?: "لا يوجد", playback.currentTrack != null) }
                                item { StatusMetric("المقاطع المكتشفة", playback.playlist.size.toString(), playback.playlist.isNotEmpty()) }
                            }

                            SettingsCategory.DRIVING -> {
                                item {
                                    Surface(color = accent.copy(alpha = .08f), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f)), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(if (gps.hasGpsFix && gps.isSpeedReliable) gps.speedKmH.toInt().toString() else "--", color = accent, fontSize = 32.sp, fontWeight = FontWeight.Black)
                                            Column { Text("كم/س", color = TextPrimary, fontWeight = FontWeight.Bold); Text(if (gps.hasGpsFix) "GPS ±${gps.accuracyMeters.toInt()}م" else gps.statusArabic, color = TextSecondary, fontSize = 11.sp) }
                                        }
                                    }
                                }
                                item {
                                    ChoiceCard("وحدة السرعة", "تُطبّق على ودجت السرعة") {
                                        FilterChip(selected = settings.speedUnit == "كم/س", onClick = { viewModel.updateSettings(settings.copy(speedUnit = "كم/س")) }, label = { Text("كم/س") })
                                        FilterChip(selected = settings.speedUnit == "MPH", onClick = { viewModel.updateSettings(settings.copy(speedUnit = "MPH")) }, label = { Text("MPH") })
                                    }
                                }
                                item { SwitchRow("بدء الرحلة تلقائيًا", "يتأكد Launcher من حركة فعلية قبل بدء التسجيل", settings.autoLogTrips) { viewModel.updateSettings(settings.copy(autoLogTrips = it)) } }
                                item { ActionButton("تصفير الرحلة الحالية", Icons.Default.Refresh) { viewModel.resetTrip() } }

                                item { SectionTitle("الخريطة دون إنترنت", Icons.Default.Map) }
                                item { InfoCard("المعتمد: خريطة السعودية العربية، مع خريطة الخليج كخيار إضافي. أضف ملف Mapsforge بامتداد .map؛ تُعرض الأسماء العربية أولًا وتعمل الخريطة بالكامل دون إنترنت.") }
                                item { ActionButton("إضافة خريطة", Icons.Default.AddLocationAlt) { mapPicker.launch(arrayOf("*/*")) } }
                                if (maps.isEmpty()) item { StatusMetric("الخرائط", "لا توجد خريطة مضافة", false) }
                                items(maps, key = { it.id }) { map ->
                                    Surface(color = if (map.isActive) accent.copy(alpha = .12f) else CarbonSurface, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, if (map.isActive) accent else CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Map, null, tint = if (map.isActive) accent else TextSecondary)
                                            Column(Modifier.weight(1f)) {
                                                Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("${map.fileSizeFormatted}${if (map.isActive) " • نشطة" else ""}", color = TextSecondary, fontSize = 10.sp)
                                            }
                                            if (!map.isActive) FilledTonalButton(onClick = { viewModel.setActiveMap(map.id) }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(34.dp)) { Text("تفعيل", fontSize = 10.sp) }
                                            IconButton(onClick = { viewModel.deleteMap(map.id) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed, modifier = Modifier.size(19.dp)) }
                                        }
                                    }
                                }
                            }

                            SettingsCategory.SECURITY -> {
                                item { InfoCard("قفل الأطفال غير مرئي ويحجب اللمسات. للتفعيل اضغط مطولًا على مؤشر Wi‑Fi أعلى الشاشة؛ ولفك القفل اضغط مطولًا في المنطقة نفسها.") }
                                item { NumberSlider("مدة الضغط لفك القفل", settings.childUnlockHoldSeconds, 2, 6, "ث") { viewModel.updateSettings(settings.copy(childUnlockHoldSeconds = it)) } }
                                item { ActionButton("تفعيل القفل الآن", Icons.Default.Lock) { viewModel.activateChildLock() } }
                            }

                            SettingsCategory.SYSTEM -> {
                                item { SwitchRow("التشغيل التلقائي", "فتح Launcher بعد تشغيل الشاشة", settings.autoStartOnBoot) { viewModel.updateSettings(settings.copy(autoStartOnBoot = it)) } }
                                item { SwitchRow("إبقاء الشاشة مضاءة", "منع إطفاء الشاشة أثناء استخدام Launcher", settings.keepScreenOn) { viewModel.updateSettings(settings.copy(keepScreenOn = it)) } }
                                item { SwitchRow("الوضع عالي التباين", "رفع وضوح النصوص والعناصر", settings.highContrastMode) { viewModel.updateSettings(settings.copy(highContrastMode = it)) } }
                                item { ClockFormatRow(settings.is24HourFormat) { is24 -> viewModel.updateSettings(settings.copy(is24HourFormat = is24)) } }
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
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    val accent = LocalSettingsAccent.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Text(title, color = accent, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
private fun ExpandableSectionHeader(title: String, icon: ImageVector, expanded: Boolean, onToggle: () -> Unit) {
    val accent = LocalSettingsAccent.current
    Surface(
        onClick = onToggle,
        color = if (expanded) accent.copy(alpha = .10f) else CarbonSurface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, if (expanded) accent.copy(alpha = .45f) else CarbonCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, content: @Composable RowScope.() -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}

@Composable
private fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
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
    Surface(color = CarbonSurface, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun NumberSlider(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    val accent = LocalSettingsAccent.current
    Surface(color = CarbonSurface, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("$value $unit", color = accent, fontWeight = FontWeight.Bold)
            }
            Slider(value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = min.toFloat()..max.toFloat())
        }
    }
}

@Composable
private fun ActionButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    val accent = LocalSettingsAccent.current
    Button(onClick = onClick, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface), border = BorderStroke(1.dp, CarbonCardBorder)) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(text: String) {
    val accent = LocalSettingsAccent.current
    Surface(color = accent.copy(alpha = .07f), shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, accent.copy(alpha = .23f)), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun StatusMetric(label: String, value: String, positive: Boolean) {
    Surface(color = CarbonSurface, shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(value, color = if (positive) EmeraldSafe else TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
