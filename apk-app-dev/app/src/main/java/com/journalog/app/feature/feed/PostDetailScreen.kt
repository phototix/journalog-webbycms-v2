package com.journalog.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.journalog.app.core.common.DateFormatter
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.CommentDto
import com.journalog.app.data.remote.dto.PostDto
import com.journalog.app.feature.gifts.GiftModal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    onBack: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var post by remember { mutableStateOf<PostDto?>(null) }
    var comments by remember { mutableStateOf<List<CommentDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showGiftModal by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun submitComment() {
        if (commentText.isBlank()) return
        scope.launch {
            try {
                val resp = api.addComment(postId, mapOf("text" to commentText))
                if (resp.isSuccessful) {
                    commentText = ""
                    val commentsResp = api.getComments(postId)
                    if (commentsResp.isSuccessful) {
                        comments = commentsResp.body()?.data?.comments?.data ?: emptyList()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun loadComments() {
        scope.launch {
            try {
                val commentsResp = api.getComments(postId)
                android.util.Log.d("Journalog-Feed", "comments resp ok=${commentsResp.isSuccessful} code=${commentsResp.code()}")
                if (commentsResp.isSuccessful) {
                    val paginated = commentsResp.body()?.data?.comments
                    android.util.Log.d("Journalog-Feed", "comments paginated=${paginated != null} listSize=${paginated?.data?.size}")
                    comments = paginated?.data ?: emptyList()
                } else {
                    val errBody = commentsResp.errorBody()?.string()
                    android.util.Log.e("Journalog-Feed", "comments API error ${commentsResp.code()}: $errBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("Journalog-Feed", "comments exception", e)
            }
        }
    }

    LaunchedEffect(postId) {
        isLoading = true
        try {
            val resp = api.getPost(postId)
            if (resp.isSuccessful) {
                post = resp.body()?.data?.get("post")
            }
        } catch (_: Exception) {}
        loadComments()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    post?.let { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val avatar = p.user?.avatar ?: ""
                            if (avatar.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = avatar,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Text(p.user?.name ?: "")
                        }
                    } ?: Text("Post")
                },
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
                if (isLoading && post == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (post == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Post not found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                post?.let { p ->
                    item {
                        PostCard(
                            post = p,
                            onLike = {
                                scope.launch { try { api.toggleLike(p.id) } catch (_: Exception) {} }
                            },
                            onComment = { },
                            onProfileClick = { },
                            hideCommentButton = true,
                            hideUserRow = true,
                            onGiftClick = { showGiftModal = true }
                        )
                    }

                    // Poll display
                    if (p.poll != null && p.poll.answers != null) {
                        item {
                            PollDisplay(poll = p.poll, postId = p.id, api = api)
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                items(comments) { comment ->
                    CommentItem(comment)
                }
                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
        CannedMessagesRow { text ->
            commentText = text
        }
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { submitComment() }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { submitComment() }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Spacer(Modifier.imePadding())
    }

    // Gift Modal
    if (showGiftModal && post != null) {
        GiftModal(
            postId = post!!.id,
            onDismiss = { showGiftModal = false },
            onGiftSent = { }
        )
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
fun PollDisplay(
    poll: com.journalog.app.data.remote.dto.PollDto,
    postId: Int,
    api: ApiService
) {
    val scope = rememberCoroutineScope()
    var votedAnswerId by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Poll", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            poll.answers?.forEach { answer ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(enabled = votedAnswerId == null) {
                            votedAnswerId = answer.id
                            scope.launch {
                                try { api.votePoll(postId, mapOf("poll_answer_id" to answer.id)) } catch (_: Exception) {}
                            }
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (votedAnswerId == answer.id) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            answer.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (votedAnswerId != null) {
                            Text(
                                "${answer.percentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (votedAnswerId != null) {
                Text(
                    "${poll.totalVotes} votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CommentItem(comment: CommentDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        AsyncImage(
            model = comment.user.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "${comment.user.name}  ${comment.text}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                DateFormatter.formatRelativeTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
