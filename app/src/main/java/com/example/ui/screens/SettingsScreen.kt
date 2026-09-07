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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.RecommendedMapStatus
import com.example.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

/** Darbak Settings UI V1 — large glass cards first, task details second. */
enum class SettingsCategory(
    val arabicTitle: String,
    val subtitle: String,
    val icon: ImageVector
) {
    INTERFACE("الواجهة", "الخلفية • الألوان • أشرطة الشاشة", Icons.Default.DashboardCustomize),
    WIDGETS("الودجت", "التصميم • الحجم • الترتيب", Icons.Default.Widgets),
    SCREENSAVER("شاشة التوقف", "السكون • العرض • الودجت", Icons.Default.NightsStay),
    MEDIA("الوسائط", "الموسيقى • الاستئناف • الملفات", Icons.Default.MusicNote),
    DRIVING("القيادة والخريطة", "GPS • الرحلة • الخرائط دون إنترنت", Icons.Default.Navigation),
    SECURITY("الأمان", "قفل الأطفال وحماية اللمس", Icons.Default.Lock),
    SYSTEM("النظام والتحديث", "التشخيص • الإقلاع • التحديثات", Icons.Default.SettingsSuggest),
    ABOUT("حول", "دربك • الإصدار • الملكية", Icons.Default.VerifiedUser)
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
    val fileImportStatus by viewModel.fileImportStatus.collectAsState()
    val recommendedMapDownload by viewModel.recommendedMapDownloadState.collectAsState()
    var selected by remember { mutableStateOf<SettingsCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var interfaceBackgroundOpen by remember { mutableStateOf(true) }
    var interfaceBarsOpen by remember { mutableStateOf(false) }
    var interfaceAppsOpen by remember { mutableStateOf(false) }
    var interfaceSafeAreaOpen by remember { mutableStateOf(false) }
    var mapPendingDelete by remember { mutableStateOf<MapItem?>(null) }
    var confirmRecommendedMapDownload by remember { mutableStateOf(false) }
    val accent = Color(settings.interfaceAccent.argb)

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::importMusicUri) }
    // ACTION_GET_CONTENT works more consistently with the old Android 7 file manager.
    val mapPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::importMapUri) }
    // More reliable than ACTION_OPEN_DOCUMENT on Android 7 car-unit file managers.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::importWallpaperUri) }

    CompositionLocalProvider(LocalSettingsAccent provides accent) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = CarbonDark.copy(alpha = .97f)
        ) {
            val activeCategory = selected
            if (activeCategory == null) {
                DarbakSettingsLanding(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    accent = accent,
                    statusFor = { category ->
                        when (category) {
                            SettingsCategory.INTERFACE -> settings.backgroundType.arabicName
                            SettingsCategory.WIDGETS -> if (isDesignMode) "وضع التصميم" else "جاهز"
                            SettingsCategory.SCREENSAVER -> if (settings.screenSaverEnabled) "مفعّلة" else "متوقفة"
                            SettingsCategory.MEDIA -> "${playback.playlist.size} مقطع"
                            SettingsCategory.DRIVING -> if (gps.hasGpsFix) "GPS متصل" else "بانتظار GPS"
                            SettingsCategory.SECURITY -> "حماية اللمس"
                            SettingsCategory.SYSTEM -> "v${BuildConfig.VERSION_NAME}"
                            SettingsCategory.ABOUT -> "أبوسلطان"
                        }
                    },
                    positiveFor = { category ->
                        when (category) {
                            SettingsCategory.SCREENSAVER -> settings.screenSaverEnabled
                            SettingsCategory.DRIVING -> gps.hasGpsFix
                            SettingsCategory.MEDIA -> playback.playlist.isNotEmpty()
                            else -> true
                        }
                    },
                    onSelect = { selected = it }
                )
            } else {
                DarbakSettingsDetailHeader(
                    category = activeCategory,
                    accent = accent,
                    onBack = { selected = null }
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        when (activeCategory) {
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
                                    fileImportStatus?.let { status -> item { StatusMetric("حالة الاستيراد", status, status.startsWith("تم")) } }
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
                                item { InfoCard("اللون أصبح واضحًا ومختصرًا: أبيض أو أسود فقط. ويمكن اختيار سطح شفاف أو زجاجي أو بطاقة بما يناسب الخلفية.") }
                                item { InfoCard("لكل نوع ثلاثة أشكال مختلفة فعليًا: بسيط، معلوماتي، وشكل لوحة قيادة؛ تم إخفاء الأشكال المتشابهة القديمة مع بقاء توافق الترتيبات المحفوظة.") }
                                item { ActionButton("إعادة فحص التطبيقات", Icons.Default.Refresh) { viewModel.loadApps() } }
                                item { ActionButton("إرجاع ودجت الرئيسية للوضع الافتراضي", Icons.Default.RestartAlt) { viewModel.resetWidgetsToDefault() } }
                            }

                            SettingsCategory.SCREENSAVER -> {
                                item { SwitchRow("تفعيل شاشة التوقف", "تظهر عند السكون ولا تقاطع شاشة الخريطة", settings.screenSaverEnabled) { viewModel.updateSettings(settings.copy(screenSaverEnabled = it)) } }
                                item { NumberSlider("وقت الانتظار", settings.screenSaverTimeoutSeconds, 30, 1800, "ث") { viewModel.updateSettings(settings.copy(screenSaverTimeoutSeconds = it)) } }
                                item { SwitchRow("استخدام الخلفية الحالية", "إظهار الخلفية خلف ودجت شاشة التوقف", settings.screenSaverUseWallpaper) { viewModel.updateSettings(settings.copy(screenSaverUseWallpaper = it)) } }
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
                                item { InfoCard("شاشة التوقف للعرض فقط ولا تحتوي أزرار تطبيقات أو تحكم. الترتيب الوحيد موجود هنا داخل الإعدادات، مع الأبيض والأسود والأسطح الثلاثة.") }
                            }

                            SettingsCategory.MEDIA -> {
                                item { SwitchRow("حفظ آخر موضع", "عند التشغيل لاحقًا يبدأ من نفس المقطع والموضع دون تشغيل تلقائي", settings.resumeMusicPlayback) { viewModel.updateSettings(settings.copy(resumeMusicPlayback = it)) } }
                                item { ActionButton("إضافة ملف صوت", Icons.Default.LibraryMusic) { viewModel.prepareForExternalPicker(); musicPicker.launch("audio/*") } }
                                item { StatusMetric("المقطع الحالي", playback.currentTrack?.title ?: "لا يوجد", playback.currentTrack != null) }
                                item { StatusMetric("المقاطع المكتشفة", playback.playlist.size.toString(), playback.playlist.isNotEmpty()) }
                            }

                            SettingsCategory.DRIVING -> {
                                item {
                                    Surface(color = accent.copy(alpha = .08f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, accent.copy(alpha = .28f)), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                item { InfoCard("Mapsforge (.map) هو الخيار المفضل: أسماء طرق ومواقع قابلة للبحث مع حفظ المواقع والتوجيه المباشر. وتدعم الخريطة أيضًا MBTiles الصورية إذا كانت الأسماء مرسومة داخلها.") }
                                item {
                                    ActionButton(
                                        title = if (recommendedMapDownload.status == RecommendedMapStatus.DOWNLOADING) {
                                            "تنزيل خريطة الخليج ${recommendedMapDownload.progressPercent}%"
                                        } else {
                                            "تنزيل خريطة الخليج 2026 (322 MB)"
                                        },
                                        icon = Icons.Default.CloudDownload,
                                        enabled = recommendedMapDownload.status != RecommendedMapStatus.DOWNLOADING
                                    ) { confirmRecommendedMapDownload = true }
                                }
                                if (recommendedMapDownload.status != RecommendedMapStatus.IDLE) {
                                    item {
                                        StatusMetric(
                                            "الخريطة المقترحة",
                                            recommendedMapDownload.message,
                                            recommendedMapDownload.status == RecommendedMapStatus.INSTALLED
                                        )
                                    }
                                }
                                item { ActionButton("إضافة خريطة من الجهاز", Icons.Default.AddLocationAlt) { viewModel.prepareForExternalPicker(); mapPicker.launch("*/*") } }
                                fileImportStatus?.let { status -> item { StatusMetric("حالة الاستيراد", status, status.startsWith("تم")) } }
                                if (maps.isEmpty()) item { StatusMetric("الخرائط", "لا توجد خريطة مضافة", false) }
                                items(maps, key = { it.id }) { map ->
                                    Surface(color = if (map.isActive) accent.copy(alpha = .12f) else CarbonSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, if (map.isActive) accent else CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Map, null, tint = if (map.isActive) accent else TextSecondary)
                                            Column(Modifier.weight(1f)) {
                                                Text(map.name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text("${map.fileSizeFormatted}${if (map.isActive) " • نشطة" else ""}", color = TextSecondary, fontSize = 10.sp)
                                            }
                                            if (!map.isActive) FilledTonalButton(onClick = { viewModel.setActiveMap(map.id) }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(34.dp)) { Text("تفعيل", fontSize = 10.sp) }
                                            IconButton(onClick = { mapPendingDelete = map }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed, modifier = Modifier.size(19.dp)) }
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

                            SettingsCategory.ABOUT -> {
                                item { AboutOwnershipPanel(accent, onOpenDiagnostics) }
                            }
                        }
                    }
                }
            }
        }
    }

    mapPendingDelete?.let { map ->
        AlertDialog(
            onDismissRequest = { mapPendingDelete = null },
            title = { Text("حذف الخريطة؟") },
            text = { Text("سيُحذف ملف «${map.name}» من Launcher. لا يمكن التراجع عن ذلك.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteMap(map.id); mapPendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed)
                ) { Text("حذف", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { mapPendingDelete = null }) { Text("إلغاء") } }
        )
    }

    if (confirmRecommendedMapDownload) {
        AlertDialog(
            onDismissRequest = { confirmRecommendedMapDownload = false },
            title = { Text("تنزيل خريطة الخليج؟") },
            text = { Text("حجم الخريطة 322 ميجابايت. يفضّل الاتصال بشبكة Wi‑Fi وترك Launcher مفتوحًا حتى يكتمل التحقق والتفعيل.") },
            confirmButton = {
                Button(onClick = {
                    confirmRecommendedMapDownload = false
                    viewModel.downloadRecommendedMap()
                }) { Text("تنزيل") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRecommendedMapDownload = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun DarbakSettingsLanding(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    accent: Color,
    statusFor: (SettingsCategory) -> String,
    positiveFor: (SettingsCategory) -> Boolean,
    onSelect: (SettingsCategory) -> Unit
) {
    val categories = remember(searchQuery) {
        val q = searchQuery.trim()
        SettingsCategory.values().filter {
            q.isBlank() || it.arabicTitle.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("الإعدادات", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("كل ما تحتاجه في مكان واحد", color = TextSecondary, fontSize = 13.sp)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                placeholder = { Text("بحث في الإعدادات...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = accent) },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.copy(alpha = .75f),
                    unfocusedBorderColor = CarbonCardBorder,
                    focusedContainerColor = CarbonCard.copy(alpha = .78f),
                    unfocusedContainerColor = CarbonCard.copy(alpha = .72f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                modifier = Modifier.width(365.dp).heightIn(min = 58.dp)
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.chunked(4).forEach { rowCategories ->
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowCategories.forEach { category ->
                        DarbakSettingsCategoryCard(
                            category = category,
                            status = statusFor(category),
                            positive = positiveFor(category),
                            accent = accent,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onSelect(category) }
                        )
                    }
                    repeat(4 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (categories.isEmpty()) {
                Surface(
                    color = CarbonCard.copy(alpha = .72f),
                    border = BorderStroke(1.dp, CarbonCardBorder),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = TextSecondary, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("لا توجد إعدادات مطابقة", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DarbakSettingsCategoryCard(
    category: SettingsCategory,
    status: String,
    positive: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = .88f)),
        border = BorderStroke(1.dp, accent.copy(alpha = .38f))
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accent.copy(alpha = .14f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = .24f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(category.icon, null, tint = accent, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                DarbakStatusPill(status, positive, accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(category.arabicTitle, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(category.subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DarbakStatusPill(text: String, positive: Boolean, accent: Color) {
    val tint = if (positive) EmeraldSafe else AmberRacing
    Surface(
        color = tint.copy(alpha = .12f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = .52f))
    ) {
        Text(
            text,
            color = tint,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun DarbakSettingsDetailHeader(
    category: SettingsCategory,
    accent: Color,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = .88f)),
            border = BorderStroke(1.dp, accent.copy(alpha = .38f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.ArrowForward, "رجوع", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Surface(color = accent.copy(alpha = .14f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(category.icon, null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(category.arabicTitle, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(category.subtitle, color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
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
        color = if (expanded) accent.copy(alpha = .10f) else CarbonSurface.copy(alpha = .92f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (expanded) accent.copy(alpha = .45f) else CarbonCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, content: @Composable RowScope.() -> Unit) {
    Surface(color = CarbonSurface.copy(alpha = .92f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
        }
    }
}

@Composable
private fun ClockFormatRow(is24: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = CarbonSurface.copy(alpha = .92f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
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
    Surface(color = CarbonSurface.copy(alpha = .92f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
    Surface(color = CarbonSurface.copy(alpha = .92f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(11.dp)) {
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
    ActionButton(title, icon, true, onClick)
}

@Composable
private fun ActionButton(title: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val accent = LocalSettingsAccent.current
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = CarbonSurface), border = BorderStroke(1.dp, CarbonCardBorder)) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(text: String) {
    val accent = LocalSettingsAccent.current
    Surface(color = accent.copy(alpha = .07f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, accent.copy(alpha = .23f)), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(11.dp))
    }
}

@Composable
private fun StatusMetric(label: String, value: String, positive: Boolean) {
    Surface(color = CarbonSurface.copy(alpha = .92f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, CarbonCardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(value, color = if (positive) EmeraldSafe else TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
