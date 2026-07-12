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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.MessageDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    userId: Int,
    userName: String,
    onBack: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun loadMessages() {
        scope.launch {
            try {
                val response = api.getMessages(userId)
                if (response.isSuccessful) {
                    messages = response.body()?.data?.get("messages") ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { loadMessages() }

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
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
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
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    scope.launch {
                                        try {
                                            api.sendMessage(mapOf("receiver_id" to userId, "message" to inputText))
                                            inputText = ""
                                            loadMessages()
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            scope.launch {
                                try {
                                    api.sendMessage(mapOf("receiver_id" to userId, "message" to inputText))
                                    inputText = ""
                                    loadMessages()
                                } catch (_: Exception) {}
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
        }
    }
}

@Composable
fun MessageBubble(msg: MessageDto) {
    val alignment = if (msg.isMine) Alignment.End else Alignment.Start
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
