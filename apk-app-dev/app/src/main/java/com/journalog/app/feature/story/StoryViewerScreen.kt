package com.journalog.app.feature.story

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.StoryGroupDto
import com.journalog.app.feature.feed.parseStories
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    userId: Int,
    onBack: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var groups by remember { mutableStateOf<List<StoryGroupDto>>(emptyList()) }
    var currentGroupIndex by remember { mutableStateOf(0) }
    var currentItemIndex by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(userId) {
        try {
            val resp = api.getStoriesFeed()
            if (resp.isSuccessful && resp.body()?.ok == true) {
                val list = resp.body()?.data?.get("stories")
                if (list is List<*>) {
                    groups = parseStories(list)
                }
            }
        } catch (_: Exception) {}
    }

    val currentGroup = groups.getOrNull(currentGroupIndex)
    val currentItem = currentGroup?.stories?.getOrNull(currentItemIndex)

    if (currentGroup == null || currentItem == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(dragOffset) > 200) {
                            onBack()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        // Background image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(currentItem.url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Progress bars
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // Progress indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentGroup.stories.forEachIndexed { index, story ->
                    val progress = if (index < currentItemIndex) 1f
                    else if (index == currentItemIndex) currentProgress
                    else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            // Top bar with user info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentGroup.user.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        currentGroup.user.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    currentItem.createdAt?.let {
                        Text(
                            it,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) {
                    Text("✕", color = Color.White, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Text overlay
            currentItem.text?.let { text ->
                if (text.isNotBlank()) {
                    Text(
                        text,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tap zones for navigation
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentItemIndex > 0) {
                            currentItemIndex--
                            currentProgress = 0f
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .clickable { isPaused = !isPaused }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentItemIndex + 1 < currentGroup.stories.size) {
                            currentItemIndex++
                            currentProgress = 0f
                        } else if (currentGroupIndex + 1 < groups.size) {
                            currentGroupIndex++
                            currentItemIndex = 0
                            currentProgress = 0f
                        } else {
                            onBack()
                        }
                    }
            )
        }
    }

    // Auto-advance timer
    LaunchedEffect(currentGroupIndex, currentItemIndex, isPaused) {
        if (isPaused) return@LaunchedEffect
        val duration = (currentItem.length ?: 5) * 1000L
        val steps = 50
        val stepMs = duration / steps
        currentProgress = 0f
        for (i in 1..steps) {
            delay(stepMs)
            currentProgress = i.toFloat() / steps
        }
        // Advance to next item
        if (currentItemIndex + 1 < currentGroup.stories.size) {
            currentItemIndex++
        } else if (currentGroupIndex + 1 < groups.size) {
            currentGroupIndex++
            currentItemIndex = 0
        } else {
            onBack()
        }
    }
}
