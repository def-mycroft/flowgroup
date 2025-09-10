package com.mfme.kernel.ui.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** Description of a panel provided to the host. */
data class PanelDescriptor(
    val id: String,
    val title: String,
    val icon: ImageVector?,
    val content: @Composable () -> Unit
)
