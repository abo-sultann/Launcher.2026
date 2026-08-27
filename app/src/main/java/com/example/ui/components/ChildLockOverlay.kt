package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChildLockOverlay(
    holdSeconds: Int,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = holdSeconds.coerceIn(2, 6)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .55f))
            .pointerInput(Unit) { detectTapGestures(onTap = { }) }
    ) {
        Surface(
            modifier = Modifier.align(Alignment.Center),
            color = CarbonDark.copy(alpha = .96f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AmberRacing)
        ) {
            Column(
                Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = AmberRacing, modifier = Modifier.size(42.dp))
                Text("قفل الأطفال مفعّل", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("جميع اللمسات مقفلة", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp)
                .pointerInput(seconds) {
                    detectTapGestures(
                        onPress = {
                            coroutineScope {
                                val unlockJob = launch {
                                    delay(seconds * 1000L)
                                    onUnlock()
                                }
                                tryAwaitRelease()
                                unlockJob.cancel()
                            }
                        }
                    )
                },
            color = CarbonDark.copy(alpha = .98f),
            shape = CircleShape,
            border = BorderStroke(2.dp, CyanNeon)
        ) {
            Row(
                Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(Icons.Default.LockOpen, null, tint = CyanNeon)
                Text("اضغط مطولاً $seconds ثوانٍ لفك القفل", color = CyanNeon, fontWeight = FontWeight.Bold)
            }
        }
    }
}
