package com.mfme.kernel.ui.cloud

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.mfme.kernel.cloud.CloudPreferences
import com.mfme.kernel.cloud.DriveServiceFactory
import com.mfme.kernel.ui.theme.KernelTheme
import com.mfme.kernel.work.ReconcilerScheduler

@Composable
fun CloudScreen() {
    val context = LocalContext.current
    val prefs = remember { CloudPreferences(context) }
    val model by prefs.flow.collectAsState(initial = CloudPreferences.Model(wifiOnly = true))
    val snack = remember { SnackbarHostState() }

    var acct: GoogleSignInAccount? by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
    val signInClient: GoogleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.result }.onSuccess { account ->
            acct = account
            DriveServiceFactory.invalidate()
        }.onFailure {
            // ignore; user cancelled or failed
        }
    }

    val scope: CoroutineScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO) }
    Column(modifier = Modifier.padding(KernelTheme.tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(KernelTheme.tokens.spacing.md)) {
        Text("Google Drive")
        SnackbarHost(hostState = snack)
        val connected = acct != null
        Text(if (connected) "Connected as ${acct?.email}" else "Not connected")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KernelTheme.tokens.spacing.sm)) {
            Button(onClick = {
                if (connected) {
                    signInClient.signOut().addOnCompleteListener {
                        acct = null
                        DriveServiceFactory.invalidate()
                    }
                } else {
                    launcher.launch(signInClient.signInIntent)
                }
            }) { Text(if (connected) "Disconnect" else "Connect Drive") }
            Button(onClick = { ReconcilerScheduler.verifyOnce(context) }) { Text("Verify now") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Upload on Wi‑Fi only")
            Switch(checked = model.wifiOnly, onCheckedChange = { checked ->
                scope.launch { prefs.setWifiOnly(checked) }
            })
        }
    }
}

