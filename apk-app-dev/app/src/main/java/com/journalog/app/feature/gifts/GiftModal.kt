package com.journalog.app.feature.gifts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.GiftDto
import com.journalog.app.data.remote.dto.GiftListData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftModal(
    postId: Int,
    onDismiss: () -> Unit,
    onGiftSent: (Double) -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var giftData by remember { mutableStateOf<GiftListData?>(null) }
    var selectedGift by remember { mutableStateOf<GiftDto?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = api.getGifts()
            if (response.isSuccessful) {
                giftData = response.body()?.data
            }
        } catch (_: Exception) { }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Send a Gift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            giftData?.let { data ->
                Text(
                    "Balance: ${data.balance.toInt()} credits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Category tabs
                val categories = data.gifts.keys.toList()
                var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Gift grid for selected category
                val gifts = data.gifts[selectedCategory] ?: emptyList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gifts, key = { it.id }) { gift ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedGift = gift }
                                .padding(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = if (selectedGift?.id == gift.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = giftIcon(gift.icon),
                                        contentDescription = gift.name,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(gift.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                            Text("${gift.credits}cr", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp))
                }

                Button(
                    onClick = {
                        selectedGift?.let { gift ->
                            scope.launch {
                                isSending = true
                                error = null
                                try {
                                    val resp = api.sendGift(mapOf("gift_id" to gift.id, "post_id" to postId))
                                    if (resp.isSuccessful && resp.body()?.ok == true) {
                                        onGiftSent(resp.body()!!.data?.balance ?: 0.0)
                                        onDismiss()
                                    } else {
                                        error = resp.body()?.message ?: "Failed to send gift"
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                }
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    enabled = selectedGift != null && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Send ${selectedGift?.name ?: ""}")
                    }
                }
            }
        }
    }
}

private fun giftIcon(iconName: String): ImageVector = when (iconName) {
    "diamond-outline" -> Icons.Filled.Diamond
    "heart" -> Icons.Filled.Favorite
    "rose-outline" -> Icons.Filled.LocalFlorist
    "star" -> Icons.Filled.Star
    "happy-outline" -> Icons.Filled.Face
    "cash-outline" -> Icons.Filled.AttachMoney
    "gift-outline" -> Icons.Filled.CardGiftcard
    "flame-outline" -> Icons.Filled.Whatshot
    "beer-outline" -> Icons.Filled.SportsBar
    "party-outline" -> Icons.Filled.Celebration
    "rocket-outline" -> Icons.Filled.RocketLaunch
    else -> Icons.Filled.CardGiftcard
}
