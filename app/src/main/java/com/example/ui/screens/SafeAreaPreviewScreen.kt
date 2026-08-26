package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SafeAreaPreviewScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeArea by viewModel.safeArea.collectAsState()

    var topMargin by remember(safeArea) { mutableStateOf(safeArea.topDp.toFloat()) }
    var bottomMargin by remember(safeArea) { mutableStateOf(safeArea.bottomDp.toFloat()) }
    var leftMargin by remember(safeArea) { mutableStateOf(safeArea.leftDp.toFloat()) }
    var rightMargin by remember(safeArea) { mutableStateOf(safeArea.rightDp.toFloat()) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: Interactive Margin Sliders
        Card(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = CyanNeon)
                        }
                        Text(
                            text = "تعديل هوامش الشاشة (Safe Area)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            topMargin = 0f
                            bottomMargin = 0f
                            leftMargin = 0f
                            rightMargin = 0f
                            viewModel.resetSafeArea()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة ضبط", tint = AmberRacing)
                    }
                }

                // 4 Margin Sliders
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MarginSliderRow(
                        label = "الهامش العلوي (Top)",
                        value = topMargin,
                        onValueChange = {
                            topMargin = it
                            viewModel.updateSafeArea(it.toInt(), bottomMargin.toInt(), leftMargin.toInt(), rightMargin.toInt())
                        }
                    )

                    MarginSliderRow(
                        label = "الهامش السفلي (Bottom)",
                        value = bottomMargin,
                        onValueChange = {
                            bottomMargin = it
                            viewModel.updateSafeArea(topMargin.toInt(), it.toInt(), leftMargin.toInt(), rightMargin.toInt())
                        }
                    )

                    MarginSliderRow(
                        label = "الهامش الأيمن (Right)",
                        value = rightMargin,
                        onValueChange = {
                            rightMargin = it
                            viewModel.updateSafeArea(topMargin.toInt(), bottomMargin.toInt(), leftMargin.toInt(), it.toInt())
                        }
                    )

                    MarginSliderRow(
                        label = "الهامش الأيسر (Left)",
                        value = leftMargin,
                        onValueChange = {
                            leftMargin = it
                            viewModel.updateSafeArea(topMargin.toInt(), bottomMargin.toInt(), it.toInt(), rightMargin.toInt())
                        }
                    )
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_save_safe_area")
                ) {
                    Text(
                        text = "حفظ وتطبيق الهوامش",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CarbonDark
                    )
                }
            }
        }

        // Right Column: Real-time Visual Boundary Preview Box
        Card(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarbonDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CarbonCardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Full Screen Glass Preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF070B10), RoundedCornerShape(10.dp))
                        .border(2.dp, Color(0xFF334A68), RoundedCornerShape(10.dp))
                ) {
                    // Safe Area Highlight Box (using actual proportional dp)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = (topMargin * 0.4f).dp,
                                bottom = (bottomMargin * 0.4f).dp,
                                start = (rightMargin * 0.4f).dp,
                                end = (leftMargin * 0.4f).dp
                            )
                            .background(CyanNeon.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.5.dp, CyanNeon, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "منطقة العرض الآمنة للواجهة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyanNeon
                            )
                            Text(
                                text = "أعلى: ${topMargin.toInt()}dp | أسفل: ${bottomMargin.toInt()}dp | يمين: ${rightMargin.toInt()}dp | يسار: ${leftMargin.toInt()}dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarginSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
            Text(text = "${value.toInt()} dp", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyanNeon)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CyanNeon,
                activeTrackColor = CyanNeon,
                inactiveTrackColor = CarbonCardBorder
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}
