package com.journalog.app.feature.messenger

import android.widget.TextView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.journalog.app.core.common.DateFormatter
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.ChatbotResponse
import com.journalog.app.data.remote.dto.MessageDto
import io.noties.markwon.Markwon
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
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var isWaitingForBot by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isBot = remember { userName.contains("Bot", ignoreCase = true) }

    fun loadMessages(page: Int = 1, append: Boolean = false) {
        scope.launch {
            if (page == 1) isLoading = true else isLoadingMore = true
            try {
                val resp = api.getMessages(userId, page, 10)
                if (resp.isSuccessful) {
                    val data = resp.body()?.data
                    val newMsgs = data?.messages?.data?.reversed() ?: emptyList()
                    messages = if (append) newMsgs + messages else newMsgs
                    hasMore = data?.hasMore ?: false
                    currentPage = page
                    if (page == 1) {
                        scope.launch {
                            try {
                                if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
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

    // Background polling (5s)
    var pollFailCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (pollFailCount >= 3) break
            try {
                val resp = api.getMessages(userId, 1, 10)
                if (resp.isSuccessful) {
                    pollFailCount = 0
                    val fresh = resp.body()?.data?.messages?.data?.reversed() ?: emptyList()
                    val currentIds = messages.map { it.id }.toSet()
                    val newOnes = fresh.filter { it.id !in currentIds }
                    if (newOnes.isNotEmpty()) {
                        messages = messages + newOnes
                        if (messages.isNotEmpty()) {
                            scope.launch { listState.scrollToItem(messages.size - 1) }
                        }
                    }
                    val currentMap = messages.associateBy { it.id }
                    val updated = fresh.filter { it.id in currentIds && it.text != currentMap[it.id]?.text }
                    if (updated.isNotEmpty()) {
                        messages = messages.map { existing ->
                            updated.find { it.id == existing.id } ?: existing
                        }
                    }
                    if (messages.none { it.text == "..." }) isWaitingForBot = false
                } else { pollFailCount++ }
            } catch (_: Exception) { pollFailCount++ }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank() || isSending) return
        isSending = true
        scope.launch {
            try {
                if (isBot) {
                    val resp = api.sendChatbotMessage(mapOf("message" to inputText))
                    if (resp.isSuccessful && resp.body()?.ok == true) {
                        val data = resp.body()?.data
                        inputText = ""
                        data?.userMessage?.let { um ->
                            messages = messages + MessageDto(
                                id = um.id, text = um.text, senderId = um.senderId,
                                receiverId = um.receiverId, isMine = true, createdAt = um.createdAt,
                                isSeen = null, price = null, attachments = null
                            )
                        }
                        data?.botMessage?.let { bm ->
                            messages = messages + MessageDto(
                                id = bm.id, text = bm.text, senderId = bm.senderId,
                                receiverId = bm.receiverId, isMine = false, createdAt = bm.createdAt,
                                isSeen = null, price = null, attachments = null
                            )
                            if (bm.text == "...") isWaitingForBot = true
                        }
                        scope.launch {
                            try { listState.scrollToItem(messages.size - 1) } catch (_: Exception) {}
                        }
                    }
                } else {
                    api.sendMessage(mapOf("receiver_id" to userId, "message" to inputText))
                    inputText = ""
                    loadMessages(page = 1, append = false)
                }
            } catch (_: Exception) {}
            isSending = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No messages yet. Send a message to start!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (messages.isEmpty() && isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        Column {
                            if (isBot && !msg.isMine) {
                                MarkdownBubble(markwon = markwon, text = msg.text ?: "", isMine = false)
                            } else {
                                MessageBubble(msg = msg, isMine = msg.isMine)
                            }
                            Text(
                                DateFormatter.formatRelativeTime(msg.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isWaitingForBot) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "Bot is thinking...",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
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
        CannedMessagesRow { text ->
            inputText = text
        }
        Spacer(Modifier.imePadding())
    }
}
}

@Composable
private fun CannedMessagesRow(onSelect: (String) -> Unit) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    if (imeBottom > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val messages = listOf("Hello!", "How are you?", "Sounds good", "Thanks!", "\uD83D\uDC4D")
            messages.forEach { msg ->
                SuggestionChip(
                    onClick = { onSelect(msg) },
                    label = { Text(msg) }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(msg: MessageDto, isMine: Boolean = msg.isMine) {
    val color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
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

@Composable
fun MarkdownBubble(markwon: Markwon, text: String, isMine: Boolean) {
    val color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = color
        ) {
            AndroidView(
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density
                    TextView(ctx).apply {
                        setTextColor(textColor.toArgb())
                        val hp = (14 * density).toInt()
                        val vp = (8 * density).toInt()
                        setPadding(hp, vp, hp, vp)
                        textSize = 14f
                    }
                },
                update = { tv ->
                    markwon.setMarkdown(tv, text)
                },
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }
    }
}
