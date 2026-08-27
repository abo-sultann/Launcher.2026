package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*

@Composable
fun WidgetFrame(
    widgetItem: WidgetItem,
    isDesignMode: Boolean,
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
    val borderColor = if (isDesignMode) AmberRacing.copy(alpha = 0.9f) else CarbonCardBorder
    val borderWidth = if (isDesignMode) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .alpha(widgetItem.opacity)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonCard.copy(alpha = 0.90f)),
        border = BorderStroke(0.dp, Color.Transparent)
    ) {
        Box(Modifier.fillMaxSize()) {
            content()

            if (isDesignMode) {
                // Thin editing veil makes handles visible without covering the widget.
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.16f)))

                Surface(
                    color = CarbonDark.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, if (widgetItem.isLocked) HighContrastRed else AmberRacing),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .then(
                            if (!widgetItem.isLocked) {
                                Modifier.pointerInput(widgetItem.id) {
                                    detectDragGestures(
                                        onDragStart = { onBringToFront() },
                                        onDragEnd = onTransformFinished,
                                        onDragCancel = onTransformFinished
                                    ) { _, dragAmount -> onMoveBy(dragAmount.x, dragAmount.y) }
                                }
                            } else Modifier
                        )
                ) {
                    Row(
                        Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (widgetItem.isLocked) Icons.Default.Lock else Icons.Default.OpenWith,
                            null,
                            tint = if (widgetItem.isLocked) HighContrastRed else AmberRacing,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (widgetItem.isLocked) "مقفل" else "اسحب للتحريك",
                            color = if (widgetItem.isLocked) HighContrastRed else AmberRacing,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(CarbonDark.copy(alpha = .95f), RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onChangeStyle, modifier = Modifier.size(30.dp).testTag("btn_change_style_${widgetItem.id}")) {
                        Icon(Icons.Default.Palette, "التصميم", tint = CyanNeon, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onToggleLock, modifier = Modifier.size(30.dp)) {
                        Icon(
                            if (widgetItem.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            if (widgetItem.isLocked) "فتح الودجت" else "قفل الودجت",
                            tint = if (widgetItem.isLocked) EmeraldSafe else TextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp).testTag("btn_delete_widget_${widgetItem.id}")) {
                        Icon(Icons.Default.Delete, "حذف", tint = HighContrastRed, modifier = Modifier.size(17.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(CarbonDark.copy(alpha = .95f), RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(Icons.Default.Opacity, null, tint = TextSecondary, modifier = Modifier.padding(start = 6.dp).size(15.dp))
                    IconButton(
                        onClick = { onOpacityChange(widgetItem.opacity - .10f) },
                        modifier = Modifier.size(28.dp)
                    ) { Icon(Icons.Default.Remove, "تقليل الشفافية", tint = TextPrimary, modifier = Modifier.size(16.dp)) }
                    Text("${(widgetItem.opacity * 100).toInt()}%", color = CyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { onOpacityChange(widgetItem.opacity + .10f) },
                        modifier = Modifier.size(28.dp)
                    ) { Icon(Icons.Default.Add, "زيادة الشفافية", tint = TextPrimary, modifier = Modifier.size(16.dp)) }
                }

                if (!widgetItem.isLocked) {
                    Surface(
                        color = CyanNeon.copy(alpha = .95f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(38.dp)
                            .pointerInput(widgetItem.id) {
                                detectDragGestures(
                                    onDragStart = { onBringToFront() },
                                    onDragEnd = onTransformFinished,
                                    onDragCancel = onTransformFinished
                                ) { _, dragAmount -> onResizeBy(dragAmount.x, dragAmount.y) }
                            }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.OpenInFull, "اسحب لتغيير الحجم", tint = CarbonDark, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
