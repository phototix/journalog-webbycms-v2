package com.journalog.app.feature.messenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.journalog.app.core.common.DateFormatter
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.MessageDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    userId: Int,
    userName: String,
    avatar: String = "",
    onBack: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun loadMessages(page: Int = 1, append: Boolean = false) {
        scope.launch {
            if (page == 1) isLoading = true else isLoadingMore = true
            try {
                val resp = api.getMessages(userId, page, 10)
                if (resp.isSuccessful) {
                    val paginated = resp.body()?.data?.get("messages")
                    val newMsgs = paginated?.data?.reversed() ?: emptyList()
                    messages = if (append) newMsgs + messages else newMsgs
                    val hasMoreData = resp.body()?.data?.get("has_more") as? Boolean
                    hasMore = hasMoreData ?: false
                    currentPage = page
                    if (page == 1) {
                        scope.launch {
                            try {
                                if (messages.isNotEmpty()) {
                                    listState.scrollToItem(messages.size - 1)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {}
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(Unit) { loadMessages() }

    // Scroll-to-top pagination
    val shouldLoadPrev = remember {
        derivedStateOf {
            val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisible != null && firstVisible.index <= 2 && hasMore && !isLoadingMore && !isLoading
        }
    }
    LaunchedEffect(shouldLoadPrev.value) {
        if (shouldLoadPrev.value) loadMessages(currentPage + 1, append = true)
    }

    // Background polling every 5s, stops after 3 consecutive failures
    var pollFailCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (pollFailCount >= 3) break
            try {
                val resp = api.getMessages(userId, 1, 10)
                if (resp.isSuccessful) {
                    pollFailCount = 0
                    val fresh = resp.body()?.data?.get("messages")?.data?.reversed() ?: emptyList()
                    val currentIds = messages.map { it.id }.toSet()
                    val newOnes = fresh.filter { it.id !in currentIds }
                    if (newOnes.isNotEmpty()) {
                        messages = messages + newOnes
                        if (messages.isNotEmpty()) {
                            scope.launch { listState.scrollToItem(messages.size - 1) }
                        }
                    }
                } else {
                    pollFailCount++
                }
            } catch (_: Exception) {
                pollFailCount++
            }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank() || isSending) return
        isSending = true
        scope.launch {
            try {
                api.sendMessage(mapOf("receiver_id" to userId, "message" to inputText))
                inputText = ""
                loadMessages(page = 1, append = false)
            } catch (_: Exception) {}
            isSending = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (avatar.isNotBlank()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(userName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { sendMessage() }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (messages.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No messages yet. Send a message to start!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (messages.isEmpty() && isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        if (hasMore && !isLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Load older messages", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        Column {
                            MessageBubble(msg)
                            Text(
                                DateFormatter.formatRelativeTime(msg.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: MessageDto) {
    val color = if (msg.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isMine) 16.dp else 4.dp,
                bottomEnd = if (msg.isMine) 4.dp else 16.dp
            ),
            color = color
        ) {
            Text(
                text = msg.text ?: "",
                color = textColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
