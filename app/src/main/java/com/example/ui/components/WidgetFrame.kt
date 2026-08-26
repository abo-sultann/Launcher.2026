package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onToggleSize: () -> Unit,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = if (isDesignMode) AmberRacing.copy(alpha = 0.8f) else CarbonCardBorder
    val borderWidth = if (isDesignMode) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxSize()
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = CarbonCard.copy(alpha = 0.90f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Widget Content
            content()

            // Design Mode Overlay Controls
            if (isDesignMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(6.dp)
                ) {
                    // Header tag with widget type and style name
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CarbonDark.copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${widgetItem.type.arabicTitle} — ${widgetItem.style.arabicName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberRacing
                        )
                    }

                    // Floating Action Buttons in Design Mode
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarbonDark.copy(alpha = 0.95f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Change Style
                        Button(
                            onClick = onChangeStyle,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp).testTag("btn_change_style_${widgetItem.id}")
                        ) {
                            Text(
                                text = "تغيير التصميم",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CarbonDark
                            )
                        }

                        // Resize Span
                        IconButton(
                            onClick = onToggleSize,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (widgetItem.spanX > 1) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "تغيير الحجم",
                                tint = TextPrimary
                            )
                        }

                        // Move Left / Right in layout
                        IconButton(
                            onClick = onMoveBackward,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "تحريك للأمام",
                                tint = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = onMoveForward,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "تحريك للخلف",
                                tint = TextPrimary
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp).testTag("btn_delete_widget_${widgetItem.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف الودجت",
                                tint = HighContrastRed
                            )
                        }
                    }
                }
            }
        }
    }
}
