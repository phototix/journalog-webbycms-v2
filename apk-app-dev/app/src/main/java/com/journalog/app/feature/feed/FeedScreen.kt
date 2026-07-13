package com.journalog.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (Int) -> Unit,
    onProfileClick: (String) -> Unit,
    onStoryClick: (Int) -> Unit,
    onCreateStory: () -> Unit = {}
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var posts by remember { mutableStateOf<List<PostDto>>(emptyList()) }
    var stories by remember { mutableStateOf<List<StoryGroupDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val likedPosts = remember { mutableStateMapOf<Int, Boolean>() }
    val scope = rememberCoroutineScope()

    fun loadFeed() {
        scope.launch {
            isLoading = true
            try {
                val resp = api.getFeed()
                if (resp.isSuccessful) {
                    resp.body()?.data?.let { feedData ->
                        posts = feedData.posts ?: emptyList()
                        feedData.posts?.forEach { likedPosts[it.id] = it.hasLiked }
                    }
                }
            } catch (e: Throwable) {
                Log.e("Journalog-Feed", "loadFeed failed", e)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadFeed() }

    LaunchedEffect(Unit) {
        try {
            val resp = api.getStoriesFeed()
            if (resp.isSuccessful) {
                resp.body()?.let { body ->
                    if (body.ok && body.data != null) {
                        val list = body.data["stories"]
                        if (list is List<*>) {
                            val parsed = parseStories(list)
                            if (parsed.isNotEmpty()) stories = parsed
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("Journalog-Feed", "stories feed failed", e)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            StoriesRow(stories = stories, onCreateStory = onCreateStory, onStoryClick = onStoryClick)
        }

        if (isLoading && posts.isEmpty()) {
            items(5) { PostShimmer() }
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
                    hasLiked = likedPosts[post.id] ?: post.hasLiked,
                    onLike = {
                        val current = likedPosts[post.id] ?: post.hasLiked
                        likedPosts[post.id] = !current
                        scope.launch {
                            try {
                                api.toggleLike(post.id)
                            } catch (_: Exception) {
                                likedPosts[post.id] = current
                            }
                        }
                    },
                    onComment = { onPostClick(post.id) },
                    onProfileClick = { post.user?.let { onProfileClick(it.username) } }
                )
            }
        }
    }
}

fun parseStories(list: List<*>): List<StoryGroupDto> {
    val result = mutableListOf<StoryGroupDto>()
    for (item in list) {
        if (item !is Map<*, *>) continue
        @Suppress("UNCHECKED_CAST")
        val map = item as Map<String, Any?>
        try {
            val userId = (map["user_id"] as? Number)?.toInt() ?: continue
            val itemsRaw: List<*> = map["items"] as? List<*> ?: emptyList<Any>()
            val firstItem = itemsRaw.firstOrNull() as? Map<String, Any?>
            val seen = firstItem?.get("seen") as? Boolean ?: false

            val storyItems = itemsRaw.mapNotNull { s ->
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
                    text = sm["text"] as? String,
                    length = sm["length"] as? Int,
                    overlay = sm["overlay"] as? Map<String, Double>,
                    bgPreset = sm["bg_preset"] as? String,
                    createdAt = timeStr
                )
            }

            result.add(StoryGroupDto(
                user = com.journalog.app.data.remote.dto.UserBriefDto(
                    id = userId,
                    name = map["name"] as? String ?: "",
                    username = map["username"] as? String ?: "",
                    avatar = map["photo"] as? String ?: ""
                ),
                hasUnseen = !seen,
                stories = storyItems
            ))
        } catch (e: Exception) { Log.w("Journalog-Feed", "parseStories item skipped", e); continue }
    }
    return result
}

@Composable
fun StoriesRow(
    stories: List<StoryGroupDto>,
    onCreateStory: () -> Unit = {},
    onStoryClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create Story button — always first
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCreateStory() }
            ) {
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
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Create Story",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your Story", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        items(stories, key = { it.user.id }) { group ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(group.user.id) }
            ) {
                val ringBrush: Brush = if (group.hasUnseen) StoryGradient
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ringBrush)
                        .padding(2.dp)
                ) {
                    val avatarUrl = group.user.avatar ?: ""
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
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

@Composable
fun PostCard(
    post: PostDto,
    hasLiked: Boolean = post.hasLiked,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onProfileClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatar = post.user?.avatar ?: ""
                if (avatar.isNotBlank()) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.user?.name ?: "", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
            }

            if (post.media.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.text ?: "", maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            } else {
                val mediaUrl = post.media.firstOrNull()?.url
                if (!mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (hasLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = "Like",
                        tint = if (hasLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onComment) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            if (post.likesCount > 0) {
                Text("${post.likesCount + if (hasLiked != post.hasLiked) (if (hasLiked) 1 else -1) else 0} likes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
            if (!post.text.isNullOrBlank()) {
                Text("${post.user?.name ?: ""} ${post.text}", style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
            if (post.commentsCount > 0) {
                TextButton(onClick = onComment, modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("View all ${post.commentsCount} comments", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(post.createdAt ?: "", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun PostShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(100.dp).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}
