package com.mfme.kernel.ui.gestures

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mfme.kernel.ui.KernelViewModel
import com.mfme.kernel.ui.theme.KernelTheme

/** Demo card showing long-press memo toggling. */
@Composable
fun CaptureCardDemo(viewModel: KernelViewModel) {
    var memo by remember { mutableStateOf(false) }
    val adapter = remember(viewModel) {
        GestureAdapter { intent ->
            if (intent is GestureIntent.MemoStart) memo = !memo
            NoopGestureAdapter(viewModel).on(intent)
        }
    }
    val tokens = KernelTheme.tokens
    Card(modifier = Modifier.padding(tokens.spacing.md).mfmeGestures(adapter)) {
        Text(if (memo) "memo…" else "long press")
    }
}
