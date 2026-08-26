package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.SafeAreaConfig

@Composable
fun SafeAreaContainer(
    safeArea: SafeAreaConfig,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = safeArea.topDp.dp,
                bottom = safeArea.bottomDp.dp,
                start = safeArea.rightDp.dp, // In RTL: start is Right
                end = safeArea.leftDp.dp     // In RTL: end is Left
            )
    ) {
        content()
    }
}
