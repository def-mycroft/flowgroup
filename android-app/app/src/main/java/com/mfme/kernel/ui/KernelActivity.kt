package com.mfme.kernel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mfme.kernel.ServiceLocator
import com.mfme.kernel.ui.panels.PluginPanelHost
import com.mfme.kernel.ui.panels.registerBuiltinPanels
import com.mfme.kernel.ui.theme.KernelTheme

class KernelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KernelApp() }
    }
}

@Composable
fun KernelApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember { ServiceLocator.repository(context) }
    val vaultConfig = remember { ServiceLocator.vaultConfig(context) }
    val viewModel: KernelViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return KernelViewModel(repository, vaultConfig) as T
        }
    })

    registerBuiltinPanels(viewModel)
    KernelTheme {
        PluginPanelHost()
    }
}
