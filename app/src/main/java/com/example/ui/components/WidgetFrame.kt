package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetItem
import com.example.ui.theme.*

private data class WidgetPreset(val label: String, val width: Float, val height: Float)

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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = when {
        isDesignMode && isSelected -> CyanNeon
        isDesignMode -> AmberRacing.copy(alpha = 0.70f)
        else -> CarbonCardBorder
    }
    val borderWidth = if (isDesignMode && isSelected) 2.dp else 1.dp
    var measured by remember { mutableStateOf(Size.Zero) }

    Card(
        modifier = modifier
            .onSizeChanged { measured = Size(it.width.toFloat(), it.height.toFloat()) }
            .alpha(widgetItem.opacity)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = 0.90f)),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Box(Modifier.fillMaxSize()) {
            content()

            if (isDesignMode) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isSelected) .08f else .03f)))

                // Entire widget becomes the drag/select target. Control buttons are drawn after
                // this layer and therefore remain directly tappable.
                Box(
                    Modifier
                        .fillMaxSize()
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
                        color = CarbonDark.copy(alpha = .82f),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (widgetItem.isLocked) Icons.Default.Lock else Icons.Default.TouchApp, null, tint = if (widgetItem.isLocked) HighContrastRed else AmberRacing, modifier = Modifier.size(15.dp))
                            Text(if (widgetItem.isLocked) "مقفل" else "المس للتعديل", color = if (widgetItem.isLocked) HighContrastRed else AmberRacing, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isSelected) {
                    EditorToolbar(
                        widgetItem = widgetItem,
                        measured = measured,
                        onChangeStyle = onChangeStyle,
                        onResizeBy = onResizeBy,
                        onResizeFinished = onTransformFinished,
                        onOpacityChange = onOpacityChange,
                        onToggleLock = onToggleLock,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.EditorToolbar(
    widgetItem: WidgetItem,
    measured: Size,
    onChangeStyle: () -> Unit,
    onResizeBy: (Float, Float) -> Unit,
    onResizeFinished: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .background(CarbonDark.copy(alpha = .97f), RoundedCornerShape(9.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onChangeStyle, modifier = Modifier.size(34.dp).testTag("btn_change_style_${widgetItem.id}")) {
            Icon(Icons.Default.Palette, "التصميم", tint = CyanNeon, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onToggleLock, modifier = Modifier.size(34.dp)) {
            Icon(if (widgetItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, if (widgetItem.isLocked) "فتح" else "قفل", tint = if (widgetItem.isLocked) EmeraldSafe else TextPrimary, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp).testTag("btn_delete_widget_${widgetItem.id}")) {
            Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed, modifier = Modifier.size(18.dp))
        }
    }

    if (!widgetItem.isLocked) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(5.dp)
                .background(CarbonDark.copy(alpha = .97f), RoundedCornerShape(9.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                EditorMiniButton(Icons.Default.Remove, "أصغر") { onResizeBy(-36f, -26f); onResizeFinished() }
                Text("الحجم", color = CyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp))
                EditorMiniButton(Icons.Default.Add, "أكبر") { onResizeBy(36f, 26f); onResizeFinished() }
                EditorMiniButton(Icons.Default.SwapHoriz, "أعرض") { onResizeBy(48f, 0f); onResizeFinished() }
                EditorMiniButton(Icons.Default.SwapVert, "أطول") { onResizeBy(0f, 36f); onResizeFinished() }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    WidgetPreset("صغير", .20f, .23f),
                    WidgetPreset("وسط", .32f, .32f),
                    WidgetPreset("كبير", .44f, .44f),
                    WidgetPreset("عريض", .58f, .30f)
                ).forEach { preset ->
                    TextButton(
                        onClick = {
                            if (measured.width > 0f && measured.height > 0f && widgetItem.widthFraction > 0f && widgetItem.heightFraction > 0f) {
                                val canvasW = measured.width / widgetItem.widthFraction
                                val canvasH = measured.height / widgetItem.heightFraction
                                onResizeBy((preset.width - widgetItem.widthFraction) * canvasW, (preset.height - widgetItem.heightFraction) * canvasH)
                                onResizeFinished()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                        modifier = Modifier.height(25.dp)
                    ) { Text(preset.label, fontSize = 8.sp, color = TextPrimary) }
                }
            }
        }

        Surface(
            color = CyanNeon.copy(alpha = .96f),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .size(42.dp)
                .pointerInput(widgetItem.id) {
                    detectDragGestures(
                        onDragEnd = onResizeFinished,
                        onDragCancel = onResizeFinished
                    ) { change, dragAmount ->
                        change.consume()
                        onResizeBy(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.OpenInFull, "تغيير الحجم", tint = CarbonDark, modifier = Modifier.size(22.dp))
            }
        }
    }

    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(5.dp)
            .background(CarbonDark.copy(alpha = .97f), RoundedCornerShape(9.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Opacity, null, tint = TextSecondary, modifier = Modifier.padding(start = 5.dp).size(14.dp))
        EditorMiniButton(Icons.Default.Remove, "شفافية أقل") { onOpacityChange(widgetItem.opacity - .10f) }
        Text("${(widgetItem.opacity * 100).toInt()}%", color = CyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        EditorMiniButton(Icons.Default.Add, "شفافية أكثر") { onOpacityChange(widgetItem.opacity + .10f) }
    }
}

@Composable
private fun EditorMiniButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(27.dp)) {
        Icon(icon, description, tint = TextPrimary, modifier = Modifier.size(15.dp))
    }
}
