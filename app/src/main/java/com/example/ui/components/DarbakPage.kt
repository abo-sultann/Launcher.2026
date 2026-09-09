package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
internal fun DarbakPageHeading(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = TextSecondary, fontSize = 14.sp)
        }
        actions()
    }
}

@Composable
internal fun DarbakSearch(value: String, onChange: (String) -> Unit, hint: String, tag: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, singleLine = true,
        placeholder = { Text(hint, fontSize = 16.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
        trailingIcon = { if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "مسح البحث") } },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon, unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = CarbonSurface, unfocusedContainerColor = CarbonSurface,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
        modifier = modifier.heightIn(min = 56.dp).testTag(tag))
}

@Composable
internal fun DarbakEmptyState(icon: ImageVector, title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = TextMuted, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = TextSecondary, fontSize = 18.sp)
        Spacer(Modifier.height(14.dp))
        action()
    }
}
