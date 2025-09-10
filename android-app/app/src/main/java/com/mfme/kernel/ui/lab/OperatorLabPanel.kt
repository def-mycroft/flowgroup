package com.mfme.kernel.ui.lab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mfme.kernel.ui.KernelViewModel
import com.mfme.kernel.ui.gestures.GestureIntent
import com.mfme.kernel.ui.theme.KernelTheme

/** Developer sandbox panel for operator experiments. */
@Composable
fun OperatorLabPanel(viewModel: KernelViewModel) {
    var timeline by remember { mutableStateOf(false) }
    var haptic by remember { mutableStateOf(0.5f) }
    val tokens = KernelTheme.tokens
    Column(Modifier.fillMaxSize().padding(tokens.spacing.md)) {
        Text("Operator Lab")
        Switch(checked = timeline, onCheckedChange = {
            timeline = it
            viewModel.logGesture(GestureIntent.Mark)
        })
        Slider(value = haptic, onValueChange = {
            haptic = it
            viewModel.logGesture(GestureIntent.Mark)
        })
        Text("timeline=$timeline haptic=$haptic")
    }
}
