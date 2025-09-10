package com.mfme.kernel.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
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
import com.mfme.kernel.ping.data.LocationPingPreferences
import com.mfme.kernel.ping.work.SmsLocationPingerWorkScheduler
import com.mfme.kernel.ui.theme.KernelTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mfme.kernel.ping.domain.LocationPingController

@Composable
fun LocationPingSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { LocationPingPreferences(context) }
    val scope = remember { CoroutineScope(Dispatchers.IO) }

    val model by prefs.flow.collectAsState(initial = LocationPingPreferences.Model(false, emptyList(), 15, null, emptyList()))

    var recipientsText by remember(model.recipients) { mutableStateOf(model.recipients.joinToString(",")) }
    var intervalText by remember(model.intervalMin) { mutableStateOf(model.intervalMin.toString()) }
    var selfTestText by remember(model.selfTestNumber) { mutableStateOf(model.selfTestNumber.orEmpty()) }
    var enabled by remember(model.enabled) { mutableStateOf(model.enabled) }

    val e164Regex = remember { Regex("^\\+[1-9]\\d{1,14}$") }
    val recipients = remember(recipientsText) { recipientsText.split(',').mapNotNull { it.trim().ifBlank { null } } }
    val allValid = recipients.isNotEmpty() && recipients.all { it.matches(e164Regex) }

    val snack: SnackbarHostState = remember { SnackbarHostState() }
    Column(modifier = Modifier.padding(KernelTheme.tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(KernelTheme.tokens.spacing.md)) {
        Text("Location Ping")
        SnackbarHost(hostState = snack)
        OutlinedTextField(
            value = recipientsText,
            onValueChange = { recipientsText = it },
            label = { Text("Recipients (comma-separated E.164)") },
            modifier = Modifier.fillMaxWidth(),
            isError = !allValid
        )
        OutlinedTextField(
            value = intervalText,
            onValueChange = { intervalText = it.filter { ch -> ch.isDigit() }.ifBlank { "15" } },
            label = { Text("Interval minutes (min 15)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = selfTestText,
            onValueChange = { selfTestText = it },
            label = { Text("Self-test number (optional E.164)") },
            modifier = Modifier.fillMaxWidth(),
            isError = selfTestText.isNotBlank() && !selfTestText.matches(e164Regex)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enable pings")
            Switch(checked = enabled, onCheckedChange = { checked ->
                // Do not allow enabling unless valid
                if (checked && !allValid) return@Switch
                enabled = checked
                scope.launch {
                    prefs.setEnabled(checked)
                    if (checked) {
                        val minutes = intervalText.toIntOrNull()?.coerceAtLeast(15) ?: 15
                        prefs.setIntervalMin(minutes)
                        prefs.setRecipients(recipients)
                        SmsLocationPingerWorkScheduler.schedule(context, minutes)
                    } else {
                        SmsLocationPingerWorkScheduler.cancel(context)
                    }
                }
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KernelTheme.tokens.spacing.sm)) {
            Button(enabled = allValid, onClick = {
                scope.launch {
                    prefs.setRecipients(recipients)
                    val controller = LocationPingController(context)
                    val result = controller.testNow()
                    when (result) {
                        is LocationPingController.Result.Ok -> snack.showSnackbar("OkSent")
                        is LocationPingController.Result.Error -> snack.showSnackbar(result.reason)
                    }
                }
            }) { Text("Test now") }
        }

        LaunchedEffect(recipientsText) {
            scope.launch { prefs.setRecipients(recipients) }
        }
        LaunchedEffect(intervalText) {
            val minutes = intervalText.toIntOrNull()?.coerceAtLeast(15) ?: 15
            scope.launch { prefs.setIntervalMin(minutes) }
        }
        LaunchedEffect(selfTestText) {
            scope.launch { prefs.setSelfTestNumber(selfTestText.ifBlank { null }) }
        }
    }
}
