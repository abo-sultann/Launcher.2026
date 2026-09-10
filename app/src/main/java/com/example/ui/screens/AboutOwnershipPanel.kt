package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutOwnershipPanel(accent: Color, onOpenDiagnostics: (() -> Unit)? = null) {
    // The approved original is kept whole and unmodified. Its white paper becomes
    // one deliberate half of the ownership card instead of a floating pasted image.
    Surface(color = CarbonSurface, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().testTag("about_identity")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 330.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color.White, modifier = Modifier.weight(1.2f).height(330.dp)) {
                Image(painterResource(R.drawable.darbak_owner_signature), "دربك — تصميم وتطوير — أبوسلطان",
                    modifier = Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
            }
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Icon(Icons.Default.Verified, null, tint = DarbakGold, modifier = Modifier.size(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Launcher 2026", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("لانشر شاشة السيارة", color = TextSecondary, fontSize = 16.sp)
                }
                Column(Modifier.combinedClickable(onClick = {}, onLongClick = { onOpenDiagnostics?.invoke() })
                    .testTag("about_version"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الإصدار", color = TextMuted, fontSize = 14.sp)
                    Text(BuildConfig.VERSION_NAME, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Copyright, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Text("جميع الحقوق محفوظة", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
