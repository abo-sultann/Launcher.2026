package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.CarbonCard
import com.example.ui.theme.CarbonCardBorder
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AboutOwnershipPanel(accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = .55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.owner_signature),
                    contentDescription = "توقيع ملكية عبدالله الحربي",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    "نسخة خاصة مملوكة لعبدالله الحربي — أبوسلطان",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "هوية الملكية مدمجة داخل التطبيق ولا تعتمد على الإنترنت.",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        OwnershipRow(
            icon = Icons.Default.Verified,
            title = "Launcher 2026",
            value = "الإصدار ${BuildConfig.VERSION_NAME}  •  البناء ${BuildConfig.VERSION_CODE}",
            accent = accent
        )
        OwnershipRow(
            icon = Icons.Default.Fingerprint,
            title = "إثبات الملكية",
            value = "عبدالله الحربي  •  ABOSULTAN",
            accent = accent
        )
        OwnershipRow(
            icon = Icons.Default.Copyright,
            title = "الاستخدام",
            value = "تطبيق شخصي خاص — جميع الحقوق محفوظة",
            accent = accent
        )
    }
}

@Composable
private fun OwnershipRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    accent: Color
) {
    Surface(
        color = CarbonSurface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, CarbonCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextSecondary, fontSize = 10.sp)
                Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
