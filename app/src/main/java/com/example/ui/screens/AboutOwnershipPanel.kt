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
        color = Color.Black.copy(alpha = .18f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, DarbakGold.copy(alpha = .52f)),
        modifier = Modifier.fillMaxWidth().testTag("about_identity"),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 265.dp).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Surface(
                color = Color(0xFF070A0D),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, DarbakGold.copy(alpha = .28f)),
                modifier = Modifier.weight(1.08f).height(225.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // The approved transparent mark is a small source asset. Keep it near its native
                    // aspect and composite it twice so semi-transparent gold/white pixels remain
                    // strong on the 1024x600 head unit instead of looking washed out.
                    val mark = Modifier.width(252.dp).height(168.dp)
                    Image(
                        painter = painterResource(R.drawable.owner_signature_darbak),
                        contentDescription = "دربك — تصميم وتطوير — أبوسلطان",
                        modifier = mark,
                        contentScale = ContentScale.Fit,
                    )
                    Image(
                        painter = painterResource(R.drawable.owner_signature_darbak),
                        contentDescription = null,
                        modifier = mark,
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(color = DarbakGold.copy(alpha = .14f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(44.dp)) {
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
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("الإصدار", color = TextMuted, fontSize = 11.sp)
                    Text(BuildConfig.VERSION_NAME, color = accent, fontSize = 29.sp, fontWeight = FontWeight.Black)
                }

                HorizontalDivider(color = DarbakGold.copy(alpha = .22f))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Copyright, null, tint = DarbakGold, modifier = Modifier.size(17.dp))
                    Text("تصميم وتطوير أبوسلطان — جميع الحقوق محفوظة", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
