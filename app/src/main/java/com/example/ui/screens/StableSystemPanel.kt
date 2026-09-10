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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.CarLauncherApp
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
    var localMessage by remember { mutableStateOf("") }

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
            localMessage = if (count > 0) "تمت استعادة $count قيمة — أعد تشغيل Launcher لتطبيقها بالكامل" else "الملف غير صالح أو تعذر الاستيراد"
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
        Surface(
            color = CarbonSurface,
            shape = RoundedCornerShape(22.dp),

            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.SystemUpdateAlt, null, tint = CyanNeon)
                    Column(Modifier.weight(1f)) {
                        Text("تحديث التطبيق", color = TextPrimary, fontWeight = FontWeight.Black)
                        Text("الإصدار الحالي ${BuildConfig.VERSION_NAME}", color = TextSecondary, fontSize = 14.sp)
                    }
                }

                Text(
                    when (updateState.status) {
                        UpdateStatus.IDLE -> "يتم فحص التحديث تلقائيًا بعد تشغيل Launcher"
                        UpdateStatus.CHECKING -> "جارٍ البحث عن تحديث..."
                        UpdateStatus.AVAILABLE -> updateState.message
                        UpdateStatus.UP_TO_DATE -> "لديك أحدث إصدار"
                        UpdateStatus.DOWNLOADING -> "${updateState.message}"
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

        Surface(color = CarbonSurface, shape = RoundedCornerShape(22.dp),  modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("النسخ الاحتياطي والصيانة", color = TextPrimary, fontWeight = FontWeight.Black)
                Text("يحفظ إعدادات Launcher والودجات والمواقع وأثر البر وحالة الخريطة. ملفات الخرائط الكبيرة نفسها لا تدخل في النسخة.", color = TextSecondary, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { backupExport.launch("Launcher-2026-backup.json") }, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Default.Backup, null); Spacer(Modifier.width(4.dp)); Text("نسخة احتياطية") }
                    OutlinedButton(onClick = { backupImport.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(4.dp)); Text("استعادة") }
                    OutlinedButton(onClick = { diagnosticsExport.launch("Launcher-2026-diagnostics.txt") }, modifier = Modifier.heightIn(min = 56.dp)) { Icon(Icons.Default.Description, null); Spacer(Modifier.width(4.dp)); Text("تقرير") }
                }
            }
        }

        if (localMessage.isNotBlank()) {
            Text(localMessage, color = if (localMessage.startsWith("تعذر") || localMessage.contains("غير صالح")) HighContrastRed else EmeraldSafe, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
