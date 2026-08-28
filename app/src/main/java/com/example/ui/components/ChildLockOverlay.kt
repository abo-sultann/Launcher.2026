package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Invisible child lock. The launcher remains visually unchanged while every touch is
 * intercepted. Unlocking is intentionally hidden: press and hold the physical top-right
 * Launcher-logo area for the configured number of seconds.
 */
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
            .pointerInput(seconds) {
                detectTapGestures(
                    onPress = { offset ->
                        val inSecretZone = offset.x >= size.width * 0.72f && offset.y <= size.height * 0.16f
                        if (!inSecretZone) {
                            tryAwaitRelease()
                            return@detectTapGestures
                        }
                        coroutineScope {
                            val unlockJob = launch {
                                delay(seconds * 1000L)
                                onUnlock()
                            }
                            tryAwaitRelease()
                            unlockJob.cancel()
                        }
                    },
                    onTap = { /* consume */ }
                )
            }
    )
}
