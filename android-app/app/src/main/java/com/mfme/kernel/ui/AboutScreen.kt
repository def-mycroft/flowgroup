package com.mfme.kernel.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mfme.kernel.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val text = remember {
        context.resources.openRawResource(R.raw.kernel_brief).bufferedReader().use { it.readText() }
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    )
}
