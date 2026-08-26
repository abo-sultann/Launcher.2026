package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.WidgetStyle
import com.example.model.WidgetType
import com.example.ui.theme.*

@Composable
fun WidgetLibraryDialog(
    initialType: WidgetType? = null,
    isStyleChangerMode: Boolean = false,
    onDismiss: () -> Unit,
    onSelectStyle: (WidgetStyle) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType ?: WidgetType.CLOCK) }
    val stylesForType = remember(selectedType) {
        WidgetStyle.values().filter { it.type == selectedType }
    }
    var selectedStyle by remember(selectedType) {
        mutableStateOf(stylesForType.firstOrNull() ?: WidgetStyle.CLOCK_AUTOMOTIVE_LARGE)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isStyleChangerMode) "تغيير تصميم الودجت" else "مكتبة الودجات (Widget Library)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Types Horizontal Tab Row (if not locked in changer mode)
                if (!isStyleChangerMode) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(WidgetType.values()) { type ->
                            val isSelected = selectedType == type
                            Surface(
                                color = if (isSelected) CyanNeon else CarbonSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanNeon else CarbonCardBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedType = type
                                    }
                                    .testTag("tab_type_${type.name}")
                            ) {
                                Text(
                                    text = type.arabicTitle,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) CarbonDark else TextPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Middle: Styles Grid / List
                Text(
                    text = "اختر النموذج المناسب لـ (${selectedType.arabicTitle}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberRacing
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stylesForType) { style ->
                        val isSelected = selectedStyle == style
                        Surface(
                            color = if (isSelected) CyanNeon.copy(alpha = 0.15f) else CarbonSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) CyanNeon else CarbonCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedStyle = style }
                                .testTag("style_item_${style.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.arabicName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) CyanNeon else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = style.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedStyle = style },
                                    colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "إلغاء", color = TextPrimary)
                    }

                    Button(
                        onClick = { onSelectStyle(selectedStyle) },
                        modifier = Modifier.weight(1f).testTag("btn_confirm_widget_style"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isStyleChangerMode) "تطبيق التصميم" else "إضافة إلى الشاشة",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CarbonDark
                        )
                    }
                }
            }
        }
    }
}
