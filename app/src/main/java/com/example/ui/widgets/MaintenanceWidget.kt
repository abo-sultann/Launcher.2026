package com.example.ui.widgets

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

private const val MAINTENANCE_PACKAGE = "com.abosultan.darbakmaintenance"
private val MAINTENANCE_URI: Uri = Uri.parse("content://com.abosultan.darbakmaintenance.status/status")

private enum class MaintenanceKind { ENGINE_OIL, TRANSMISSION_OIL, DIESEL_FILTER, BATTERY, TIRES, BRAKES }
private enum class MaintenanceHealth { UNCONFIGURED, GOOD, SOON, DUE }

private data class MaintenanceRow(
    val kind: MaintenanceKind,
    val configured: Boolean,
    val remaining: Long,
    val unit: String,
    val progress: Float,
    val health: MaintenanceHealth
)

@Composable
fun MaintenanceWidget(interactionEnabled: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf(emptyList<MaintenanceRow>()) }

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { readMaintenance(context) }
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = reload()
            override fun onChange(selfChange: Boolean, uri: Uri?) = reload()
        }
        runCatching { context.contentResolver.registerContentObserver(MAINTENANCE_URI, true, observer) }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reload()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        reload()
        onDispose {
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val displayRows = remember(rows) {
        if (rows.isNotEmpty()) rows else MaintenanceKind.values().map {
            MaintenanceRow(it, false, 0L, if (it == MaintenanceKind.BATTERY) "MONTHS" else "KM", 0f, MaintenanceHealth.UNCONFIGURED)
        }
    }

    Surface(
        color = CarbonDark.copy(alpha = .64f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (interactionEnabled) Modifier.clickable {
                    context.packageManager.getLaunchIntentForPackage(MAINTENANCE_PACKAGE)?.let(context::startActivity)
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            displayRows.forEach { row -> MaintenanceCompactRow(row) }
        }
    }
}

@Composable
private fun MaintenanceCompactRow(row: MaintenanceRow) {
    val accent = when (row.health) {
        MaintenanceHealth.GOOD -> EmeraldSafe
        MaintenanceHealth.SOON -> AmberRacing
        MaintenanceHealth.DUE -> Color(0xFFFF5F57)
        MaintenanceHealth.UNCONFIGURED -> TextMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(43.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = .10f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                if (row.configured) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * row.progress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Icon(iconFor(row.kind), null, tint = if (row.configured) TextPrimary else TextMuted, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            if (row.configured) {
                Text(formatNumber(row.remaining), color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(if (row.unit == "MONTHS") "شهر" else "كم", color = TextSecondary, fontSize = 7.sp, maxLines = 1)
            } else {
                Text("—", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun readMaintenance(context: android.content.Context): List<MaintenanceRow> {
    return runCatching {
        val cursor = context.contentResolver.query(MAINTENANCE_URI, null, null, null, null) ?: return@runCatching emptyList()
        cursor.use {
            val typeIndex = it.getColumnIndexOrThrow("type")
            val configuredIndex = it.getColumnIndexOrThrow("configured")
            val remainingIndex = it.getColumnIndexOrThrow("remaining_value")
            val unitIndex = it.getColumnIndexOrThrow("unit")
            val progressIndex = it.getColumnIndexOrThrow("progress")
            val statusIndex = it.getColumnIndexOrThrow("status")
            buildList {
                while (it.moveToNext()) {
                    val kind = runCatching { MaintenanceKind.valueOf(it.getString(typeIndex)) }.getOrNull() ?: continue
                    val health = runCatching { MaintenanceHealth.valueOf(it.getString(statusIndex)) }.getOrDefault(MaintenanceHealth.UNCONFIGURED)
                    add(
                        MaintenanceRow(
                            kind = kind,
                            configured = it.getInt(configuredIndex) == 1,
                            remaining = it.getLong(remainingIndex),
                            unit = it.getString(unitIndex),
                            progress = it.getFloat(progressIndex).coerceIn(0f, 1f),
                            health = health
                        )
                    )
                }
            }.sortedBy { it.kind.ordinal }
        }
    }.getOrDefault(emptyList())
}

private fun iconFor(kind: MaintenanceKind): ImageVector = when (kind) {
    MaintenanceKind.ENGINE_OIL -> Icons.Default.Opacity
    MaintenanceKind.TRANSMISSION_OIL -> Icons.Default.Settings
    MaintenanceKind.DIESEL_FILTER -> Icons.Default.FilterAlt
    MaintenanceKind.BATTERY -> Icons.Default.BatteryFull
    MaintenanceKind.TIRES -> Icons.Default.TireRepair
    MaintenanceKind.BRAKES -> Icons.Default.DiscFull
}

private fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)
