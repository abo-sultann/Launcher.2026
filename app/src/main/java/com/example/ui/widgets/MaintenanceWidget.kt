package com.example.ui.widgets

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.WidgetStyle
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
fun MaintenanceWidget(style: WidgetStyle, interactionEnabled: Boolean = true) {
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
        when (style) {
            WidgetStyle.MAINTENANCE_GRID -> MaintenanceGrid(displayRows)
            WidgetStyle.MAINTENANCE_ALERTS -> MaintenanceAlerts(displayRows)
            else -> MaintenanceVertical(displayRows)
        }
    }
}

@Composable
private fun MaintenanceVertical(rows: List<MaintenanceRow>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row -> MaintenanceCompactRow(row) }
    }
}

@Composable
private fun MaintenanceGrid(rows: List<MaintenanceRow>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pair.forEach { row ->
                    MaintenanceGridCell(row, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MaintenanceGridCell(row: MaintenanceRow, modifier: Modifier = Modifier) {
    val accent = healthColor(row.health)
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.White.copy(alpha = .035f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .20f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ProgressGlyph(row, Modifier.size(39.dp), 2.6f)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(kindName(row.kind), color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (row.configured) {
                    Text(
                        "${formatNumber(row.remaining)} ${unitName(row)}",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                } else {
                    Text("غير مهيأ", color = TextMuted, fontSize = 8.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MaintenanceAlerts(rows: List<MaintenanceRow>) {
    val important = remember(rows) {
        rows.sortedWith(
            compareBy<MaintenanceRow> { healthRank(it.health) }
                .thenBy { if (it.configured) it.remaining else Long.MAX_VALUE }
        ).take(3)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("الصيانة", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            val dueCount = rows.count { it.health == MaintenanceHealth.DUE }
            val soonCount = rows.count { it.health == MaintenanceHealth.SOON }
            Text(
                when {
                    dueCount > 0 -> "$dueCount مستحق"
                    soonCount > 0 -> "$soonCount قريب"
                    rows.any { it.configured } -> "الحالة جيدة"
                    else -> "بانتظار التهيئة"
                },
                color = when {
                    dueCount > 0 -> Color(0xFFFF5F57)
                    soonCount > 0 -> AmberRacing
                    rows.any { it.configured } -> EmeraldSafe
                    else -> TextMuted
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
        important.forEach { row ->
            MaintenanceAlertRow(row, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MaintenanceAlertRow(row: MaintenanceRow, modifier: Modifier = Modifier) {
    val accent = healthColor(row.health)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MaintenanceGlyph(
            kind = row.kind,
            tint = if (row.configured) TextPrimary else TextMuted,
            modifier = Modifier.size(25.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(kindName(row.kind), color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(statusName(row.health), color = accent, fontSize = 7.sp, maxLines = 1)
        }
        Text(
            if (row.configured) "${formatNumber(row.remaining)} ${unitName(row)}" else "—",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun MaintenanceCompactRow(row: MaintenanceRow) {
    val accent = healthColor(row.health)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProgressGlyph(row, Modifier.size(43.dp), 3f)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            if (row.configured) {
                Text(formatNumber(row.remaining), color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(unitName(row), color = TextSecondary, fontSize = 7.sp, maxLines = 1)
            } else {
                Text("—", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProgressGlyph(row: MaintenanceRow, modifier: Modifier, strokeDp: Float) {
    val accent = healthColor(row.health)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeDp.dp.toPx()
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
        MaintenanceGlyph(
            kind = row.kind,
            tint = if (row.configured) TextPrimary else TextMuted,
            modifier = Modifier.fillMaxSize(.56f)
        )
    }
}

/**
 * Purpose-built automotive glyphs. They are drawn with primitives instead of depending on
 * generic phone UI icons, which keeps the launcher lightweight and gives the maintenance widget
 * a consistent instrument-cluster visual language on API 25.
 */
@Composable
private fun MaintenanceGlyph(kind: MaintenanceKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = (size.minDimension * .075f).coerceAtLeast(1.4f)
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thin = Stroke(width = sw * .72f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val center = Offset(w * .5f, h * .5f)

        when (kind) {
            MaintenanceKind.ENGINE_OIL -> {
                val body = Path().apply {
                    moveTo(w * .18f, h * .42f)
                    lineTo(w * .58f, h * .42f)
                    lineTo(w * .68f, h * .68f)
                    lineTo(w * .22f, h * .68f)
                    close()
                }
                drawPath(body, tint, style = stroke)
                drawLine(tint, Offset(w * .58f, h * .46f), Offset(w * .80f, h * .34f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * .80f, h * .34f), Offset(w * .90f, h * .39f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * .25f, h * .42f), Offset(w * .25f, h * .28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * .25f, h * .28f), Offset(w * .48f, h * .28f), sw, StrokeCap.Round)
                drawCircle(tint, radius = w * .045f, center = Offset(w * .84f, h * .62f))
            }

            MaintenanceKind.TRANSMISSION_OIL -> {
                val outer = size.minDimension * .31f
                val inner = outer * .46f
                drawCircle(tint, radius = outer, center = center, style = stroke)
                drawCircle(tint, radius = inner, center = center, style = stroke)
                repeat(8) { index ->
                    val a = index * PI / 4.0
                    val start = Offset(
                        center.x + (outer + sw * .20f) * cos(a).toFloat(),
                        center.y + (outer + sw * .20f) * sin(a).toFloat()
                    )
                    val end = Offset(
                        center.x + (outer + sw * 1.25f) * cos(a).toFloat(),
                        center.y + (outer + sw * 1.25f) * sin(a).toFloat()
                    )
                    drawLine(tint, start, end, sw, StrokeCap.Round)
                }
                drawCircle(tint, radius = w * .045f, center = Offset(w * .78f, h * .76f))
            }

            MaintenanceKind.DIESEL_FILTER -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .28f, h * .22f),
                    size = Size(w * .44f, h * .56f),
                    cornerRadius = CornerRadius(w * .08f, w * .08f),
                    style = stroke
                )
                drawLine(tint, Offset(w * .22f, h * .22f), Offset(w * .78f, h * .22f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * .34f, h * .14f), Offset(w * .66f, h * .14f), sw, StrokeCap.Round)
                listOf(.39f, .50f, .61f).forEach { x ->
                    drawLine(tint, Offset(w * x, h * .34f), Offset(w * x, h * .66f), sw * .55f, StrokeCap.Round)
                }
                drawCircle(tint, radius = w * .038f, center = Offset(w * .82f, h * .68f))
            }

            MaintenanceKind.BATTERY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * .15f, h * .31f),
                    size = Size(w * .70f, h * .48f),
                    cornerRadius = CornerRadius(w * .07f, w * .07f),
                    style = stroke
                )
                drawLine(tint, Offset(w * .27f, h * .23f), Offset(w * .37f, h * .23f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * .63f, h * .23f), Offset(w * .73f, h * .23f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * .29f, h * .50f), Offset(w * .43f, h * .50f), sw * .72f, StrokeCap.Round)
                drawLine(tint, Offset(w * .36f, h * .43f), Offset(w * .36f, h * .57f), sw * .72f, StrokeCap.Round)
                drawLine(tint, Offset(w * .59f, h * .50f), Offset(w * .73f, h * .50f), sw * .72f, StrokeCap.Round)
            }

            MaintenanceKind.TIRES -> {
                val r = size.minDimension * .34f
                drawCircle(tint, radius = r, center = center, style = stroke)
                drawCircle(tint, radius = r * .48f, center = center, style = thin)
                drawCircle(tint, radius = r * .12f, center = center)
                repeat(5) { index ->
                    val a = -PI / 2.0 + index * 2.0 * PI / 5.0
                    val end = Offset(
                        center.x + r * .45f * cos(a).toFloat(),
                        center.y + r * .45f * sin(a).toFloat()
                    )
                    drawLine(tint, center, end, sw * .62f, StrokeCap.Round)
                }
            }

            MaintenanceKind.BRAKES -> {
                val r = size.minDimension * .31f
                drawCircle(tint, radius = r, center = Offset(w * .45f, h * .52f), style = stroke)
                drawCircle(tint, radius = r * .38f, center = Offset(w * .45f, h * .52f), style = thin)
                repeat(6) { index ->
                    val a = index * PI / 3.0
                    drawCircle(
                        tint,
                        radius = sw * .34f,
                        center = Offset(
                            w * .45f + r * .68f * cos(a).toFloat(),
                            h * .52f + r * .68f * sin(a).toFloat()
                        )
                    )
                }
                drawArc(
                    color = tint,
                    startAngle = -72f,
                    sweepAngle = 144f,
                    useCenter = false,
                    topLeft = Offset(w * .39f, h * .19f),
                    size = Size(w * .48f, h * .66f),
                    style = Stroke(sw * 1.7f, cap = StrokeCap.Round)
                )
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

private fun healthColor(health: MaintenanceHealth): Color = when (health) {
    MaintenanceHealth.GOOD -> EmeraldSafe
    MaintenanceHealth.SOON -> AmberRacing
    MaintenanceHealth.DUE -> Color(0xFFFF5F57)
    MaintenanceHealth.UNCONFIGURED -> TextMuted
}

private fun healthRank(health: MaintenanceHealth): Int = when (health) {
    MaintenanceHealth.DUE -> 0
    MaintenanceHealth.SOON -> 1
    MaintenanceHealth.GOOD -> 2
    MaintenanceHealth.UNCONFIGURED -> 3
}

private fun kindName(kind: MaintenanceKind): String = when (kind) {
    MaintenanceKind.ENGINE_OIL -> "زيت المحرك"
    MaintenanceKind.TRANSMISSION_OIL -> "زيت القير"
    MaintenanceKind.DIESEL_FILTER -> "فلتر الديزل"
    MaintenanceKind.BATTERY -> "البطارية"
    MaintenanceKind.TIRES -> "الكفرات"
    MaintenanceKind.BRAKES -> "الفحمات"
}

private fun statusName(health: MaintenanceHealth): String = when (health) {
    MaintenanceHealth.DUE -> "حان التغيير"
    MaintenanceHealth.SOON -> "اقترب الموعد"
    MaintenanceHealth.GOOD -> "جيد"
    MaintenanceHealth.UNCONFIGURED -> "غير مهيأ"
}

private fun unitName(row: MaintenanceRow): String = if (row.unit == "MONTHS") "شهر" else "كم"
private fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(value)
