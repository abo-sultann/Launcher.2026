package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.WidgetVisualStore
import com.example.model.WidgetItem
import com.example.model.WidgetSizePreset
import com.example.model.WidgetSurfaceStyle
import com.example.model.WidgetType
import com.example.ui.theme.*

/**
 * Widget V3 keeps the widget itself clean. Editing controls live in a fixed popup dock,
 * so even a tiny clock can be selected, moved, recolored or deleted without covering it.
 */
@Composable
fun WidgetFrame(
    widgetItem: WidgetItem,
    isDesignMode: Boolean,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onChangeStyle: () -> Unit,
    onMoveBy: (Float, Float) -> Unit,
    onResizeBy: (Float, Float) -> Unit,
    onTransformFinished: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onBringToFront: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onSetSizePreset: (WidgetSizePreset) -> Unit,
    onSurfaceChange: (WidgetSurfaceStyle) -> Unit,
    onToggleBorder: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val visualStore = remember { WidgetVisualStore(context.applicationContext) }
    var appearanceVersion by remember { mutableIntStateOf(0) }
    val foregroundArgb = remember(widgetItem.id, appearanceVersion) { visualStore.getForegroundColorArgb(widgetItem.id) }
    val accentArgb = remember(widgetItem.id, appearanceVersion) { visualStore.getAccentColorArgb(widgetItem.id) }
    val surfaceOpacity = remember(widgetItem.id, appearanceVersion) { visualStore.getSurfaceOpacity(widgetItem.id) }
    val foregroundColor = foregroundArgb?.let { Color(it) }
    val accentColor = accentArgb?.let { Color(it) }

    // A widget type keeps a recognizable silhouette even when it uses the same color palette.
    val shape = when (widgetItem.type) {
        WidgetType.CLOCK -> RoundedCornerShape(28.dp)
        WidgetType.SPEEDOMETER -> RoundedCornerShape(8.dp)
        WidgetType.DATE -> RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)
        WidgetType.GPS -> RoundedCornerShape(10.dp)
        WidgetType.MUSIC -> RoundedCornerShape(24.dp)
        WidgetType.MAP -> RoundedCornerShape(18.dp)
        WidgetType.TRIP -> RoundedCornerShape(12.dp)
        WidgetType.APPS -> RoundedCornerShape(22.dp)
        WidgetType.CONTROLS -> RoundedCornerShape(20.dp)
    }
    val normalBackground = when (widgetItem.surfaceStyle) {
        WidgetSurfaceStyle.TRANSPARENT -> Color.Transparent
        WidgetSurfaceStyle.GLASS -> CarbonDark.copy(alpha = (.18f + .46f * surfaceOpacity).coerceAtMost(.72f))
        WidgetSurfaceStyle.CARD -> CarbonCard.copy(alpha = (.55f + .40f * surfaceOpacity).coerceAtMost(.97f))
    }
    val outlineColor = when {
        isDesignMode && isSelected -> CyanNeon
        isDesignMode -> AmberRacing.copy(alpha = .18f)
        widgetItem.showBorder -> CarbonCardBorder.copy(alpha = .85f)
        else -> Color.Transparent
    }
    val outlineWidth = if (isDesignMode && isSelected) 2.dp else if (isDesignMode || widgetItem.showBorder) 1.dp else 0.dp

    Box(
        modifier = modifier
            .alpha(widgetItem.opacity)
            .background(normalBackground, shape)
            .then(if (outlineWidth > 0.dp) Modifier.border(outlineWidth, outlineColor, shape) else Modifier)
            .then(if (isDesignMode) Modifier.clickable { onSelect() } else Modifier)
            .then(
                if (isDesignMode && !widgetItem.isLocked) {
                    Modifier.pointerInput(widgetItem.id, widgetItem.isLocked) {
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                                onSelect()
                                onBringToFront()
                            },
                            onDragEnd = onTransformFinished,
                            onDragCancel = onTransformFinished
                        ) { change, dragAmount ->
                            change.consume()
                            // Home geometry is based on the position at drag start, therefore send
                            // the cumulative gesture delta rather than one tiny event delta.
                            totalX += dragAmount.x
                            totalY += dragAmount.y
                            onMoveBy(totalX, totalY)
                        }
                    }
                } else Modifier
            )
    ) {
        CompositionLocalProvider(
            LocalWidgetVisualTokens provides WidgetVisualTokens(foregroundColor, accentColor),
            LocalWidgetForegroundColor provides foregroundColor
        ) {
            content()
        }

        if (isDesignMode && isSelected) {
            Surface(
                color = CyanNeon,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(20.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (widgetItem.isLocked) Icons.Default.Lock else Icons.Default.OpenWith,
                        null,
                        tint = CarbonDark,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }

    if (isDesignMode && isSelected) {
        val density = LocalDensity.current
        val yOffset = with(density) { (-92).dp.roundToPx() }
        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, yOffset),
            properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            WidgetV3ControlDock(
                widgetItem = widgetItem,
                foregroundArgb = foregroundArgb,
                accentArgb = accentArgb,
                surfaceOpacity = surfaceOpacity,
                onForeground = { argb ->
                    visualStore.setForegroundColorArgb(widgetItem.id, argb)
                    appearanceVersion++
                },
                onAccent = { argb ->
                    visualStore.setAccentColorArgb(widgetItem.id, argb)
                    appearanceVersion++
                },
                onSurfaceOpacity = { opacity ->
                    visualStore.setSurfaceOpacity(widgetItem.id, opacity)
                    appearanceVersion++
                },
                onChangeStyle = onChangeStyle,
                onOpacityChange = onOpacityChange,
                onToggleLock = onToggleLock,
                onDelete = onDelete,
                onDuplicate = onDuplicate,
                onSetSizePreset = onSetSizePreset,
                onSurfaceChange = onSurfaceChange,
                onToggleBorder = onToggleBorder,
                onNudge = { dx, dy ->
                    onMoveBy(dx, dy)
                    onTransformFinished()
                },
                onResizeStep = { dw, dh ->
                    onResizeBy(dw, dh)
                    onTransformFinished()
                }
            )
        }
    }
}

@Composable
private fun WidgetV3ControlDock(
    widgetItem: WidgetItem,
    foregroundArgb: Int?,
    accentArgb: Int?,
    surfaceOpacity: Float,
    onForeground: (Int?) -> Unit,
    onAccent: (Int?) -> Unit,
    onSurfaceOpacity: (Float) -> Unit,
    onChangeStyle: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onSetSizePreset: (WidgetSizePreset) -> Unit,
    onSurfaceChange: (WidgetSurfaceStyle) -> Unit,
    onToggleBorder: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onResizeStep: (Float, Float) -> Unit
) {
    Surface(
        color = CarbonDark.copy(alpha = .97f),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = .55f)),
        shadowElevation = 8.dp,
        modifier = Modifier.widthIn(max = 980.dp).padding(horizontal = 12.dp)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(widgetItem.type.arabicTitle, color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.widthIn(min = 55.dp))

                CompactEditorButton(Icons.Default.Palette, "الشكل", CyanNeon, onChangeStyle, "btn_change_style_${widgetItem.id}")

                WidgetSurfaceStyle.values().forEach { surface ->
                    val selected = widgetItem.surfaceStyle == surface
                    Surface(
                        onClick = { onSurfaceChange(surface) },
                        color = if (selected) CyanNeon else CarbonSurface,
                        shape = RoundedCornerShape(7.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) CyanNeon else CarbonCardBorder)
                    ) {
                        Text(
                            surface.arabicName,
                            color = if (selected) CarbonDark else TextPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        )
                    }
                }

                CompactEditorButton(Icons.Default.BorderStyle, "الإطار", if (widgetItem.showBorder) AmberRacing else TextSecondary, onToggleBorder)
                CompactEditorButton(if (widgetItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, if (widgetItem.isLocked) "فتح" else "قفل", TextPrimary, onToggleLock)
                CompactEditorButton(Icons.Default.ContentCopy, "نسخ", TextPrimary, onDuplicate)
                Spacer(Modifier.weight(1f))
                CompactEditorButton(Icons.Default.Delete, "حذف", HighContrastRed, onDelete, "btn_delete_widget_${widgetItem.id}")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WidgetSizePreset.values().forEach { preset ->
                    TextButton(
                        onClick = { onSetSizePreset(preset) },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        modifier = Modifier.height(27.dp)
                    ) {
                        Text(preset.arabicName, color = if (preset == WidgetSizePreset.CONTENT) CyanNeon else TextPrimary, fontSize = 8.sp)
                    }
                }

                VerticalDivider(Modifier.height(22.dp), color = CarbonCardBorder)
                CompactEditorButton(Icons.Default.KeyboardArrowLeft, "يسار", TextPrimary, { onNudge(-18f, 0f) })
                CompactEditorButton(Icons.Default.KeyboardArrowRight, "يمين", TextPrimary, { onNudge(18f, 0f) })
                CompactEditorButton(Icons.Default.KeyboardArrowUp, "أعلى", TextPrimary, { onNudge(0f, -18f) })
                CompactEditorButton(Icons.Default.KeyboardArrowDown, "أسفل", TextPrimary, { onNudge(0f, 18f) })
                CompactEditorButton(Icons.Default.ZoomOut, "تصغير", TextSecondary, { onResizeStep(-24f, -15f) })
                CompactEditorButton(Icons.Default.ZoomIn, "تكبير", CyanNeon, { onResizeStep(24f, 15f) })

                Spacer(Modifier.weight(1f))
                CompactEditorButton(Icons.Default.Remove, "شفافية أقل", TextPrimary, { onOpacityChange(widgetItem.opacity - .10f) })
                Text("${(widgetItem.opacity * 100).toInt()}%", color = CyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CompactEditorButton(Icons.Default.Add, "شفافية أكثر", TextPrimary, { onOpacityChange(widgetItem.opacity + .10f) })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("النص", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                ColorChoice(null, foregroundArgb == null, onForeground)
                FOREGROUND_PRESETS.forEach { argb -> ColorChoice(argb, foregroundArgb == argb, onForeground) }

                VerticalDivider(Modifier.height(22.dp), color = CarbonCardBorder)
                Text("اللون المميز", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                ColorChoice(null, accentArgb == null, onAccent)
                ACCENT_PRESETS.forEach { argb -> ColorChoice(argb, accentArgb == argb, onAccent) }

                Spacer(Modifier.weight(1f))
                Text("خلفية", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CompactEditorButton(Icons.Default.Remove, "خلفية أخف", TextPrimary, { onSurfaceOpacity(surfaceOpacity - .10f) })
                Text("${(surfaceOpacity * 100).toInt()}%", color = CyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CompactEditorButton(Icons.Default.Add, "خلفية أوضح", TextPrimary, { onSurfaceOpacity(surfaceOpacity + .10f) })
            }
        }
    }
}

@Composable
private fun CompactEditorButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    tag: String? = null
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(29.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier)
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ColorChoice(argb: Int?, selected: Boolean, onSelect: (Int?) -> Unit) {
    val color = argb?.let { Color(it) } ?: Color.Transparent
    Surface(
        onClick = { onSelect(argb) },
        color = if (argb == null) CarbonSurface else color,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CyanNeon else CarbonCardBorder),
        modifier = Modifier.size(21.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (argb == null) Text("A", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            else if (selected) Icon(Icons.Default.Check, null, tint = if (argb == BLACK_ARGB) Color.White else Color.Black, modifier = Modifier.size(12.dp))
        }
    }
}

private const val WHITE_ARGB: Int = -1
private const val BLACK_ARGB: Int = -15724528 // 0xFF101010
private val FOREGROUND_PRESETS = listOf(
    WHITE_ARGB,
    BLACK_ARGB,
    0xFFB7C0CC.toInt(),
    0xFF59E6F2.toInt(),
    0xFFFFD166.toInt(),
    0xFFFF6B6B.toInt()
)

private val ACCENT_PRESETS = listOf(
    0xFF00E5FF.toInt(),
    0xFFFFB84D.toInt(),
    0xFF37E6A1.toInt(),
    0xFFB892FF.toInt(),
    0xFFFF5C75.toInt()
)
