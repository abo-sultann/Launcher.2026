package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetItem
import com.example.model.WidgetSizePreset
import com.example.model.WidgetSurfaceStyle
import com.example.ui.theme.*

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
    val shape = RoundedCornerShape(16.dp)
    val normalBackground = when (widgetItem.surfaceStyle) {
        WidgetSurfaceStyle.TRANSPARENT -> Color.Transparent
        WidgetSurfaceStyle.GLASS -> CarbonDark.copy(alpha = .48f)
        WidgetSurfaceStyle.CARD -> CarbonCard.copy(alpha = .92f)
    }
    val outlineColor = when {
        isDesignMode && isSelected -> CyanNeon
        isDesignMode -> AmberRacing.copy(alpha = .42f)
        widgetItem.showBorder -> CarbonCardBorder.copy(alpha = .85f)
        else -> Color.Transparent
    }
    val outlineWidth = if (isDesignMode && isSelected) 2.dp else if (isDesignMode || widgetItem.showBorder) 1.dp else 0.dp

    Box(
        modifier = modifier
            .alpha(widgetItem.opacity)
            .background(normalBackground, shape)
            .then(if (outlineWidth > 0.dp) Modifier.border(outlineWidth, outlineColor, shape) else Modifier)
    ) {
        content()

        if (isDesignMode) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isSelected) .035f else .015f), shape)
                    .pointerInput(widgetItem.id, widgetItem.isLocked) {
                        if (widgetItem.isLocked) {
                            detectTapGestures(onTap = { onSelect() })
                        } else {
                            detectDragGestures(
                                onDragStart = { onSelect(); onBringToFront() },
                                onDragEnd = onTransformFinished,
                                onDragCancel = onTransformFinished
                            ) { change, dragAmount ->
                                change.consume()
                                onMoveBy(dragAmount.x, dragAmount.y)
                            }
                        }
                    }
                    .pointerInput(widgetItem.id) { detectTapGestures(onTap = { onSelect() }) }
            )

            if (!isSelected) {
                Surface(
                    color = CarbonDark.copy(alpha = .70f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(if (widgetItem.isLocked) Icons.Default.Lock else Icons.Default.TouchApp, null, tint = if (widgetItem.isLocked) HighContrastRed else AmberRacing, modifier = Modifier.size(13.dp))
                    }
                }
            } else {
                WidgetV2Editor(
                    widgetItem = widgetItem,
                    onChangeStyle = onChangeStyle,
                    onOpacityChange = onOpacityChange,
                    onToggleLock = onToggleLock,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onSetSizePreset = onSetSizePreset,
                    onSurfaceChange = onSurfaceChange,
                    onToggleBorder = onToggleBorder,
                    onResizeBy = onResizeBy,
                    onResizeFinished = onTransformFinished
                )
            }
        }
    }
}

@Composable
private fun BoxScope.WidgetV2Editor(
    widgetItem: WidgetItem,
    onChangeStyle: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onSetSizePreset: (WidgetSizePreset) -> Unit,
    onSurfaceChange: (WidgetSurfaceStyle) -> Unit,
    onToggleBorder: () -> Unit,
    onResizeBy: (Float, Float) -> Unit,
    onResizeFinished: () -> Unit
) {
    Surface(
        color = CarbonDark.copy(alpha = .96f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = .45f)),
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditorIcon(Icons.Default.Palette, "التصميم", CyanNeon, onChangeStyle, "btn_change_style_${widgetItem.id}")
            EditorIcon(Icons.Default.ContentCopy, "نسخ الودجت", TextPrimary, onDuplicate)
            EditorIcon(if (widgetItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, if (widgetItem.isLocked) "فتح" else "قفل", if (widgetItem.isLocked) EmeraldSafe else TextPrimary, onToggleLock)
            EditorIcon(Icons.Default.Delete, "حذف", HighContrastRed, onDelete, "btn_delete_widget_${widgetItem.id}")
        }
    }

    Surface(
        color = CarbonDark.copy(alpha = .96f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
    ) {
        Row(Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            WidgetSurfaceStyle.values().forEach { surface ->
                val selected = widgetItem.surfaceStyle == surface
                Surface(
                    onClick = { onSurfaceChange(surface) },
                    color = if (selected) CyanNeon else CarbonSurface,
                    shape = RoundedCornerShape(7.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) CyanNeon else CarbonCardBorder)
                ) {
                    Text(surface.arabicName, color = if (selected) CarbonDark else TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
            }
            Surface(
                onClick = onToggleBorder,
                color = if (widgetItem.showBorder) AmberRacing else CarbonSurface,
                shape = RoundedCornerShape(7.dp)
            ) {
                Text("إطار", color = if (widgetItem.showBorder) CarbonDark else TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
            }
        }
    }

    if (!widgetItem.isLocked) {
        Surface(
            color = CarbonDark.copy(alpha = .96f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        ) {
            Row(Modifier.padding(horizontal = 3.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                WidgetSizePreset.values().forEach { preset ->
                    TextButton(
                        onClick = { onSetSizePreset(preset) },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(preset.arabicName, color = if (preset == WidgetSizePreset.CONTENT) CyanNeon else TextPrimary, fontSize = 8.sp, fontWeight = if (preset == WidgetSizePreset.CONTENT) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        ResizeHandle(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp).width(22.dp).height(58.dp),
            icon = Icons.Default.DragHandle,
            description = "تغيير العرض",
            onDrag = { dx, _ -> onResizeBy(dx, 0f) },
            onFinished = onResizeFinished
        )
        ResizeHandle(
            modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(46.dp),
            icon = Icons.Default.OpenInFull,
            description = "تغيير الحجم",
            onDrag = { dx, dy -> onResizeBy(dx, dy) },
            onFinished = onResizeFinished
        )
    }

    Surface(
        color = CarbonDark.copy(alpha = .96f),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Opacity, null, tint = TextSecondary, modifier = Modifier.padding(start = 5.dp).size(13.dp))
            EditorIcon(Icons.Default.Remove, "شفافية أقل", TextPrimary, { onOpacityChange(widgetItem.opacity - .10f) })
            Text("${(widgetItem.opacity * 100).toInt()}%", color = CyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            EditorIcon(Icons.Default.Add, "شفافية أكثر", TextPrimary, { onOpacityChange(widgetItem.opacity + .10f) })
        }
    }
}

@Composable
private fun EditorIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    tag: String? = null
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp).then(if (tag != null) Modifier.testTag(tag) else Modifier)) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ResizeHandle(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onDrag: (Float, Float) -> Unit,
    onFinished: () -> Unit
) {
    Surface(
        color = CyanNeon.copy(alpha = .96f),
        shape = RoundedCornerShape(9.dp),
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = onFinished,
                onDragCancel = onFinished
            ) { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x, dragAmount.y)
            }
        }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = CarbonDark, modifier = Modifier.size(20.dp))
        }
    }
}
