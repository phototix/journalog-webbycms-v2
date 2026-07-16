package com.journalog.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.journalog.app.data.remote.dto.PostDto
import com.journalog.app.data.remote.dto.UserDto
import com.journalog.app.data.remote.dto.SubscribeRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    username: String,
    currentUsername: String? = null,
    refreshTrigger: Int = 0,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPostClick: (Int) -> Unit,
    onSubscribeClick: ((UserDto) -> Unit)? = null
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var user by remember { mutableStateOf<UserDto?>(null) }
    var posts by remember { mutableStateOf<List<PostDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }

    fun loadPosts(page: Int = 1) {
        scope.launch {
            if (page == 1) isLoading = true else isLoadingMore = true
            try {
                val resp = api.getUserPosts(username, page)
                if (resp.isSuccessful) {
                    val data = resp.body()?.data
                    val newPosts = data?.posts ?: emptyList()
                    posts = if (page == 1) newPosts else posts + newPosts
                    hasMore = data?.hasMore ?: false
                    currentPage = page
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load posts"
            }
            isLoading = false
            isLoadingMore = false
            loadAttempt++
        }
    }

    fun loadProfile() {
        scope.launch {
            try {
                val userResp = api.getProfile(username)
                if (userResp.isSuccessful) {
                    user = userResp.body()?.data?.get("user")
                } else {
                    error = "User not found"
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load profile"
            }
        }
    }

    LaunchedEffect(username, refreshTrigger) {
        loadProfile()
        loadPosts()
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = "More")
                }
            }
        }

        if (isLoading) {
            item { ProfileShimmer() }
        } else if (error != null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error ?: "Error", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        user?.let { u ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = u.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("${u.postsCount ?: 0}", "Posts")
                        StatItem("${u.subscribersCount ?: 0}", "Followers")
                        StatItem("${u.giftsReceivedCount ?: 0}", "Gifts")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(u.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (u.isVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("@${u.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    u.bio?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isOwnProfile = currentUsername == username
                if (!isOwnProfile) {
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    if (u.paidProfile && u.openProfile != true) {
                        Box {
                            Button(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(36.dp),
                                shape = CircleShape
                            ) {
                                Text(
                                    if (u.hasSubscribed == true) "Subscribed"
                                    else "Subscribe \$${u.profileAccessPrice}/mo"
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                if (u.hasSubscribed == true) {
                                    DropdownMenuItem(
                                        text = { Text("Unsubscribe") },
                                        onClick = {
                                            dropdownExpanded = false
                                            scope.launch {
                                                try { api.cancelSubscription(mapOf("creator_user_id" to u.id)) } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Subscribe - \$${u.profileAccessPrice}/mo") },
                                        onClick = {
                                            dropdownExpanded = false
                                            onSubscribeClick?.invoke(u)
                                        }
                                    )
                                }
                                if (u.isFollowing == true) {
                                    DropdownMenuItem(
                                        text = { Text("Unfollow") },
                                        onClick = {
                                            dropdownExpanded = false
                                            scope.launch { try { api.toggleFollow(username) } catch (_: Exception) {} }
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Follow") },
                                        onClick = {
                                            dropdownExpanded = false
                                            scope.launch { try { api.toggleFollow(username) } catch (_: Exception) {} }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch { try { api.toggleFollow(username) } catch (_: Exception) {} }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(36.dp),
                            shape = CircleShape
                        ) {
                            Text(if (u.isFollowing == true) "Following" else "Follow")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Public", "Subscribers", "Paid", "Media")
                    filters.forEachIndexed { i, label ->
                        FilterChip(
                            selected = selectedFilter == i,
                            onClick = { selectedFilter = i },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (!isLoading && user != null && posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No posts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (!isLoading && user != null && posts.isNotEmpty()) {
            val filteredPosts = when (selectedFilter) {
                0 -> posts
                1 -> posts.filter { it.price == 0.0 }
                2 -> posts.filter { it.price == 0.0 && user?.paidProfile == true }
                3 -> posts.filter { it.price > 0 }
                4 -> posts.filter { !it.media.isNullOrEmpty() }
                else -> posts
            }

            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No posts match this filter",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val chunked = filteredPosts.chunked(3)
                items(chunked) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rowItems.forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onPostClick(post.id) }
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                val mediaUrl = post.media?.firstOrNull()?.let { it.thumbnail ?: it.url }
                                if (mediaUrl != null) {
                                    AsyncImage(
                                        model = mediaUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val firstChar = post.text?.firstOrNull()?.uppercase() ?: "?"
                                    Text(
                                        firstChar,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (post.price > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.Lock, contentDescription = "Locked",
                                                tint = Color.White, modifier = Modifier.size(20.dp))
                                            Text("\$${String.format("%.2f", post.price)}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }

        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        } else if (hasMore && !isLoading && posts.isNotEmpty()) {
            item {
                Button(
                    onClick = { loadPosts(currentPage + 1) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ExpandMore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load More")
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileShimmer() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(modifier = Modifier.width(24.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(24.dp, 16.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(40.dp, 12.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
