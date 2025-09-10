package com.mfme.kernel.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mfme.kernel.ServiceLocator

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

    val navController = rememberNavController()
    val items = listOf(
        NavItem("capture", "Capture", Icons.Filled.CameraAlt),
        NavItem("history", "History", Icons.Filled.History),
        NavItem("settings", "Settings", Icons.Filled.Settings),
        NavItem("about", "About", Icons.Filled.Info)
    )

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "capture",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("capture") { CaptureScreen(viewModel) }
                composable("history") { HistoryScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
                composable("about") { AboutScreen() }
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

