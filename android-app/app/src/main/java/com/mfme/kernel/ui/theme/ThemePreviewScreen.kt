package com.mfme.kernel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Simple screen that previews theme tokens. */
@Composable
fun ThemePreviewScreen() {
    val t = KernelTheme.tokens
    val colors = listOf(
        "primary" to t.colors.primary,
        "secondary" to t.colors.secondary,
        "background" to t.colors.background,
        "surface" to t.colors.surface,
        "onPrimary" to t.colors.onPrimary,
        "onSecondary" to t.colors.onSecondary,
        "onBackground" to t.colors.onBackground,
        "onSurface" to t.colors.onSurface,
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(t.spacing.md), verticalArrangement = Arrangement.spacedBy(t.spacing.sm)) {
        items(colors) { (name, color) ->
            Row(Modifier.height(40.dp)) {
                Column(Modifier.size(40.dp).background(color)) {}
                Text(name, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
