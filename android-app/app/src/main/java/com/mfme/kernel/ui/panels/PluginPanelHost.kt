package com.mfme.kernel.ui.panels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Host composable that displays the provided [panels]. */
@Composable
fun PluginPanelHost(panels: List<PanelDescriptor>) {
    var activeId by rememberSaveable(panels) { mutableStateOf(panels.firstOrNull()?.id ?: "") }
    val active = panels.firstOrNull { it.id == activeId }

    Scaffold(
        bottomBar = {
            NavigationBar {
                panels.forEach { panel ->
                    NavigationBarItem(
                        selected = panel.id == activeId,
                        onClick = { activeId = panel.id },
                        icon = { panel.icon?.let { Icon(it, contentDescription = panel.title) } },
                        label = { Text(panel.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            active?.content?.invoke()
        }
    }
}
