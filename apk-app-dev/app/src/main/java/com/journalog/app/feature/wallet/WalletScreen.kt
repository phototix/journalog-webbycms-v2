package com.journalog.app.feature.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.DepositRequest
import com.journalog.app.data.remote.dto.WithdrawalRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(onBack: () -> Unit) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    val scope = rememberCoroutineScope()
    var balance by remember { mutableDoubleStateOf(0.0) }
    var pendingBalance by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Deposit state
    var depositAmount by remember { mutableStateOf("") }
    var depositProvider by remember { mutableStateOf("stripe") }
    var isDepositing by remember { mutableStateOf(false) }

    // Withdrawal state
    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawMethod by remember { mutableStateOf("bank_transfer") }
    var withdrawIdentifier by remember { mutableStateOf("") }
    var withdrawMessage by remember { mutableStateOf("") }
    var isWithdrawing by remember { mutableStateOf(false) }
    var calculatedFee by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        try {
            val resp = api.getWalletBalance()
            if (resp.isSuccessful) {
                balance = resp.body()?.data?.total ?: 0.0
                pendingBalance = resp.body()?.data?.pendingBalance ?: 0.0
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Balance card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Balance", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("\$${String.format("%.2f", balance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (pendingBalance > 0) {
                        Text("\$${String.format("%.2f", pendingBalance)} pending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Deposit", modifier = Modifier.padding(vertical = 8.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Withdraw", modifier = Modifier.padding(vertical = 8.dp)) }
            }

            Spacer(Modifier.height(16.dp))

            successMsg?.let {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp))
            }

            if (selectedTab == 0) {
                // DEPOSIT TAB
                OutlinedTextField(
                    value = depositAmount,
                    onValueChange = { depositAmount = it },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(Modifier.height(12.dp))

                Text("Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                listOf("stripe" to "Stripe", "paypal" to "PayPal", "coinbase" to "Coinbase", "credit" to "Wallet Credit").forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = depositProvider == value, onClick = { depositProvider = value })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = depositAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            error = "Enter a valid amount"
                            return@Button
                        }
                        scope.launch {
                            isDepositing = true
                            error = null
                            successMsg = null
                            try {
                                val resp = api.initiateDeposit(DepositRequest(amount = amount, provider = depositProvider))
                                if (resp.isSuccessful && resp.body()?.ok == true) {
                                    successMsg = "Deposit initiated! Transaction ID: ${resp.body()?.data?.transactionId}"
                                    depositAmount = ""
                                } else {
                                    error = resp.body()?.message ?: "Deposit failed"
                                }
                            } catch (e: Exception) { error = e.message }
                            isDepositing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isDepositing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isDepositing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Add Funds")
                }

            } else {
                // WITHDRAW TAB
                OutlinedTextField(
                    value = withdrawAmount,
                    onValueChange = {
                        withdrawAmount = it
                        val amt = it.toDoubleOrNull() ?: 0.0
                        calculatedFee = amt * 0.05 // 5% fee
                    },
                    label = { Text("Amount") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(Modifier.height(8.dp))

                val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    Text("Fee: \$${String.format("%.2f", calculatedFee)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Net: \$${String.format("%.2f", amt - calculatedFee)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = withdrawMethod,
                    onValueChange = { withdrawMethod = it },
                    label = { Text("Payment Method") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = withdrawIdentifier,
                    onValueChange = { withdrawIdentifier = it },
                    label = { Text("Account / Wallet ID") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = withdrawMessage,
                    onValueChange = { withdrawMessage = it },
                    label = { Text("Message (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = withdrawAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            error = "Enter a valid amount"
                            return@Button
                        }
                        if (withdrawIdentifier.isBlank()) {
                            error = "Enter your account/wallet ID"
                            return@Button
                        }
                        scope.launch {
                            isWithdrawing = true
                            error = null
                            successMsg = null
                            try {
                                val resp = api.requestWithdrawal(WithdrawalRequest(
                                    amount = amount,
                                    paymentMethod = withdrawMethod,
                                    paymentIdentifier = withdrawIdentifier,
                                    message = withdrawMessage.ifBlank { null }
                                ))
                                if (resp.isSuccessful && resp.body()?.ok == true) {
                                    val data = resp.body()?.data
                                    successMsg = "Withdrawal requested! Net: \$${String.format("%.2f", data?.netAmount ?: 0.0)}"
                                    balance = data?.newBalance ?: balance
                                    pendingBalance = data?.pendingBalance ?: pendingBalance
                                    withdrawAmount = ""
                                    withdrawIdentifier = ""
                                    withdrawMessage = ""
                                } else {
                                    error = resp.body()?.message ?: "Withdrawal failed"
                                }
                            } catch (e: Exception) { error = e.message }
                            isWithdrawing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isWithdrawing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isWithdrawing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Request Withdrawal")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
