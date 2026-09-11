package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.CarLauncherApp
import com.example.core.bridge.DarbakAppBridge
import com.example.core.bridge.DarbakModuleId
import com.example.core.bridge.DarbakModuleState
import com.example.core.bridge.DarbakSystemStateStore
import com.example.data.StableBackupManager
import com.example.data.readUtf8TextLimited
import com.example.data.UpdateStatus
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StableSystemPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as CarLauncherApp
    val updateState by app.updateManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val backupManager = remember { StableBackupManager(context.applicationContext) }
    val systemStore = remember { DarbakSystemStateStore(DarbakAppBridge(context.applicationContext)) }
    val systemModules by systemStore.modules.collectAsState()
    var localMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { systemStore.refresh() }

    val backupExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            localMessage = "جارٍ إنشاء النسخة الاحتياطية..."
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = backupManager.exportBackup()
                    (context.contentResolver.openOutputStream(uri, "wt") ?: error("No output stream")).bufferedWriter().use { it.write(raw) }
                    true
                }.getOrDefault(false)
            }
            localMessage = if (ok) "تم حفظ النسخة الاحتياطية" else "تعذر حفظ النسخة الاحتياطية"
        }
    }

    val backupImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            localMessage = "جارٍ استعادة الإعدادات..."
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = context.contentResolver.readUtf8TextLimited(uri)
                    backupManager.importBackup(raw)
                }.getOrDefault(0)
            }
            localMessage = if (count > 0) "تمت استعادة $count قيمة — أعد تشغيل Darbak Launcher لتطبيقها بالكامل" else "الملف غير صالح أو تعذر الاستيراد"
        }
    }

    val diagnosticsExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    (context.contentResolver.openOutputStream(uri, "wt") ?: error("No output stream")).bufferedWriter().use { it.write(backupManager.exportDiagnostics()) }
                    true
                }.getOrDefault(false)
            }
            localMessage = if (ok) "تم تصدير تقرير التشخيص" else "تعذر تصدير التقرير"
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DarbakSystemDashboard(
            modules = systemModules,
            onRefresh = { systemStore.refresh() },
            onLaunch = { id ->
                if (!systemStore.launch(id)) localMessage = "تعذر فتح التطبيق — تحقق من تثبيته"
            }
        )

        Surface(
            color = CarbonSurface,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SystemUpdateAlt, null, tint = CyanNeon)
                    Column(Modifier.weight(1f)) {
                        Text("تحديث Darbak Launcher", color = TextPrimary, fontWeight = FontWeight.Black)
                        Text("الإصدار الحالي ${BuildConfig.VERSION_NAME}", color = TextSecondary, fontSize = 14.sp)
                    }
                }

                Text(
                    when (updateState.status) {
                        UpdateStatus.IDLE -> "يتم فحص التحديث تلقائيًا بعد تشغيل Darbak Launcher"
                        UpdateStatus.CHECKING -> "جارٍ البحث عن تحديث..."
                        UpdateStatus.AVAILABLE -> updateState.message
                        UpdateStatus.UP_TO_DATE -> "لديك أحدث إصدار"
                        UpdateStatus.DOWNLOADING -> updateState.message
                        UpdateStatus.READY_TO_INSTALL -> "تم تنزيل ${updateState.info?.versionName ?: "الإصدار الجديد"} وهو جاهز للتثبيت"
                        UpdateStatus.ERROR -> updateState.message
                    },
                    color = if (updateState.status == UpdateStatus.ERROR) HighContrastRed else if (updateState.status == UpdateStatus.READY_TO_INSTALL) EmeraldSafe else TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (updateState.status == UpdateStatus.DOWNLOADING) {
                    LinearProgressIndicator(progress = { updateState.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { scope.launch { app.updateManager.checkAndAutoDownload() } },
                        modifier = Modifier.heightIn(min = 56.dp),
                        enabled = updateState.status != UpdateStatus.CHECKING && updateState.status != UpdateStatus.DOWNLOADING,
                        colors = ButtonDefaults.buttonColors(containerColor = CarbonCard)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = CyanNeon)
                        Spacer(Modifier.width(5.dp))
                        Text("البحث عن تحديث", color = TextPrimary, fontSize = 16.sp)
                    }
                    if (updateState.status == UpdateStatus.READY_TO_INSTALL) {
                        Button(onClick = { app.updateManager.installDownloadedUpdate() }, modifier = Modifier.heightIn(min = 56.dp)) {
                            Icon(Icons.Default.InstallMobile, null)
                            Spacer(Modifier.width(5.dp))
                            Text("تثبيت التحديث")
                        }
                    }
                }
            }
        }

        Surface(color = CarbonSurface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("النسخ الاحتياطي والصيانة", color = TextPrimary, fontWeight = FontWeight.Black)
                Text("يحفظ إعدادات Darbak Launcher والودجات والمواقع وأثر البر وحالة الخريطة. ملفات الخرائط الكبيرة نفسها لا تدخل في النسخة.", color = TextSecondary, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { backupExport.launch("Darbak-Launcher-backup.json") }, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Default.Backup, null); Spacer(Modifier.width(4.dp)); Text("نسخة احتياطية") }
                    OutlinedButton(onClick = { backupImport.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(4.dp)); Text("استعادة") }
                    OutlinedButton(onClick = { diagnosticsExport.launch("Darbak-Launcher-diagnostics.txt") }, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Default.Description, null); Spacer(Modifier.width(4.dp)); Text("تقرير") }
                }
            }
        }

        if (localMessage.isNotBlank()) {
            Text(localMessage, color = if (localMessage.startsWith("تعذر") || localMessage.contains("غير صالح")) HighContrastRed else EmeraldSafe, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DarbakSystemDashboard(
    modules: List<DarbakModuleState>,
    onRefresh: () -> Unit,
    onLaunch: (DarbakModuleId) -> Unit,
) {
    val installed = modules.count { it.installed }
    val companions = modules.filter { it.spec.id != DarbakModuleId.LAUNCHER }
    val primaryIds = setOf(DarbakModuleId.VEHICLE_HUB, DarbakModuleId.MAINTENANCE, DarbakModuleId.MEDIA)
    val primary = companions.filter { it.spec.id in primaryIds }
    val secondary = companions.filterNot { it.spec.id in primaryIds }

    Surface(
        color = CarbonCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CyanNeon.copy(alpha = .32f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = CyanNeon.copy(alpha = .12f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(46.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Hub, null, tint = CyanNeon, modifier = Modifier.size(25.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Darbak System", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text("$installed/${modules.size} مكوّنات مثبتة على الشاشة", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "تحديث حالة التطبيقات", tint = CyanNeon)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                primary.forEach { module ->
                    DarbakModuleCard(module, Modifier.weight(1f), onLaunch)
                }
            }

            if (secondary.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    secondary.forEach { module ->
                        DarbakCompactModule(module, Modifier.weight(1f), onLaunch)
                    }
                }
            }

            Text(
                "التطبيقات تبقى مستقلة، واللانشر يعرض حالتها ويفتحها دون أن يعتمد استقراره على أي تطبيق منها.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DarbakModuleCard(
    module: DarbakModuleState,
    modifier: Modifier,
    onLaunch: (DarbakModuleId) -> Unit,
) {
    val tint = if (module.installed && module.enabled) EmeraldSafe else AmberRacing
    Surface(
        onClick = { if (module.launchable) onLaunch(module.spec.id) },
        enabled = module.launchable,
        color = CarbonSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = .30f)),
        modifier = modifier.heightIn(min = 116.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(moduleIcon(module.spec.id), null, tint = tint, modifier = Modifier.size(23.dp))
                Spacer(Modifier.weight(1f))
                Surface(color = tint.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                    Text(if (module.installed) "مثبت" else "غير مثبت", color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            Text(moduleTitle(module.spec.id), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(module.versionName?.let { "v$it" } ?: "لا يوجد إصدار", color = TextSecondary, fontSize = 11.sp)
            Text(if (module.launchable) "اضغط للفتح" else "غير متاح على الشاشة", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DarbakCompactModule(
    module: DarbakModuleState,
    modifier: Modifier,
    onLaunch: (DarbakModuleId) -> Unit,
) {
    val tint = if (module.installed && module.enabled) EmeraldSafe else TextMuted
    Surface(
        onClick = { if (module.launchable) onLaunch(module.spec.id) },
        enabled = module.launchable,
        color = CarbonDark.copy(alpha = .45f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.heightIn(min = 64.dp)
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(moduleIcon(module.spec.id), null, tint = tint, modifier = Modifier.size(19.dp))
            Column(Modifier.weight(1f)) {
                Text(moduleTitle(module.spec.id), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (module.installed) module.versionName?.let { "v$it" } ?: "مثبت" else "غير مثبت", color = TextMuted, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

private fun moduleTitle(id: DarbakModuleId): String = when (id) {
    DarbakModuleId.LAUNCHER -> "Darbak Launcher"
    DarbakModuleId.VEHICLE_HUB -> "السيارة"
    DarbakModuleId.MAINTENANCE -> "الصيانة"
    DarbakModuleId.MEDIA -> "الوسائط"
    DarbakModuleId.KIDS_TV -> "Kids TV"
    DarbakModuleId.LAQQINNI -> "لقّني"
    DarbakModuleId.ADHKAR -> "الأذكار"
}

private fun moduleIcon(id: DarbakModuleId): ImageVector = when (id) {
    DarbakModuleId.LAUNCHER -> Icons.Default.Home
    DarbakModuleId.VEHICLE_HUB -> Icons.Default.DirectionsCar
    DarbakModuleId.MAINTENANCE -> Icons.Default.Build
    DarbakModuleId.MEDIA -> Icons.Default.MusicNote
    DarbakModuleId.KIDS_TV -> Icons.Default.Tv
    DarbakModuleId.LAQQINNI -> Icons.Default.MenuBook
    DarbakModuleId.ADHKAR -> Icons.Default.Favorite
}
