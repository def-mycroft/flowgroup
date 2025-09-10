package com.mfme.kernel.ui.panels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mfme.kernel.ui.AboutScreen
import com.mfme.kernel.ui.CaptureScreen
import com.mfme.kernel.ui.HistoryScreen
import com.mfme.kernel.ui.KernelViewModel
import com.mfme.kernel.ui.log.LogSurfacePanel
import com.mfme.kernel.ui.lab.OperatorLabPanel

/** Registers built-in panels used by the kernel shell. */
fun registerBuiltinPanels(viewModel: KernelViewModel, devMode: Boolean = true) {
    PanelRegistry.clear()
    PanelRegistry.register(
        PanelDescriptor("capture", "Capture", Icons.Filled.CameraAlt) {
            CaptureScreen(viewModel)
        }
    )
    PanelRegistry.register(
        PanelDescriptor("history", "History", Icons.Filled.History) {
            HistoryScreen(viewModel)
        }
    )
    PanelRegistry.register(
        PanelDescriptor("cloud", "Cloud", Icons.Filled.Cloud) {
            Text("Cloud")
        }
    )
    PanelRegistry.register(
        PanelDescriptor("settings", "Settings", Icons.Filled.Settings) {
            AboutScreen()
        }
    )
    PanelRegistry.register(
        PanelDescriptor("log", "Log", Icons.Filled.Info) {
            LogSurfacePanel(viewModel)
        }
    )
    if (devMode) {
        PanelRegistry.register(
            PanelDescriptor("lab", "Lab", Icons.Filled.Info) {
                OperatorLabPanel(viewModel)
            }
        )
    }
    PanelRegistry.register(
        PanelDescriptor("hello", "Hello", null) {
            Text("Hello Plugin")
        }
    )
}
