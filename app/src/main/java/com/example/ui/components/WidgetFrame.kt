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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.model.WidgetItem
import com.example.model.WidgetSizePreset
import com.example.model.WidgetSurfaceStyle
import com.example.model.WidgetTone
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
    onToneChange: (WidgetTone) -> Unit,
    onSurfaceOpacityChange: (Float) -> Unit,
    onResetWidget: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tone = WidgetTone.fromArgb(widgetItem.foregroundColorArgb)
    val surfaceOpacity = widgetItem.surfaceOpacity
    val foregroundColor = Color(tone.argb)
    val accentColor = Color(tone.argb)

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
        WidgetType.MAINTENANCE -> RoundedCornerShape(18.dp)
    }
    val normalBackground = when (widgetItem.surfaceStyle) {
        WidgetSurfaceStyle.TRANSPARENT -> Color.Transparent
        WidgetSurfaceStyle.GLASS -> if (tone == WidgetTone.BLACK) {
            Color.White.copy(alpha = (.26f + .46f * surfaceOpacity).coerceAtMost(.74f))
        } else {
            Color.Black.copy(alpha = (.20f + .48f * surfaceOpacity).coerceAtMost(.76f))
        }
        WidgetSurfaceStyle.CARD -> if (tone == WidgetTone.BLACK) {
            Color.White.copy(alpha = (.66f + .32f * surfaceOpacity).coerceAtMost(.98f))
        } else {
            Color.Black.copy(alpha = (.64f + .33f * surfaceOpacity).coerceAtMost(.97f))
        }
    }
    val outlineColor = when {
        isDesignMode && isSelected -> CyanNeon
        isDesignMode -> AmberRacing.copy(alpha = .18f)
        widgetItem.showBorder -> foregroundColor.copy(alpha = .75f)
        else -> Color.Transparent
    }
    val outlineWidth = if (isDesignMode && isSelected) 2.dp else if (isDesignMode || widgetItem.showBorder) 1.dp else 0.dp

    Box(
        modifier = modifier
            .alpha(widgetItem.opacity)
            .background(normalBackground, shape)
            .then(if (outlineWidth > 0.dp) Modifier.border(outlineWidth, outlineColor, shape) else Modifier)
    ) {
        CompositionLocalProvider(
            LocalWidgetVisualTokens provides WidgetVisualTokens(foregroundColor, accentColor, normalBackground.takeIf { it != Color.Transparent }),
            LocalWidgetForegroundColor provides foregroundColor
        ) {
            content()
        }

        // A transparent editor layer sits above widget content. This is essential for map and
        // app widgets whose own click targets otherwise intercept the first touch.
        if (isDesignMode) {
            Box(
                Modifier
                    .matchParentSize()
                    .clickable { onSelect() }
                    .then(
                        if (!widgetItem.isLocked) {
                            Modifier.pointerInput(widgetItem.id, widgetItem.isLocked) {
                                detectDragGestures(
                                    onDragStart = {
                                        onSelect()
                                        onBringToFront()
                                    },
                                    onDragEnd = onTransformFinished,
                                    onDragCancel = onTransformFinished
                                ) { change, dragAmount ->
                                    change.consume()
                                    onMoveBy(dragAmount.x, dragAmount.y)
                                }
                            }
                        } else Modifier
                    )
            )
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
                tone = tone,
                surfaceOpacity = surfaceOpacity,
                onTone = onToneChange,
                onSurfaceOpacity = onSurfaceOpacityChange,
                onChangeStyle = onChangeStyle,
                onOpacityChange = onOpacityChange,
                onToggleLock = onToggleLock,
                onDelete = onDelete,
                onDuplicate = onDuplicate,
                onSetSizePreset = onSetSizePreset,
                onSurfaceChange = onSurfaceChange,
                onToggleBorder = onToggleBorder,
                onResetWidget = onResetWidget,
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
    tone: WidgetTone,
    surfaceOpacity: Float,
    onTone: (WidgetTone) -> Unit,
    onSurfaceOpacity: (Float) -> Unit,
    onChangeStyle: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onSetSizePreset: (WidgetSizePreset) -> Unit,
    onSurfaceChange: (WidgetSurfaceStyle) -> Unit,
    onToggleBorder: () -> Unit,
    onResetWidget: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onResizeStep: (Float, Float) -> Unit
) {
    var placementSection by remember(widgetItem.id) { mutableStateOf(false) }
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
                Text(widgetItem.type.arabicTitle, color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.widthIn(min = 62.dp))
                FilterChip(selected = !placementSection, onClick = { placementSection = false }, label = { Text("المظهر", fontSize = 9.sp) }, leadingIcon = { Icon(Icons.Default.Palette, null, Modifier.size(14.dp)) })
                FilterChip(selected = placementSection, onClick = { placementSection = true }, label = { Text("المكان والمقاس", fontSize = 9.sp) }, leadingIcon = { Icon(Icons.Default.OpenWith, null, Modifier.size(14.dp)) })
                Spacer(Modifier.weight(1f))
                CompactEditorButton(if (widgetItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, if (widgetItem.isLocked) "فتح" else "قفل", TextPrimary, onToggleLock)
                CompactEditorButton(Icons.Default.ContentCopy, "نسخ", TextPrimary, onDuplicate)
                CompactEditorButton(Icons.Default.RestartAlt, "إعادة هذا الودجت", AmberRacing, onResetWidget)
                CompactEditorButton(Icons.Default.Delete, "حذف", HighContrastRed, onDelete, "btn_delete_widget_${widgetItem.id}")
            }

            if (placementSection) Row(
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

            }

            if (!placementSection) Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CompactEditorButton(Icons.Default.Palette, "الشكل", CyanNeon, onChangeStyle, "btn_change_style_${widgetItem.id}")
                WidgetSurfaceStyle.values().forEach { surface ->
                    val selected = widgetItem.surfaceStyle == surface
                    FilterChip(selected = selected, onClick = { onSurfaceChange(surface) }, label = { Text(surface.arabicName, fontSize = 8.sp) })
                }
                CompactEditorButton(Icons.Default.BorderStyle, "الإطار", if (widgetItem.showBorder) AmberRacing else TextSecondary, onToggleBorder)
                VerticalDivider(Modifier.height(22.dp), color = CarbonCardBorder)
                Text("اللون", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                WidgetTone.values().forEach { option ->
                    ToneChoice(option, tone == option) { onTone(option) }
                }

                Spacer(Modifier.weight(1f))
                Text("خلفية", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CompactEditorButton(Icons.Default.Remove, "خلفية أخف", TextPrimary, { onSurfaceOpacity(surfaceOpacity - .10f) })
                Text("${(surfaceOpacity * 100).toInt()}%", color = CyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CompactEditorButton(Icons.Default.Add, "خلفية أوضح", TextPrimary, { onSurfaceOpacity(surfaceOpacity + .10f) })
                VerticalDivider(Modifier.height(22.dp), color = CarbonCardBorder)
                CompactEditorButton(Icons.Default.Remove, "شفافية الودجت أقل", TextPrimary, { onOpacityChange(widgetItem.opacity - .10f) })
                Text("${(widgetItem.opacity * 100).toInt()}%", color = CyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                CompactEditorButton(Icons.Default.Add, "شفافية الودجت أكثر", TextPrimary, { onOpacityChange(widgetItem.opacity + .10f) })
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
private fun ToneChoice(tone: WidgetTone, selected: Boolean, onSelect: () -> Unit) {
    val color = Color(tone.argb)
    Surface(
        onClick = onSelect,
        color = color,
        shape = RoundedCornerShape(7.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CyanNeon else CarbonCardBorder),
        modifier = Modifier.size(width = 49.dp, height = 24.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (selected) Icon(Icons.Default.Check, null, tint = if (tone == WidgetTone.BLACK) Color.White else Color.Black, modifier = Modifier.size(11.dp))
                Text(tone.arabicName, color = if (tone == WidgetTone.BLACK) Color.White else Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
