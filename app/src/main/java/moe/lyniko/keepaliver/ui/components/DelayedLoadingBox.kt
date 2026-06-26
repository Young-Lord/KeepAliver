package moe.lyniko.keepaliver.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Shows a centered progress indicator only when loading lasts longer than [delayMillis].
 * Quick loads leave composition before the delay elapses, so no spinner flashes.
 */
@Composable
fun DelayedLoadingBox(
    modifier: Modifier = Modifier,
    delayMillis: Long = 300L
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            CircularProgressIndicator()
        }
    }
}
