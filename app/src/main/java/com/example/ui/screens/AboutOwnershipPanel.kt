package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
    Surface(
        color = CarbonSurface.copy(alpha = .38f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, DarbakGold.copy(alpha = .34f)),
        modifier = Modifier.fillMaxWidth().testTag("about_identity"),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 280.dp).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Surface(
                color = CarbonDark.copy(alpha = .28f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .07f)),
                modifier = Modifier.weight(1.15f).height(245.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.owner_signature_darbak),
                        contentDescription = "دربك — تصميم وتطوير — أبوسلطان",
                        modifier = Modifier.fillMaxSize().padding(22.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(
                        color = DarbakGold.copy(alpha = .13f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Verified, null, tint = DarbakGold, modifier = Modifier.size(25.dp))
                        }
                    }
                    Column {
                        Text("Darbak Launcher", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("واجهة شاشة السيارة", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                Column(
                    Modifier.combinedClickable(onClick = {}, onLongClick = { onOpenDiagnostics?.invoke() }).testTag("about_version"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("الإصدار", color = TextMuted, fontSize = 12.sp)
                    Text(BuildConfig.VERSION_NAME, color = accent, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }

                HorizontalDivider(color = Color.White.copy(alpha = .07f))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Copyright, null, tint = DarbakGold, modifier = Modifier.size(17.dp))
                    Text("تصميم وتطوير أبوسلطان — جميع الحقوق محفوظة", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
