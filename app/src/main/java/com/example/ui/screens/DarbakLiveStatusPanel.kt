package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.bridge.*
import com.example.ui.theme.*

/** Live, low-cost status strip for the three primary private Darbak services. */
@Composable
internal fun DarbakLiveStatusPanel(
    modules: List<DarbakModuleState>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val client = remember { DarbakStatusClient(context.applicationContext) }
    val snapshots by client.snapshots.collectAsState()

    DisposableEffect(client) {
        client.start()
        onDispose { client.stop() }
    }

    LaunchedEffect(modules) {
        client.requestAll(modules.filter { it.spec.id in PRIMARY_MODULES })
    }

    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, CarbonCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الحالة المباشرة", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("قراءة خفيفة من تطبيقات دربك الأساسية", color = TextSecondary, fontSize = 11.sp)
                }
                TextButton(onClick = { client.requestAll(modules.filter { it.spec.id in PRIMARY_MODULES }) }) {
                    Text("تحديث", color = CyanNeon)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRIMARY_MODULES.forEach { id ->
                    val module = modules.firstOrNull { it.spec.id == id }
                    LiveModuleStatusCard(
                        id = id,
                        installed = module?.installed == true,
                        snapshot = snapshots[id],
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveModuleStatusCard(
    id: DarbakModuleId,
    installed: Boolean,
    snapshot: DarbakModuleSnapshot?,
    modifier: Modifier = Modifier,
) {
    val health = snapshot?.health
    val tint = when {
        !installed -> TextMuted
        health == DarbakSystemProtocol.HEALTH_READY -> EmeraldSafe
        health == DarbakSystemProtocol.HEALTH_DEGRADED -> AmberRacing
        health == DarbakSystemProtocol.HEALTH_UNAVAILABLE -> HighContrastRed
        else -> TextSecondary
    }
    val title = when (id) {
        DarbakModuleId.VEHICLE_HUB -> "السيارة"
        DarbakModuleId.MAINTENANCE -> "الصيانة"
        DarbakModuleId.MEDIA -> "الوسائط"
        else -> id.name
    }
    val icon: ImageVector = when (id) {
        DarbakModuleId.VEHICLE_HUB -> Icons.Default.DirectionsCar
        DarbakModuleId.MAINTENANCE -> Icons.Default.Build
        DarbakModuleId.MEDIA -> Icons.Default.MusicNote
        else -> Icons.Default.Build
    }
    val primary = when {
        !installed -> "غير مثبت"
        snapshot != null && snapshot.primaryText.isNotBlank() -> snapshot.primaryText
        else -> "الربط غير متاح"
    }
    val secondary = when {
        !installed -> ""
        snapshot != null && snapshot.secondaryText.isNotBlank() -> snapshot.secondaryText
        else -> "ثبّت إصدار Darbak System للتطبيق"
    }
    val metric = snapshot?.takeIf { it.metricValue.isNotBlank() }?.let {
        if (it.metricUnit.isBlank()) it.metricValue else "${it.metricValue} ${it.metricUnit}"
    }

    Surface(
        color = CarbonCard.copy(alpha = .76f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = .35f)),
        modifier = modifier.heightIn(min = 106.dp)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
                Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(7.dp)) {
                    Surface(color = tint, shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxSize()) {}
                }
            }
            Text(primary, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (secondary.isNotBlank()) {
                Text(secondary, color = TextSecondary, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (metric != null) {
                Text(metric, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val PRIMARY_MODULES = listOf(
    DarbakModuleId.VEHICLE_HUB,
    DarbakModuleId.MAINTENANCE,
    DarbakModuleId.MEDIA,
)
