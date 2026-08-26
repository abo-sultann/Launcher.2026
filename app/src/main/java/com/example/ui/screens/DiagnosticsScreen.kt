package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ComponentHealth
import com.example.model.ComponentStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report by viewModel.diagnosticReport.collectAsState()
    val isSafeModeActive by viewModel.isSafeModeActive.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.runDiagnostics()
    }

    val components = remember(report) {
        report?.let {
            listOf(
                it.homeStatus,
                it.gpsStatus,
                it.musicStatus,
                it.mapsStatus,
                it.databaseStatus,
                it.widgetsStatus,
                it.storageStatus
            )
        } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = CyanNeon)
                }
                Text(
                    text = "تشخيص النظام وفحص الأمان (System Diagnostics)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Button(
                onClick = { viewModel.runDiagnostics() },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = CarbonDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة الفحص", color = CarbonDark, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Safe Mode Alert Card (if any issues or active)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSafeModeActive) HighContrastRed.copy(alpha = 0.15f) else EmeraldSafe.copy(alpha = 0.12f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSafeModeActive) HighContrastRed else EmeraldSafe
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isSafeModeActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isSafeModeActive) HighContrastRed else EmeraldSafe,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (isSafeModeActive) "الوضع الآمن مفعّل لحماية الواجهة" else "جميع أنظمة المشغل تعمل بكفاءة 100%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (isSafeModeActive) "عدد مرات الإيقاف غير المتوقع: ${report?.crashCount ?: 0}" else "نظام العزل التلقائي Crash Isolation يحمي الشاشة الرئيسية من أي توقف",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (isSafeModeActive) {
                    Button(
                        onClick = { viewModel.resetSafeMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = HighContrastRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_reset_safe_mode")
                    ) {
                        Text("إلغاء الوضع الآمن", color = TextPrimary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Component Health Status List
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(components) { comp ->
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (comp.status) {
                                            ComponentStatus.RUNNING -> EmeraldSafe
                                            ComponentStatus.NOT_STARTED -> AmberRacing
                                            ComponentStatus.ERROR -> HighContrastRed
                                        }
                                    )
                            )
                            Column {
                                Text(
                                    text = comp.nameArabic,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = comp.details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            color = when (comp.status) {
                                ComponentStatus.RUNNING -> EmeraldSafe.copy(alpha = 0.15f)
                                ComponentStatus.NOT_STARTED -> AmberRacing.copy(alpha = 0.15f)
                                ComponentStatus.ERROR -> HighContrastRed.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (comp.status) {
                                    ComponentStatus.RUNNING -> EmeraldSafe
                                    ComponentStatus.NOT_STARTED -> AmberRacing
                                    ComponentStatus.ERROR -> HighContrastRed
                                }
                            )
                        ) {
                            Text(
                                text = when (comp.status) {
                                    ComponentStatus.RUNNING -> "يعمل بشكل سليم"
                                    ComponentStatus.NOT_STARTED -> "جاهز للبدء"
                                    ComponentStatus.ERROR -> "خطأ"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (comp.status) {
                                    ComponentStatus.RUNNING -> EmeraldSafe
                                    ComponentStatus.NOT_STARTED -> AmberRacing
                                    ComponentStatus.ERROR -> HighContrastRed
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
