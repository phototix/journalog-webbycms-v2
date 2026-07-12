package com.journalog.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.journalog.app.core.designsystem.StoryGradient
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.PostDto
import com.journalog.app.data.remote.dto.StoryGroupDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FeedScreen(
    onPostClick: (Int) -> Unit,
    onProfileClick: (String) -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var posts by remember { mutableStateOf<List<PostDto>>(emptyList()) }
    var stories by remember { mutableStateOf<List<StoryGroupDto>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                loadFeed(api, { posts = it }, { stories = it })
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(Unit) {
        loadFeed(api, { posts = it }, { stories = it })
        isInitialLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                StoriesRow(stories = stories, onProfileClick = onProfileClick)
            }

            if (isInitialLoading && posts.isEmpty()) {
                items(5) {
                    PostShimmer()
                }
            } else if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No posts yet. Follow some creators to see their content!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLike = {
                            scope.launch {
                                try { api.toggleLike(post.id) } catch (_: Exception) {}
                            }
                        },
                        onComment = { onPostClick(post.id) },
                        onProfileClick = { post.user?.let { onProfileClick(it.username) } }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private suspend fun loadFeed(
    api: ApiService,
    onPosts: (List<PostDto>) -> Unit,
    onStories: (List<StoryGroupDto>) -> Unit
) {
    try {
        val feedResp = api.getFeed()
        if (feedResp.isSuccessful) {
            onPosts(feedResp.body()?.data?.posts ?: emptyList())
        }
        val storiesResp = api.getStoriesFeed()
        if (storiesResp.isSuccessful) {
            val body = storiesResp.body()
            if (body?.ok == true && body.data != null) {
                val storiesArray = body.data["stories"]
                if (storiesArray is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val parsed = storiesArray.mapNotNull { item ->
                        if (item !is Map<*, *>) return@mapNotNull null
                        @Suppress("UNCHECKED_CAST")
                        val map = item as Map<String, Any?>
                        try {
                            val userId = (map["user_id"] as? Number)?.toInt()
                            if (userId == null) return@mapNotNull null
                            val itemsRaw = map["items"] as? List<*>
                            val firstItem = itemsRaw?.firstOrNull() as? Map<String, Any?>
                            val hasSeen = firstItem?.get("seen") as? Boolean ?: false
                            StoryGroupDto(
                                user = com.journalog.app.data.remote.dto.UserBriefDto(
                                    id = userId,
                                    name = map["name"] as? String ?: "",
                                    username = map["username"] as? String ?: "",
                                    avatar = map["photo"] as? String ?: ""
                                ),
                                hasUnseen = !hasSeen,
                                stories = itemsRaw?.mapNotNull { s ->
                                    if (s !is Map<*, *>) return@mapNotNull null
                                    @Suppress("UNCHECKED_CAST")
                                    val sm = s as Map<String, Any?>
                                    val timeSecs = sm["time"] as? Number
                                    val timeStr = if (timeSecs != null) {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        sdf.format(java.util.Date(timeSecs.toLong() * 1000))
                                    } else null
                                    com.journalog.app.data.remote.dto.StoryItemDto(
                                        id = sm["id"]?.toString() ?: "",
                                        type = sm["type"] as? String ?: "image",
                                        url = sm["src"] as? String ?: "",
                                        thumbnail = sm["preview"] as? String,
                                        createdAt = timeStr
                                    )
                                } ?: emptyList()
                            )
                        } catch (_: Exception) { null }
                    }
                    onStories(parsed)
                }
            }
        }
    } catch (_: Exception) { }
}

@Composable
fun StoriesRow(
    stories: List<StoryGroupDto>,
    onProfileClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (stories.isEmpty()) {
            items(3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StoryGradient)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Story",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            items(stories, key = { it.user.id }) { group ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onProfileClick(group.user.username) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                .background(if (group.hasUnseen) StoryGradient else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline)))
                .padding(2.dp)
                    ) {
                        AsyncImage(
                            model = group.user.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        group.user.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostDto,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.user?.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    post.user?.name ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            if (post.media.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.text ?: "", maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } else {
                AsyncImage(
                    model = post.media.first().url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        if (post.hasLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = "Like",
                        tint = if (post.hasLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onComment) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.Send, contentDescription = "Share")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmark")
                }
            }

            if (post.likesCount > 0) {
                Text(
                    "${post.likesCount} likes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            if (!post.text.isNullOrBlank()) {
                Text(
                    text = "${post.user?.name ?: ""} ${post.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            if (post.commentsCount > 0) {
                TextButton(
                    onClick = onComment,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        "View all ${post.commentsCount} comments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                post.createdAt ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun PostShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
