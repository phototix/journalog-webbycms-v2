package com.journalog.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(onBack: () -> Unit) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    var paidProfile by remember { mutableStateOf(false) }
    var price1m by remember { mutableStateOf("") }
    var price3m by remember { mutableStateOf("") }
    var price6m by remember { mutableStateOf("") }
    var price12m by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val resp = api.getProfileSettings()
            if (resp.isSuccessful) {
                val u = resp.body()?.data?.get("user")
                if (u != null) {
                    paidProfile = (u as Map<String, Any>)["paid_profile"] as? Boolean ?: false
                    price1m = ((u as Map<String, Any>)["profile_access_price"] as? Double)?.toString() ?: ""
                    price3m = ((u as Map<String, Any>)["profile_access_price_3_months"] as? Double)?.toString()?.ifEmpty { price1m } ?: price1m
                    price6m = ((u as Map<String, Any>)["profile_access_price_6_months"] as? Double)?.toString()?.ifEmpty { price1m } ?: price1m
                    price12m = ((u as Map<String, Any>)["profile_access_price_12_months"] as? Double)?.toString()?.ifEmpty { price1m } ?: price1m
                }
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription Rates") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Scaffold
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Paid Profile", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(checked = paidProfile, onCheckedChange = { paidProfile = it })
            }

            if (paidProfile) {
                Spacer(Modifier.height(16.dp))
                listOf("1 Month" to price1m, "3 Months" to price3m, "6 Months" to price6m, "12 Months" to price12m).forEachIndexed { i, (label, value) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { v ->
                            when (i) { 0 -> price1m = v; 1 -> price3m = v; 2 -> price6m = v; 3 -> price12m = v }
                        },
                        label = { Text(label) },
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            successMsg?.let {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true; error = null; successMsg = null
                        try {
                            val body = mutableMapOf<String, Any>("paid_profile" to paidProfile)
                            if (price1m.isNotBlank()) body["profile_access_price"] = price1m.toDoubleOrNull() ?: 0.0
                            if (price3m.isNotBlank()) body["profile_access_price_3_months"] = price3m.toDoubleOrNull() ?: 0.0
                            if (price6m.isNotBlank()) body["profile_access_price_6_months"] = price6m.toDoubleOrNull() ?: 0.0
                            if (price12m.isNotBlank()) body["profile_access_price_12_months"] = price12m.toDoubleOrNull() ?: 0.0
                            val resp = api.updateProfile(body)
                            if (resp.isSuccessful) successMsg = "Rates saved"
                            else error = resp.body()?.message ?: "Failed to save"
                        } catch (e: Exception) { error = e.message }
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save Rates")
            }
        }
    }
}
