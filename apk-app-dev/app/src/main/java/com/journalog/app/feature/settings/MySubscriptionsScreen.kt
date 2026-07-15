package com.journalog.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.SubscriptionItemDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySubscriptionsScreen(onBack: () -> Unit) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    val scope = rememberCoroutineScope()
    var subscriptions by remember { mutableStateOf<List<SubscriptionItemDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            try {
                val resp = api.getMySubscriptions()
                if (resp.isSuccessful) {
                    subscriptions = resp.body()?.data?.subscriptions ?: emptyList()
                }
            } catch (e: Exception) { error = e.message }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Subscribing", modifier = Modifier.padding(vertical = 8.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Subscribers", modifier = Modifier.padding(vertical = 8.dp)) }
            }

            val filtered = if (selectedTab == 0) {
                subscriptions.filter { it.recipient != null }
            } else {
                subscriptions.filter { it.sender != null }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subscriptions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { sub ->
                        val user = if (selectedTab == 0) sub.recipient else sub.sender
                        SubscriptionRow(sub = sub, userAvatar = user?.avatar, userName = user?.name ?: "Unknown", userUsername = user?.username ?: "", onCancel = {
                            scope.launch {
                                try {
                                    val targetUserId = if (selectedTab == 0) sub.recipient?.id else sub.sender?.id
                                    if (targetUserId != null) {
                                        api.cancelSubscription(mapOf("creator_user_id" to targetUserId))
                                        load()
                                    }
                                } catch (_: Exception) {}
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(sub: SubscriptionItemDto, userAvatar: String?, userName: String, userUsername: String, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = userAvatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(userName, fontWeight = FontWeight.SemiBold)
                Text("@$userUsername", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                val color = when (sub.status) {
                    "completed" -> MaterialTheme.colorScheme.primary
                    "canceled" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(sub.status ?: "", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
                if (sub.expiresAt != null) {
                    Text(sub.expiresAt.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (sub.status == "completed") {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
