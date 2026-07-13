package com.journalog.app.feature.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.SubscribeRequest
import com.journalog.app.data.remote.dto.UserDto
import kotlinx.coroutines.launch

data class PlanOption(
    val key: String,
    val label: String,
    val price: Double
)

@Composable
fun SubscriptionScreen(
    creator: UserDto,
    onBack: () -> Unit,
    onSubscribed: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var selectedPlan by remember { mutableStateOf(0) }
    var balance by remember { mutableDoubleStateOf(0.0) }
    var isSubscribing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var subscribed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val plans = listOf(
        PlanOption("one-month", "1 Month", creator.profileAccessPrice),
        PlanOption("three-months", "3 Months", (creator.profileAccessPrice3Months ?: creator.profileAccessPrice) * 3),
        PlanOption("six-months", "6 Months", (creator.profileAccessPrice6Months ?: creator.profileAccessPrice) * 6),
        PlanOption("yearly", "12 Months", (creator.profileAccessPrice12Months ?: creator.profileAccessPrice) * 12),
    )

    LaunchedEffect(Unit) {
        try {
            val resp = api.getWalletBalance()
            if (resp.isSuccessful) {
                balance = resp.body()?.data?.total ?: 0.0
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (subscribed) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Subscribed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You are now subscribed to ${creator.name}", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onSubscribed) {
                        Text("Done")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subscribe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onBack) { Text("✕") }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = creator.avatar,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(creator.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${creator.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select a plan:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    plans.forEachIndexed { index, plan ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPlan == index) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { selectedPlan = index }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlan == index,
                                    onClick = { selectedPlan = index }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(plan.label, modifier = Modifier.weight(1f))
                                Text("\$${String.format("%.2f", plan.price)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wallet, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Balance: \$${String.format("%.2f", balance)}", style = MaterialTheme.typography.bodyMedium)
                    }

                    error?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSubscribing = true
                                error = null
                                try {
                                    val plan = plans[selectedPlan]
                                    val resp = api.subscribe(SubscribeRequest(
                                        recipientUserId = creator.id,
                                        plan = plan.key
                                    ))
                                    if (resp.isSuccessful && resp.body()?.ok == true) {
                                        subscribed = true
                                    } else {
                                        val msg = resp.body()?.message ?: "Subscription failed"
                                        error = msg
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Network error"
                                }
                                isSubscribing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        enabled = !isSubscribing
                    ) {
                        if (isSubscribing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Subscribe with Wallet")
                        }
                    }
                }
            }
        }
    }
}
