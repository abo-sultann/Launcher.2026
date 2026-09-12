package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DisplayAutomationController
import com.example.ui.theme.*

@Composable
internal fun NightBrightnessPanel() {
    val context = LocalContext.current
    val controller = remember(context) { DisplayAutomationController(context) }
    var config by remember { mutableStateOf(controller.readConfig()) }
    var canWrite by remember { mutableStateOf(controller.hasWriteSettingsPermission()) }
    var override by remember { mutableStateOf(controller.manualOverride()) }

    fun save(next: DisplayAutomationController.Config) {
        config = next
        controller.saveConfig(next)
        canWrite = controller.hasWriteSettingsPermission()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = DarbakGold.copy(alpha = .08f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, DarbakGold.copy(alpha = .25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = DarbakGold.copy(alpha = .14f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(46.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DarkMode, null, tint = DarbakGold, modifier = Modifier.size(25.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("السطوع والوضع الليلي", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (controller.isNightActive(config)) "الوضع الليلي نشط الآن" else "الوضع النهاري نشط الآن",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    when (override) {
                        DisplayAutomationController.ManualOverride.AUTO -> "تلقائي"
                        DisplayAutomationController.ManualOverride.FORCE_NIGHT -> "ليلي يدوي"
                        DisplayAutomationController.ManualOverride.FORCE_DAY -> "نهاري يدوي"
                    },
                    color = if (override == DisplayAutomationController.ManualOverride.FORCE_NIGHT) DarbakGold else LocalSettingsAccent.current,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        SwitchRow(
            "السطوع الليلي التلقائي",
            "يخفض سطوع النظام في الوقت المحدد ويعيد سطوع النهار السابق تلقائيًا",
            config.enabled
        ) { save(config.copy(enabled = it)) }

        if (config.enabled) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    NumberSlider("بداية الليل", config.startHour, 16, 23, "ساعة") { save(config.copy(startHour = it)) }
                }
                Box(Modifier.weight(1f)) {
                    NumberSlider("نهاية الليل", config.endHour, 4, 10, "ساعة") { save(config.copy(endHour = it)) }
                }
            }
            NumberSlider("سطوع الليل", config.nightBrightnessPercent, 10, 60, "%") { save(config.copy(nightBrightnessPercent = it)) }
            NumberSlider("تعتيم إضافي للخلفية", config.extraDimPercent, 0, 40, "%") { save(config.copy(extraDimPercent = it)) }
        }

        if (!canWrite) {
            InfoCard("لتطبيق السطوع على النظام كله، امنح Darbak Launcher صلاحية «تعديل إعدادات النظام» مرة واحدة. بدونها يبقى تعتيم واجهة دربك فقط.")
            ActionButton("منح صلاحية التحكم بالسطوع", Icons.Default.SettingsBrightness) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Throwable) {
                    context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } else {
            StatusMetric("صلاحية السطوع", "مفعلة — التحكم يشمل جميع التطبيقات", true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("تجربة الليل الآن", Icons.Default.DarkMode) {
                override = controller.toggleQuickNight()
            }
            ActionButton("العودة للتلقائي", Icons.Default.AutoMode) {
                controller.useAutomaticSchedule()
                override = DisplayAutomationController.ManualOverride.AUTO
            }
        }
    }
}

@Composable
internal fun DrivingSafetySettingsPanel() {
    val context = LocalContext.current
    val controller = remember(context) { DisplayAutomationController(context) }
    var config by remember { mutableStateOf(controller.readConfig()) }

    fun save(next: DisplayAutomationController.Config) {
        config = next
        controller.saveConfig(next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchRow(
            "وضع القيادة الآمن",
            "يقفل إعدادات التعديل والاستيراد والحذف أثناء الحركة ويترك الموسيقى والخريطة والتحكم الأساسي متاحًا",
            config.safeDrivingEnabled
        ) { save(config.copy(safeDrivingEnabled = it)) }
        if (config.safeDrivingEnabled) {
            NumberSlider("سرعة تفعيل القفل", config.safeDrivingThresholdKmH, 5, 40, "كم/س") {
                save(config.copy(safeDrivingThresholdKmH = it))
            }
        }
    }
}

@Composable
internal fun DrivingSafetyLockPanel(speedKmH: Int, thresholdKmH: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = CarbonSurface.copy(alpha = .55f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, AmberRacing.copy(alpha = .48f)),
            modifier = Modifier.fillMaxWidth(.82f)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = AmberRacing, modifier = Modifier.size(40.dp))
                Text("وضع القيادة الآمن", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("الإعدادات الحساسة مقفلة أثناء الحركة", color = TextSecondary, fontSize = 14.sp)
                Text("السرعة $speedKmH كم/س • القفل يبدأ من $thresholdKmH كم/س", color = AmberRacing, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("توقف بالمركبة لفتح الإعدادات مرة أخرى.", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}
